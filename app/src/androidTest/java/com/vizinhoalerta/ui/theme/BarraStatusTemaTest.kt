package com.vizinhoalerta.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BarraStatusTemaTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun escuro_usa_barra_escura_e_icones_claros() {
        composeTestRule.setContent { VizinhoAlertaTheme(PreferenciaTema.ESCURO) {} }
        composeTestRule.waitForIdle()

        val window = composeTestRule.activity.window
        assertEquals(0xFF1C1B1F.toInt(), window.statusBarColor)
        assertEquals(
            false,
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars
        )
    }

    @Test
    fun claro_usa_barra_clara_e_icones_escuros() {
        composeTestRule.setContent { VizinhoAlertaTheme(PreferenciaTema.CLARO) {} }
        composeTestRule.waitForIdle()

        val window = composeTestRule.activity.window
        assertEquals(0xFFFFFBE8.toInt(), window.statusBarColor)
        assertEquals(
            true,
            WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars
        )
    }
}
