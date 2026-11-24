package com.vizinhoalerta.ui.mapa

import com.vizinhoalerta.model.Ocorrencia

enum class CategoriaFiltro {
    TODAS,
    SEGURANCA,
    INFRA,
    SUSPEITA
}

data class FiltroOcorrencias(
    val busca: String = "",
    val categoria: CategoriaFiltro = CategoriaFiltro.TODAS
) {
    fun match(o: Ocorrencia): Boolean {
        val matchesBusca = if (busca.isBlank()) {
            true
        } else {
            val query = busca.lowercase()
            o.tipo.lowercase().contains(query) ||
            o.bairro.lowercase().contains(query) ||
            o.cidade.lowercase().contains(query) ||
            o.descricao.lowercase().contains(query)
        }

        val matchesCategoria = when (categoria) {
            CategoriaFiltro.TODAS -> true
            CategoriaFiltro.INFRA -> o.tipo == "Iluminação danificada" || o.tipo == "Outra"
            CategoriaFiltro.SEGURANCA -> o.tipo == "Suspeita" || o.tipo == "Briga" || o.tipo == "Vandalismo" || o.tipo == "Animal perigoso"
            CategoriaFiltro.SUSPEITA -> o.tipo == "Suspeita"
        }

        return matchesBusca && matchesCategoria
    }
}