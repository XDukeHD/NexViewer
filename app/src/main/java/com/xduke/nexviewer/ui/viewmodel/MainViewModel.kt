package com.xduke.nexviewer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xduke.nexviewer.data.DataStoreManager
import com.xduke.nexviewer.data.PreferencesManager
import com.xduke.nexviewer.data.WeatherInfo
import com.xduke.nexviewer.data.WeatherRepository
import com.xduke.nexviewer.data.WebSocketManager
import com.xduke.nexviewer.data.remote.api.NetworkClient
import com.xduke.nexviewer.data.remote.model.SystemStats
import com.xduke.nexviewer.ui.screens.WidgetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val context: Context
) : ViewModel() {
    private val dataStoreManager = DataStoreManager(context)
    private val preferencesManager = PreferencesManager(context)
    private val weatherRepository = WeatherRepository(context)
    private var webSocketManager: WebSocketManager? = null

    private val _stats = MutableStateFlow<SystemStats?>(null)
    val stats: StateFlow<SystemStats?> = _stats.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _widgetOrder = MutableStateFlow<List<WidgetType>>(WidgetType.values().toList())
    val widgetOrder: StateFlow<List<WidgetType>> = _widgetOrder.asStateFlow()

    private val _widgetVisibility = MutableStateFlow<Map<WidgetType, Boolean>>(emptyMap())
    val widgetVisibility: StateFlow<Map<WidgetType, Boolean>> = _widgetVisibility.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _currentTime = MutableStateFlow<String>("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    init {
        initializeConnection()
        loadPreferences()
        startClock()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            launch {
                try {
                    preferencesManager.widgetOrder.collect { _widgetOrder.value = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            launch {
                try {
                    preferencesManager.widgetVisibility.collect { _widgetVisibility.value = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateWidgetOrder(newOrder: List<WidgetType>) {
        _widgetOrder.value = newOrder
        viewModelScope.launch {
            preferencesManager.saveWidgetOrder(newOrder)
        }
    }

    fun updateWidgetVisibility(type: WidgetType, isVisible: Boolean) {
        val current = _widgetVisibility.value.toMutableMap()
        current[type] = isVisible
        _widgetVisibility.value = current
        viewModelScope.launch {
            preferencesManager.saveWidgetVisibility(current)
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            val location = weatherRepository.getCurrentLocation()
            if (location != null) {
                val weather = weatherRepository.getWeather(location.latitude, location.longitude)
                _weather.value = weather
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            while (true) {
                val time = sdf.format(Date())
                _currentTime.value = time
                delay(1000)
            }
        }
    }

    private fun initializeConnection() {
        viewModelScope.launch {
            try {
                val ip = dataStoreManager.getIpAddress.first()
                val port = dataStoreManager.getPort.first()
                val token = dataStoreManager.getAuthToken.first()

                if (!ip.isNullOrEmpty() && !port.isNullOrEmpty() && !token.isNullOrEmpty()) {
                    _serverUrl.value = "http://$ip:$port"
                    val apiService = NetworkClient.getApiService(ip, port)
                    webSocketManager = WebSocketManager(apiService)

                    // Collect stats
                    launch {
                        try {
                            webSocketManager?.systemStats?.collect {
                                _stats.value = it
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Connect
                    webSocketManager?.connect(token)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
