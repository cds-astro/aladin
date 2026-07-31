package com.aladin.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aladin.android.data.hips.HiPSProperties
import com.aladin.android.data.hips.HiPSPropertiesParser
import com.aladin.android.data.hips.HiPSSurvey
import com.aladin.android.data.hips.SurveyRegistry
import com.aladin.android.data.hips.TileRepository
import com.aladin.android.data.simbad.SimbadObject
import com.aladin.android.data.simbad.SimbadService
import com.aladin.android.data.vizier.VizieRService
import com.aladin.android.model.CatalogRow
import com.aladin.android.model.Marker
import com.aladin.android.model.Measurement
import com.aladin.android.model.SkyPosition
import com.aladin.android.sky.SkyCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SkyUiState(
    val camera: SkyCamera = SkyCamera(),
    val survey: HiPSSurvey = SurveyRegistry.builtIn.first(),
    val availableSurveys: List<HiPSSurvey> = SurveyRegistry.builtIn,
    val surveysLoading: Boolean = false,
    val props: HiPSProperties? = null,
    val propsLoading: Boolean = true,
    val catalogRows: List<CatalogRow> = emptyList(),
    val catalogLoading: Boolean = false,
    val markers: List<Marker> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val selected: SimbadObject? = null,
    val identifying: Boolean = false,
    val searching: Boolean = false,
    val message: String? = null,
    val tileVersion: Int = 0,
    val measureMode: Boolean = false,
    val pendingMeasureFrom: SkyPosition? = null,
    val settings: AppSettings = AppSettings(),
    val downloadingTiles: Int = 0,
    val catalogVisible: Boolean = true,
    val activeCatalogId: String? = null,
)

class SkyViewModel(app: Application) : AndroidViewModel(app) {
    private val simbad = SimbadService()
    private val vizier = VizieRService()
    // Persistent settings (cache, quality, concurrency). Loaded before
    // building the TileRepository so that the memory cache and concurrency
    // start with the saved values.
    private var settings = AppSettings.load(app)
    val tileRepo = TileRepository(app, settings.memCacheMb, settings.concurrency)
    val downloader = com.aladin.android.data.hips.HiPSDownloader(app)
    val catalogDownloader = com.aladin.android.data.vizier.CatalogDownloader(app)

    private val _state = MutableStateFlow(SkyUiState())
    val state: StateFlow<SkyUiState> = _state.asStateFlow()

    init {
        // The tile repository notifies (coalesced) when new bitmaps arrive
        // -> we increment tileVersion to force a Canvas redraw. The same
        // notification triggers when tiles are QUEUED (see TileRepository.request), so
        // we take advantage of it to read activeDownloads and feed the loading
        // indicator: a separate flow is not needed, it already coalesces and synchronizes itself.
        tileRepo.onTilesChanged = {
            _state.update { it.copy(tileVersion = it.tileVersion + 1, downloadingTiles = tileRepo.activeDownloads) }
        }
        _state.update { it.copy(settings = settings) }
        loadSurveyProperties(_state.value.survey)
        fetchSurveys()
    }

    fun fetchSurveys() {
        _state.update { it.copy(surveysLoading = true) }
        viewModelScope.launch {
            val localDirs = mutableListOf<java.io.File>()
            localDirs.add(java.io.File(app.getExternalFilesDir(null), "HiPS_Downloads"))
            settings.customHiPSDirs.forEach { localDirs.add(java.io.File(it)) }
            
            val list = com.aladin.android.data.hips.HiPSListService.fetchAvailableSurveys(localDirs)
            
            _state.update { 
                // If offline and properties failed, auto-select local
                var surveyToSet = it.survey
                var loading = it.propsLoading
                var msg = it.message
                if (it.props == null && !it.propsLoading) {
                    val firstLocal = list.firstOrNull { s -> s.id.endsWith("_local") }
                    if (firstLocal != null && it.survey.id != firstLocal.id) {
                        surveyToSet = firstLocal
                        loading = true
                        msg = "Offline: Using local dataset"
                    }
                }
                it.copy(availableSurveys = list, surveysLoading = false, survey = surveyToSet, propsLoading = loading, message = msg)
            }
            
            if (_state.value.propsLoading && _state.value.props == null) {
                // We auto-switched to a local survey, so load it
                loadSurveyProperties(_state.value.survey)
            }
        }
    }

