package com.xduke.nexviewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xduke.nexviewer.data.remote.model.AudioPlayer
import com.xduke.nexviewer.data.remote.model.SystemStats
import com.xduke.nexviewer.ui.viewmodel.MainViewModel
import com.xduke.nexviewer.ui.viewmodel.MainViewModelFactory

enum class WidgetType(val title: String, val icon: ImageVector) {
    CPU("CPU Usage", Icons.Default.Computer),
    MEMORY("Memory", Icons.Default.Memory),
    DISK("Disk Usage", Icons.Default.Storage),
    NETWORK("Network", Icons.Default.Wifi),
    UPTIME("Uptime", Icons.Default.CheckCircle),
    BATTERY("Battery", Icons.Default.BatteryFull),
    VOLUME("Volume", Icons.Default.VolumeUp),
    MEDIA("Media Players", Icons.Default.PlayArrow)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context)
    )
    val stats by viewModel.stats.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

    // State for widget visibility configuration
    // Simple map for now. In real app might want to persist this.
    var widgetVisibility by remember {
        mutableStateOf(
            WidgetType.values().associateWith { true }
        )
    }

    var showCustomizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NexViewer Monitor") },
                actions = {
                    IconButton(onClick = { showCustomizeDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Customize")
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        stats?.let { systemStats ->
            // Use Staggered Grid for Dashboard layout
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 300.dp), // Good for tablet
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Widget: CPU (Using Gauge-like simple text for now)
                if (widgetVisibility[WidgetType.CPU] == true) {
                    item {
                        DashboardCard(WidgetType.CPU, "${systemStats.cpu_absolute}%", "Utilization")
                    }
                }

                // Widget: Memory
                if (widgetVisibility[WidgetType.MEMORY] == true) {
                    item {
                        DashboardCard(
                            WidgetType.MEMORY,
                            formatBytes(systemStats.memory_bytes),
                            "RAM Used"
                        )
                    }
                }

                // Widget: Disk
                if (widgetVisibility[WidgetType.DISK] == true) {
                    item {
                        DashboardCard(
                            WidgetType.DISK,
                            formatBytes(systemStats.disk_bytes),
                            "Storage Used"
                        )
                    }
                }

                // Widget: Uptime
                if (widgetVisibility[WidgetType.UPTIME] == true) {
                    item {
                        DashboardCard(
                            WidgetType.UPTIME,
                            formatUptime(systemStats.uptime),
                            "System Up"
                        )
                    }
                }

                // Widget: Network
                if (widgetVisibility[WidgetType.NETWORK] == true) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(WidgetType.NETWORK.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(WidgetType.NETWORK.title, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Download", style = MaterialTheme.typography.labelSmall)
                                        Text(formatBytes(systemStats.network.rx_bytes), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Upload", style = MaterialTheme.typography.labelSmall)
                                        Text(formatBytes(systemStats.network.tx_bytes), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                                systemStats.wifi?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("WiFi: ${it.ssid}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Widget: Battery
                if (widgetVisibility[WidgetType.BATTERY] == true) {
                    systemStats.battery?.let { battery ->
                        item {
                            DashboardCard(
                                WidgetType.BATTERY,
                                "${battery.percentage}%",
                                if (battery.plugged_in) "Charging" else "On Battery"
                            )
                        }
                    }
                }

                // Widget: Volume/Backlight
                if (widgetVisibility[WidgetType.VOLUME] == true) {
                    item {
                         Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(WidgetType.VOLUME.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Controls", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Volume")
                                        Text("${systemStats.volume}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Backlight")
                                        Text("${systemStats.backlight}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                         }
                    }
                }

                // Widget: Media Players
                if (widgetVisibility[WidgetType.MEDIA] == true) {
                    if (systemStats.audio.isNotEmpty()) {
                        items(systemStats.audio) { player ->
                            MediaPlayerCard(
                                player = player,
                                serverUrl = serverUrl ?: "",
                                onCommand = { cmd -> viewModel.sendAudioCommand(player.id, cmd) }
                            )
                        }
                    } else {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No media playing")
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Connecting...", modifier = Modifier.padding(top = 48.dp))
            }
        }

        if (showCustomizeDialog) {
            CustomizeDialog(
                currentVisibility = widgetVisibility,
                onVisibilityChange = { type, isVisible ->
                    widgetVisibility = widgetVisibility.toMutableMap().apply { put(type, isVisible) }
                },
                onDismiss = { showCustomizeDialog = false }
            )
        }
    }
}

@Composable
fun DashboardCard(type: WidgetType, value: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(type.title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun MediaPlayerCard(player: AudioPlayer, serverUrl: String, onCommand: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Album Art
                val imageUrl = if (player.art_url != null) "$serverUrl${player.art_url}" else null

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    error = remember {
                        // Fallback composable logic isn't directly supported in 'error' param which takes a drawable,
                        // so we might handle placeholder via Box if needed, but simple null is OK for now or we rely on placeholder
                        null
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(player.title ?: "Unknown Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(player.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(player.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onCommand("audio-previous") }) {
                    Icon(Icons.Default.SkipPrevious, "Prev")
                }
                FilledTonalIconButton(onClick = { onCommand("audio-play-pause") }) {
                    Icon(
                        if (player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                         if (player.playing) "Pause" else "Play"
                    )
                }
                IconButton(onClick = { onCommand("audio-next") }) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
            }

            // Progress Bar (Mocked as we have timestamp/duration)
            if (player.duration > 0) {
                 Spacer(modifier = Modifier.height(8.dp))
                 LinearProgressIndicator(
                     progress = { (player.timestamp.toFloat() / player.duration.toFloat()).coerceIn(0f, 1f) },
                     modifier = Modifier.fillMaxWidth()
                 )
                 Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                     Text(formatDuration(player.timestamp), style = MaterialTheme.typography.labelSmall)
                     Text(formatDuration(player.duration), style = MaterialTheme.typography.labelSmall)
                 }
            }
        }
    }
}

@Composable
fun CustomizeDialog(
    currentVisibility: Map<WidgetType, Boolean>,
    onVisibilityChange: (WidgetType, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Dashboard") },
        text = {
            Column {
                WidgetType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentVisibility[type] == true,
                            onCheckedChange = { onVisibilityChange(type, it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(type.icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.0f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}

fun formatUptime(seconds: Long): String {
    val days = seconds / (24 * 3600)
    val hours = (seconds % (24 * 3600)) / 3600
    val minutes = (seconds % 3600) / 60
    return "${days}d ${hours}h"
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
