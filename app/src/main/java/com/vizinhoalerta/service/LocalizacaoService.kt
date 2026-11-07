package com.vizinhoalerta.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object LocalizacaoService {

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
                onSuccess(location)
            } else {
                client.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        onSuccess(lastLoc)
                    } else {
                        onFailure(Exception("Não foi possível obter a localização atual"))
                    }
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun obterLocalizacaoAtualLatLng(
        context: Context,
        onSuccess: (Double, Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        obterLocalizacaoAtual(
            context = context,
            onSuccess = { loc -> onSuccess(loc.latitude, loc.longitude) },
            onFailure = onFailure
        )
    }
}
