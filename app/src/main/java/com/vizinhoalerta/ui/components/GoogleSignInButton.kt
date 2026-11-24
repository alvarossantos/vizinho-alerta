package com.vizinhoalerta.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.GoogleCredentialResult
import com.vizinhoalerta.data.GoogleSignInHelper
import com.vizinhoalerta.data.MSG_GOOGLE_NAO_CONFIGURADO
import kotlinx.coroutines.launch

private const val MSG_ERRO_GOOGLE = "Não foi possível entrar com o Google. Tente novamente."

@Composable
fun GoogleSignInButton(
    googleSignIn: GoogleSignInHelper? = null,
    authRepository: AuthRepository,
    modoDemo: Boolean,
    onSucesso: (Boolean) -> Unit,
    onMensagem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            if (modoDemo || googleSignIn == null || !googleSignIn.disponivel) {
                onMensagem(MSG_GOOGLE_NAO_CONFIGURADO)
                return@OutlinedButton
            }
            carregando = true
            scope.launch {
                try {
                    when (val credencial = googleSignIn.signIn()) {
                        is GoogleCredentialResult.Sucesso -> {
                            val resultado = authRepository.signInWithGoogle(credencial.idToken)
                            resultado.onSuccess { onSucesso(it.precisaCompletarPerfil) }
                                .onFailure { onMensagem(it.message ?: MSG_ERRO_GOOGLE) }
                        }
                        GoogleCredentialResult.Cancelado -> Unit
                        is GoogleCredentialResult.Erro -> onMensagem(credencial.mensagem)
                    }
                } finally {
                    carregando = false
                }
            }
        },
        enabled = !carregando,
        modifier = modifier
    ) {
        if (carregando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text("Continuar com Google")
        }
    }
}
