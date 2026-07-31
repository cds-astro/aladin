package com.aladin.android.data.hips

import android.content.Context
import com.aladin.android.data.net.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

data class DownloadState(
    val isRunning: Boolean = false,
    val datasetName: String = "",
    val tilesScanned: Int = 0,
    val tilesDownloaded: Int = 0,
    val bytesDownloaded: Long = 0L,
    val activeThreads: Int = 0,
    val totalTilesQueued: Int = 0
)

class HiPSDownloader(private val context: Context) {
    private val _states = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()
    private val jobs = ConcurrentHashMap<String, Job>()

    fun getState(surveyId: String): StateFlow<DownloadState> {
        return _states.getOrPut(surveyId) { MutableStateFlow(DownloadState()) }.asStateFlow()
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDownload(survey: HiPSSurvey, maxOrder: Int, format: String = "jpg", fastCheck: Boolean = true, maxThreads: Int = 8, customPath: String? = null) {
        if (jobs[survey.id]?.isActive == true) return

        val datasetName = survey.id.replace(Regex("[^a-zA-Z0-9]"), "_")
        val baseDir = if (customPath.isNullOrBlank()) {
            File(context.getExternalFilesDir(null), "HiPS_Downloads/$datasetName")
        } else {
            File(customPath)
        }
        baseDir.mkdirs()

        val _state = _states.getOrPut(survey.id) { MutableStateFlow(DownloadState()) }

        jobs[survey.id] = scope.launch {
            _state.value = DownloadState(isRunning = true, datasetName = survey.title)
            
            val propertiesFile = File(baseDir, "properties")
            if (!propertiesFile.exists()) {
                val req = Request.Builder().url(survey.propertiesUrl()).build()
                try {
                    HttpClient.client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.let { body ->
                                propertiesFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
            
            val semaphore = Semaphore(maxThreads)

            data class TileTask(val order: Int, val npix: Long)
            val queue = ConcurrentLinkedQueue<TileTask>()
            
            // Queue base tiles (order 0: 0 to 11)
            for (i in 0L..11L) {
                queue.add(TileTask(0, i))
            }
            _state.update { it.copy(totalTilesQueued = queue.size) }

            val activeTasksCount = AtomicInteger(0)

            suspend fun processTile(task: TileTask) {
                activeTasksCount.incrementAndGet()
                _state.update { it.copy(activeThreads = activeTasksCount.get()) }

                try {
                    val url = survey.tileUrl(task.order, task.npix, format)
                    val dir = (task.npix / 10000L) * 10000L
                    
                    val fileDir = File(baseDir, "Norder${task.order}/Dir$dir")
                    val file = File(fileDir, "Npix${task.npix}.$format")

                    var success = false

                    if (fastCheck && file.exists() && file.length() > 0) {
                        success = true
                        _state.update { it.copy(tilesDownloaded = it.tilesDownloaded + 1) }
                    } else {
                        val req = Request.Builder().url(url).build()
                        try {
                            HttpClient.client.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val body = resp.body
                                    if (body != null) {
                                        fileDir.mkdirs()
                                        val tempFile = File(fileDir, "Npix${task.npix}.tmp")
                                        tempFile.outputStream().use { out ->
                                            body.byteStream().copyTo(out)
                                        }
                                        tempFile.renameTo(file)
                                        success = true
                                        _state.update {
                                            it.copy(
                                                tilesDownloaded = it.tilesDownloaded + 1,
                                                bytesDownloaded = it.bytesDownloaded + file.length()
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignored: network failure, could log if needed
                        }
                    }

                    _state.update { it.copy(tilesScanned = it.tilesScanned + 1) }

                    if (success && task.order < maxOrder) {
                        val childrenOrder = task.order + 1
                        val firstChild = task.npix * 4
                        for (i in 0L..3L) {
                            queue.add(TileTask(childrenOrder, firstChild + i))
                        }
                        _state.update { it.copy(totalTilesQueued = it.totalTilesQueued + 4) }
                    }
                } finally {
                    activeTasksCount.decrementAndGet()
                    _state.update { it.copy(activeThreads = activeTasksCount.get()) }
                }
            }

            val workers = (1..maxThreads).map {
                launch {
                    while (isActive) {
                        val task = queue.poll()
                        if (task != null) {
                            semaphore.withPermit {
                                processTile(task)
                            }
                        } else {
                            if (activeTasksCount.get() == 0) {
                                break
                            } else {
                                delay(50)
                            }
                        }
                    }
                }
            }
            workers.forEach { it.join() }

            _state.update { it.copy(isRunning = false, activeThreads = 0) }
        }
    }

    fun stopDownload(surveyId: String) {
        jobs[surveyId]?.cancel()
        _states[surveyId]?.update { it.copy(isRunning = false, activeThreads = 0) }
    }
}
