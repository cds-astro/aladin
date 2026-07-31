package com.aladin.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.aladin.android.AppSettings
import com.aladin.android.R
import com.aladin.android.SkyViewModel
import com.aladin.android.TileQuality
import com.aladin.android.data.hips.SurveyRegistry
import com.aladin.android.data.vizier.VizieRService
import com.aladin.android.sky.AstroMath
import com.aladin.android.ui.theme.SkyPanel
import com.aladin.android.ui.theme.TextPrimary
import com.aladin.android.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyScreen(vm: SkyViewModel) {
    val state by vm.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showCatalogSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }

    val catalogList = remember { VizieRService().catalogs }
    var maxRowsInput by remember { mutableStateOf("500") }

    var surveySearch by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Available Data",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                )
                OutlinedTextField(
                    value = surveySearch,
                    onValueChange = { surveySearch = it },
                    label = { Text("Search collections, e.g. PanSTARRS") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                if (state.surveysLoading) {
                    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                
                val filteredSurveys = remember(surveySearch, state.availableSurveys) {
                    if (surveySearch.isBlank()) state.availableSurveys
                    else state.availableSurveys.filter {
                        it.title.contains(surveySearch, ignoreCase = true) ||
                        it.id.contains(surveySearch, ignoreCase = true) ||
                        it.description.contains(surveySearch, ignoreCase = true)
                    }
                }
                
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filteredSurveys, key = { it.id }) { survey ->
                        NavigationDrawerItem(
                            label = { Text(survey.title) },
                            selected = survey.id == state.survey.id,
                            onClick = {
                                vm.setSurvey(survey)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            SkyCanvas(state = state, vm = vm, modifier = Modifier.fillMaxSize())

            // Loading indicator: small, top-left, only visible
            // while there are tiles pending download. Uses `downloadingTiles`
            // (see SkyViewModel/TileRepository.activeDownloads), which is updated
            // both when QUEUING and completing each tile, so it appears
            // immediately on pan/zoom and disappears as soon as the queue is empty.
            //
            // WARNING: it goes BELOW the top bar (top = 100.dp, not 36.dp). The
            // top bar is an opaque Surface with fillMaxWidth() that is drawn
            // LATER in this same Box, so at top=36.dp it was completely covered
            // -the indicator was never seen even though the state did update
            // (confirmed via log: activeDownloads reached 78).
            if (state.downloadingTiles > 0) {
                Surface(
                    color = SkyPanel,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 104.dp, start = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Loading ${state.downloadingTiles}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            // Top bar: menu (HiPS surveys) + VizieR catalogs.
            // The name search bar (Simbad) was removed: the Simbad endpoint
            // does not respond from the device, so a broken bar only confuses.
            // The VizieR catalog (which does work) is kept.
            Surface(
                color = SkyPanel,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.surveys), tint = androidx.compose.ui.graphics.Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = { showDownloadSheet = true }) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "Download HiPS", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = { showCatalogSheet = true }) {
                        Icon(Icons.Filled.TableChart, contentDescription = stringResource(R.string.catalogs), tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }

            // Action FABs
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, top = 120.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Fab(Icons.Filled.Straighten, "Measure", state.measureMode) { vm.toggleMeasureMode() }
                Fab(Icons.Filled.Add, "Marker") {
                    vm.addMarker(state.camera.center)
                }
                if (state.catalogRows.isNotEmpty()) {
                    Fab(
                        icon = if (state.catalogVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        desc = "Toggle catalog",
                        highlighted = !state.catalogVisible
                    ) {
                        vm.toggleCatalogVisibility()
                    }
                }
                if (state.markers.isNotEmpty()) {
                    Fab(Icons.Filled.Delete, "Clear markers") { vm.clearMarkers() }
                }
                if (state.measurements.isNotEmpty()) {
                    Fab(Icons.Filled.Clear, "Clear measurements") { vm.clearMeasurements() }
                }
            }

            // Bottom status bar
            Surface(
                color = SkyPanel,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusItem("RA", AstroMath.formatRa(state.camera.center.raDeg))
                    StatusItem("Dec", AstroMath.formatDec(state.camera.center.decDeg))
                    StatusItem("FoV", "%.2f°".format(state.camera.fovDeg))
                }
            }

            // Measure hint
            if (state.pendingMeasureFrom != null) {
                Surface(
                    color = SkyPanel,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        "Tap the second point to measure",
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // Ephemeral message
            state.message?.let { msg ->
                Surface(
                    color = SkyPanel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 96.dp)
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        msg,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    // Bottom sheet: selected object info
    val selected = state.selected
    if (selected != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { vm.clearSelected() },
            sheetState = sheetState,
        ) {
            ObjectSheetContent(
                obj = selected,
                onCenter = {
                    vm.recenter(selected.position)
                    vm.clearSelected()
                },
            )
        }
    }

    // Bottom sheet: catalog selector
    if (showCatalogSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCatalogSheet = false },
            sheetState = sheetState,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("VizieR Catalogs", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxRowsInput,
                    onValueChange = { maxRowsInput = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Max rows (e.g. 500)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                LazyColumn {
                    items(catalogList) { cat ->
                        NavigationDrawerItem(
                            label = { Text(cat.name) },
                            selected = false,
                            onClick = {
                                val limit = maxRowsInput.toIntOrNull() ?: 500
                                vm.loadCatalog(cat.id, limit)
                                showCatalogSheet = false
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Remove catalog") },
                            selected = false,
                            onClick = {
                                vm.clearCatalog()
                                showCatalogSheet = false
                            },
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet: settings (cache, quality, concurrency)
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
        ) {
            SettingsSheet(
                settings = state.settings,
                onSave = { vm.setSettings(it) },
                onClearCache = { vm.clearCache() },
            )
        }
    }

    if (showDownloadSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = sheetState,
        ) {
            DownloadSheet(
                downloader = vm.downloader,
                currentSurvey = state.survey,
                onDismiss = { showDownloadSheet = false },
                onAddCustomDir = { vm.addCustomHiPSDir(it) }
            )
        }
    }
}

/**
 * Settings with draft state: changes ARE NOT applied on the fly,
 * only when tapping "Save" (except Clear cache, which is an immediate action
 * unrelated to the rest of the settings). This way the user can type a large MB
 * figure without every intermediate digit triggering a cache resize.
 */
@Composable
private fun SettingsSheet(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onClearCache: () -> Unit,
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    val dirty = draft != settings

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        // --- Tile quality (density -> speed vs sharpness) ---
        Text("Image quality", color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TileQuality.entries.forEach { q ->
                androidx.compose.material3.FilterChip(
                    selected = draft.quality == q,
                    onClick = { draft = draft.copy(quality = q) },
                    label = { Text(q.label) },
                )
            }
        }
        Text(
            "Lower quality = fewer tiles = faster loading (at the cost of sharpness).",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(12.dp))

        // --- Memory cache (user types the MB value directly, no upper limit) ---
        Text("Memory cache", color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        CacheMbField(mb = draft.memCacheMb) { v -> draft = draft.copy(memCacheMb = v) }
        Text(
            "More MB = more tiles cached in RAM (fewer re-downloads), but more memory usage. Default 48 MB. No upper limit — pick any amount your device can handle.",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(12.dp))

        // --- Download concurrency ---
        Text("Concurrent downloads: ${draft.concurrency}", color = TextSecondary)
        Stepper(
            value = draft.concurrency,
            min = 2,
            max = 24,
            step = 1,
            onStep = { v -> draft = draft.copy(concurrency = v) },
        )
        Text(
            "The CDS server tolerates ~12 in parallel. More = faster if your network is good.",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(16.dp))

        // --- View modes (same as Aladin desktop: pixmaps/colormap) ---
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text("View modes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))

        // Coordinate grid
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Coordinate grid", color = TextSecondary)
            Switch(
                checked = draft.showGrid,
                onCheckedChange = { v -> draft = draft.copy(showGrid = v) },
            )
        }
        // Color inversion (photographic negative)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Invert colors", color = TextSecondary)
            Switch(
                checked = draft.invert,
                onCheckedChange = { v -> draft = draft.copy(invert = v) },
            )
        }
        // Brightness
        Text("Brightness: ${"%.2f".format(draft.brightness)}", color = TextSecondary)
        Slider(
            value = draft.brightness,
            onValueChange = { v -> draft = draft.copy(brightness = v) },
            valueRange = -0.5f..0.5f,
            steps = 19,
        )
        // Contrast
        Text("Contrast: ${"%.2f".format(draft.contrast)}", color = TextSecondary)
        Slider(
            value = draft.contrast,
            onValueChange = { v -> draft = draft.copy(contrast = v) },
            valueRange = 0.5f..2.0f,
            steps = 29,
        )
        Spacer(Modifier.height(12.dp))

        // --- Save (applies and persists all settings above) ---
        androidx.compose.material3.Button(
            onClick = { onSave(draft) },
            enabled = dirty,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (dirty) "Save settings" else "Saved")
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // --- Clear cache (immediate action, independent of Save) ---
        androidx.compose.material3.OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear cache (memory + disk)")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, step: Int, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = { onStep((value - step).coerceAtLeast(min)) }, enabled = value > min) {
            Icon(Icons.Filled.Clear, contentDescription = "Decrease")
        }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(48.dp))
        IconButton(onClick = { onStep((value + step).coerceAtMost(max)) }, enabled = value < max) {
            Icon(Icons.Filled.Add, contentDescription = "Increase")
        }
    }
}

/**
 * Text field where the user types directly how many MB of memory cache they
 * want — NO upper limit, only a sane floor (8 MB) so the cache always holds at
 * least a couple of tiles. Feeds the draft in `SettingsSheet`; only takes
 * effect when the user taps Save. Digits only.
 */
@Composable
private fun CacheMbField(mb: Int, onApply: (Int) -> Unit) {
    var text by remember(mb) { mutableStateOf(mb.toString()) }
    var hadFocus by remember { mutableStateOf(false) }
    val apply: () -> Unit = {
        text.toIntOrNull()?.let { v -> onApply(v.coerceAtLeast(8)) }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { s -> text = s.filter { it.isDigit() }.take(5) },
        label = { Text("Megabytes (MB)") },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { apply() }),
        trailingIcon = { Text("MB", color = TextSecondary) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { fs ->
                if (!fs.isFocused && hadFocus) apply()
                hadFocus = fs.isFocused
            },
    )
}

@Composable
private fun Fab(icon: ImageVector, desc: String, highlighted: Boolean = false, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
    ) {
        Icon(icon, contentDescription = desc, tint = if (highlighted) androidx.compose.ui.graphics.Color.Black else TextPrimary)
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ObjectSheetContent(obj: com.aladin.android.data.simbad.SimbadObject, onCenter: () -> Unit) {
    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Text(obj.mainId, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text("Type: ${obj.otype}", color = TextSecondary)
        Text("RA: ${obj.position.formatRa()}", color = TextSecondary)
        Text("Dec: ${obj.position.formatDec()}", color = TextSecondary)
        obj.magV?.let { Text("Mag V: $it", color = TextSecondary) }
        if (obj.spectralType.isNotBlank()) Text("Spectral type: ${obj.spectralType}", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        FloatingActionButton(onClick = onCenter, containerColor = MaterialTheme.colorScheme.primary) {
            Icon(Icons.Filled.Search, contentDescription = "Center")
        }
    }
}