package com.vizinhoalerta.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.vizinhoalerta.model.Usuario
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return Result.failure(Exception("Usuário não encontrado"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cadastrar(nome: String, email: String, pass: String, cidade: String, bairro: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return Result.failure(Exception("Falha ao criar usuário"))

            val usuario = Usuario(
                id = user.uid,
                nome = nome,
                email = email,
                cidade = cidade,
                bairro = bairro
            )

            firestore.collection("usuarios")
                .document(user.uid)
                .set(usuario)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDadosUsuario(uid: String): Usuario? {
        return try {
            val doc = firestore.collection("usuarios").document(uid).get().await()
            doc.toObject(Usuario::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
    }
}
