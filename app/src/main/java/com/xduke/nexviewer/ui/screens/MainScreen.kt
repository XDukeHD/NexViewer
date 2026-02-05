package com.xduke.nexviewer.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    val widgetOrder by viewModel.widgetOrder.collectAsState()
    val widgetVisibility by viewModel.widgetVisibility.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }

    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            viewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    val visibleWidgets = remember(widgetOrder, widgetVisibility, isEditMode) {
        widgetOrder.filter { type -> (widgetVisibility[type] ?: true) || isEditMode }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NexViewer Monitor") },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Done" else "Customize"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(currentTime, style = MaterialTheme.typography.titleMedium)
                    }

                    // Weather
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        weather?.let {
                            Icon(if ((it.isDay ?: 1) == 1) Icons.Filled.WbSunny else Icons.Filled.NightsStay, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${it.temperature ?: 0}°C", style = MaterialTheme.typography.titleMedium)
                        } ?: Text("Refreshing weather...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    ) { paddingValues ->
        stats?.let { systemStats ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(visibleWidgets, key = { it }) { type ->
                    val isVisible = widgetVisibility[type] ?: true

                    val alpha = if (!isVisible && isEditMode) 0.5f else 1f

                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                            .shadow(0.dp, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().then(if (isEditMode) Modifier.padding(8.dp) else Modifier)) {
                            if (isEditMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        viewModel.updateWidgetVisibility(type, !isVisible)
                                    }) {
                                        Icon(
                                            if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle Visibility"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Render Widget Content
                            Box(modifier = Modifier.alpha(alpha)) {
                                when (type) {
                                    WidgetType.CPU -> DashboardCard(
                                        type,
                                        "%.2f%%".format(java.util.Locale.US, systemStats.cpu_absolute ?: 0.0),
                                        "Utilization"
                                    )
                                    WidgetType.MEMORY -> DashboardCard(
                                        type,
                                        formatBytes(systemStats.memory_bytes ?: 0L),
                                        "RAM Used"
                                    )
                                    WidgetType.DISK -> DashboardCard(
                                        type,
                                        formatBytes(systemStats.disk_bytes ?: 0L),
                                        "Storage Used"
                                    )
                                    WidgetType.UPTIME -> DashboardCard(
                                        type,
                                        formatUptime(systemStats.uptime ?: 0L),
                                        "System Up"
                                    )
                                    WidgetType.NETWORK -> NetworkCard(systemStats)
                                    WidgetType.BATTERY -> BatteryCard(systemStats)
                                    WidgetType.VOLUME -> VolumeCard(systemStats)
                                    WidgetType.MEDIA -> MediaPlayersList(systemStats, serverUrl ?: "", viewModel)
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
    }
}

// Extracted Cards to clean up logic
@Composable
fun NetworkCard(systemStats: SystemStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TitleRow(WidgetType.NETWORK)
            Spacer(modifier = Modifier.height(8.dp))
             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Download", style = MaterialTheme.typography.labelSmall)
                    Text(formatBytes(systemStats.network?.rx_bytes ?: 0L), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Upload", style = MaterialTheme.typography.labelSmall)
                    Text(formatBytes(systemStats.network?.tx_bytes ?: 0L), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            systemStats.wifi?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("WiFi: ${it.ssid}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun BatteryCard(systemStats: SystemStats) {
    systemStats.battery?.let { battery ->
        DashboardCard(
            WidgetType.BATTERY,
            "${battery.percentage ?: 0}%",
            if (battery.plugged_in ?: false) "Charging" else "On Battery"
        )
    } ?: DashboardCard(WidgetType.BATTERY, "N/A", "No Battery")
}

@Composable
fun VolumeCard(systemStats: SystemStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TitleRow(WidgetType.VOLUME)
             Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Volume")
                    Text("${systemStats.volume ?: 0}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Backlight")
                    Text("${systemStats.backlight ?: 0}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MediaPlayersList(systemStats: SystemStats, serverUrl: String, viewModel: MainViewModel) {
    val audioPlayers = systemStats.audio.orEmpty()
    if (audioPlayers.isNotEmpty()) {
        // Since we are in a Grid item, we can't use lazy list inside nicely without strict height
        // We will just stack them in a Column
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
             audioPlayers.forEach { player ->
                MediaPlayerCard(
                    player = player,
                    serverUrl = serverUrl,
                    onCommand = { cmd -> viewModel.sendAudioCommand(player.id ?: "", cmd) }
                )
            }
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No media playing")
            }
        }
    }
}

@Composable
fun TitleRow(type: WidgetType) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(type.title, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
fun DashboardCard(type: WidgetType, value: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TitleRow(type)
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
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(player.title ?: "Unknown Title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(player.artist ?: "Unknown Artist", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(player.name ?: "Unknown Player", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onCommand("audio-previous") }) {
                    Icon(Icons.Filled.SkipPrevious, "Prev")
                }
                FilledTonalIconButton(onClick = { onCommand("audio-play-pause") }) {
                    Icon(
                        if (player.playing ?: false) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                         if (player.playing ?: false) "Pause" else "Play"
                    )
                }
                IconButton(onClick = { onCommand("audio-next") }) {
                    Icon(Icons.Filled.SkipNext, "Next")
                }
            }

            if ((player.duration ?: 0L) > 0) {
                 Spacer(modifier = Modifier.height(8.dp))
                 LinearProgressIndicator(
                     progress = { ((player.timestamp ?: 0L).toFloat() / (player.duration ?: 1L).toFloat()).coerceIn(0f, 1f) },
                     modifier = Modifier.fillMaxWidth()
                 )
                 Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                     Text(formatDuration(player.timestamp ?: 0L), style = MaterialTheme.typography.labelSmall)
                     Text(formatDuration(player.duration ?: 0L), style = MaterialTheme.typography.labelSmall)
                 }
            }
        }
    }
}
// Helper functions remain same
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
    return "${days}d ${hours}h ${minutes}m"
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
