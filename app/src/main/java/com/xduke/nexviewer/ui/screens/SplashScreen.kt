package com.xduke.nexviewer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds delay
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NexViewer",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
