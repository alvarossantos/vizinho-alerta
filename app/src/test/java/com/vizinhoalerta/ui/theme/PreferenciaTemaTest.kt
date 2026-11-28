package com.vizinhoalerta.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenciaTemaTest {

    @Test
    fun `sistema claro`() {
        assertFalse(temaEfetivo(PreferenciaTema.SISTEMA, false))
    }

    @Test
    fun `sistema escuro`() {
        assertTrue(temaEfetivo(PreferenciaTema.SISTEMA, true))
    }

    @Test
    fun `claro ignora sistema`() {
        assertFalse(temaEfetivo(PreferenciaTema.CLARO, true))
    }

    @Test
    fun `escuro ignora sistema`() {
        assertTrue(temaEfetivo(PreferenciaTema.ESCURO, false))
    }

    @Test
    fun `override ignora mudanca do sistema`() {
        assertTrue(temaEfetivo(PreferenciaTema.ESCURO, false))
        assertTrue(temaEfetivo(PreferenciaTema.ESCURO, true))
    }
}
