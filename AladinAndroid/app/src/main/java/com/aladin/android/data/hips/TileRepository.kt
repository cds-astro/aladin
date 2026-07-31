package com.aladin.android.data.hips

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.BufferedInputStream
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * In-memory LRU cache with size limit in `Long` bytes. `android.util.LruCache`
 * uses an `Int` for max size: `mb * 1024 * 1024` overflows an Int starting at
 * ~2048 MB (2048*1024*1024 == Int.MIN_VALUE) and the cache gets a negative maxSize,
 * which causes an immediate crash when inserting the first tile. Since there is no
 * upper limit in the UI (the user can enter any MB figure), we need Long arithmetic
 * end-to-end.
 */
private class LongLruCache<K, V>(private val maxBytes: Long, private val sizeOf: (V) -> Long) {
    private val map = LinkedHashMap<K, V>(16, 0.75f, true)
    private var currentBytes = 0L

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map.remove(key)?.let { currentBytes -= sizeOf(it) }
        map[key] = value
        currentBytes += sizeOf(value)
        trim()
    }

    private fun trim() {
        val it = map.entries.iterator()
        while (currentBytes > maxBytes && it.hasNext()) {
            val e = it.next()
            currentBytes -= sizeOf(e.value)
            it.remove()
        }
    }

    @Synchronized
    fun evictAll() {
        map.clear()
        currentBytes = 0L
    }
}

/**
 * Loads and caches HiPS tiles.
 *
 * Anti-OOM measures:
 * - BOUNDED and fixed in-memory cache (24 MB), independent of largeHeap.
 * - Downsampling via `inSampleSize` based on on-screen tile size: a
 *   tile viewed at 140 px is decoded to 128x128 (32 KB) instead of 512x512
 *   (512 KB) -> up to 16x less memory.
 * - Reuse of recycled bitmaps via `inBitmap` (without allocating new ones).
 * - Stream decoding (without materializing the body into a ByteArray) and in
 *   RGB_565 (HiPS tiles are opaque): half the memory of ARGB_8888.
 * - Limited concurrency (semaphore) to avoid decoding many tiles at once.
 * - Bitmap recycling when evicted from cache (frees native memory).
 * - Coalesced invalidation: a burst of tiles produces a single UI notification.
 */
