package com.vizinhoalerta.ui.reportar

import android.content.Context
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CircleMarker
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizinhoalerta.ui.mapa.DialogChooseLocation
import com.vizinhoalerta.service.AudioService
import com.vizinhoalerta.service.CameraService
import com.vizinhoalerta.service.LocalizacaoService
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.service.AudioService
import com.vizinhoalerta.service.CameraService
import com.vizinhoalerta.service.AudioService
import com.vizinhoalerta.service.CameraService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportarScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    authRepository: AuthRepository,
    onSucesso: () -> Unit,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val tiposOcorrencia = listOf(
        "Suspeita", "Vandalismo", "Briga",
        "Animal perigoso", "Iluminação danificada", "Outra"
    )

    var tipoSelecionado by remember { mutableStateOf(tiposOcorrencia.first()) }
    var descricao by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }

    // Foto
    var fotoFile by remember { mutableStateOf<File?>(null) }
    var mostrandoCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Áudio
    var audioFile by remember { mutableStateOf<File?>(null) }
    var gravandoAudio by remember { mutableStateOf(false) }
    var tempoGravacao by remember { mutableStateOf(0) }
    var reproduzindoAudio by remember { mutableStateOf(false) }

    // Localização — seja por clique no mapa ou GPS
    var escolheuLocal by remember { mutableStateOf<LatLng?>(null) }
    var obtendoGps by remember { mutableStateOf(false) }
    var dialogConfirmando by remember { mutableStateOf(false) }

    var enviando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    // Pré-preenche cidade/bairro do usuário, mas o usuário pode alterar para qualquer cidade/bairro
    LaunchedEffect(Unit) {
        authRepository.currentUser?.uid?.let { uid ->
            val u = authRepository.getDadosUsuario(uid)
            if (u != null) {
                cidade = u.cidade
                bairro = u.bairro
            }
        }
    }

    // Cronômetro para o gravador de áudio (máx 15 segundos)
    LaunchedEffect(gravandoAudio) {
        if (gravandoAudio) {
            tempoGravacao = 0
            while (gravandoAudio && tempoGravacao < 15) {
                delay(1000)
                tempoGravacao++
            }
            if (gravandoAudio && tempoGravacao >= 15) {
                audioFile = AudioService.pararGravacao()
                gravandoAudio = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Ocorrência", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tipo de Ocorrência
            Text(
                text = "Tipo de Ocorrência",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiposOcorrencia.forEach { tipo ->
                    FilterChip(
                        selected = (tipo == tipoSelecionado),
                        onClick = { tipoSelecionado = tipo },
                        label = { Text(tipo) },
                        leadingIcon = if (tipo == tipoSelecionado) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Descrição e Bairro
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição detalhada") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade da ocorrência") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                label = { Text("Bairro / Ponto de referência") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            // Câmera / Evidência Fotográfica
            Text(
                text = "Evidência em Foto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (mostrandoCamera) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val capture = ImageCapture.Builder().build()
                                    imageCapture = capture

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            capture
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        FloatingActionButton(
                            onClick = {
                                val cap = imageCapture
                                if (cap != null) {
                                    CameraService.tirarFoto(
                                        context = context,
                                        imageCapture = cap,
                                        onFotoCapturada = { file ->
                                            fotoFile = file
                                            mostrandoCamera = false
                                        },
                                        onError = { ex ->
                                            mensagemErro = "Erro na foto: ${ex.localizedMessage}"
                                            mostrandoCamera = false
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Capturar Foto")
                        }
                    }
                }
            } else {
                val file = fotoFile
                if (file != null && file.exists()) {
                    val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto capturada",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(
                                onClick = { fotoFile = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover Foto", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { mostrandoCamera = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tirar Foto com Câmera")
                    }
                }
            }

            HorizontalDivider()

            // Gravador de Áudio
            Text(
                text = "Relato de Voz (10-15s)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (gravandoAudio) {
                        Text(
                            text = "Gravando: ${tempoGravacao}s / 15s",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = {
                            audioFile = AudioService.pararGravacao()
                            gravandoAudio = false
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "Parar Gravação", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        val aFile = audioFile
                        if (aFile != null && aFile.exists()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = {
                                    if (reproduzindoAudio) {
                                        AudioService.pararReproducao()
                                        reproduzindoAudio = false
                                    } else {
                                        reproduzindoAudio = true
                                        AudioService.reproduzirAudio(aFile.absolutePath) {
                                            reproduzindoAudio = false
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (reproduzindoAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Ouvir áudio"
                                    )
                                }
                                Text("Áudio gravado")
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    AudioService.pararReproducao()
                                    reproduzindoAudio = false
                                    audioFile = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Áudio", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    AudioService.iniciarGravacao(context)
                                    gravandoAudio = true
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gravar Relato de Voz")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Escolher local no mapa ou GPS
            Text(
                text = "Localização",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val mapaChosePos = remember { LatLng(-23.55052, -46.633308) } // Brasil default
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(escolheuLocal ?: mapaChosePos, 13f)
            }

            // Dialog de confirmação quando o usuário clica no mapa
            if (dialogConfirmando) {
                AlertDialog(
                    onDismissRequest = {
                        dialogConfirmando = false
                    },
                    title = { Text("Confirmar local", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Latitude: ${(escolheuLocal?.latitude * 1000000).toRadixString(3)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Longitude: ${(escolheuLocal?.longitude * 1000000).toRadixString(3)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Confirmar o local escolhido?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            escolheuLocal = LatLng(
                                escolheuLocal?.latitude ?: 0.0,
                                escolheuLocal?.longitude ?: 0.0
                            )
                            dialogConfirmando = false
                            cameraState.position = CameraPosition.fromLatLngZoom(escolheuLocal!!, 15f)
                        }) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            dialogConfirmando = false
                        }) {
                            Text("Cancelar")
                        }
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState
                        ) {
                            // Marcador quando escolheu local
                            escolheuLocal?.let { loc ->
                                Marker(
                                    state = MarkerState(position = loc),
                                    title = "Escolhido",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                // Botão "Usar Local Atual" (GPS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            obtendoGps = true
                            LocalizacaoService.obterLocalizacaoAtualLatLng(
                                context = context,
                                onSuccess = { lat, lng ->
                                    escolheuLocal = LatLng(lat, lng)
                                    obtendoGps = false
                                    cameraState.position = CameraPosition.fromLatLngZoom(
                                        LatLng(lat, lng), 16f
                                    )
                                },
                                onFailure = { ex ->
                                    obtendoGps = false
                                    mensagemErro = "Não foi possível obter GPS"
                                }
                            )
                        },
                        enabled = !obtendoGps
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (obtendoGps) "Obtendo GPS..." else "Local Atual (GPS)")
                    }

                    OutlinedButton(
                        onClick = {
                            dialogConfirmando = true
                        },
                        enabled = escolheuLocal == null
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clique no mapa para escolher")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            mensagemErro?.let { erro ->
                Text(
                    text = erro,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Enviar
            Button(
                onClick = {
                    mensagemErro = null
                    val loc = escolheuLocal ?: return@Button

                    enviando = true
                    scope.launch {
                        val uid = authRepository.currentUser?.uid ?: ""
                        val nomeUser = authRepository.currentUser?.displayName ?: "Morador"

                        val ocorrencia = Ocorrencia(
                            userId = uid,
                            usuarioNome = nomeUser,
                            tipo = tipoSelecionado,
                            descricao = descricao,
                            lat = loc.latitude,
                            lng = loc.longitude,
                            cidade = cidade,
                            bairro = bairro,
                            status = "aberto",
                            createdAt = System.currentTimeMillis()
                        )

                        val res = ocorrenciaRepository.inserir(
                            ocorrencia = ocorrencia,
                            fotoFile = fotoFile,
                            audioFile = audioFile
                        )

                        enviando = false
                        res.onSuccess {
                            onSucesso()
                        }.onFailure { ex ->
                            mensagemErro = ex.localizedMessage ?: "Erro ao enviar ocorrência"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !enviando
            ) {
                if (enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("ENVIAR OCORRÊNCIA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
