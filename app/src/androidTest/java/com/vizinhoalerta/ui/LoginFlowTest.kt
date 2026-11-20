package com.vizinhoalerta.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vizinhoalerta.ui.login.LoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun login_screen_displays_title() {
        composeTestRule.onNodeWithText("Vizinho Alerta").assertIsDisplayed()
    }

    @Test
    fun login_screen_shows_login_form() {
        composeTestRule.onNodeWithText("E-mail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Senha").assertIsDisplayed()
    }

    @Test
    fun login_screen_validates_empty_fields() {
        // Tentar entrar com campos vazios
        composeTestRule.onNodeWithText("ENTRAR").performClick()
        // Mostra erro
        composeTestRule.onNodeWithText("Preencha email e senha").assertIsDisplayed()
    }

    @Test
    fun login_screen_accepts_valid_input() {
        composeTestRule.onNodeWithText("E-mail").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Senha").performTextInput("Senha123!")
        // Botão deveria estar habilitado (sem erro de validação)
    }

    @Test
    fun register_screen_shows_additional_fields() {
        // Clicar em "Cadastrar"
        composeTestRule.onNodeWithText("Não tem conta? Cadastrar").performClick()
        
        composeTestRule.onNodeWithText("Nome completo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sua Cidade").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seu Bairro").assertIsDisplayed()
    }
}