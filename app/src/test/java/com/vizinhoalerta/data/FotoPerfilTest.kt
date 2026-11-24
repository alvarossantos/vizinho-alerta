package com.vizinhoalerta.data

import com.vizinhoalerta.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FotoPerfilTest {

    @Test
    fun `aceita jpeg e png ate 2MB`() {
        assertNull(validarImagemPerfil("image/jpeg", 2 * 1024 * 1024))
        assertNull(validarImagemPerfil("image/png", 1024))
    }

    @Test
    fun `rejeita tipo diferente de jpg ou png`() {
        assertEquals("Use JPG ou PNG.", validarImagemPerfil("image/gif", 1024))
        assertEquals("Use JPG ou PNG.", validarImagemPerfil(null, 1024))
    }

    @Test
    fun `rejeita acima de 2MB`() {
        assertEquals("A foto deve ter ate 2 MB.", validarImagemPerfil("image/png", 2 * 1024 * 1024 + 1))
    }

    @Test
    fun `caminho demo e estavel por uid`() {
        assertEquals("perfil/demo-morador.jpg", caminhoFotoPerfilDemo("demo-morador"))
    }

    @Test
    fun `mescla foto sem perder dados do usuario`() {
        val u = Usuario(id = "u1", nome = "Ana", cidade = "Cassia")
        assertEquals("/data/ana.jpg", mesclarFotoDemo(u, "/data/ana.jpg").fotoUrl)
        assertEquals("Ana", mesclarFotoDemo(u, "/data/ana.jpg").nome)
        assertEquals("", mesclarFotoDemo(u, null).fotoUrl)
    }
}