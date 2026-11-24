package com.vizinhoalerta.ui.cadastro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
fun CadastroScreen(
    authRepository: AuthRepository,
    onCadastroSucesso: () -> Unit,
    onLoginClick: () -> Unit,
    modoDemo: Boolean = false,
    googleSignIn: GoogleSignInHelper? = null,
    onGoogleSucesso: (Boolean) -> Unit = {}
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var mostrarSenha by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cadastrar") }) },
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
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
            Text("Cadastrar-se", style = MaterialTheme.typography.headlineSmall)
            Text("Crie sua conta para acompanhar ocorrências", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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
            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                label = { Text("Bairro") },
                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            mensagemErro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                texto = "Criar minha conta",
                onClick = {
                    mensagemErro = null
                    if (nome.isBlank() || email.isBlank() || senha.isBlank() || cidade.isBlank() || bairro.isBlank()) {
                        mensagemErro = "Preencha todos os campos"
                        return@PrimaryButton
                    }
                    carregando = true
                    scope.launch {
                        val result = authRepository.cadastrar(nome = nome, email = email, pass = senha, cidade = cidade, bairro = bairro)
                        carregando = false
                        result.onSuccess { onCadastroSucesso() }
                            .onFailure { mensagemErro = it.localizedMessage }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !carregando,
                carregando = carregando
            )
            GoogleSignInButton(
                googleSignIn = googleSignIn,
                authRepository = authRepository,
                modoDemo = modoDemo,
                onSucesso = onGoogleSucesso,
                onMensagem = { mensagemErro = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLoginClick) { Text("Já tem conta? Entrar") }
        }
    }
}