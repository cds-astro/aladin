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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    downloader: HiPSDownloader,
    currentSurvey: HiPSSurvey,
    onDismiss: () -> Unit,
    onAddCustomDir: (String) -> Unit = {}
) {
    val state by downloader.state.collectAsState()
    var selectedOrder by remember { mutableStateOf(3f) }
    var maxThreads by remember { mutableStateOf(8f) }
    var fastCheck by remember { mutableStateOf(true) }
    var customPath by remember { mutableStateOf("") }

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

        if (state.isRunning) {
            Text("Downloading: ${state.datasetName}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Tiles scanned: ${state.tilesScanned} / ${state.totalTilesQueued}")
            Text("Tiles downloaded: ${state.tilesDownloaded}")
            Text("Data downloaded: ${formatBytes(state.bytesDownloaded)}")
            Text("Active threads: ${state.activeThreads}")
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { downloader.stopDownload() },
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
