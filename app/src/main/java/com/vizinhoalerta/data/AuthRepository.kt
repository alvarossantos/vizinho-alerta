package com.vizinhoalerta.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.vizinhoalerta.model.Usuario
import kotlinx.coroutines.tasks.await

internal fun formatAuthException(e: Exception): String {
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
        message.contains("wrong-password", ignoreCase = true) || message.contains("invalid-credential", ignoreCase = true) ||
            message.contains("The password is invalid", ignoreCase = true) ->
            "E-mail ou senha incorretos."
        message.contains("PERMISSION_DENIED", ignoreCase = true) || message.contains("Cloud Firestore", ignoreCase = true) ->
            "Erro de permissão no Firestore. Verifique se o Cloud Firestore está criado e ativado no console do Firebase."
        else -> e.localizedMessage ?: message.ifBlank { "Ocorreu um erro na autenticação." }
    }
}

class AuthRepository(
    private val demo: Boolean = false,
    private val auth: FirebaseAuth? = if (demo) null else FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore? = if (demo) null else FirebaseFirestore.getInstance()
) {

    private var demoUid: String? = null

    val currentUser: FirebaseUser?
        get() = if (demo) null else auth?.currentUser

    val currentUid: String?
        get() = if (demo) demoUid else auth?.currentUser?.uid

    val currentNome: String
        get() = if (demo) {
            DemoData.usuarios.firstOrNull { it.id == demoUid }?.nome ?: "Morador"
        } else {
            auth?.currentUser?.displayName ?: "Morador"
        }

    suspend fun login(email: String, pass: String): Result<Unit> {
        if (demo) {
            val emailTrimmed = email.trim()
            if (emailTrimmed.isBlank()) return Result.failure(Exception("Informe o e-mail."))
            if (pass.length < 6) return Result.failure(Exception("A senha deve ter no mínimo 6 caracteres."))
            val usuario = DemoData.usuarios.firstOrNull { it.email.equals(emailTrimmed, ignoreCase = true) }
                ?: criarUsuarioDemo(emailTrimmed)
            demoUid = usuario.id
            return Result.success(Unit)
        }
        return try {
            val emailTrimmed = email.trim()
            val result = auth!!.signInWithEmailAndPassword(emailTrimmed, pass).await()
            result.user ?: return Result.failure(Exception("Usuário não encontrado"))
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = formatAuthException(e)
            Result.failure(Exception(msg))
        }
    }

    private fun criarUsuarioDemo(email: String): Usuario {
        val base = "@" + email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")
        var username = base + (100..999).random()
        while (DemoData.usuarios.any { it.username.equals(username, ignoreCase = true) }) {
            username = base + (1000..9999).random()
        }
        val usuario = Usuario(
            id = "demo-user-${DemoData.usuarios.size + 1}",
            nome = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            username = username,
            email = email,
            role = "user"
        )
        DemoData.usuarios.add(usuario)
        return usuario
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
        val cleanUsername = normalizarUsername(username)
        if (demo) {
            return DemoData.usuarios.none {
                it.username.equals(cleanUsername, ignoreCase = true) && it.id != currentUid
            }
        }
        return try {
            val query = firestore!!.collection("usuarios")
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
    ): Result<Unit> {
        val senhaErro = validarSenhaSegura(pass)
        if (senhaErro != null) return Result.failure(Exception(senhaErro))
        val emailTrimmed = email.trim()

        if (demo) {
            if (DemoData.usuarios.any { it.email.equals(emailTrimmed, ignoreCase = true) }) {
                return Result.failure(Exception("Este e-mail já está cadastrado."))
            }
            val base = "@" + emailTrimmed.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")
            var username = base + (100..999).random()
            while (!isUsernameDisponivel(username)) {
                username = base + (1000..9999).random()
            }
            val usuario = Usuario(
                id = "demo-user-${DemoData.usuarios.size + 1}",
                nome = nome.trim(),
                username = username,
                email = emailTrimmed,
                telefone = telefone.trim(),
                estado = estado.trim(),
                cidade = cidade.trim(),
                bairro = bairro.trim(),
                role = "user"
            )
            DemoData.usuarios.add(usuario)
            demoUid = usuario.id
            return Result.success(Unit)
        }

        return try {
            // Gera username único
            val baseUsername = "@" + emailTrimmed.substringBefore("@").lowercase().replace(Regex("[^a-z0-9]"), "")
            var username = baseUsername + (100..999).random()
            while (!isUsernameDisponivel(username)) {
                username = baseUsername + (1000..9999).random()
            }

            val result = auth!!.createUserWithEmailAndPassword(emailTrimmed, pass).await()
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

            firestore!!.collection("usuarios")
                .document(user.uid)
                .set(usuario)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            val msg = formatAuthException(e)
            Result.failure(Exception(msg))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        if (demo) return Result.failure(Exception("Login com Google indisponível no modo demo."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth!!.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Falha ao autenticar com o Google"))

            val docRef = firestore!!.collection("usuarios").document(user.uid)
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
        if (demo) {
            val uid = demoUid ?: return Result.failure(Exception("Usuário não logado"))
            val cleanUsername = normalizarUsername(username)
            if (!isUsernameDisponivel(cleanUsername, uid)) {
                return Result.failure(Exception("Este username ($cleanUsername) já está em uso por outro usuário."))
            }
            val index = DemoData.usuarios.indexOfFirst { it.id == uid }
            if (index < 0) return Result.failure(Exception("Usuário não encontrado"))
            val anterior = DemoData.usuarios[index]
            DemoData.usuarios[index] = anterior.copy(
                nome = nome.trim(),
                username = cleanUsername,
                telefone = telefone.trim(),
                estado = estado.trim(),
                cidade = cidade.trim(),
                bairro = bairro.trim()
            )
            return Result.success(Unit)
        }
        return try {
            val user = auth?.currentUser ?: return Result.failure(Exception("Usuário não logado"))

            val cleanUsername = normalizarUsername(username)
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

            firestore!!.collection("usuarios")
                .document(user.uid)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun alterarSenha(novaSenha: String): Result<Unit> {
        if (demo) return Result.failure(Exception("Alteração de senha indisponível no modo demo."))
        return try {
            val senhaErro = validarSenhaSegura(novaSenha)
            if (senhaErro != null) {
                return Result.failure(Exception(senhaErro))
            }

            val user = auth?.currentUser ?: return Result.failure(Exception("Usuário não logado"))
            user.updatePassword(novaSenha).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthException(e)))
        }
    }

    suspend fun isAdmin(): Boolean {
        if (demo) {
            return DemoData.usuarios.firstOrNull { it.id == demoUid }?.isAdmin == true
        }
        val user = auth?.currentUser ?: return false
        return try {
            val claim = user.getIdToken(false).await().claims?.get("admin") as? Boolean == true
            if (claim) return true
            val role = firestore?.collection("usuarios")?.document(user.uid)?.get()?.await()?.getString("role")
            role.equals("admin", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listarUsuarios(): List<Usuario> {
        if (demo) return DemoData.usuarios.toList()
        return try {
            val snapshot = firestore!!.collection("usuarios").orderBy("nome").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Usuario::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDadosUsuario(uid: String): Usuario? {
        if (demo) return DemoData.usuarios.firstOrNull { it.id == uid }
        return try {
            val doc = firestore!!.collection("usuarios").document(uid).get().await()
            doc.toObject(Usuario::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        if (demo) demoUid = null else auth?.signOut()
    }

    private fun normalizarUsername(username: String): String =
        if (username.startsWith("@")) username.trim().lowercase() else "@${username.trim().lowercase()}"

    private fun formatAuthException(e: Exception): String = com.vizinhoalerta.data.formatAuthException(e)
}
