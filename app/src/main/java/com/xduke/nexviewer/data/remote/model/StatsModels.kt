package com.xduke.nexviewer.data.remote.model

data class SystemStats(
    val memory_bytes: Long,
    val cpu_absolute: Double,
    val network: NetworkStats,
    val uptime: Long,
    val disk_bytes: Long,
    val audio: List<AudioPlayer> = emptyList(),
    val wifi: WifiStats? = null,
    val battery: BatteryStats? = null,
    val volume: Int,
    val backlight: Int
)

data class NetworkStats(
    val rx_bytes: Long,
    val tx_bytes: Long
)

data class AudioPlayer(
    val id: String,
    val name: String,
    val playing: Boolean,
    val artist: String?,
    val title: String?,
    val album: String?,
    val art_url: String?,
    val timestamp: Long,
    val duration: Long
)

data class WifiStats(
    val ssid: String,
    val connected: Boolean
)

data class BatteryStats(
    val percentage: Int,
    val plugged_in: Boolean
)
