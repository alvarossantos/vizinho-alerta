package com.vizinhoalerta.ui.detalhes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.service.AudioService
import com.vizinhoalerta.ui.mapa.MapaEstilo
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesScreen(
    ocorrenciaId: String,
    ocorrenciaRepository: OcorrenciaRepository,
    authRepository: AuthRepository,
    onVoltar: () -> Unit
) {
    var ocorrencia by remember { mutableStateOf<Ocorrencia?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var atualizandoStatus by remember { mutableStateOf(false) }
    var reproduzindoAudio by remember { mutableStateOf(false) }
    var podeGerenciar by remember { mutableStateOf(false) }
    var editandoLocalizacao by remember { mutableStateOf(false) }
    var posicaoSelecionada by remember { mutableStateOf<LatLng?>(null) }
    var salvandoLocalizacao by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(ocorrenciaId) {
        carregando = true
        editandoLocalizacao = false
        posicaoSelecionada = null
        val oCarregado = ocorrenciaRepository.obterPorId(ocorrenciaId)
        ocorrencia = oCarregado
        carregando = false
        podeGerenciar = oCarregado?.userId == authRepository.currentUid || authRepository.isAdmin()
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioService.pararReproducao()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da Ocorrência", fontWeight = FontWeight.Bold) },
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
        if (carregando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val o = ocorrencia
            if (o == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ocorrência não encontrada.")
                }
            } else {
                val isResolvido = o.status == "resolvido"
                val dateStr = remember(o.createdAt) {
                    if (o.createdAt > 0) {
                        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(o.createdAt))
                    } else ""
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = o.tipo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                SuggestionChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            if (isResolvido) "RESOLVIDO" else "ABERTO",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isResolvido) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        labelColor = if (isResolvido) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }

                            if (o.cidade.isNotBlank() || o.bairro.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = buildString {
                                            if (o.bairro.isNotBlank()) append(o.bairro)
                                            if (o.cidade.isNotBlank()) {
                                                if (isNotEmpty()) append(" — ")
                                                append(o.cidade)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            if (dateStr.isNotBlank()) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Descrição
                    if (o.descricao.isNotBlank()) {
                        Column {
                            Text(
                                text = "Descrição",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = o.descricao,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Foto
                    if (o.fotoUrl.isNotBlank()) {
                        Column {
                            Text(
                                text = "Evidência Fotográfica",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            ) {
                                AsyncImage(
                                    model = o.fotoUrl,
                                    contentDescription = "Foto da Ocorrência",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Áudio
                    if (o.audioUrl.isNotBlank()) {
                        Column {
                            Text(
                                text = "Relato de Voz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (reproduzindoAudio) {
                                        AudioService.pararReproducao()
                                        reproduzindoAudio = false
                                    } else {
                                        reproduzindoAudio = true
                                        AudioService.reproduzirAudio(o.audioUrl) {
                                            reproduzindoAudio = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (reproduzindoAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (reproduzindoAudio) "Pausar Áudio" else "Ouvir Relato de Voz")
                            }
                        }
                    }

                    // Mapa
                    if (o.lat != 0.0 && o.lng != 0.0 || editandoLocalizacao) {
                        Column {
                            Text(
                                text = "Localização no Mapa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val pos = posicaoSelecionada ?: if (o.lat != 0.0 && o.lng != 0.0) {
                                LatLng(o.lat, o.lng)
                            } else {
                                LatLng(-23.55052, -46.633308)
                            }
                            val context = LocalContext.current
                            val lifecycleOwner = LocalLifecycleOwner.current
                            val mapView = remember {
                                MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
                                MapView(context).apply { onCreate(null) }
                            }
                            val editandoAtual by rememberUpdatedState(editandoLocalizacao)
                            val posAtual by rememberUpdatedState(pos)
                            var mapAtual by remember { mutableStateOf<MapLibreMap?>(null) }

                            DisposableEffect(lifecycleOwner) {
                                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                    when (event) {
                                        androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                                        androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                                        androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                                        else -> {}
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose {
                                    lifecycleOwner.lifecycle.removeObserver(observer)
                                    mapView.onDestroy()
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AndroidView(
                                    factory = { mapView },
                                    modifier = Modifier.fillMaxSize()
                                ) { view ->
                                    view.getMapAsync { mapReady ->
                                        mapAtual = mapReady
                                    }
                                }
                            }

                            DisposableEffect(mapAtual) {
                                val map = mapAtual
                                val listener = MapLibreMap.OnMapClickListener { point ->
                                    if (editandoAtual) {
                                        posicaoSelecionada = point
                                    }
                                    true
                                }
                                if (map != null) {
                                    map.setStyle(Style.Builder().fromJson(MapaEstilo.OSM)) { _ ->
                                        map.clear()
                                        map.addMarker(MarkerOptions().position(posAtual).title(o.tipo))
                                        map.setCameraPosition(CameraPosition.Builder().target(posAtual).zoom(15.0).build())
                                    }
                                    map.addOnMapClickListener(listener)
                                }
                                onDispose {
                                    map?.removeOnMapClickListener(listener)
                                }
                            }

                            LaunchedEffect(pos) {
                                val map = mapAtual
                                if (map != null) {
                                    map.clear()
                                    map.addMarker(MarkerOptions().position(pos).title(o.tipo))
                                    map.setCameraPosition(CameraPosition.Builder().target(pos).zoom(15.0).build())
                                }
                            }
                        }
                    }

                    // Botão para Marcar como Resolvido
                    if (podeGerenciar && !isResolvido) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                atualizandoStatus = true
                                scope.launch {
                                    val res = ocorrenciaRepository.atualizarSituacaoComStatus(o.id, "resolvido", "resolvido")
                                    atualizandoStatus = false
                                    res.onSuccess {
                                        ocorrencia = o.copy(status = "resolvido")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            ),
                            enabled = !atualizandoStatus
                        ) {
                            if (atualizandoStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MARCAR COMO RESOLVIDO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Ações de gestão: exclusão (somente para quem pode gerenciar)
                    if (podeGerenciar) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = ocorrenciaRepository.deletarComMidia(o.id)
                                    res.onSuccess { onVoltar() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EXCLUIR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Alterar localização
                    if (podeGerenciar && !editandoLocalizacao) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { editandoLocalizacao = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ALTERAR LOCALIZAÇÃO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (editandoLocalizacao) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (o.lat == 0.0 && o.lng == 0.0 && posicaoSelecionada == null) {
                            Text(
                                text = "Toque no mapa para escolher o ponto.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = {
                                val latLng = posicaoSelecionada ?: LatLng(o.lat, o.lng)
                                salvandoLocalizacao = true
                                scope.launch {
                                    val res = ocorrenciaRepository.atualizarLocalizacao(
                                        o.id,
                                        latLng.latitude,
                                        latLng.longitude
                                    )
                                    salvandoLocalizacao = false
                                    res.onSuccess {
                                        ocorrencia = o.copy(lat = latLng.latitude, lng = latLng.longitude)
                                        posicaoSelecionada = null
                                        editandoLocalizacao = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !salvandoLocalizacao && !(o.lat == 0.0 && o.lng == 0.0 && posicaoSelecionada == null)
                        ) {
                            if (salvandoLocalizacao) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SALVAR LOCALIZAÇÃO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                editandoLocalizacao = false
                                posicaoSelecionada = null
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CANCELAR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
