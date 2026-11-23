package com.vizinhoalerta.ui.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.model.Usuario
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    authRepository: AuthRepository,
    onLogoutClick: () -> Unit,
    onAdminClick: () -> Unit,
    onVoltar: () -> Unit
) {
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var ehAdmin by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun recarregar() {
        usuario = authRepository.currentUid?.let { authRepository.getDadosUsuario(it) }
    }

    LaunchedEffect(Unit) {
        recarregar()
        ehAdmin = authRepository.isAdmin()
    }

    LaunchedEffect(usuario) {
        usuario?.let {
            nome = it.nome
            username = it.username
            telefone = it.telefone
            estado = it.estado
            cidade = it.cidade
            bairro = it.bairro
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = usuario?.nome ?: "Usuário",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = usuario?.email ?: "Email não disponível",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${usuario?.bairro}, ${usuario?.cidade}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (editando) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Estado") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        erro = null
                        mensagem = null
                        carregando = true
                        scope.launch {
                            val result = authRepository.atualizarPerfil(nome, username, telefone, estado, cidade, bairro)
                            carregando = false
                            result.onSuccess {
                                mensagem = "Dados atualizados."
                                editando = false
                                scope.launch { recarregar() }
                            }.onFailure { erro = it.localizedMessage }
                        }
                    },
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (carregando) "Salvando..." else "Salvar") }
                TextButton(onClick = { editando = false }) { Text("Cancelar") }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { erro = null; mensagem = null; editando = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Editar dados") }
            }

            erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            mensagem?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }

            if (ehAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAdminClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Painel Admin")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sair", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
