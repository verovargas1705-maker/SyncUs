package com.example.syncus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
fun SyncUsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = Typography(),
        content = content
    )
}