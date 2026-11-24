package com.vizinhoalerta.ui.mapa

import com.vizinhoalerta.model.Ocorrencia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroOcorrenciasTest {

    private val iluminacao = Ocorrencia(
        id = "1",
        tipo = "Iluminação danificada",
        descricao = "Poste apagado na Av. Presidente Kubitschek.",
        bairro = "Centro",
        cidade = "Cássia"
    )
    private val suspeita = Ocorrencia(
        id = "2",
        tipo = "Suspeita",
        descricao = "Movimento suspeito",
        bairro = "Centro",
        cidade = "Cássia"
    )

    @Test
    fun `filtro padrao aceita todas`() {
        assertTrue(FiltroOcorrencias().match(iluminacao))
        assertTrue(FiltroOcorrencias().match(suspeita))
    }

    @Test
    fun `busca ignora caixa e procura descricao`() {
        assertTrue(FiltroOcorrencias(busca = "kubitschek").match(iluminacao))
        assertFalse(FiltroOcorrencias(busca = "KUBITSCHEK").match(suspeita))
    }

    @Test
    fun `categoria infra so aceita iluminacao e outra`() {
        assertTrue(FiltroOcorrencias(categoria = CategoriaFiltro.INFRA).match(iluminacao))
        assertFalse(FiltroOcorrencias(categoria = CategoriaFiltro.INFRA).match(suspeita))
    }

    @Test
    fun `categoria seguranca so aceita tipos de seguranca`() {
        assertTrue(FiltroOcorrencias(categoria = CategoriaFiltro.SEGURANCA).match(Ocorrencia(tipo = "Briga")))
        assertTrue(FiltroOcorrencias(categoria = CategoriaFiltro.SEGURANCA).match(Ocorrencia(tipo = "Vandalismo")))
        assertTrue(FiltroOcorrencias(categoria = CategoriaFiltro.SEGURANCA).match(Ocorrencia(tipo = "Animal perigoso")))
        assertTrue(FiltroOcorrencias(categoria = CategoriaFiltro.SEGURANCA).match(suspeita)) // Suspeita está em SEGURANCA per brief
        assertFalse(FiltroOcorrencias(categoria = CategoriaFiltro.SEGURANCA).match(iluminacao))
    }
}