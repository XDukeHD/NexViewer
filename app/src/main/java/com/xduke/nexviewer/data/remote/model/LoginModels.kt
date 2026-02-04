package com.xduke.nexviewer.data.remote.model

data class LoginRequest(
    val username: String,
    val type: String = "login",
    val password:  String? = null // Adding password field, making it nullable if needed, but likely required.
)

data class LoginResponse(
    val token: String
)
