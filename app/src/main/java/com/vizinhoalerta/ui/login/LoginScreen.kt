package com.vizinhoalerta.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizinhoalerta.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSucesso: () -> Unit
) {
    var isCadastro by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }

    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isCadastro) "Criar nova conta" else "Bem-vindo de volta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Segurança comunitária colaborativa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isCadastro) {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bairro,
                        onValueChange = { bairro = it },
                        label = { Text("Seu Bairro") },
                        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            mensagemErro?.let { erro ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = erro,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    mensagemErro = null
                    if (email.isBlank() || senha.isBlank()) {
                        mensagemErro = "Preencha e-mail e senha"
                        return@Button
                    }
                    if (isCadastro && (nome.isBlank() || bairro.isBlank())) {
                        mensagemErro = "Preencha nome e bairro"
                        return@Button
                    }

                    carregando = true
                    scope.launch {
                        val res = if (isCadastro) {
                            authRepository.cadastrar(nome, email, senha, bairro)
                        } else {
                            authRepository.login(email, senha)
                        }
                        carregando = false
                        res.onSuccess {
                            onLoginSucesso()
                        }.onFailure { ex ->
                            mensagemErro = ex.localizedMessage ?: "Ocorreu um erro"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !carregando
            ) {
                if (carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isCadastro) "CADASTRAR" else "ENTRAR", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    isCadastro = !isCadastro
                    mensagemErro = null
                }
            ) {
                Text(
                    if (isCadastro) "Já tem conta? Entrar" else "Não tem conta? Cadastrar-se"
                )
            }
        }
    }
}
