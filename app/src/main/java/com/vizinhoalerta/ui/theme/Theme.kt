package com.vizinhoalerta.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

private val DarkColorScheme = darkColorScheme(
    primary = Laranja,
    onPrimary = Color.White,
    secondary = Marinho,
    onSecondary = Color.White,
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    error = Color(0xFFEF5350),
    tertiary = Color(0xFF81C784),
    outline = Borda,
)

val Shapes = Shapes(
    extraSmall = CircleShape,
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VizinhoAlertaTheme(
    preferencia: PreferenciaTema = PreferenciaTema.SISTEMA,
    content: @Composable () -> Unit
) {
    val uiMode = LocalConfiguration.current.uiMode
    val isNightMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val escuro = temaEfetivo(preferencia, isNightMode)
    val context = LocalContext.current

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = if (escuro) 0xFF1C1B1F.toInt() else 0xFFFFFBE8.toInt()
        WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = !escuro
    }

    MaterialTheme(
        colorScheme = if (escuro) DarkColorScheme else LightColorScheme,
        shapes = Shapes,
        typography = Typography(),
        content = content
    )
}