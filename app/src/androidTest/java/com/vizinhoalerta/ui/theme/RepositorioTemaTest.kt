package com.vizinhoalerta.ui.theme

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RepositorioTemaTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun limpar() {
        context.getSharedPreferences("tema_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun salvar_e_ler() {
        RepositorioTema.salvar(context, PreferenciaTema.ESCURO)
        assertEquals(PreferenciaTema.ESCURO, RepositorioTema.ler(context))
    }

    @Test
    fun preferencia_padrao_quando_ausente() {
        assertEquals(PreferenciaTema.SISTEMA, RepositorioTema.ler(context))
    }

    @Test
    fun valor_invalido_volta_para_sistema() {
        context.getSharedPreferences("tema_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("preferencia_tema", "luz")
            .commit()
        assertEquals(PreferenciaTema.SISTEMA, RepositorioTema.ler(context))
    }
}
