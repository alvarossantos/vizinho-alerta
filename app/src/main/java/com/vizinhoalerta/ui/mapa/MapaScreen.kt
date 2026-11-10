package com.vizinhoalerta.ui.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.service.LocalizacaoService
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import java.util.Locale

val OSM_STYLE_JSON = """
    {
      "version": 8,
      "sources": {
        "osm-tiles": {
          "type": "raster",
          "tiles": [
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          ],
          "tileSize": 256,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "osm-tiles-layer",
          "type": "raster",
          "source": "osm-tiles",
          "minzoom": 0,
          "maxzoom": 19
        }
      ]
    }
""".trimIndent()

fun createColoredMarkerIcon(context: Context, colorInt: Int): Icon {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = colorInt
        isAntiAlias = true
    }
    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)
    paint.color = AndroidColor.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    isLoggedIn: Boolean,
    onNovaOcorrenciaClick: () -> Unit,
    onOcorrenciaClick: (String) -> Unit,
    onListaClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUserLocationReady: (Double, Double) -> Unit = { _, _ -> },
    onConfirmLocation: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ocorrencias by ocorrenciaRepository.observarOcorrencias().collectAsState(initial = emptyList())

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var loadingLocation by remember { mutableStateOf(true) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

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

    fun recenter() {
        userLocation?.let { loc ->
            mapInstance?.cameraPosition = CameraPosition.Builder()
                .target(loc)
                .zoom(16.0)
                .build()
        } ?: run {
            LocalizacaoService.obterLocalizacaoAtualLatLng(
                context = context,
                onSuccess = { lat, lng ->
                    val loc = LatLng(lat, lng)
                    userLocation = loc
                    mapInstance?.cameraPosition = CameraPosition.Builder()
                        .target(loc)
                        .zoom(16.0)
                        .build()
                },
                onFailure = {}
            )
        }
    }

    // Obter localização do usuário assim que o mapa estiver pronto
    LaunchedEffect(mapInstance) {
        if (mapInstance != null) {
            LocalizacaoService.obterLocalizacaoAtualLatLng(
                context = context,
                onSuccess = { lat, lng ->
                    val loc = LatLng(lat, lng)
                    userLocation = loc
                    loadingLocation = false
                    mapInstance?.cameraPosition = CameraPosition.Builder()
                        .target(loc)
                        .zoom(16.0)
                        .build()
                    onUserLocationReady(lat, lng)
                },
                onFailure = { _ ->
                    loadingLocation = false
                }
            )
        }
    }

    // Atualizar marcadores com cores distintas
    LaunchedEffect(mapInstance, ocorrencias, userLocation, selectedLocation) {
        mapInstance?.let { map ->
            map.clear()

            val openIcon = createColoredMarkerIcon(context, AndroidColor.parseColor("#C62828"))
            val resolvedIcon = createColoredMarkerIcon(context, AndroidColor.parseColor("#2E7D32"))
            val userIcon = createColoredMarkerIcon(context, AndroidColor.parseColor("#1A73E8"))
            val selectedIcon = createColoredMarkerIcon(context, AndroidColor.parseColor("#EF6C00"))

            userLocation?.let { uLoc ->
                map.addMarker(
                    MarkerOptions()
                        .position(uLoc)
                        .title("Sua Localização")
                        .icon(userIcon)
                )
            }

            selectedLocation?.let { sLoc ->
                map.addMarker(
                    MarkerOptions()
                        .position(sLoc)
                        .title("Local Selecionado para Ocorrência")
                        .icon(selectedIcon)
                )
            }

            ocorrencias.forEach { o ->
                if (o.lat != 0.0 && o.lng != 0.0) {
                    val pos = LatLng(o.lat, o.lng)
                    val isResolvido = o.status == "resolvido"
                    val statusText = if (isResolvido) "RESOLVIDO" else "ABERTO"
                    val markerIcon = if (isResolvido) resolvedIcon else openIcon

                    map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title(o.tipo)
                            .snippet("${o.bairro} — ${o.cidade} — $statusText")
                            .icon(markerIcon)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Mapa") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Lista") },
                    selected = false,
                    onClick = onListaClick
                )
                if (isLoggedIn) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Perfil") },
                        selected = false,
                        onClick = onProfileClick
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                        label = { Text("Sair") },
                        selected = false,
                        onClick = onLogoutClick
                    )
                } else {
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        label = { Text("Entrar") },
                        selected = false,
                        onClick = onLoginClick
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão flutuante para centralizar na posição atual
                FloatingActionButton(
                    onClick = { recenter() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Minha Posição")
                }

                if (selectedLocation == null) {
                    ExtendedFloatingActionButton(
                        onClick = onNovaOcorrenciaClick,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Reportar") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                view.getMapAsync { map ->
                    if (mapInstance == null) {
                        map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) { _ ->
                            mapInstance = map
                            map.addOnMapClickListener { latLng ->
                                selectedLocation = latLng
                                true
                            }
                            map.setOnInfoWindowClickListener { marker ->
                                val matched = ocorrencias.find { it.tipo == marker.title && it.lat == marker.position.latitude && it.lng == marker.position.longitude }
                                if (matched != null) {
                                    onOcorrenciaClick(matched.id)
                                    true
                                } else {
                                    val target = marker.position
                                    selectedLocation = target
                                    true
                                }
                            }
                        }
                    }
                }
            }

            if (loadingLocation) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Card flutuante quando o usuário clica em um local no mapa para criar ocorrência
            selectedLocation?.let { pos ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Local Selecionado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "Lat: %.5f, Lng: %.5f", pos.latitude, pos.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onConfirmLocation(pos.latitude, pos.longitude)
                                        selectedLocation = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Criar Ocorrência Aqui")
                                }
                                OutlinedButton(
                                    onClick = { selectedLocation = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
