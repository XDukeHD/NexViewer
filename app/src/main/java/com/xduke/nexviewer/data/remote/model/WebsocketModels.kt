package com.xduke.nexviewer.data.remote.model

import com.google.gson.annotations.SerializedName

data class WebSocketResponse(
    @SerializedName("object") val objectType: String,
    val data: WebSocketData
)

data class WebSocketData(
    val token: String,
    val socket: String
)

data class WebSocketEvent(
    val event: String,
    val args: List<String>
)