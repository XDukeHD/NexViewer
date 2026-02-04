package com.xduke.nexviewer.data.remote.api

import com.xduke.nexviewer.data.remote.model.LoginRequest
import com.xduke.nexviewer.data.remote.model.LoginResponse
import com.xduke.nexviewer.data.remote.model.WebSocketResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("v1/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("v1/websocket")
    suspend fun getWebsocketDetails(@Header("Authorization") token: String): WebSocketResponse
}
