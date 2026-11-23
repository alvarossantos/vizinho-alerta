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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class OcorrenciaRepository(
    private val demo: Boolean = false,
    private val authRepository: AuthRepository? = null,
    private val firestore: FirebaseFirestore? = if (demo) null else FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage? = if (demo) null else FirebaseStorage.getInstance()
) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    suspend fun seedOcorrenciasDeTeste() {
        if (demo) return // fixture já vem populada
        try {
            val snapshot = firestore!!.collection("ocorrencias").limit(1).get().await()
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
                    firestore!!.collection("ocorrencias").document(ex.id).set(ex).await()
                }
            }
        } catch (e: Exception) {
            // Ignora erro de seed
        }
    }

    fun observarOcorrencias(): Flow<List<Ocorrencia>> {
        if (demo) return flow { emit(DemoData.ocorrencias.toList()) }
        return callbackFlow {
        val listener = firestore!!.collection("ocorrencias")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Ex.: logout dispara PERMISSION_DENIED. Não fechar o flow com erro:
                    // isso derrubaria a coroutine que coleta o estado da UI.
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val ocorrencias = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(ocorrencias)
            }
            awaitClose { listener.remove() }
        }
    }

    private fun exigirPermissaoDemo(id: String): Result<Unit> {
        val index = DemoData.ocorrencias.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(Exception("Ocorrência não encontrada"))
        val ocorrencia = DemoData.ocorrencias[index]
        val uid = authRepository?.currentUid
        val admin = DemoData.usuarios.firstOrNull { it.id == uid }?.isAdmin == true
        if (uid == null || (ocorrencia.userId != uid && !admin)) {
            return Result.failure(Exception("Você não tem permissão para alterar esta ocorrência."))
        }
        return Result.success(Unit)
    }

    suspend fun listar(): Result<List<Ocorrencia>> {
        if (demo) return Result.success(DemoData.ocorrencias.toList())
        return try {
            val snapshot = firestore!!.collection("ocorrencias")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Result.success(snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ocorrencia::class.java)?.copy(id = doc.id)
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obterPorId(id: String): Ocorrencia? {
        if (demo) return DemoData.ocorrencias.firstOrNull { it.id == id }
        return try {
            val doc = firestore!!.collection("ocorrencias").document(id).get().await()
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
        if (demo) {
            DemoData.ocorrencias.add(ocorrencia.copy(id = UUID.randomUUID().toString()))
            return Result.success(Unit)
        }
        return tryWithRetry {
            val id = UUID.randomUUID().toString()
            val userId = ocorrencia.userId
            var fotoUrl = ocorrencia.fotoUrl
            var audioUrl = ocorrencia.audioUrl

            if (fotoFile != null && fotoFile.exists() && userId.isNotBlank()) {
                val fotoRef = storage!!.reference.child("fotos/$userId/$id.jpg")
                fotoRef.putFile(Uri.fromFile(fotoFile)).await()
                fotoUrl = fotoRef.downloadUrl.await().toString()
            }

            if (audioFile != null && audioFile.exists() && userId.isNotBlank()) {
                val audioRef = storage!!.reference.child("audios/$userId/$id.m4a")
                audioRef.putFile(Uri.fromFile(audioFile)).await()
                audioUrl = audioRef.downloadUrl.await().toString()
            }

            val novaOcorrencia = ocorrencia.copy(
                id = id,
                fotoUrl = fotoUrl,
                audioUrl = audioUrl
            )

            firestore!!.collection("ocorrencias")
                .document(id)
                .set(novaOcorrencia)
                .await()

            Result.success(Unit)
        }
    }

    suspend fun resolver(id: String, userId: String): Result<Unit> {
        if (demo) {
            val perm = exigirPermissaoDemo(id)
            if (perm.isFailure) return perm
            return atualizarDemo(id) { it.copy(situacao = "resolvido", status = "resolvido") }
        }
        return tryWithRetry {
            // Deve ser chamado pelo autor - atualiza status e situacao juntos
            firestore!!.collection("ocorrencias")
                .document(id)
                .update("status", "resolvido", "situacao", "resolvido")
                .await()
            Result.success(Unit)
        }
    }

    suspend fun atualizarSituacao(id: String, situacao: String): Result<Unit> {
        if (demo) {
            val perm = exigirPermissaoDemo(id)
            if (perm.isFailure) return perm
            return atualizarDemo(id) { it.copy(situacao = situacao) }
        }
        return tryWithRetry {
            firestore!!.collection("ocorrencias")
                .document(id)
                .update("situacao", situacao)
                .await()
            Result.success(Unit)
        }
    }

    suspend fun atualizarSituacaoComStatus(id: String, situacao: String, status: String): Result<Unit> {
        if (demo) {
            val perm = exigirPermissaoDemo(id)
            if (perm.isFailure) return perm
            return atualizarDemo(id) { it.copy(situacao = situacao, status = status) }
        }
        return tryWithRetry {
            firestore!!.collection("ocorrencias")
                .document(id)
                .update("situacao", situacao, "status", status)
                .await()
            Result.success(Unit)
        }
    }

    suspend fun atualizarLocalizacao(id: String, lat: Double, lng: Double): Result<Unit> {
        val perm = exigirPermissaoDemo(id)
        if (perm.isFailure) return perm
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            return Result.failure(Exception("Coordenadas fora da faixa válida."))
        }
        if (demo) {
            val index = DemoData.ocorrencias.indexOfFirst { it.id == id }
            if (index < 0) return Result.failure(Exception("Ocorrência não encontrada"))
            DemoData.ocorrencias[index] = DemoData.ocorrencias[index].copy(lat = lat, lng = lng)
            return Result.success(Unit)
        }
        return tryWithRetry {
            firestore!!.collection("ocorrencias")
                .document(id)
                .update("lat", lat, "lng", lng)
                .await()
            Result.success(Unit)
        }
    }

    suspend fun deletar(id: String): Result<Unit> {
        if (demo) {
            val perm = exigirPermissaoDemo(id)
            if (perm.isFailure) return perm
            val index = DemoData.ocorrencias.indexOfFirst { it.id == id }
            if (index < 0) return Result.failure(Exception("Ocorrência não encontrada"))
            DemoData.ocorrencias.removeAt(index)
            return Result.success(Unit)
        }
        return tryWithRetry {
            firestore!!.collection("ocorrencias")
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
        if (demo) {
            val perm = exigirPermissaoDemo(ocorrenciaId)
            if (perm.isFailure) return perm
            return deletar(ocorrenciaId)
        }
        return tryWithRetry {
            // Primeiro, obter a ocorrência para pegar os URLs de mídia
            val doc = firestore!!.collection("ocorrencias").document(ocorrenciaId).get().await()
            val ocorrencia = doc.toObject(Ocorrencia::class.java)
            
            // Deletar arquivos no Storage se existentes
            ocorrencia?.let { corr ->
                if (corr.fotoUrl.isNotBlank()) {
                    val fotoPath = extractStoragePath(corr.fotoUrl)
                    if (fotoPath != null) {
                        storage!!.reference.child(fotoPath).delete().await()
                    }
                }
                if (corr.audioUrl.isNotBlank()) {
                    val audioPath = extractStoragePath(corr.audioUrl)
                    if (audioPath != null) {
                        storage!!.reference.child(audioPath).delete().await()
                    }
                }
            }
            
            // Deletar documento
            firestore!!.collection("ocorrencias").document(ocorrenciaId).delete().await()
            Result.success(Unit)
        }
    }

    private fun atualizarDemo(id: String, transform: (Ocorrencia) -> Ocorrencia): Result<Unit> {
        val index = DemoData.ocorrencias.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(Exception("Ocorrência não encontrada"))
        DemoData.ocorrencias[index] = transform(DemoData.ocorrencias[index])
        return Result.success(Unit)
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
     * Executa uma operação com retry exponencial em caso de falha de rede.
     * Propaga CancellationException e não repete em erros de permissão (PERMISSION_DENIED).
     */
    private suspend fun <T> tryWithRetry(
        maxRetries: Int = MAX_RETRIES,
        delayMs: Long = RETRY_DELAY_MS,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var attempt = 0
        var lastError: Throwable? = null
        
        while (attempt < maxRetries) {
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                }
                lastError = result.exceptionOrNull()
                
                // Erros permanentes não devem ser repetidos
                if (isPermanentError(lastError)) {
                    return Result.failure(lastError!!)
                }
            } catch (e: Exception) {
                // Propagar CancellationException (cancelamento de coroutine)
                if (e is java.util.concurrent.CancellationException || e is kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                }
                lastError = e
                
                // Erros de permissão são permanentes
                if (isPermanentError(e)) {
                    return Result.failure(e)
                }
            }
            attempt++
            if (attempt < maxRetries) {
                delay(delayMs * (1L shl (attempt - 1))) // Exponencial: 1000, 2000, 4000
            }
        }
        
        return Result.failure(lastError!!)
    }
    
    private fun isPermanentError(e: Throwable?): Boolean {
        if (e == null) return false
        val msg = e.message?.lowercase() ?: return false
        return msg.contains("permission_denied") || 
               msg.contains("unauthenticated") ||
               msg.contains("forbidden") ||
               msg.contains("invalid") // Erro de validação de regras
    }
}