    // --- Settings ---
    /** Applies new settings: persists, reconfigures the repo and recalculates visible
     *  tiles (the quality `targetScale` changes tile density). */
    fun setSettings(s: AppSettings) {
        settings = s
        AppSettings.save(app, s)
        tileRepo.setMemCacheMb(s.memCacheMb)
        tileRepo.setConcurrency(s.concurrency)
        // Trigger recalculation: settings changed -> the collect re-emits and
        // computeVisibleTiles uses the new targetScale. We force an emission
        // by touching tileVersion as well in case the camera did not change.
        _state.update { it.copy(settings = s, tileVersion = it.tileVersion + 1) }
    }

    /** Clears memory and disk cache. */
    fun clearCache() {
        tileRepo.clearAll()
        _state.update { it.copy(tileVersion = it.tileVersion + 1) }
    }

    fun addCustomHiPSDir(path: String) {
        val newDirs = settings.customHiPSDirs + path
        setSettings(settings.copy(customHiPSDirs = newDirs))
        fetchSurveys()
    }

    fun setSurvey(survey: HiPSSurvey) {
        // DO NOT clear props or survey here. If we did, the screen would
        // be left without tiles during the `properties` fetch (~3s on the CDS
        // server) —or forever if the fetch fails—. Instead, we mark loading
        // and leave the old survey visible until the new `properties` arrive.
        // Then we swap survey+props ATOMICALLY (see loadSurveyProperties):
        // this way the fetch of new tiles never uses the new survey with old props
        // (mixing baseUrls/orders would result in wrong tiles).
        if (survey.id == _state.value.survey.id) return
        _state.update { it.copy(propsLoading = true, message = null) }
        loadSurveyProperties(survey)
    }

    private fun loadSurveyProperties(survey: HiPSSurvey) {
        viewModelScope.launch {
            val props = fetchProperties(survey)
            _state.update {
                if (props != null) {
                    // Atomic swap of survey+props+camera. The old survey remains
                    // visible until exactly here; from here on the draw and fetch
                    // use the new survey (and its new cache key, see TileRepository).
                    it.copy(
                        survey = survey,
                        props = props,
                        propsLoading = false,
                        catalogRows = emptyList(),
                        camera = SkyCamera(
                            center = SkyPosition(props.initialRa, props.initialDec),
                            fovDeg = props.initialFov.coerceIn(SkyCamera.MIN_FOV, MAX_INITIAL_FOV),
                        ),
                    )
                } else {
                    // Failed fetch
                    val firstLocal = it.availableSurveys.firstOrNull { s -> s.id.endsWith("_local") }
                    if (firstLocal != null && survey.id != firstLocal.id) {
                        // Switch to local dataset
                        it.copy(propsLoading = false, message = "Offline: Switching to local dataset")
                    } else {
                        it.copy(propsLoading = false, message = "Could not load survey")
                    }
                }
            }
            
            if (!_state.value.propsLoading && _state.value.props == null && _state.value.message == "Offline: Switching to local dataset") {
                val local = _state.value.availableSurveys.firstOrNull { s -> s.id.endsWith("_local") }
                if (local != null) setSurvey(local)
            }
        }
    }

    private suspend fun fetchProperties(survey: HiPSSurvey): HiPSProperties? =
        withContext(Dispatchers.IO) {
            try {
                val url = survey.propertiesUrl()
                val text = if (url.startsWith("file://")) {
                    val file = java.io.File(java.net.URI(url))
                    if (file.exists()) file.readText() else return@withContext null
                } else {
                    val r = com.aladin.android.data.net.HttpClient.client
                        .newCall(okhttp3.Request.Builder().url(url).build()).execute()
                    r.use { resp ->
                        if (!resp.isSuccessful) return@withContext null
                        resp.body?.string() ?: return@withContext null
                    }
                }
                HiPSPropertiesParser.parse(survey.baseUrl, text)
            } catch (e: Exception) { null }
        }

    // --- Camera ---
    fun panBy(dx: Float, dy: Float, w: Float, h: Float) {
        _state.update { it.copy(camera = it.camera.panBy(dx, dy, w, h)) }
    }

    fun zoomBy(factor: Float, w: Float, h: Float, fx: Float, fy: Float) {
        _state.update { it.copy(camera = it.camera.zoom(factor, w, h, fx, fy)) }
    }

    fun recenter(pos: SkyPosition) {
        _state.update { it.copy(camera = it.camera.recenterTo(pos)) }
    }

