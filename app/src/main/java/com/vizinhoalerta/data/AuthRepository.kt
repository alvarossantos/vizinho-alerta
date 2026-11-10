package com.vizinhoalerta.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
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
            val emailTrimmed = email.trim()
            val result = auth.signInWithEmailAndPassword(emailTrimmed, pass).await()
            val user = result.user ?: return Result.failure(Exception("Usuário não encontrado"))
            Result.success(user)
        } catch (e: Exception) {
            val msg = formatAuthException(e)
            Result.failure(Exception(msg))
        }
    }

    fun validarSenhaSegura(senha: String): String? {
        if (senha.length < 8) {
            return "A senha deve ter no mínimo 8 caracteres."
        }
        if (!senha.any { it.isUpperCase() }) {
            return "A senha deve conter pelo menos uma letra maiúscula."
        }
        if (!senha.any { it.isDigit() }) {
            return "A senha deve conter pelo menos um número."
        }
        if (!senha.any { !it.isLetterOrDigit() }) {
            return "A senha deve conter pelo menos um caractere especial (!@#$...). "
        }
        return null
    }

    suspend fun isUsernameDisponivel(username: String, currentUid: String? = null): Boolean {
        return try {
            val cleanUsername = if (username.startsWith("@")) username.trim().lowercase() else "@${username.trim().lowercase()}"
            val query = firestore.collection("usuarios")
                .whereEqualTo("username", cleanUsername)
                .get()
                .await()

            if (query.isEmpty) return true
            if (currentUid != null && query.documents.size == 1 && query.documents[0].id == currentUid) {
                return true
            }
            false
        } catch (e: Exception) {
            true
        }
    }

    suspend fun cadastrar(
        nome: String,
        email: String,
        pass: String,
        estado: String = "",
        cidade: String,
        bairro: String,
        telefone: String = "",
        isAdmin: Boolean = false
    ): Result<FirebaseUser> {
        return try {
            val senhaErro = validarSenhaSegura(pass)
            if (senhaErro != null) {
                return Result.failure(Exception(senhaErro))
            }

            val emailTrimmed = email.trim()
            
            // Gera username único
            val baseUsername = "@" + emailTrimmed.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")
            var username = baseUsername + (100..999).random()
            while (!isUsernameDisponivel(username)) {
                username = baseUsername + (1000..9999).random()
            }

            val result = auth.createUserWithEmailAndPassword(emailTrimmed, pass).await()
            val user = result.user ?: return Result.failure(Exception("Falha ao criar usuário"))

            // Atualiza o nome de exibição no Firebase Auth
            val profileUpdates = userProfileChangeRequest {
                displayName = nome.trim()
            }
            user.updateProfile(profileUpdates).await()

            val role = if (isAdmin) "admin" else "user"
            val usuario = Usuario(
                id = user.uid,
                nome = nome.trim(),
                username = username,
                email = emailTrimmed,
                telefone = telefone.trim(),
                estado = estado.trim(),
                cidade = cidade.trim(),
                bairro = bairro.trim(),
                role = role
            )

            firestore.collection("usuarios")
                .document(user.uid)
                .set(usuario)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            val msg = formatAuthException(e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Falha ao autenticar com o Google"))

            val docRef = firestore.collection("usuarios").document(user.uid)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val nomeUser = user.displayName ?: "Morador"
                val emailUser = user.email ?: ""
                var username = "@" + emailUser.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "") + (100..999).random()
                while (!isUsernameDisponivel(username)) {
                    username = "@" + emailUser.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "") + (1000..9999).random()
                }
                val usuario = Usuario(
                    id = user.uid,
                    nome = nomeUser,
                    username = username,
                    email = emailUser
                )
                docRef.set(usuario).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthException(e)))
        }
    }

    suspend fun atualizarPerfil(
        nome: String,
        username: String,
        telefone: String,
        estado: String,
        cidade: String,
        bairro: String
    ): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Usuário não logado"))

            val cleanUsername = if (username.startsWith("@")) username.trim().lowercase() else "@${username.trim().lowercase()}"
            val disponivel = isUsernameDisponivel(cleanUsername, user.uid)
            if (!disponivel) {
                return Result.failure(Exception("Este username ($cleanUsername) já está em uso por outro usuário."))
            }

            val profileUpdates = userProfileChangeRequest {
                displayName = nome.trim()
            }
            user.updateProfile(profileUpdates).await()

            val updates = mapOf(
                "nome" to nome.trim(),
                "username" to cleanUsername,
                "telefone" to telefone.trim(),
                "estado" to estado.trim(),
                "cidade" to cidade.trim(),
                "bairro" to bairro.trim()
            )

            firestore.collection("usuarios")
                .document(user.uid)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun alterarSenha(novaSenha: String): Result<Unit> {
        return try {
            val senhaErro = validarSenhaSegura(novaSenha)
            if (senhaErro != null) {
                return Result.failure(Exception(senhaErro))
            }

            val user = auth.currentUser ?: return Result.failure(Exception("Usuário não logado"))
            user.updatePassword(novaSenha).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthException(e)))
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

    private fun formatAuthException(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("The email address is badly formatted", ignoreCase = true) ->
                "O formato do e-mail é inválido. Verifique se há espaços ou erros."
            message.contains("Password should be at least", ignoreCase = true) || message.contains("weak-password", ignoreCase = true) ->
                "A senha deve conter no mínimo 8 caracteres, letra maiúscula, número e caractere especial."
            message.contains("email-already-in-use", ignoreCase = true) || message.contains("already in use", ignoreCase = true) ->
                "Este e-mail já está cadastrado."
            message.contains("user-not-found", ignoreCase = true) || message.contains("no user record", ignoreCase = true) ->
                "Usuário não encontrado. Verifique o e-mail digitado."
            message.contains("wrong-password", ignoreCase = true) || message.contains("invalid-credential", ignoreCase = true) ->
                "E-mail ou senha incorretos."
            message.contains("PERMISSION_DENIED", ignoreCase = true) || message.contains("Cloud Firestore", ignoreCase = true) ->
                "Erro de permissão no Firestore. Verifique se o Cloud Firestore está criado e ativado no console do Firebase."
            else -> e.localizedMessage ?: message.ifBlank { "Ocorreu um erro na autenticação." }
        }
    }
}
