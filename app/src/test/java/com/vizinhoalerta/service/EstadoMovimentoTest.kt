package com.vizinhoalerta.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoMovimentoTest {

    @Test
    fun `acima do limiar em qualquer eixo liga aviso`() {
        val estado = EstadoMovimento()
        assertTrue("eixo Y acima do limiar deve ligar o aviso", estado.atualizar(floatArrayOf(0f, 0.2f, 0f), 1000))
    }

    @Test
    fun `limiar exato conta como movimento`() {
        val estado = EstadoMovimento()
        assertTrue("0,15 rad/s deve contar como movimento", estado.atualizar(floatArrayOf(0.15f, 0f, 0f), 1000))
    }

    @Test
    fun `abaixo do limiar antes de 500ms mantem aviso`() {
        val estado = EstadoMovimento()
        assertTrue(estado.atualizar(floatArrayOf(0.2f, 0f, 0f), 1000))
        assertTrue("400ms parado ainda mantem o aviso", estado.atualizar(floatArrayOf(0f, 0f, 0f), 1400))
    }

    @Test
    fun `500ms contiguos abaixo do limiar escondem aviso`() {
        val estado = EstadoMovimento()
        assertTrue(estado.atualizar(floatArrayOf(0.2f, 0f, 0f), 1000))
        assertFalse("500ms parado deve esconder o aviso", estado.atualizar(floatArrayOf(0f, 0f, 0f), 1500))
    }

    @Test
    fun `giro antes de 500ms reinicia intervalo`() {
        val estado = EstadoMovimento()
        assertTrue(estado.atualizar(floatArrayOf(0.2f, 0f, 0f), 1000))
        assertTrue(estado.atualizar(floatArrayOf(0f, 0f, 0f), 1300))
        assertTrue(estado.atualizar(floatArrayOf(0.2f, 0f, 0f), 1400))
        assertTrue("novo giro reinicia a contagem", estado.atualizar(floatArrayOf(0f, 0f, 0f), 1899))
        assertFalse("500ms desde o ultimo giro devem esconder", estado.atualizar(floatArrayOf(0f, 0f, 0f), 1900))
    }

    @Test
    fun `reiniciar limpa estado`() {
        val estado = EstadoMovimento()
        assertTrue(estado.atualizar(floatArrayOf(0.2f, 0f, 0f), 1000))
        estado.reiniciar()
        assertFalse("estado limpo nao deve mostrar aviso", estado.atualizar(floatArrayOf(0f, 0f, 0f), 5000))
    }
}
