package com.vizinhoalerta.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizinhoalerta.model.Ocorrencia

/** Release: Google Maps com a chave real do google-services.json. */
internal object MapaBackend {
    @Composable
    fun Content(
        ocorrencias: List<Ocorrencia>,
        userLocation: Pair<Double, Double>?,
        centralizar: Int,
        onOcorrenciaClick: (String) -> Unit
    ) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(-23.55052, -46.633308), 13f)
        }

        LaunchedEffect(userLocation) {
            userLocation?.let { (lat, lng) ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 16f)
            }
        }
        LaunchedEffect(centralizar) {
            if (centralizar > 0 && userLocation != null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(userLocation.first, userLocation.second), 16f
                )
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            ocorrencias.filter { it.lat != 0.0 && it.lng != 0.0 }.forEach { o ->
                Marker(
                    state = MarkerState(position = LatLng(o.lat, o.lng)),
                    title = o.tipo,
                    snippet = "${o.bairro} — ${o.status}",
                    onClick = {
                        onOcorrenciaClick(o.id)
                        true
                    }
                )
            }
            userLocation?.let { (lat, lng) ->
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = "Minha localização",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    onClick = { false }
                )
            }
        }
    }
}
