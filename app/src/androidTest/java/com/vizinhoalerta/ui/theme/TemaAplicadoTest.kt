package com.vizinhoalerta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TemaAplicadoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun preferencia_escuro_usa_fundo_escuro() {
        var fundo: Color? = null
        composeTestRule.setContent {
            VizinhoAlertaTheme(PreferenciaTema.ESCURO) {
                fundo = MaterialTheme.colorScheme.background
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(Color(0xFF121212), fundo)
    }

    @Test
    fun preferencia_claro_usa_fundo_claro() {
        var fundo: Color? = null
        composeTestRule.setContent {
            VizinhoAlertaTheme(PreferenciaTema.CLARO) {
                fundo = MaterialTheme.colorScheme.background
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(Color(0xFFF8FAFC), fundo)
    }
}
