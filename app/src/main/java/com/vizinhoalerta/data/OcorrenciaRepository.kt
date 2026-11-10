package com.vizinhoalerta.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vizinhoalerta.model.Ocorrencia
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class OcorrenciaRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

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
                        estado = "SP",
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
                        estado = "SP",
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
                        estado = "MG",
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

    suspend fun inserir(ocorrencia: Ocorrencia, fotoFile: File? = null, audioFile: File? = null): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            var fotoUrl = ocorrencia.fotoUrl
            var audioUrl = ocorrencia.audioUrl

            if (fotoFile != null && fotoFile.exists()) {
                val fotoRef = storage.reference.child("fotos/$id.jpg")
                fotoRef.putFile(Uri.fromFile(fotoFile)).await()
                fotoUrl = fotoRef.downloadUrl.await().toString()
            }

            if (audioFile != null && audioFile.exists()) {
                val audioRef = storage.reference.child("audios/$id.m4a")
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolver(id: String): Result<Unit> {
        return try {
            firestore.collection("ocorrencias")
                .document(id)
                .update("status", "resolvido")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
