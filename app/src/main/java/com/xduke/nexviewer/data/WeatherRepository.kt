package com.xduke.nexviewer.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class WeatherInfo(
    val temperature: Double?,
    val isDay: Int?
)

class WeatherRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    cont.resume(location)
                }
                .addOnFailureListener {
                    cont.resume(null) // Return null on failure
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            cont.resume(null)
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(null)
        }
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string()

                if (response.isSuccessful && json != null) {
                    val weatherResponse = gson.fromJson(json, OpenMeteoResponse::class.java)
                    WeatherInfo(
                        temperature = weatherResponse.current_weather.temperature,
                        isDay = weatherResponse.current_weather.is_day
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

private data class OpenMeteoResponse(
    val current_weather: CurrentWeather
)

private data class CurrentWeather(
    val temperature: Double,
    val is_day: Int
)
