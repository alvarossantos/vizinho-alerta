package com.vizinhoalerta.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.service.LocalidadesService
import kotlinx.coroutines.launch

val UFS_BRASIL = listOf(
    "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
    "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
    "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
)

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
    var estado by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }

    var expandedEstado by remember { mutableStateOf(false) }
    var cidadesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var expandedCidade by remember { mutableStateOf(false) }

    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Carrega cidades do IBGE sempre que o Estado (UF) mudar
    LaunchedEffect(estado) {
        if (estado.isNotBlank()) {
            cidadesList = LocalidadesService.listarCidadesPorUf(estado)
        } else {
            cidadesList = emptyList()
            cidade = ""
        }
    }

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

                    ExposedDropdownMenuBox(
                        expanded = expandedEstado,
                        onExpandedChange = { expandedEstado = !expandedEstado },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = estado,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado (UF)") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEstado) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedEstado,
                            onDismissRequest = { expandedEstado = false }
                        ) {
                            UFS_BRASIL.forEach { uf ->
                                DropdownMenuItem(
                                    text = { Text(uf) },
                                    onClick = {
                                        estado = uf
                                        expandedEstado = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cidade com suporte a digitação e sugestões do IBGE
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cidade,
                            onValueChange = { cidade = it },
                            label = { Text("Cidade") },
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                            trailingIcon = if (cidadesList.isNotEmpty()) {
                                {
                                    IconButton(onClick = { expandedCidade = !expandedCidade }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Ver cidades")
                                    }
                                }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (cidadesList.isNotEmpty()) {
                            DropdownMenu(
                                expanded = expandedCidade,
                                onDismissRequest = { expandedCidade = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                cidadesList.forEach { mun ->
                                    DropdownMenuItem(
                                        text = { Text(mun) },
                                        onClick = {
                                            cidade = mun
                                            expandedCidade = false
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                label = { Text("Senha (mín. 8 chars, 1 maiúscula, 1 número, 1 símbolo)") },
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
                    val emailTrimmed = email.trim()
                    if (emailTrimmed.isBlank() || senha.isBlank()) {
                        mensagemErro = "Preencha e-mail e senha"
                        return@Button
                    }
                    if (isCadastro && (nome.isBlank() || estado.isBlank() || cidade.isBlank() || bairro.isBlank())) {
                        mensagemErro = "Preencha todos os campos do cadastro"
                        return@Button
                    }

                    // Formata nome, cidade e bairro com ProperCase correto (ex: "São Paulo", "Bela Vista")
                    val nomeFormatado = LocalidadesService.formatarNomeProperCase(nome)
                    val cidadeFormatada = LocalidadesService.formatarNomeProperCase(cidade)
                    val bairroFormatado = LocalidadesService.formatarNomeProperCase(bairro)

                    carregando = true
                    scope.launch {
                        val res = if (isCadastro) {
                            authRepository.cadastrar(
                                nome = nomeFormatado,
                                email = emailTrimmed,
                                pass = senha,
                                estado = estado,
                                cidade = cidadeFormatada,
                                bairro = bairroFormatado,
                                isAdmin = false
                            )
                        } else {
                            authRepository.login(emailTrimmed, senha)
                        }
                        carregando = false
                        res.onSuccess {
                            onLoginSucesso()
                        }.onFailure { ex ->
                            mensagemErro = ex.message ?: ex.localizedMessage ?: "Ocorreu um erro"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
