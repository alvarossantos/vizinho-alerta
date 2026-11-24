package com.vizinhoalerta.ui

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vizinhoalerta.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginDemoLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun linhaDemoAbaixoDoGoogle_eDistanciaIgualAoCadastro() {
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()

        val entrarBottom = composeRule.onNodeWithText("ENTRAR").getBoundsInRoot().bottom
        val googleLoginTop = composeRule.onNodeWithText("Continuar com Google").getBoundsInRoot().top
        val demoTop = composeRule.onNodeWithText("Demo: admin@demo.com / Demo@123").getBoundsInRoot().top

        assertTrue(
            "linha demo deve ficar abaixo do botao Continuar com Google",
            demoTop > googleLoginTop
        )

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cadastrar-se").performClick()
        composeRule.waitForIdle()

        val criarBottom = composeRule.onNodeWithText("Criar minha conta").getBoundsInRoot().bottom
        val googleCadastroTop = composeRule.onNodeWithText("Continuar com Google").getBoundsInRoot().top

        val gapLogin = googleLoginTop - entrarBottom
        val gapCadastro = googleCadastroTop - criarBottom
        assertTrue(
            "distancia ENTRAR->Google no login ($gapLogin) deve igualar Criar minha conta->Google no cadastro ($gapCadastro)",
            gapLogin == gapCadastro
        )
    }
}
