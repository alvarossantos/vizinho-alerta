package com.vizinhoalerta.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OcorrenciaRepositoryDemoTest {
    private lateinit var repo: OcorrenciaRepository

    @Before fun setUp() {
        DemoData.reset()
        repo = OcorrenciaRepository(demo = true)
    }

    @Test fun `listar devolve as cinco situacoes`() = runBlocking {
        val lista = repo.listar().getOrThrow()
        assertEquals(5, lista.size)
        assertTrue(lista.any { it.situacao == "rejeitado" })
    }

    @Test fun `atualizar situacao persiste no demo`() = runBlocking {
        assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isSuccess)
        assertEquals("rejeitado", repo.listar().getOrThrow().first { it.id == "cassia_1" }.situacao)
        assertEquals("resolvido", repo.listar().getOrThrow().first { it.id == "cassia_1" }.status)
    }
}
