package com.vizinhoalerta.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OcorrenciaTest {

    @Test
    fun `criar ocorrencia com valores padrao`() {
        val ocorrencia = Ocorrencia()
        
        assertEquals("", ocorrencia.id)
        assertEquals("", ocorrencia.userId)
        assertEquals("", ocorrencia.tipo)
        assertEquals("", ocorrencia.descricao)
        assertEquals("", ocorrencia.fotoUrl)
        assertEquals("", ocorrencia.audioUrl)
        assertEquals(0.0, ocorrencia.lat, 0.0)
        assertEquals(0.0, ocorrencia.lng, 0.0)
        assertEquals("registrado", ocorrencia.situacao)
        assertEquals("", ocorrencia.cidade)
        assertEquals("", ocorrencia.bairro)
        assertEquals("aberto", ocorrencia.status)
    }

    @Test
    fun `copia com nova situacao`() {
        val original = Ocorrencia(id = "123", situacao = "registrado")
        val atualizado = original.copy(situacao = Ocorrencia.SITUACAO_CONFIRMADO)
        
        assertEquals("123", atualizado.id)
        assertEquals("confirmado", atualizado.situacao)
    }

    @Test
    fun `constantes de situacao validas`() {
        assertEquals("registrado", Ocorrencia.SITUACAO_REGISTRADO)
        assertEquals("em_verificacao", Ocorrencia.SITUACAO_EM_VERIFICACAO)
        assertEquals("confirmado", Ocorrencia.SITUACAO_CONFIRMADO)
        assertEquals("resolvido", Ocorrencia.SITUACAO_RESOLVIDO)
        assertEquals("rejeitado", Ocorrencia.SITUACAO_REJEITADO)
    }
}