class TileRepository(
    context: Context,
    memCacheMb: Int = 48,
    concurrency: Int = 12,
) {

    private val cacheDir = File(context.cacheDir, "hips_tiles").apply { mkdirs() }
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(cacheDir, "net"), DISK_CACHE_BYTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    // Fetch/decode concurrency. The CDS server tolerates ~12 parallel connections
    // without rate-limit (tested: 12 tiles in parallel all 200 in ~4s).
    // With 12 slots, the coarse base (few tiles) arrives in the first batch and
    // fine refinement completes much earlier than with 4-8 slots. Adjustable
    // from Settings (see setConcurrency).
    private var permits = Semaphore(concurrency.coerceIn(2, 24))

    // In-memory cache adjustable from Settings (see setMemCacheMb). It is `var`
    // to allow hot resizing: we clear the old one and create a new one with the
    // requested size. We DO NOT recycle on eviction (a bitmap used by draw on
    // another thread would cause flickering / native crash). No upper limit:
    // the user can enter any MB figure (see LongLruCache, which uses Long to
    // avoid Int overflow that crashed above ~2048 MB).
    private var memCacheMbCurrent = memCacheMb.coerceAtLeast(8)
    private var memCache: LongLruCache<String, Bitmap> = newMemCache(memCacheMbCurrent)

    private fun newMemCache(mb: Int) =
        LongLruCache<String, Bitmap>(mb.toLong().coerceAtLeast(8L) * 1024L * 1024L) { it.byteCount.toLong() }

    /** Resizes the in-memory cache (clears the current one and creates a new one). */
    fun setMemCacheMb(mb: Int) {
        val clamped = mb.coerceAtLeast(8)
        if (clamped == memCacheMbCurrent) return
        synchronized(scope) {
            memCache.evictAll()
            memCache = newMemCache(clamped)
            memCacheMbCurrent = clamped
        }
    }

    /** Changes download concurrency (takes effect on subsequent fetches). */
    fun setConcurrency(n: Int) {
        permits = Semaphore(n.coerceIn(2, 24))
    }

    /** Clears in-memory and disk caches. */
    fun clearAll() {
        memCache.evictAll()
        try { client.cache?.delete() } catch (_: Exception) {}
    }

    /** Coalesced callback to the main thread when new tiles arrive. */
    var onTilesChanged: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val invalidateRunnable = Runnable {
        pendingInvalidate = false
        onTilesChanged?.invoke()
    }
    @Volatile private var pendingInvalidate = false

    private fun scheduleInvalidate() {
        if (pendingInvalidate) return
        pendingInvalidate = true
        mainHandler.postDelayed(invalidateRunnable, 80)
    }

    /** Number of tiles currently pending download (for the loading indicator). */
    val activeDownloads: Int get() = inFlight.size

    fun get(surveyId: String, order: Int, npix: Long, sampleSize: Int): Bitmap? =
        memCache.get(key(surveyId, order, npix, sampleSize))

    /**
     * `scalePx` = screen pixels per native tile pixel. Used to decide the
     * `inSampleSize`: lower zoom means more downsampling -> less memory.
     */
    fun request(survey: HiPSSurvey, ext: String, order: Int, npix: Long, scalePx: Float) {
        val sample = sampleSizeFor(scalePx)
        val k = key(survey.id, order, npix, sample)
        if (memCache.get(k) != null) return
        if (!inFlight.add(k)) return
        // We notify when QUEUING (not just on completion) so the loading indicator
        // appears as soon as tiles are requested, not ~80ms+fetch later.
        scheduleInvalidate()
        val url = survey.tileUrl(order, npix, ext)
        scope.launch {
            val bmp = permits.withPermit { fetch(url, sample) }
            if (bmp != null) memCache.put(k, bmp)
            inFlight.remove(k)
            scheduleInvalidate()
        }
    }

    private suspend fun fetch(url: String, sampleSize: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inMutable = false
                inSampleSize = sampleSize
            }
            if (url.startsWith("file://")) {
                val file = java.io.File(java.net.URI(url))
                if (file.exists()) {
                    return@withContext BitmapFactory.decodeFile(file.absolutePath, opts)
                }
                return@withContext null
            }
            val req = okhttp3.Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                BufferedInputStream(body.byteStream(), 32 * 1024).use { ins ->
                    BitmapFactory.decodeStream(ins, null, opts)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Cancels all ongoing work (call when destroying the ViewModel). */
    fun shutdown() {
        scope.cancel()
        memCache.evictAll()
    }

    // WARNING: the key includes the survey id. Without it, changing surveys makes
    // the NEW survey tiles (same order/npix) cache-hit the bitmaps of the OLD
    // survey and endlessly display old images (never re-requested). With the id,
    // each survey has its own key space.
    private fun key(surveyId: String, order: Int, npix: Long, sample: Int) =
        "$surveyId/$order/$npix/$sample"

    companion object {
        /** Disk cache (fixed; cannot be hot-resized). */
        private const val DISK_CACHE_BYTES = 64L * 1024 * 1024

        /**
         * Downsampling based on on-screen tile size.
         * scalePx >= 0.5  -> tile >= 256 px -> sample 1 (full res)
         * scalePx >= 0.25 -> tile >= 128 px -> sample 2
         * smaller        -> sample 4 (enough for an overview)
         */
        fun sampleSizeFor(scalePx: Float): Int = when {
            scalePx >= 0.5f -> 1
            scalePx >= 0.25f -> 2
            else -> 4
        }
    }
}