package com.vizinhoalerta.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMessagesTest {

    @Test
    fun `senha invalida do firebase em ingles e traduzida`() {
        val e = Exception("The password is invalid or the user does not have a password.")
        assertEquals("E-mail ou senha incorretos.", formatAuthException(e))
    }
}
