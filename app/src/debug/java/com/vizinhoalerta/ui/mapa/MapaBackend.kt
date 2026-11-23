package com.vizinhoalerta.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vizinhoalerta.model.Ocorrencia
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Debug: mapa OpenStreetMap via MapLibre, sem API key do Google. */
internal object MapaBackend {
    @Composable
    fun Content(
        ocorrencias: List<Ocorrencia>,
        userLocation: Pair<Double, Double>?,
        centralizar: Int,
        onOcorrenciaClick: (String) -> Unit
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val mapView = remember {
            MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
            MapView(context).apply { onCreate(null) }
        }

        DisposableEffect(lifecycleOwner, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onDestroy()
            }
        }

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(MapaEstilo.OSM)) {
                    map.clear()
                    ocorrencias.filter { it.lat != 0.0 && it.lng != 0.0 }.forEach { o ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(o.lat, o.lng))
                                .title(o.tipo)
                                .snippet(o.id)
                        )
                    }
                    userLocation?.let { (lat, lng) ->
                        map.addMarker(MarkerOptions().position(LatLng(lat, lng)).title("Minha localização"))
                    }
                    val alvo = userLocation ?: (-23.55052 to -46.633308)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(alvo.first, alvo.second))
                        .zoom(if (userLocation != null) 16.0 else 13.0)
                        .build()
                }
                map.setOnMarkerClickListener { marker ->
                    val id = marker.snippet
                    if (!id.isNullOrBlank()) onOcorrenciaClick(id)
                    true
                }
            }
        }

        LaunchedEffect(centralizar, userLocation) {
            if (centralizar > 0 && userLocation != null) {
                mapView.getMapAsync { map ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(userLocation.first, userLocation.second))
                        .zoom(16.0)
                        .build()
                }
            }
        }
    }
}
