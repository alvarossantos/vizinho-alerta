package com.vizinhoalerta.ui.detalhes

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.service.AudioService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesScreen(
    ocorrenciaId: String,
    ocorrenciaRepository: OcorrenciaRepository,
    onVoltar: () -> Unit
) {
    var ocorrencia by remember { mutableStateOf<Ocorrencia?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var atualizandoStatus by remember { mutableStateOf(false) }
    var reproduzindoAudio by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(ocorrenciaId) {
        carregando = true
        ocorrencia = ocorrenciaRepository.obterPorId(ocorrenciaId)
        carregando = false
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
                    if (o.lat != 0.0 && o.lng != 0.0) {
                        Column {
                            Text(
                                text = "Localização no Mapa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val pos = LatLng(o.lat, o.lng)
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(pos, 15f)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(
                                        state = MarkerState(position = pos),
                                        title = o.tipo,
                                        icon = BitmapDescriptorFactory.defaultMarker(
                                            if (isResolvido) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Botão para Marcar como Resolvido
                    if (!isResolvido) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                atualizandoStatus = true
                                scope.launch {
                                    val res = ocorrenciaRepository.resolver(o.id)
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
                }
            }
        }
    }
}
