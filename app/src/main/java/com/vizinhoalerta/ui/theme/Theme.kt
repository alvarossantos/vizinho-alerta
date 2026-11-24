package com.vizinhoalerta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Paleta extraída dos mockups (tokens públicos para o overlay consumir)
val Laranja = Color(0xFFF97316)
val LaranjaEscuro = Color(0xFFEA580C)
val Marinho = Color(0xFF0F172A)
val Slate = Color(0xFF64748B)
val Borda = Color(0xFFE2E8F0)
val Fundo = Color(0xFFF8FAFC)
val Vermelho = Color(0xFFEF4444)
val VermelhoClaro = Color(0xFFFEE2E2)
val Ambar = Color(0xFFF59E0B)
val AmbarClaro = Color(0xFFFEF3C7)
val Azul = Color(0xFF3B82F6)
val AzulClaro = Color(0xFFDBEAFE)
val Verde = Color(0xFF10B981)
val VerdeClaro = Color(0xFFD1FAE5)

private val LightColorScheme = lightColorScheme(
    primary = Laranja,
    onPrimary = Color.White,
    background = Fundo,
    onBackground = Marinho,
    surface = Color.White,
    onSurface = Marinho,
    outline = Borda,
    error = Vermelho,
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