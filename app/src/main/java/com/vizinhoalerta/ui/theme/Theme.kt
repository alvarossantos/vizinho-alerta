package com.vizinhoalerta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF12203A),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F9),
    error = Color(0xFFC62828),
    tertiary = Color(0xFF2E7D32),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF12203A),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    error = Color(0xFFEF5350),
    tertiary = Color(0xFF81C784),
)

val Shapes = Shapes(
    extraSmall = CircleShape,
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VizinhoAlertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = Shapes,
        typography = Typography(),
        content = content
    )
}