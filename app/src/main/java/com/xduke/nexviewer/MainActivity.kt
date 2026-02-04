package com.xduke.nexviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xduke.nexviewer.data.DataStoreManager
import com.xduke.nexviewer.ui.screens.LoginScreen
import com.xduke.nexviewer.ui.screens.MainScreen
import com.xduke.nexviewer.ui.screens.SplashScreen
import com.xduke.nexviewer.ui.theme.NexViewerTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexViewerTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val dataStoreManager = remember { DataStoreManager(context) }
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val token = dataStoreManager.getAuthToken.first()
                    startDestination = if (!token.isNullOrEmpty()) "main" else "splash"
                }

                if (startDestination != null) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("splash") {
                                SplashScreen(onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                })
                            }
                            composable("login") {
                                LoginScreen(onLoginSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                })
                            }
                            composable("main") {
                                MainScreen(
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! Setup Complete.",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NexViewerTheme {
        Greeting("Android")
    }
}