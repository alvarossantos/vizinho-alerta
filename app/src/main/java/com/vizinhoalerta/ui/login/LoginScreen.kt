package com.vizinhoalerta.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.GoogleSignInHelper
import com.vizinhoalerta.ui.components.GoogleSignInButton
import com.vizinhoalerta.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSucesso: () -> Unit,
    onCadastrarClick: () -> Unit,
    modoDemo: Boolean = false,
    googleSignIn: GoogleSignInHelper? = null,
    onGoogleSucesso: (Boolean) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var mostrarSenha by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Entrar") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Entrar", style = MaterialTheme.typography.headlineSmall)
            Text("Acesse sua conta para continuar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                        Icon(
                            imageVector = if (mostrarSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (mostrarSenha) "Ocultar senha" else "Mostrar senha",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (mostrarSenha) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            mensagemErro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                texto = "ENTRAR",
                onClick = {
                    mensagemErro = null
                    if (email.isBlank() || senha.isBlank()) {
                        mensagemErro = "Preencha email e senha"
                        return@PrimaryButton
                    }
                    carregando = true
                    scope.launch {
                        val result = authRepository.login(email = email, pass = senha)
                        carregando = false
                        result.onSuccess { onLoginSucesso() }
                            .onFailure { mensagemErro = it.localizedMessage }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                carregando = carregando
            )
            if (modoDemo) {
                Spacer(Modifier.height(8.dp))
                Text("Demo: admin@demo.com / Demo@123", fontSize = 12.sp)
            }
            GoogleSignInButton(
                googleSignIn = googleSignIn,
                authRepository = authRepository,
                modoDemo = modoDemo,
                onSucesso = onGoogleSucesso,
                onMensagem = { mensagemErro = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCadastrarClick) { Text("Não tem conta? Cadastrar") }
        }
    }
}