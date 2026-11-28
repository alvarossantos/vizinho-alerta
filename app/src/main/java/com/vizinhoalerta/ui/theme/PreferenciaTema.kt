package com.vizinhoalerta.ui.theme

import android.content.Context

enum class PreferenciaTema {
    SISTEMA, CLARO, ESCURO
}

fun temaEfetivo(preferencia: PreferenciaTema, modoNoturnoSistema: Boolean): Boolean =
    when (preferencia) {
        PreferenciaTema.SISTEMA -> modoNoturnoSistema
        PreferenciaTema.CLARO -> false
        PreferenciaTema.ESCURO -> true
    }

object RepositorioTema {
    private const val NOME = "tema_prefs"
    private const val CHAVE = "preferencia_tema"

    fun ler(context: Context): PreferenciaTema {
        val nome = context.getSharedPreferences(NOME, Context.MODE_PRIVATE)
            .getString(CHAVE, null) ?: return PreferenciaTema.SISTEMA
        return PreferenciaTema.entries.firstOrNull { it.name == nome } ?: PreferenciaTema.SISTEMA
    }

    fun salvar(context: Context, preferencia: PreferenciaTema) {
        context.getSharedPreferences(NOME, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE, preferencia.name)
            .apply()
    }
}
