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
