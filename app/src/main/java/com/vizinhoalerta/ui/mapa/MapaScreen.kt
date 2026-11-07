package com.vizinhoalerta.ui.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.CircleMarker
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizinhoalerta.service.LocalizacaoService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    onNovaOcorrenciaClick: () -> Unit,
    onOcorrenciaClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onUserLocationReady: (Double, Double) -> Unit = { _, _ -> },
    onConfirmLocation: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var ocorrencias by ocorrenciaRepository.observarOcorrencias().collectAsState(initial = emptyList())

    // Localização do usuário
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var loadingLocation by remember { mutableStateOf(true) }

    val defaultPos = LatLng(-23.55052, -46.633308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 12f)
    }

    // Obter localização do usuário no carregamento
    LaunchedEffect(Unit) {
        LocalizacaoService.obterLocalizacaoAtualLatLng(
            context = context,
            onSuccess = { lat, lng ->
                userLocation = LatLng(lat, lng)
                loadingLocation = false
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(lat, lng), 16f
                )
                onUserLocationReady(lat, lng)
            },
            onFailure = { _ ->
                loadingLocation = false
            }
        )
    }

    // Botão "Local Atual" — recente câmera para a posição do usuário
    fun recenter() {
        userLocation?.let { loc ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(loc, 16f)
        }
    }

    // Clique no mapa → menu de confirmação do local escolhido
    val mapClicked = { clickedPos: LatLng ->
        DialogChooseLocation(
            lat = clickedPos.latitude,
            lng = clickedPos.longitude,
            onConfirm = { onConfirmLocation(clickedPos.latitude, clickedPos.longitude) },
            onCancel = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { recenter() }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Local Atual"
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
            if (loadingLocation) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = mapClicked
                ) {
                    // Marcadores das ocorrências
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
                                snippet = "${o.bairro} — ${o.cidade} — ${if (o.status == "resolvido") "RESOLVIDO" else "ABERTO}",
                                icon = BitmapDescriptorFactory.defaultMarker(hue),
                                onClick = {
                                    onOcorrenciaClick(o.id)
                                    true
                                }
                            )
                        }
                    }

                    // Azul: localização do usuário
                    userLocation?.let { loc ->
                        CircleMarker(
                            position = loc,
                            radius = 40f,
                            color = Color(0xFF1A73E8),
                            strokeWidth = 2f,
                            zIndex = 1f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogChooseLocation(
    lat: Double,
    lng: Double,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirmar local", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Latitude: ${(lat * 1000000).toRadixString(3)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Longitude: ${(lng * 1000000).toRadixString(3)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Clique OK para confirmar ou Cancelar para manter o GPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}
