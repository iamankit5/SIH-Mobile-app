package com.example.sailfreight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy900 = Color(0xFF0F172A)
val Navy800 = Color(0xFF1E293B)
val Navy700 = Color(0xFF334155)
val Indigo600 = Color(0xFF4F46E5)
val Blue600 = Color(0xFF2563EB)
val Sky400 = Color(0xFF38BDF8)
val Emerald500 = Color(0xFF10B981)
val Amber500 = Color(0xFFF59E0B)
val Rose500 = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Navy900,
    primaryContainer = Blue600,
    onPrimaryContainer = Color.White,
    secondary = Amber500,
    onSecondary = Navy900,
    background = Navy900,
    onBackground = Color(0xFFF8FAFC),
    surface = Navy800,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Navy700,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Rose500,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Navy900,
    secondary = Indigo600,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    error = Rose500,
    onError = Color.White
)

@Composable
fun SAILFreightTheme(
    darkTheme: Boolean = true, // Industrial maritime dashboard defaults to high-contrast dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
