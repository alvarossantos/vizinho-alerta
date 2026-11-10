package com.vizinhoalerta.model

data class Usuario(
    val id: String = "",
    val nome: String = "",
    val username: String = "",
    val email: String = "",
    val telefone: String = "",
    val estado: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val role: String = "user" // "user" | "admin"
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
}
