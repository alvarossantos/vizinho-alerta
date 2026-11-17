package com.vizinhoalerta.ui.mapa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.service.LocalizacaoService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    authRepository: AuthRepository,
    onNovaOcorrenciaClick: () -> Unit,
    onOcorrenciaClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onListaClick: () -> Unit,
    onProfileClick: () -> Unit,
    onConfirmLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val ocorrencias by ocorrenciaRepository.observarOcorrencias()
        .collectAsState(initial = emptyList())

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var loadingLocation by remember { mutableStateOf(true) }

    val defaultPos = LatLng(-23.55052, -46.633308)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(defaultPos, 13f)
    }

    LaunchedEffect(Unit) {
        LocalizacaoService.obterLocalizacaoAtualLatLng(
            context = context,
            onSuccess = { lat, lng ->
                userLocation = LatLng(lat, lng)
                loadingLocation = false
                cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                    LatLng(lat, lng), 16f
                )
            },
            onFailure = {
                loadingLocation = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onListaClick) {
                        Icon(Icons.Default.List, contentDescription = "Lista")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                    IconButton(onClick = {
                        authRepository.logout()
                        onLogoutClick()
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Sair")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (authRepository.currentUser != null) {
                        onNovaOcorrenciaClick()
                    }
                },
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
                    cameraPositionState = cameraPositionState
                ) {
                    ocorrencias.forEach { ocorrencia ->
                        if (ocorrencia.lat != 0.0 && ocorrencia.lng != 0.0) {
                            Marker(
                                state = MarkerState(position = LatLng(ocorrencia.lat, ocorrencia.lng)),
                                title = ocorrencia.tipo,
                                snippet = "${ocorrencia.bairro} — ${ocorrencia.status}",
                                onClick = {
                                    onOcorrenciaClick(ocorrencia.id)
                                    true
                                }
                            )
                        }
                    }

                    userLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = loc),
                            title = "Minha localização",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                            ),
                            onClick = false
                        )
                    }
                }
            }

            if (userLocation != null) {
                FloatingActionButton(
                    onClick = {
                        userLocation?.let { loc ->
                            cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(loc, 16f)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Ir para mim")
                }
            }
        }
    }
}