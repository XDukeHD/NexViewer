package com.xduke.nexviewer.data.remote.model

data class SystemStats(
    val memory_bytes: Long? = 0L,
    val cpu_absolute: Double? = 0.0,
    val network: NetworkStats? = null,
    val uptime: Long? = 0L,
    val disk_bytes: Long? = 0L,
    val audio: List<AudioPlayer>? = emptyList(),
    val wifi: WifiStats? = null,
    val battery: BatteryStats? = null,
    val volume: Int? = 0,
    val backlight: Int? = 0
)

data class NetworkStats(
    val rx_bytes: Long? = 0L,
    val tx_bytes: Long? = 0L
)

data class AudioPlayer(
    val id: String? = null,
    val name: String? = null,
    val playing: Boolean? = null,
    val artist: String? = null,
    val title: String? = null,
    val album: String? = null,
    val art_url: String? = null,
    val timestamp: Long? = null,
    val duration: Long? = null
)

data class WifiStats(
    val ssid: String?,
    val connected: Boolean?
)

data class BatteryStats(
    val percentage: Int?,
    val plugged_in: Boolean?
)
