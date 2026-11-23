package com.vizinhoalerta.data

import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.model.Usuario

/** Dados locais usados apenas no modo demo (debug sem Firebase). */
object DemoData {
    const val ADMIN_EMAIL = "admin@demo.com"
    const val SENHA_DEMO = "Demo@123"

    val usuarios: MutableList<Usuario> = mutableListOf()
    val ocorrencias: MutableList<Ocorrencia> = mutableListOf()

    init { reset() }

    fun reset() {
        usuarios.clear()
        usuarios.addAll(
            listOf(
                Usuario(
                    id = "demo-admin",
                    nome = "Admin Demo",
                    username = "@admin",
                    email = ADMIN_EMAIL,
                    telefone = "(35) 99999-0001",
                    estado = "MG",
                    cidade = "Cássia",
                    bairro = "Centro",
                    role = "admin"
                ),
                Usuario(
                    id = "demo-morador",
                    nome = "Morador Demo",
                    username = "@morador",
                    email = "morador@demo.com",
                    telefone = "(35) 99999-0002",
                    estado = "MG",
                    cidade = "Cássia",
                    bairro = "Centro",
                    role = "user"
                )
            )
        )

        ocorrencias.clear()
        ocorrencias.addAll(
            listOf(
                ocorrenciaDemo("cassia_1", "Iluminação danificada", "Poste apagado na Av. Presidente Kubitschek.", -20.5825, -46.9219, "Cássia", "Centro", Ocorrencia.SITUACAO_REGISTRADO),
                ocorrenciaDemo("cassia_2", "Suspeita", "Movimento suspeito próximo à praça central.", -20.5852, -46.9205, "Cássia", "Centro", Ocorrencia.SITUACAO_EM_VERIFICACAO),
                ocorrenciaDemo("cassia_3", "Animal perigoso", "Cachorro solto circulando pelo bairro.", -20.5840, -46.9230, "Cássia", "Centro", Ocorrencia.SITUACAO_CONFIRMADO),
                ocorrenciaDemo("passos_1", "Vandalismo", "Pichação em patrimônio público no centro.", -20.7160, -46.6150, "Passos", "Centro", Ocorrencia.SITUACAO_RESOLVIDO),
                ocorrenciaDemo("passos_2", "Iluminação danificada", "Ponta de rua sem funcionamento na região.", -20.7180, -46.6170, "Passos", "Jardim", Ocorrencia.SITUACAO_REJEITADO)
            )
        )
    }

    private fun ocorrenciaDemo(
        id: String,
        tipo: String,
        descricao: String,
        lat: Double,
        lng: Double,
        cidade: String,
        bairro: String,
        situacao: String
    ) = Ocorrencia(
        id = id,
        userId = "demo-admin",
        usuarioNome = "Admin Demo",
        tipo = tipo,
        descricao = descricao,
        lat = lat,
        lng = lng,
        situacao = situacao,
        cidade = cidade,
        bairro = bairro,
        status = if (situacao == Ocorrencia.SITUACAO_RESOLVIDO || situacao == Ocorrencia.SITUACAO_REJEITADO) "resolvido" else "aberto"
    )
}
