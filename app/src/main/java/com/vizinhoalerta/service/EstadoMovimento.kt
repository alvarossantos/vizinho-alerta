package com.vizinhoalerta.service

import kotlin.math.abs

class EstadoMovimento {

    private var ultimoMovimentoMs: Long? = null
    private var avisoVisivel = false

    fun atualizar(valores: FloatArray, agoraMs: Long): Boolean {
        val x = abs(valores[0])
        val y = abs(valores[1])
        val z = abs(valores[2])

        if (x >= LIMIAR_RAD_S || y >= LIMIAR_RAD_S || z >= LIMIAR_RAD_S) {
            ultimoMovimentoMs = agoraMs
            avisoVisivel = true
            return true
        }

        val ultimo = ultimoMovimentoMs
        if (avisoVisivel && ultimo != null && agoraMs - ultimo >= INTERVALO_ESTABILIDADE_MS) {
            avisoVisivel = false
        }
        return avisoVisivel
    }

    fun reiniciar() {
        ultimoMovimentoMs = null
        avisoVisivel = false
    }

    companion object {
        const val LIMIAR_RAD_S = 0.15f
        const val INTERVALO_ESTABILIDADE_MS = 500L
    }
}
