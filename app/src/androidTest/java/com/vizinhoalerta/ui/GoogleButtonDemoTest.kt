package com.vizinhoalerta.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vizinhoalerta.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleButtonDemoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun botao_google_no_login_mostra_aviso_no_demo() {
        composeTestRule.onNodeWithText("Entrar").performClick()

        composeTestRule.onNodeWithText("Continuar com Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continuar com Google").performClick()
        composeTestRule.onNodeWithText(
            "Login com Google exige Firebase configurado (google-services.json com OAuth Web)."
        ).assertIsDisplayed()
    }

    @Test
    fun botao_google_no_cadastro_mostra_aviso_no_demo() {
        composeTestRule.onNodeWithText("Entrar").performClick()
        composeTestRule.onNodeWithText("Não tem conta? Cadastrar").performClick()

        composeTestRule.onNodeWithText("Continuar com Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continuar com Google").performClick()
        composeTestRule.onNodeWithText(
            "Login com Google exige Firebase configurado (google-services.json com OAuth Web)."
        ).assertIsDisplayed()
    }
}
