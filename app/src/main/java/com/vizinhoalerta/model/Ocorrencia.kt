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
    val situacao: String = "registrado", // registrado | em_verificacao | confirmado | resolvido | rejeitado
    val cidade: String = "",
    val bairro: String = "",
    val status: String = "aberto", // aberto | resolvido
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SITUACAO_REGISTRADO = "registrado"
        const val SITUACAO_EM_VERIFICACAO = "em_verificacao"
        const val SITUACAO_CONFIRMADO = "confirmado"
        const val SITUACAO_RESOLVIDO = "resolvido"
        const val SITUACAO_REJEITADO = "rejeitado"
    }
}

fun rotuloSituacao(situacao: String): String = when (situacao) {
    Ocorrencia.SITUACAO_REGISTRADO -> "Registrado"
    Ocorrencia.SITUACAO_EM_VERIFICACAO -> "Em verificação"
    Ocorrencia.SITUACAO_CONFIRMADO -> "Confirmado"
    Ocorrencia.SITUACAO_RESOLVIDO -> "Resolvido"
    Ocorrencia.SITUACAO_REJEITADO -> "Rejeitado"
    else -> situacao.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
