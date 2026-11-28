package com.vizinhoalerta.ui.theme

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SeletorTemaTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
}
