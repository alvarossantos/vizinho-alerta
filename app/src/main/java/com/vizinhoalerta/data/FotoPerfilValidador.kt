package com.vizinhoalerta.data

import com.vizinhoalerta.model.Usuario

internal fun validarImagemPerfil(mime: String?, sizeBytes: Int): String? {
    val allowedMimes = setOf("image/jpeg", "image/png")
    if (mime == null || mime !in allowedMimes) {
        return "Use JPG ou PNG."
    }
    if (sizeBytes > 2 * 1024 * 1024) {
        return "A foto deve ter ate 2 MB."
    }
    return null
}

internal fun caminhoFotoPerfilDemo(uid: String): String = "perfil/$uid.jpg"

internal fun mesclarFotoDemo(usuario: Usuario, fotoUrl: String?): Usuario {
    return usuario.copy(fotoUrl = fotoUrl ?: "")
}