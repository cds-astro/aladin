@file:Suppress("EmptyCatchBlock", "TooGenericExceptionThrown", "VariableNaming", "ImplicitDefaultLocale", "WildcardImport", "MagicNumber", "MaxLineLength", "ComplexCondition", "CyclomaticComplexMethod", "LongMethod", "ReturnCount", "LongParameterList", "SwallowedException", "TooGenericExceptionCaught", "NestedBlockDepth", "FunctionNaming", "LoopWithTooManyJumpStatements", "TooManyFunctions")
package com.aladin.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aladin.android.data.hips.HiPSDownloader
import com.aladin.android.data.hips.HiPSSurvey
import com.aladin.android.data.vizier.CatalogDownloader
import com.aladin.android.model.SkyPosition
import com.aladin.android.data.vizier.VizieRService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    downloader: HiPSDownloader,
    catalogDownloader: CatalogDownloader,
    currentSurvey: HiPSSurvey,
    cameraCenter: SkyPosition,
    onDismiss: () -> Unit,
    onAddCustomDir: (String) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Offline Downloader",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("HiPS Tiles") })
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Catalogs") })
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTabIndex == 0) {
            HiPSTabContent(downloader, currentSurvey, onDismiss, onAddCustomDir)
        } else {
            CatalogTabContent(catalogDownloader, cameraCenter)
        }
    }
}

@Composable
fun HiPSTabContent(
    downloader: HiPSDownloader,
    currentSurvey: HiPSSurvey,
    onDismiss: () -> Unit,
    onAddCustomDir: (String) -> Unit
) {
    val state by downloader.getState(currentSurvey.id).collectAsState()
    var selectedOrder by remember { mutableFloatStateOf(3f) }
    var maxThreads by remember { mutableFloatStateOf(8f) }
    var fastCheck by remember { mutableStateOf(true) }
    var customPath by remember { mutableStateOf("") }

    if (state.isRunning) {
        Text("Downloading: ${state.datasetName}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Tiles scanned: ${state.tilesScanned} / ${state.totalTilesQueued}")
        Text("Tiles ready (downloaded + skipped): ${state.tilesDownloaded}")
        Text("Data downloaded: ${formatBytes(state.bytesDownloaded)}")
        Text("Active threads: ${state.activeThreads}")
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { downloader.stopDownload(currentSurvey.id) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Download")
        }
    } else {
        Text("Target Dataset: ${currentSurvey.title}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Select Maximum Order (Depth): ${selectedOrder.toInt()}")
        Slider(
            value = selectedOrder,
            onValueChange = { selectedOrder = it },
            valueRange = 0f..8f,
            steps = 7,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Higher order means more detail, but exponentially more files. Max order 3-4 is recommended for offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Max Threads: ${maxThreads.toInt()}")
        Slider(
            value = maxThreads,
            onValueChange = { maxThreads = it },
            valueRange = 1f..16f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fastCheck, onCheckedChange = { fastCheck = it })
            Text("Fast Check (skip existing files)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = customPath,
            onValueChange = { customPath = it },
            label = { Text("Custom Output/Input Directory (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    if (customPath.isNotBlank()) onAddCustomDir(customPath)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Folder to Catalog")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (customPath.isNotBlank()) onAddCustomDir(customPath)
                    downloader.startDownload(currentSurvey, selectedOrder.toInt(), "jpg", fastCheck, maxThreads.toInt(), customPath.ifBlank { null }) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Download")
            }
        }
    }
}

@Composable
fun CatalogTabContent(
    catalogDownloader: CatalogDownloader,
    cameraCenter: SkyPosition
) {
    val state by catalogDownloader.state.collectAsState()
    var raText by remember { mutableStateOf(cameraCenter.raDeg.toString()) }
    var decText by remember { mutableStateOf(cameraCenter.decDeg.toString()) }
    var radiusText by remember { mutableStateOf("15.0") } // arcmin
    var allSky by remember { mutableStateOf(false) }
    
    val catalogs = remember { VizieRService().catalogs }
    var selectedCatalogIndex by remember { mutableIntStateOf(0) }

    if (state.isRunning) {
        Text("Downloading: ${state.catalogName}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text("Data downloaded: ${formatBytes(state.bytesDownloaded)}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { catalogDownloader.stopDownload() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Download")
        }
    } else {
        if (state.isSuccess) {
            Text("Download Complete!", color = MaterialTheme.colorScheme.primary)
        }
        if (state.errorMessage != null) {
            Text("Error: ${state.errorMessage}", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = allSky, onCheckedChange = { allSky = it })
            Text("Download Full Catalog (All-Sky)")
        }
        
        OutlinedTextField(
            value = raText,
            onValueChange = { raText = it },
            label = { Text("RA (Degrees)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !allSky
        )
        OutlinedTextField(
            value = decText,
            onValueChange = { decText = it },
            label = { Text("Dec (Degrees)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !allSky
        )
        OutlinedTextField(
            value = radiusText,
            onValueChange = { radiusText = it },
            label = { Text("Radius (Arcmin)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !allSky
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Select Catalog:")
        catalogs.forEachIndexed { index, cat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = index == selectedCatalogIndex,
                    onClick = { selectedCatalogIndex = index }
                )
                Text(cat.name)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val r = raText.toDoubleOrNull() ?: 0.0
                val d = decText.toDoubleOrNull() ?: 0.0
                val rad = radiusText.toDoubleOrNull() ?: 0.0
                val cat = catalogs[selectedCatalogIndex]
                if (cat.id.uppercase() == "SIMBAD") {
                    if (allSky) {
                        // Simbad doesn't easily support unbounded queries without criteria
                        catalogDownloader.startSimbadDownload(r, d, 999999.0)
                    } else {
                        catalogDownloader.startSimbadDownload(r, d, rad)
                    }
                } else {
                    catalogDownloader.startVizieRDownload(cat.id, r, d, rad, allSky)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Catalog Download")
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
