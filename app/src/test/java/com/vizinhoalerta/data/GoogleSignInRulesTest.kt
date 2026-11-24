package com.vizinhoalerta.data

import com.vizinhoalerta.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInRulesTest {

    @Test fun `web client id valido so aceita id real`() {
        assertTrue(webClientIdValido("123456789-abc.apps.googleusercontent.com"))
        assertFalse(webClientIdValido(null))
        assertFalse(webClientIdValido(""))
        assertFalse(webClientIdValido("   "))
        assertFalse(webClientIdValido("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"))
        assertFalse(webClientIdValido("123-DUMMY.apps.googleusercontent.com"))
        assertFalse(webClientIdValido("000000000000-abc.apps.googleusercontent.com"))
        assertFalse(webClientIdValido("123-abc.example.com"))
        assertFalse(webClientIdValido("123-abc.apps.googleusercontent.com.br"))
    }

    @Test fun `perfil nulo ou sem localizacao exige onboarding`() {
        assertTrue(perfilIncompleto(null))
        assertTrue(perfilIncompleto(Usuario(cidade = "", bairro = "Centro")))
        assertTrue(perfilIncompleto(Usuario(cidade = "Cássia", bairro = "  ")))
        assertFalse(perfilIncompleto(Usuario(cidade = "Cássia", bairro = "Centro")))
    }

    @Test fun `conflito de conta google tem mensagem especifica`() {
        assertEquals(
            "Este e-mail já possui conta com senha. Entre com sua senha para continuar.",
            mensagemConflitoConta("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", null)
        )
        assertEquals(
            "Este e-mail já possui conta com senha. Entre com sua senha para continuar.",
            mensagemConflitoConta(null, "account-exists-with-different-credential")
        )
        assertNull(mensagemConflitoConta("ERROR_EMAIL_ALREADY_IN_USE", null))
    }
}
