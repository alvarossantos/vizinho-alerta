package com.vizinhoalerta.model

data class Ocorrencia(
    val id: String = "",
    val userId: String = "",
    val usuarioNome: String = "",
    val tipo: String = "", // Suspeita, Vandalismo, Briga, Animal perigoso, Iluminação danificada, Outra
    val descricao: String = "",
    val fotoUrl: String = "",
    val audioUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val bairro: String = "",
    val status: String = "aberto", // aberto | resolvido
    val createdAt: Long = System.currentTimeMillis()
)
