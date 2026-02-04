package com.xduke.nexviewer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xduke.nexviewer.data.DataStoreManager
import com.xduke.nexviewer.data.WebSocketManager
import com.xduke.nexviewer.data.remote.api.NetworkClient
import com.xduke.nexviewer.data.remote.model.SystemStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context
) : ViewModel() {
    private val dataStoreManager = DataStoreManager(context)
    private var webSocketManager: WebSocketManager? = null

    private val _stats = MutableStateFlow<SystemStats?>(null)
    val stats: StateFlow<SystemStats?> = _stats.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    init {
        initializeConnection()
    }

    private fun initializeConnection() {
        viewModelScope.launch {
            val ip = dataStoreManager.getIpAddress.first()
            val port = dataStoreManager.getPort.first()
            val token = dataStoreManager.getAuthToken.first()

            if (!ip.isNullOrEmpty() && !port.isNullOrEmpty() && !token.isNullOrEmpty()) {
                _serverUrl.value = "http://$ip:$port"
                val apiService = NetworkClient.getApiService(ip, port)
                webSocketManager = WebSocketManager(apiService)

                // Collect stats
                launch {
                    webSocketManager?.systemStats?.collect {
                        _stats.value = it
                    }
                }

                // Connect
                webSocketManager?.connect(token)
            }
        }
    }

    fun sendAudioCommand(playerId: String, command: String) {
        webSocketManager?.sendAudioCommand(playerId, command)
    }

    fun logout() {
        viewModelScope.launch {
            webSocketManager?.disconnect()
            dataStoreManager.clearConnectionDetails()
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager?.disconnect()
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
