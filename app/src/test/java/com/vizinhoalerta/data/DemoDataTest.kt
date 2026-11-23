package com.vizinhoalerta.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DemoDataTest {
    @Before fun setUp() { DemoData.reset() }

    @Test fun `fixture tem admin e morador`() {
        assertEquals(2, DemoData.usuarios.size)
        assertTrue(DemoData.usuarios.first { it.email == DemoData.ADMIN_EMAIL }.isAdmin)
        assertFalse(DemoData.usuarios.first { it.email == "morador@demo.com" }.isAdmin)
    }

    @Test fun `fixture cobre todas as situacoes em Cassia e Passos`() {
        assertEquals(5, DemoData.ocorrencias.size)
        assertEquals(
            setOf("registrado", "em_verificacao", "confirmado", "resolvido", "rejeitado"),
            DemoData.ocorrencias.map { it.situacao }.toSet()
        )
        assertTrue(DemoData.ocorrencias.all { it.lat < -19.0 && it.lat > -21.0 })
        assertTrue(DemoData.ocorrencias.all { it.cidade in setOf("Cássia", "Passos") })
    }

    @Test fun `reset restaura a fixture`() {
        DemoData.usuarios.clear()
        DemoData.reset()
        assertEquals(2, DemoData.usuarios.size)
    }
}
