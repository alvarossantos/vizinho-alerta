package com.vizinhoalerta.data

import com.vizinhoalerta.model.Ocorrencia

// TODO: ligar ao Firestore — coleção "ocorrencias"
// ponytail: repository acessa Firestore + Storage (foto/audio)
class OcorrenciaRepository {
    suspend fun listar(): List<Ocorrencia> { TODO("implementar") }
    suspend fun inserir(ocorrencia: Ocorrencia) { TODO("implementar") }
    suspend fun resolver(id: String) { TODO("implementar") }
}
