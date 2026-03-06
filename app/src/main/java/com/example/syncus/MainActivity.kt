package com.example.syncus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.syncus.ui.navigation.AppNavHost
import com.example.syncus.ui.theme.SyncUsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SyncUsTheme {
                AppNavHost()
            }
        }
    }
}