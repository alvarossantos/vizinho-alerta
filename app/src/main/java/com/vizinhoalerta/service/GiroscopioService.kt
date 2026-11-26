package com.vizinhoalerta.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

object GiroscopioService {
    const val TAG = "GiroscopioService"

    fun observar(
        context: Context,
        lifecycle: Lifecycle,
        onMovimento: (Boolean) -> Unit
    ): () -> Unit {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (sensorManager == null || sensor == null) {
            Log.d(TAG, "giroscopio indisponivel")
            return { }
        }

        Log.d(TAG, "giroscopio disponivel")
        val estado = EstadoMovimento()
        var registrado = false
        var avisoAnterior = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val aviso = estado.atualizar(event.values, SystemClock.elapsedRealtime())
                if (aviso != avisoAnterior) {
                    avisoAnterior = aviso
                    Log.d(TAG, "aviso=$aviso")
                }
                onMovimento(aviso)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun registrar() {
            if (registrado || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            registrado = true
            Log.d(TAG, "listener registrado")
        }

        fun remover() {
            if (registrado) {
                sensorManager.unregisterListener(listener)
                registrado = false
                Log.d(TAG, "listener removido")
            }
            estado.reiniciar()
            avisoAnterior = false
            onMovimento(false)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> registrar()
                Lifecycle.Event.ON_STOP -> remover()
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)
        registrar()

        var descartado = false
        return {
            if (!descartado) {
                descartado = true
                lifecycle.removeObserver(observer)
                remover()
            }
        }
    }
}
