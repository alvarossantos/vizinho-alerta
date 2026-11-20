package com.vizinhoalerta.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vizinhoalerta.model.Ocorrencia
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class OcorrenciaRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    suspend fun seedOcorrenciasDeTeste() {
        try {
            val snapshot = firestore.collection("ocorrencias").limit(1).get().await()
            if (snapshot.isEmpty) {
                val exemplos = listOf(
                    Ocorrencia(
                        id = "teste_1",
                        tipo = "Iluminação danificada",
                        descricao = "Poste de luz apagado na rua principal há 3 dias.",
                        lat = -23.55052,
                        lng = -46.633308,
                        situacao = "confirmado",
                        cidade = "São Paulo",
                        bairro = "Centro",
                        status = "aberto",
                        createdAt = System.currentTimeMillis()
                    ),
                    Ocorrencia(
                        id = "teste_2",
                        tipo = "Suspeita",
                        descricao = "Movimento estranho de veículo próximo à praça.",
                        lat = -23.55552,
                        lng = -46.643308,
                        situacao = "em_verificacao",
                        cidade = "São Paulo",
                        bairro = "Consolação",
                        status = "aberto",
                        createdAt = System.currentTimeMillis() - 86400000
                    ),
                    Ocorrencia(
                        id = "teste_3",
                        tipo = "Vandalismo",
                        descricao = "Pichação em patrimônio público na praça central.",
                        lat = -19.91668,
                        lng = -43.93449,
                        situacao = "resolvido",
                        cidade = "Belo Horizonte",
                        bairro = "Savassi",
                        status = "resolvido",
                        createdAt = System.currentTimeMillis() - 172800000
                    )
                )
                for (ex in exemplos) {
                    firestore.collection("ocorrencias").document(ex.id).set(ex).await()
                }
            }
        } catch (e: Exception) {
            // Ignora erro de seed
        }
    }

    fun observarOcorrencias(): Flow<List<Ocorrencia>> = callbackFlow {
        val listener = firestore.collection("ocorrencias")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ocorrencias = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(ocorrencias)
            }
        awaitClose { listener.remove() }
    }

    suspend fun listar(): List<Ocorrencia> {
        return try {
            val snapshot = firestore.collection("ocorrencias")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun obterPorId(id: String): Ocorrencia? {
        return try {
            val doc = firestore.collection("ocorrencias").document(id).get().await()
            doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun inserir(
        ocorrencia: Ocorrencia,
        fotoFile: File? = null,
        audioFile: File? = null
    ): Result<Unit> {
        return tryWithRetry {
            val id = UUID.randomUUID().toString()
            val userId = ocorrencia.userId
            var fotoUrl = ocorrencia.fotoUrl
            var audioUrl = ocorrencia.audioUrl

            if (fotoFile != null && fotoFile.exists() && userId.isNotBlank()) {
                val fotoRef = storage.reference.child("fotos/$userId/$id.jpg")
                fotoRef.putFile(Uri.fromFile(fotoFile)).await()
                fotoUrl = fotoRef.downloadUrl.await().toString()
            }

            if (audioFile != null && audioFile.exists() && userId.isNotBlank()) {
                val audioRef = storage.reference.child("audios/$userId/$id.m4a")
                audioRef.putFile(Uri.fromFile(audioFile)).await()
                audioUrl = audioRef.downloadUrl.await().toString()
            }

            val novaOcorrencia = ocorrencia.copy(
                id = id,
                fotoUrl = fotoUrl,
                audioUrl = audioUrl
            )

            firestore.collection("ocorrencias")
                .document(id)
                .set(novaOcorrencia)
                .await()

            Result.success(Unit)
        }
    }

    suspend fun resolver(id: String, userId: String): Result<Unit> {
        return tryWithRetry {
            // Deve ser chamado pelo autor - atualiza status e situacao juntos
            firestore.collection("ocorrencias")
                .document(id)
                .update("status", "resolvido", "situacao", "resolvido")
                .await()
            Result.success(Unit)
        }
    }

    suspend fun atualizarSituacao(id: String, situacao: String): Result<Unit> {
        return tryWithRetry {
            firestore.collection("ocorrencias")
                .document(id)
                .update("situacao", situacao)
                .await()
            Result.success(Unit)
        }
    }

    suspend fun atualizarSituacaoComStatus(id: String, situacao: String, status: String): Result<Unit> {
        return tryWithRetry {
            firestore.collection("ocorrencias")
                .document(id)
                .update("situacao", situacao, "status", status)
                .await()
            Result.success(Unit)
        }
    }

    suspend fun deletar(id: String): Result<Unit> {
        return tryWithRetry {
            firestore.collection("ocorrencias")
                .document(id)
                .delete()
                .await()
            Result.success(Unit)
        }
    }

    /**
     * Deleta uma ocorrência e seus arquivos de mídia associados
     */
    suspend fun deletarComMidia(ocorrenciaId: String): Result<Unit> {
        return tryWithRetry {
            // Primeiro, obter a ocorrência para pegar os URLs de mídia
            val doc = firestore.collection("ocorrencias").document(ocorrenciaId).get().await()
            val ocorrencia = doc.toObject(Ocorrencia::class.java)
            
            // Deletar arquivos no Storage se existentes
            ocorrencia?.let { corr ->
                if (corr.fotoUrl.isNotBlank()) {
                    val fotoPath = extractStoragePath(corr.fotoUrl)
                    if (fotoPath != null) {
                        storage.reference.child(fotoPath).delete().await()
                    }
                }
                if (corr.audioUrl.isNotBlank()) {
                    val audioPath = extractStoragePath(corr.audioUrl)
                    if (audioPath != null) {
                        storage.reference.child(audioPath).delete().await()
                    }
                }
            }
            
            // Deletar documento
            firestore.collection("ocorrencias").document(ocorrenciaId).delete().await()
            Result.success(Unit)
        }
    }

    private fun extractStoragePath(fullUrl: String): String? {
        // Extrai o caminho do Storage de uma URL completa
        // Formato: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}?...
        return try {
            val regex = """/o/([^?]+)""".toRegex()
            val match = regex.find(fullUrl)
            match?.groupValues?.get(1)?.let { 
                java.net.URLDecoder.decode(it, "UTF-8")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executa uma operação com retry exponencial em caso de falha de rede
     */
    private suspend fun <T> tryWithRetry(
        operation: suspend () -> Result<T>,
        maxRetries: Int = MAX_RETRIES,
        delayMs: Long = RETRY_DELAY_MS
    ): Result<T> {
        var attempt = 0
        var lastError: Exception? = null
        
        while (attempt < maxRetries) {
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                }
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }
            attempt++
            if (attempt < maxRetries) {
                delay(delayMs * (1L shl (attempt - 1))) // Exponencial: 1000, 2000, 4000
            }
        }
        
        return Result.failure(lastError!!)
    }
}
