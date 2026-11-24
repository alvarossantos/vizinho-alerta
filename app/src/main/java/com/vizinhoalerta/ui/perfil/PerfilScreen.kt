package com.vizinhoalerta.ui.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.model.Usuario
import com.vizinhoalerta.ui.components.Avatar
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
    var mostrarDialogoOrigem by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    fun processarFotoSelecionada(uri: Uri) {
        erro = null
        mensagem = null
        carregando = true
        scope.launch {
            val result = authRepository.salvarFotoPerfil(uri)
            carregando = false
            result.onSuccess {
                mensagem = "Foto atualizada."
                scope.launch { recarregar() }
            }.onFailure { erro = it.localizedMessage ?: "Não foi possível salvar a foto." }
        }
    }

    // Seletor de imagem da galeria
    val seletorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processarFotoSelecionada(it) }
    }

    // Captura de foto pela câmera - captura UID no lançamento para evitar valor stale no resultado
    var pendingCameraUid by remember { mutableStateOf<String?>(null) }
    val seletorCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso: Boolean ->
        if (sucesso) {
            val uid = pendingCameraUid ?: return@rememberLauncherForActivityResult
            val dir = context.cacheDir.resolve("perfil_camera")
            val file = java.io.File(dir, "$uid.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            processarFotoSelecionada(uri)
        }
        pendingCameraUid = null
    }

    fun abrirGaleria() {
        seletorGaleria.launch("image/*")
        mostrarDialogoOrigem = false
    }

    fun abrirCamera() {
        val uid = authRepository.currentUid ?: return
        pendingCameraUid = uid
        val dir = context.cacheDir.resolve("perfil_camera")
        dir.mkdirs()
        val file = java.io.File(dir, "$uid.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        seletorCamera.launch(uri)
        mostrarDialogoOrigem = false
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar(
                    nome = usuario?.nome ?: "Usuário",
                    fotoUrl = usuario?.fotoUrl?.takeIf { it.isNotBlank() },
                    tamanho = 96.dp,
                    editavel = true,
                    onEditarClick = { mostrarDialogoOrigem = true }
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

            if (mostrarDialogoOrigem) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoOrigem = false },
                    title = { Text("Escolher foto") },
                    text = { Text("Selecione a origem da foto") },
                    confirmButton = {
                        TextButton(onClick = { abrirGaleria() }) {
                            Text("Galeria")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { abrirCamera() }) {
                            Text("Câmera")
                        }
                    }
                )
            }
        }
    }
}