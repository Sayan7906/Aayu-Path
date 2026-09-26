package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GeoPrimaryTeal,
    secondary = GeoNavTextInactive,
    background = Color(0xFF101210),
    surface = Color(0xFF1C1E1C),
    onBackground = Color(0xFFFCFDF6),
    onSurface = Color(0xFFFCFDF6),
    onSurfaceVariant = Color(0xFFBFC9C8),
    primaryContainer = GeoPrimaryTealContainer,
    onPrimaryContainer = GeoOnPrimaryTealContainer
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimaryTeal,
    primaryContainer = GeoPrimaryTealContainer,
    onPrimaryContainer = GeoOnPrimaryTealContainer,
    secondary = GeoNavTextInactive,
    background = Color(0xFFF2F4F3),  // Beautiful soft clinical off-white
    surface = Color(0xFFFAFCFA),     // Slightly lighter off-white card surfaces for crisp card delineation
    onBackground = Color(0xFF050807), // Extremely dark, high-contrast charcoal black
    onSurface = Color(0xFF050807),    // Extremely dark, high-contrast charcoal black
    onSurfaceVariant = Color(0xFF141A18), // Darkened secondary text color for perfect readability
    error = GeoEmergencyAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force white/light theme for complete visibility
    dynamicColor: Boolean = false, // Disable default dynamic color to preserve Geometric Balance style
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme // Force light scheme to ensure a consistent high-contrast white theme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
