package com.vizinhoalerta.ui.reportar

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AvisoMovimentoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aviso_aparece_quando_ha_movimento() {
        composeTestRule.setContent { Box { AvisoMovimento(true) } }
        composeTestRule.onNodeWithText("Segure o celular firme").assertIsDisplayed()
    }

    @Test
    fun aviso_desaparece_quando_estavel() {
        composeTestRule.setContent { Box { AvisoMovimento(false) } }
        composeTestRule.onAllNodesWithText("Segure o celular firme").assertCountEquals(0)
    }
}
