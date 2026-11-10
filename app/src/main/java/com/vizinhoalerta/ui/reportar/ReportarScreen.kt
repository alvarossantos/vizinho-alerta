package com.vizinhoalerta.ui.reportar

import android.graphics.BitmapFactory
import android.location.Location
import androidx.camera.core.ImageCapture
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.service.AudioService
import com.vizinhoalerta.service.CameraService
import com.vizinhoalerta.service.LocalizacaoService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportarScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    authRepository: AuthRepository,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
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

    var fotoFile by remember { mutableStateOf<File?>(null) }
    var mostrandoCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var audioFile by remember { mutableStateOf<File?>(null) }
    var gravandoAudio by remember { mutableStateOf(false) }
    var tempoGravacao by remember { mutableStateOf(0) }

    var localizacao by remember { mutableStateOf<LatLng?>(null) }
    var obtendoGps by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    // Pré-preenche cidade/bairro do usuário
    LaunchedEffect(Unit) {
        authRepository.currentUser?.uid?.let { uid ->
            authRepository.getDadosUsuario(uid)?.let { u ->
                cidade = u.cidade
                bairro = u.bairro
            }
        }
        // Se tem latitude/longitude inicial, preenche
        if (initialLat != 0.0 && initialLng != 0.0) {
            localizacao = LatLng(initialLat, initialLng)
        }
    }

    // Cronômetro áudio 15s
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    obtendoGps = true
                    LocalizacaoService.obterLocalizacaoAtualLatLng(
                        context = context,
                        onSuccess = { lat, lng ->
                            localizacao = LatLng(lat, lng)
                            obtendoGps = false
                        },
                        onFailure = {
                            obtendoGps = false
                            mensagemErro = "Não foi possível obter GPS"
                        }
                    )
                },
                enabled = !obtendoGps
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                if (obtendoGps) "Obtendo GPS..." else "Local Atual (GPS)"
            }

            Text(
                text = "Tipo de Ocorrência",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column {
                tiposOcorrencia.forEach { tipo ->
                    FilterChip(
                        selected = (tipo == tipoSelecionado),
                        onClick = { tipoSelecionado = tipo },
                        label = { Text(tipo) }
                    )
                }
            }

            OutlinedTextField(
                value = cidade,
                onValueChange = { cidade = it },
                label = { Text("Cidade") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                label = { Text("Bairro / Ponto de referência") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Text("Foto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (mostrandoCamera) {
                Card(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                ProcessCameraProvider.getInstance(ctx).addListener({
                                    val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    imageCapture = ImageCapture.Builder().build()
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageCapture!!
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(context))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        FloatingActionButton(onClick = {
                            val cap = imageCapture
                            if (cap != null) {
                                CameraService.tirarFoto(
                                    context = context,
                                    imageCapture = cap,
                                    onFotoCapturada = { file ->
                                        fotoFile = file
                                        mostrandoCamera = false
                                    },
                                    onError = { onVoltar() }
                                )
                            }
                        }, modifier = Modifier.align(Alignment.BottomCenter)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Capturar")
                        }
                    }
                }
            } else {
                if (fotoFile != null && fotoFile!!.exists()) {
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val bitmap = remember(fotoFile!!) { BitmapFactory.decodeFile(fotoFile!!.absolutePath) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(onClick = { fotoFile = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red)
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { mostrandoCamera = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tirar Foto")
                    }
                }
            }

            Divider()

            Text("Relato de Voz (15s)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (audioFile != null && audioFile!!.exists()) {
                    Row(verticalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            if (audioFile!!.exists()) {
                                AudioService.reproduzirAudio(audioFile!!.absolutePath)
                            }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Ouvir")
                        }
                        Text("Áudio gravado")
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { audioFile = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red)
                        }
                    }
                } else {
                    OutlinedButton(onClick = {
                        AudioService.iniciarGravacao(context)
                        gravandoAudio = true
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gravar Voz")
                    }
                    if (gravandoAudio) {
                        Text("Gravando: ${tempoGravacao}s/15s", color = Color.Red)
                        IconButton(onClick = {
                            audioFile = AudioService.pararGravacao()
                            gravandoAudio = false
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "Parar", tint = Color.Red)
                        }
                    }
                }
            }

            Divider()

            Text("Localização", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterHorizontally) {
                val loc = localizacao
                Text(
                    text = if (loc != null) "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}"
                    else "Não definido",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            mensagemErro?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val loc = localizacao ?: return@Button
                    enviando = true
                    scope.launch {
                        val uid = authRepository.currentUser?.uid ?: ""
                        val nome = authRepository.currentUser?.displayName ?: "Morador"
                        val ocorrencia = Ocorrencia(
                            userId = uid,
                            usuarioNome = nome,
                            tipo = tipoSelecionado,
                            descricao = descricao,
                            lat = loc.latitude,
                            lng = loc.longitude,
                            cidade = cidade,
                            bairro = bairro,
                            createdAt = System.currentTimeMillis()
                        )
                        ocorrenciaRepository.inserir(ocorrencia, fotoFile, audioFile)
                        enviando = false
                        onSucesso()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !enviando
            ) {
                if (enviando) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("ENVIAR OCORRÊNCIA", fontWeight = FontWeight.Bold)
            }
        }
    }
}