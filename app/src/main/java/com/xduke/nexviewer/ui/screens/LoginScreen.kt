package com.xduke.nexviewer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xduke.nexviewer.data.DataStoreManager
import com.xduke.nexviewer.data.remote.api.NetworkClient
import com.xduke.nexviewer.data.remote.model.LoginRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    var isConnectionSet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var currentIp by remember { mutableStateOf("") }
    var currentPort by remember { mutableStateOf("") }

    // Check for saved connection details
    LaunchedEffect(Unit) {
        val savedIp = dataStoreManager.getIpAddress.first()
        val savedPort = dataStoreManager.getPort.first()

        if (!savedIp.isNullOrEmpty() && !savedPort.isNullOrEmpty()) {
            currentIp = savedIp
            currentPort = savedPort
            isConnectionSet = true
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (!isConnectionSet) {
            ServerConnectionForm(
                onConnect = { ip, port ->
                    scope.launch {
                        dataStoreManager.saveConnectionDetails(ip, port)
                        currentIp = ip
                        currentPort = port
                        isConnectionSet = true
                    }
                }
            )
        } else {
            UserLoginForm(
                ip = currentIp,
                port = currentPort,
                onLoginSuccess = { token ->
                    scope.launch {
                        dataStoreManager.saveAuthToken(token)
                        onLoginSuccess()
                    }
                },
                onChangeServer = {
                    scope.launch {
                        dataStoreManager.clearConnectionDetails()
                        currentIp = ""
                        currentPort = ""
                        isConnectionSet = false
                    }
                }
            )
        }
    }
}

@Composable
fun ServerConnectionForm(onConnect: (String, String) -> Unit) {
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9384") } // Default value
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connect to Server",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (ipAddress.isNotBlank() && port.isNotBlank()) {
                    onConnect(ipAddress, port)
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}

@Composable
fun UserLoginForm(
    ip: String,
    port: String,
    onLoginSuccess: (String) -> Unit,
    onChangeServer: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "User Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoggingIn) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        isLoggingIn = true
                        scope.launch {
                            try {
                                val apiService = NetworkClient.getApiService(ip, port)
                                val response = apiService.login(LoginRequest(username = username, password = password))
                                onLoginSuccess(response.token)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoggingIn = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter username and password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onChangeServer,
            enabled = !isLoggingIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Server IP/Port")
        }
    }
}
