package com.aladin.android.data.vizier

import android.content.Context
import com.aladin.android.data.net.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Request
import java.io.File
import java.net.URLEncoder

data class CatalogDownloadState(
    val isRunning: Boolean = false,
    val catalogName: String = "",
    val bytesDownloaded: Long = 0L,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class CatalogDownloader(private val context: Context) {
    private val _state = MutableStateFlow(CatalogDownloadState())
    val state: StateFlow<CatalogDownloadState> = _state.asStateFlow()

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startVizieRDownload(catalogId: String, raDeg: Double, decDeg: Double, radiusArcmin: Double) {
        if (downloadJob?.isActive == true) return

        val safeCatalogId = catalogId.replace(Regex("[^a-zA-Z0-9]"), "_")
        val baseDir = File(context.getExternalFilesDir(null), "Catalog_Downloads")
        baseDir.mkdirs()
        val outputFile = File(baseDir, "${safeCatalogId}_${raDeg}_${decDeg}_${radiusArcmin}.xml")

        downloadJob = scope.launch {
            _state.value = CatalogDownloadState(isRunning = true, catalogName = catalogId)
            try {
                val c = URLEncoder.encode(String.format(java.util.Locale.US, "%.6f %+.6f", raDeg, decDeg), "UTF-8")
                val url = "https://vizier.cds.unistra.fr/viz-bin/votable" +
                        "?-source=$catalogId" +
                        "&-c=$c" +
                        "&-c.rm=$radiusArcmin" +
                        "&-out.add=_RAJ,_DEJ" +
                        "&-out.max=unlimited" +
                        "&-out.meta=DhuL" +
                        "&-oc.form=dm"

                downloadFile(url, outputFile)
                _state.update { it.copy(isRunning = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isRunning = false, errorMessage = e.message) }
            }
        }
    }

    fun startSimbadDownload(raDeg: Double, decDeg: Double, radiusArcmin: Double) {
        if (downloadJob?.isActive == true) return

        val baseDir = File(context.getExternalFilesDir(null), "Catalog_Downloads")
        baseDir.mkdirs()
        val outputFile = File(baseDir, "Simbad_${raDeg}_${decDeg}_${radiusArcmin}.xml")

        downloadJob = scope.launch {
            _state.value = CatalogDownloadState(isRunning = true, catalogName = "Simbad")
            try {
                val q = String.format(java.util.Locale.US, "%.6f %+.6f", raDeg, decDeg)
                val ident = URLEncoder.encode(q, "UTF-8")
                val columns = "main_id,otype,ra,dec,flux(V),sp"
                val url = "https://simbad.u-strasbg.fr/simbad/sim-all?Ident=$ident" +
                        "&Radius=$radiusArcmin&Radius.unit=arcmin&Votable=$columns"

                downloadFile(url, outputFile)
                _state.update { it.copy(isRunning = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isRunning = false, errorMessage = e.message) }
            }
        }
    }

    private fun downloadFile(url: String, outputFile: File) {
        val req = Request.Builder().url(url).build()
        HttpClient.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("HTTP Error ${resp.code}")
            }
            val body = resp.body ?: throw Exception("Empty response body")
            outputFile.outputStream().use { out ->
                val buf = ByteArray(8192)
                var len: Int
                val inStream = body.byteStream()
                while (inStream.read(buf).also { len = it } != -1) {
                    out.write(buf, 0, len)
                    _state.update {
                        it.copy(bytesDownloaded = it.bytesDownloaded + len)
                    }
                }
            }
        }
    }

    fun stopDownload() {
        downloadJob?.cancel()
        _state.update { it.copy(isRunning = false) }
    }
}
