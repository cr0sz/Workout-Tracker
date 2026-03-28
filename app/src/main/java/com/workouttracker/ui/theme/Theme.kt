package com.workouttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = AccentOrange,
    onPrimary        = Color.White,
    secondary        = AccentOrangeLight,
    onSecondary      = Color.White,
    background       = BackgroundDark,
    onBackground     = OnSurface,
    surface          = SurfaceDark,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFF3A3A3A)
)

private val LightColors = lightColorScheme(
    primary          = Color(0xFFD4521A),
    onPrimary        = Color.White,
    secondary        = AccentOrange,
    onSecondary      = Color.White,
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF1A1A1A),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A1A),
    surfaceVariant   = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF666666),
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFFDDDDDD)
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}
