package com.vizinhoalerta.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object LocalizacaoService {
    private var cachedLat: Double? = null
    private var cachedLng: Double? = null
    private var lastFetchTime: Long = 0L
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutos de cache válido

    @SuppressLint("MissingPermission")
    fun obterLocalizacaoAtual(
        context: Context,
        onSuccess: (Location) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                cachedLat = location.latitude
                cachedLng = location.longitude
                lastFetchTime = System.currentTimeMillis()
                onSuccess(location)
            } else {
                client.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        cachedLat = lastLoc.latitude
                        cachedLng = lastLoc.longitude
                        lastFetchTime = System.currentTimeMillis()
                        onSuccess(lastLoc)
                    } else if (cachedLat != null && cachedLng != null) {
                        val mockLoc = Location("cached").apply {
                            latitude = cachedLat!!
                            longitude = cachedLng!!
                        }
                        onSuccess(mockLoc)
                    } else {
                        onFailure(Exception("Não foi possível obter a localização atual"))
                    }
                }.addOnFailureListener { e ->
                    if (cachedLat != null && cachedLng != null) {
                        val mockLoc = Location("cached").apply {
                            latitude = cachedLat!!
                            longitude = cachedLng!!
                        }
                        onSuccess(mockLoc)
                    } else {
                        onFailure(e)
                    }
                }
            }
        }.addOnFailureListener { e ->
            if (cachedLat != null && cachedLng != null) {
                val mockLoc = Location("cached").apply {
                    latitude = cachedLat!!
                    longitude = cachedLng!!
                }
                onSuccess(mockLoc)
            } else {
                onFailure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun obterLocalizacaoAtualLatLng(
        context: Context,
        onSuccess: (Double, Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        if (cachedLat != null && cachedLng != null && (currentTime - lastFetchTime < CACHE_DURATION_MS)) {
            onSuccess(cachedLat!!, cachedLng!!)
            return
        }

        obterLocalizacaoAtual(
            context = context,
            onSuccess = { loc ->
                cachedLat = loc.latitude
                cachedLng = loc.longitude
                lastFetchTime = System.currentTimeMillis()
                onSuccess(loc.latitude, loc.longitude)
            },
            onFailure = { e ->
                if (cachedLat != null && cachedLng != null) {
                    onSuccess(cachedLat!!, cachedLng!!)
                } else {
                    onFailure(e)
                }
            }
        )
    }
}
