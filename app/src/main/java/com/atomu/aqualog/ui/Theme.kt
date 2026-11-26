package com.atomu.aqualog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0B5F82),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF00696E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB6EEF2),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFFB5482E),
    background = Color(0xFFF5FAFD),
    surface = Color(0xFFF5FAFD),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF84CFFF),
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF004C6C),
    onPrimaryContainer = Color(0xFFC6E7FF),
    secondary = Color(0xFF80D4DA),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F53),
    onSecondaryContainer = Color(0xFFB6EEF2),
    tertiary = Color(0xFFFFB4A1),
)

@Composable
fun AquaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
