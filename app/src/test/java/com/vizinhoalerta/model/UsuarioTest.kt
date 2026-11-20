package com.vizinhoalerta.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UsuarioTest {

    @Test
    fun `usuario com valores padrao`() {
        val usuario = Usuario()
        
        assertEquals("", usuario.id)
        assertEquals("", usuario.nome)
        assertEquals("", usuario.username)
        assertEquals("", usuario.email)
        assertEquals("", usuario.telefone)
        assertEquals("", usuario.estado)
        assertEquals("", usuario.cidade)
        assertEquals("", usuario.bairro)
        assertEquals("user", usuario.role)
    }

    @Test
    fun `isAdmin retorna false para usuario comum`() {
        val usuario = Usuario(role = "user")
        assertEquals(false, usuario.isAdmin)
    }

    @Test
    fun `isAdmin retorna true para admin`() {
        val usuario = Usuario(role = "admin")
        assertEquals(true, usuario.isAdmin)
    }

    @Test
    fun `isAdmin e case insensitive`() {
        val usuario = Usuario(role = "ADMIN")
        assertEquals(true, usuario.isAdmin)
    }
}