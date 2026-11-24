package com.vizinhoalerta.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val MSG_SEM_CONTA_GOOGLE =
    "Nenhuma conta do Google está disponível neste dispositivo. Adicione uma conta e tente novamente."
private const val MSG_ERRO_GOOGLE =
    "Não foi possível entrar com o Google. Tente novamente."

/** Único ponto de contato com o Credential Manager: sem UI, Firebase ou repositório. */
class GoogleSignInHelper(private val context: Context) {

    /**
     * O plugin google-services só gera `default_web_client_id` com um JSON real.
     * Consulta o recurso por nome para não quebrar o build com o template DUMMY.
     */
    private val serverClientId: String? = try {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) null else context.getString(resourceId)
    } catch (e: Exception) {
        null
    }

    val disponivel: Boolean = webClientIdValido(serverClientId)

    suspend fun signIn(): GoogleCredentialResult {
        val clientId = serverClientId
        if (!disponivel || clientId == null) {
            return GoogleCredentialResult.Erro(MSG_GOOGLE_NAO_CONFIGURADO)
        }

        return try {
            val response = getCredential(clientId, filterByAuthorizedAccounts = true)
            extrairSucesso(response)
        } catch (e: NoCredentialException) {
            // Padrão oficial: sem conta autorizada, abre o seletor completo.
            try {
                val response = getCredential(clientId, filterByAuthorizedAccounts = false)
                extrairSucesso(response)
            } catch (e: NoCredentialException) {
                GoogleCredentialResult.Erro(MSG_SEM_CONTA_GOOGLE)
            } catch (e: GetCredentialCancellationException) {
                GoogleCredentialResult.Cancelado
            } catch (e: GetCredentialException) {
                GoogleCredentialResult.Erro(MSG_ERRO_GOOGLE)
            } catch (e: Exception) {
                GoogleCredentialResult.Erro(MSG_ERRO_GOOGLE)
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelado
        } catch (e: GetCredentialException) {
            GoogleCredentialResult.Erro(MSG_ERRO_GOOGLE)
        } catch (e: Exception) {
            GoogleCredentialResult.Erro(MSG_ERRO_GOOGLE)
        }
    }

    /** Limpeza best-effort: falha não impede o logout. */
    suspend fun clearCredentialState() {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Sem log: nada sobre credenciais ou conta deve sair do fluxo.
        }
    }

    private suspend fun getCredential(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean
    ): GetCredentialResponse {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return CredentialManager.create(context).getCredential(context, request)
    }

    private fun extrairSucesso(response: GetCredentialResponse): GoogleCredentialResult {
        val credential = response.credential as? CustomCredential ?: return erroGenerico()
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return erroGenerico()
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return if (idToken.isBlank()) erroGenerico() else GoogleCredentialResult.Sucesso(idToken)
    }

    private fun erroGenerico(): GoogleCredentialResult.Erro = GoogleCredentialResult.Erro(MSG_ERRO_GOOGLE)
}
