package com.xduke.nexviewer.data

import android.util.Log
import com.google.gson.Gson
import com.xduke.nexviewer.data.remote.api.ApiService
import com.xduke.nexviewer.data.remote.model.SystemStats
import com.xduke.nexviewer.data.remote.model.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val apiService: ApiService
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket requires 0
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private val _systemStats = MutableStateFlow<SystemStats?>(null)
    val systemStats: StateFlow<SystemStats?> = _systemStats

    private var currentAuthToken: String? = null
    private var reconnectJob: Job? = null

    fun connect(authToken: String) {
        currentAuthToken = authToken
        if (reconnectJob?.isActive == true) return

        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("WebSocketManager", "Fetching WS details...")
                // 1. Get WS details
                val response = apiService.getWebsocketDetails("Bearer $authToken")
                val wsUrl = response.data.socket
                val wsToken = response.data.token

                Log.d("WebSocketManager", "Connecting to $wsUrl")

                // 2. Connect
                val request = Request.Builder().url(wsUrl).build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d("WebSocketManager", "Connected")
                        // 3. Authenticate
                        val authFrame = WebSocketEvent("auth", listOf(wsToken))
                        val json = gson.toJson(authFrame)
                        webSocket.send(json)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d("WebSocketManager", "Closing: $code $reason")
                        if (code == 4004 || code == 4001) {
                             scheduleReconnect()
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e("WebSocketManager", "Failure", t)
                        scheduleReconnect()
                    }
                })
            } catch (e: Exception) {
                Log.e("WebSocketManager", "Connection setup failed", e)
                scheduleReconnect()
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val event = gson.fromJson(text, WebSocketEvent::class.java)
            if (event.event == "stats") {
                if (event.args.isNotEmpty()) {
                    val statsJson = event.args[0]
                    val stats = gson.fromJson(statsJson, SystemStats::class.java)
                    _systemStats.value = stats
                }
            } else if (event.event.contains("session expiring")) {
                Log.d("WebSocketManager", "Session expiring, reconnecting...")
                scheduleReconnect()
            }
        } catch (e: Exception) {
            Log.e("WebSocketManager", "Error parsing message", e)
        }
    }

    private fun scheduleReconnect() {
        webSocket?.close(1000, "Reconnecting")
        webSocket = null
        CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            currentAuthToken?.let { connect(it) }
        }
    }

    fun sendAudioCommand(playerId: String, command: String) {
        val event = WebSocketEvent(command, listOf(playerId))
        webSocket?.send(gson.toJson(event))
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout")
        webSocket = null
    }
}
