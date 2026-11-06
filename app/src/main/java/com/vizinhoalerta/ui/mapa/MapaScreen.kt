package com.vizinhoalerta.ui.mapa

// import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    onNovaOcorrenciaClick: () -> Unit,
    onOcorrenciaClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val ocorrencias by ocorrenciaRepository.observarOcorrencias()
        .collectAsState(initial = emptyList())

    var visualizacaoMapa by remember { mutableStateOf(true) }

    // Posicionamento inicial padrão (Brasil / São Paulo de fallback)
    val defaultPos = LatLng(-23.55052, -46.633308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 12f)
    }

    // Atualiza a câmera para a ocorrência mais recente se houver
    LaunchedEffect(ocorrencias) {
        if (ocorrencias.isNotEmpty()) {
            val primeira = ocorrencias.first()
            if (primeira.lat != 0.0 && primeira.lng != 0.0) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(primeira.lat, primeira.lng), 14f
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { visualizacaoMapa = !visualizacaoMapa }) {
                        Icon(
                            imageVector = if (visualizacaoMapa) Icons.Default.List else Icons.Default.Map,
                            contentDescription = "Alternar Visualização"
                        )
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovaOcorrenciaClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Reportar") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (visualizacaoMapa) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    ocorrencias.forEach { o ->
                        if (o.lat != 0.0 && o.lng != 0.0) {
                            val pos = LatLng(o.lat, o.lng)
                            val hue = if (o.status == "resolvido") {
                                BitmapDescriptorFactory.HUE_GREEN
                            } else {
                                BitmapDescriptorFactory.HUE_RED
                            }
                            Marker(
                                state = MarkerState(position = pos),
                                title = o.tipo,
                                snippet = "${o.bairro} — ${o.cidade} - ${if (o.status == "resolvido") "RESOLVIDO" else "ABERTO"}",
                                icon = BitmapDescriptorFactory.defaultMarker(hue),
                                onClick = {
                                    onOcorrenciaClick(o.id)
                                    true
                                }
                            )
                        }
                    }
                }
            } else {
                if (ocorrencias.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma ocorrência registrada.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ocorrencias) { item ->
                            OcorrenciaCard(
                                ocorrencia = item,
                                onClick = { onOcorrenciaClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OcorrenciaCard(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit
) {
    val isResolvido = ocorrencia.status == "resolvido"
    val dateStr = remember(ocorrencia.createdAt) {
        if (ocorrencia.createdAt > 0) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ocorrencia.createdAt))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isResolvido) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ocorrencia.tipo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                if (isResolvido) "Resolvido" else "Aberto",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isResolvido) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            labelColor = if (isResolvido) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    )
                }

                if (ocorrencia.cidade.isNotBlank() || ocorrencia.bairro.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (ocorrencia.bairro.isNotBlank()) append("Bairro: ${ocorrencia.bairro}")
                            if (ocorrencia.cidade.isNotBlank()) append(" — ${ocorrencia.cidade}")
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (ocorrencia.descricao.isNotBlank()) {
                    Text(
                        text = ocorrencia.descricao,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (dateStr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
