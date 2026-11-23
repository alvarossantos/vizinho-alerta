package com.vizinhoalerta.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryDemoTest {
    private lateinit var repo: AuthRepository

    @Before fun setUp() {
        DemoData.reset()
        repo = AuthRepository(demo = true)
    }

    @Test fun `login admin demo`() = runBlocking {
        assertTrue(repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO).isSuccess)
        assertEquals("demo-admin", repo.currentUid)
        assertTrue(repo.isAdmin())
    }

    @Test fun `cadastro demo valida senha e cria usuario comum`() = runBlocking {
        assertTrue(repo.cadastrar("Novo", "novo@demo.com", "fraca", cidade = "Cássia", bairro = "Centro").isFailure)
        assertTrue(repo.cadastrar("Novo", "novo@demo.com", "Demo@123", cidade = "Cássia", bairro = "Centro").isSuccess)
        assertTrue(repo.listarUsuarios().any { it.email == "novo@demo.com" && !it.isAdmin })
    }

    @Test fun `atualizar perfil demo persiste e bloqueia username duplicado`() = runBlocking {
        repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
        assertTrue(repo.atualizarPerfil("Admin Novo", "@morador", "", "", "Passos", "Centro").isFailure)
        assertTrue(repo.atualizarPerfil("Admin Novo", "@admin2", "", "", "Passos", "Centro").isSuccess)
        assertEquals("Passos", repo.getDadosUsuario("demo-admin")?.cidade)
    }

    @Test fun `logout demo restaura dados padrao`() = runBlocking {
        repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
        assertTrue(repo.atualizarPerfil("Admin Novo", "@admin2", "", "", "Passos", "Centro").isSuccess)

        repo.logout()

        assertEquals(null, repo.currentUid)
        val admin = repo.getDadosUsuario("demo-admin")
        assertEquals("Admin Demo", admin?.nome)
        assertEquals("Cássia", admin?.cidade)
        assertEquals(2, repo.listarUsuarios().size)
    }

    @Test fun `username disponivel permite edicao`() {
        assertEquals(null, erroDisponibilidadeUsername(true, "@novo"))
    }

    @Test fun `username duplicado e rejeitado na edicao`() {
        assertEquals(
            "Este username (@morador) já está em uso por outro usuário.",
            erroDisponibilidadeUsername(false, "@morador")
        )
    }

    @Test fun `falha ao consultar username nao e tratada como disponivel`() {
        val erro = erroDisponibilidadeUsername(null, "@novo")
        assertTrue(erro != null && erro.contains("Não foi possível validar"))
    }
}
