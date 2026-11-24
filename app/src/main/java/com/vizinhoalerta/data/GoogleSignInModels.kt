package com.vizinhoalerta.data

import com.vizinhoalerta.model.Usuario

data class GoogleSignInResult(
    val uid: String,
    val precisaCompletarPerfil: Boolean,
)

sealed interface GoogleCredentialResult {
    data class Sucesso(val idToken: String) : GoogleCredentialResult
    data object Cancelado : GoogleCredentialResult
    data class Erro(val mensagem: String) : GoogleCredentialResult
}

internal fun perfilIncompleto(usuario: Usuario?): Boolean =
    usuario == null || usuario.cidade.isBlank() || usuario.bairro.isBlank()
