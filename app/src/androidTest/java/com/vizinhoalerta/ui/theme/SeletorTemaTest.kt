package com.vizinhoalerta.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeletorTemaTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun seletor_mostra_tres_opcoes_e_confirma() {
        var escolhida: PreferenciaTema? = null
        composeTestRule.setContent {
            SeletorTema(PreferenciaTema.SISTEMA, onSelecionar = { escolhida = it })
        }

        composeTestRule.onNodeWithText("Tema", substring = true).performClick()
        composeTestRule.onNodeWithText("Seguir o sistema").assertIsDisplayed()
        composeTestRule.onNodeWithText("Claro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escuro").assertIsDisplayed()

        composeTestRule.onNodeWithText("Escuro").performClick()
        assertEquals(PreferenciaTema.ESCURO, escolhida)
        composeTestRule.onAllNodesWithText("Seguir o sistema").assertCountEquals(0)
    }

    @Test
    fun trocar_tema_mantem_activity_e_tela() {
        composeTestRule.setContent {
            var preferencia by remember { mutableStateOf(PreferenciaTema.SISTEMA) }
            VizinhoAlertaTheme(preferencia) {
                Text("Perfil")
                SeletorTema(preferencia, onSelecionar = { preferencia = it })
            }
        }
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
        val atividadeAntes = composeTestRule.activity

        composeTestRule.onNodeWithText("Tema", substring = true).performClick()
        composeTestRule.onNodeWithText("Escuro").performClick()

        assertEquals(atividadeAntes, composeTestRule.activity)
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tema (Escuro)").assertIsDisplayed()
    }
}
