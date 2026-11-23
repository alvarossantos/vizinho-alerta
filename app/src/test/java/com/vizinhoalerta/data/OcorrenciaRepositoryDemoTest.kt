package com.vizinhoalerta.data

import com.vizinhoalerta.model.Ocorrencia
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OcorrenciaRepositoryDemoTest {
    private lateinit var repo: OcorrenciaRepository

    @Before fun setUp() {
        DemoData.reset()
        repo = OcorrenciaRepository(demo = true)
    }

    private fun login(email: String, pass: String): AuthRepository =
        AuthRepository(demo = true).also { assertTrue(runBlocking { it.login(email, pass) }.isSuccess) }

    @Test fun `usuario comum nao altera ocorrencia alheia`() = runBlocking {
        val auth = login("morador@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        val antes = repo.listar().getOrThrow().first { it.id == "cassia_1" }
        assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isFailure)
        assertTrue(repo.deletar("cassia_1").isFailure)
        assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -46.0).isFailure)
        assertEquals(antes, repo.listar().getOrThrow().first { it.id == "cassia_1" })
    }

    @Test fun `autor altera exclui e reposiciona`() = runBlocking {
        val auth = login("morador@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        assertTrue(repo.inserir(Ocorrencia(tipo = "Suspeita", userId = auth.currentUid!!)).isSuccess)
        val id = repo.listar().getOrThrow().last().id
        assertTrue(repo.atualizarSituacaoComStatus(id, "rejeitado", "resolvido").isSuccess)
        assertTrue(repo.atualizarLocalizacao(id, -20.5, -46.9).isSuccess)
        val o = repo.obterPorId(id)!!
        assertEquals(-20.5, o.lat, 0.0)
        assertEquals(-46.9, o.lng, 0.0)
        assertTrue(repo.deletarComMidia(id).isSuccess)
        assertFalse(repo.listar().getOrThrow().any { it.id == id })
    }

    @Test fun `admin altera exclui e reposiciona`() = runBlocking {
        val auth = login("admin@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isSuccess)
        assertTrue(repo.atualizarLocalizacao("cassia_1", -20.5, -46.9).isSuccess)
        assertTrue(repo.deletarComMidia("cassia_1").isSuccess)
        assertFalse(repo.listar().getOrThrow().any { it.id == "cassia_1" })
    }

    @Test fun `localizacao fora da faixa e negada`() = runBlocking {
        val auth = login("admin@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        val antes = repo.obterPorId("cassia_1")!!
        assertTrue(repo.atualizarLocalizacao("cassia_1", 91.0, -46.9).isFailure)
        assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -181.0).isFailure)
        assertEquals(antes.lat, repo.obterPorId("cassia_1")!!.lat, 0.0)
        assertEquals(antes.lng, repo.obterPorId("cassia_1")!!.lng, 0.0)
    }

    @Test fun `sem authRepository o repositorio nega`() = runBlocking {
        val repo = OcorrenciaRepository(demo = true)
        assertTrue(repo.atualizarSituacao("cassia_1", "rejeitado").isFailure)
        assertTrue(repo.resolver("cassia_1", "demo-admin").isFailure)
        assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -46.0).isFailure)
        assertTrue(repo.deletar("cassia_1").isFailure)
    }

    @Test fun `sem login o repositorio nega`() = runBlocking {
        val repo = OcorrenciaRepository(demo = true, authRepository = AuthRepository(demo = true))
        assertTrue(repo.deletar("cassia_1").isFailure)
    }

    @Test fun `id inexistente continua falhando`() = runBlocking {
        val auth = login("admin@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        val r = repo.atualizarSituacaoComStatus("nao_existe", "rejeitado", "resolvido")
        assertTrue(r.isFailure)
        assertEquals("Ocorrência não encontrada", r.exceptionOrNull()?.message)
    }

    @Test fun `admin altera exclui e reposiciona persiste`() = runBlocking {
        val auth = login("admin@demo.com", "Demo@123")
        val repo = OcorrenciaRepository(demo = true, authRepository = auth)
        assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isSuccess)
        assertEquals("rejeitado", repo.listar().getOrThrow().first { it.id == "cassia_1" }.situacao)
        assertEquals("resolvido", repo.listar().getOrThrow().first { it.id == "cassia_1" }.status)
    }
}
