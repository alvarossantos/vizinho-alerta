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

    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var loadingLocation by remember { mutableStateOf(true) }
    var centralizar by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        LocalizacaoService.obterLocalizacaoAtualLatLng(
            context = context,
            onSuccess = { lat, lng ->
                userLocation = lat to lng
                loadingLocation = false
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
                    if (authRepository.currentUid != null) {
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
                MapaBackend.Content(ocorrencias, userLocation, centralizar, onOcorrenciaClick)
            }

            if (userLocation != null) {
                FloatingActionButton(
                    onClick = { centralizar++ },
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