    // --- Simbad Search ---
    fun searchObject(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(searching = true, message = null) }
        viewModelScope.launch {
            val results = simbad.search(query, radiusArcmin = 30.0)
            val first = results.firstOrNull()
            _state.update {
                it.copy(
                    searching = false,
                    selected = first,
                    message = if (first == null) app.getString(R.string.no_results) else null,
                    camera = if (first != null) it.camera.recenterTo(first.position) else it.camera,
                )
            }
        }
    }

    fun identifyAt(raDeg: Double, decDeg: Double) {
        _state.update { it.copy(identifying = true, message = null) }
        viewModelScope.launch {
            val obj = simbad.coneSearch(raDeg, decDeg, radiusArcmin = 0.5)
            _state.update { it.copy(identifying = false, selected = obj) }
        }
    }

    fun clearSelected() = _state.update { it.copy(selected = null) }

    // --- VizieR Catalogs ---
    fun longPressAt(pos: SkyPosition) {
        if (!_state.value.catalogVisible) return
        val rows = _state.value.catalogRows
        if (rows.isEmpty()) return
        val closest = rows.minByOrNull { SkyPosition.separation(pos, SkyPosition(it.raDeg, it.decDeg)) }
        if (closest != null) {
            val dist = SkyPosition.separation(pos, SkyPosition(closest.raDeg, closest.decDeg))
            if (dist < 0.5) {
                val obj = com.aladin.android.data.simbad.SimbadObject(
                    mainId = closest.id,
                    otype = closest.columns["otype"] ?: "Catalog Source",
                    position = SkyPosition(closest.raDeg, closest.decDeg),
                    magV = closest.mag,
                    raw = closest.columns
                )
                _state.update { it.copy(selected = obj) }
            }
        }
    }

    fun loadCatalog(catalogId: String, maxRows: Int = 500) {
        val s = _state.value
        val radius = (s.camera.fovDeg / 2.0).coerceIn(0.05, 60.0) * 60.0 // arcmin
        _state.update { it.copy(catalogLoading = true, message = null, activeCatalogId = catalogId) }
        viewModelScope.launch {
            try {
                val rows = if (catalogId == "SIMBAD") {
                    val simbad = com.aladin.android.data.simbad.SimbadService()
                    simbad.search(String.format(java.util.Locale.US, "%.6f %+.6f", s.camera.center.raDeg, s.camera.center.decDeg), radius).map { simbad.toCatalogRow(it) }
                } else {
                    vizier.coneSearch(catalogId, s.camera.center.raDeg, s.camera.center.decDeg, radius, maxRows)
                }
                _state.update { 
                    it.copy(
                        catalogLoading = false, 
                        catalogRows = rows,
                        message = if (rows.isEmpty()) "No objects found in this area" else "Loaded ${rows.size} objects"
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(catalogLoading = false, message = "Error loading catalog: ${e.message}") }
            }
        }
    }

    fun clearCatalog() = _state.update { it.copy(catalogRows = emptyList(), catalogVisible = true, activeCatalogId = null) }

    fun toggleCatalogVisibility() = _state.update { it.copy(catalogVisible = !it.catalogVisible) }

    // --- Markers and Measurements ---
    fun addMarker(pos: SkyPosition, label: String = "M${state.value.markers.size + 1}") {
        _state.update {
            it.copy(markers = it.markers + Marker(position = pos, label = label))
        }
    }

    fun clearMarkers() = _state.update { it.copy(markers = emptyList()) }

    fun toggleMeasureMode() = _state.update {
        it.copy(measureMode = !it.measureMode, pendingMeasureFrom = null)
    }

    /** Taps the sky in measure mode: first tap sets the origin, second sets the destination. */
    fun measureTap(pos: SkyPosition) {
        val s = _state.value
        val from = s.pendingMeasureFrom
        if (from == null) {
            _state.update { it.copy(pendingMeasureFrom = pos) }
        } else {
            _state.update {
                it.copy(
                    pendingMeasureFrom = null,
                    measurements = it.measurements + Measurement(from = from, to = pos),
                )
            }
        }
    }

    fun clearMeasurements() = _state.update { it.copy(measurements = emptyList(), pendingMeasureFrom = null) }

    private val app get() = getApplication<Application>()

    override fun onCleared() {
        // Cancels ongoing fetches and frees cached bitmaps.
        tileRepo.shutdown()
        super.onCleared()
    }

    companion object {
        // Initial FoV limit to avoid loading the whole sky at once on startup.
        private const val MAX_INITIAL_FOV = 60.0
    }
}