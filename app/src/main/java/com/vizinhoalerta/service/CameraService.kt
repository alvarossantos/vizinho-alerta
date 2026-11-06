package com.vizinhoalerta.service

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object CameraService {

    fun criarArquivoFotoTemp(context: Context): File {
        val storageDir = context.cacheDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun tirarFoto(
        context: Context,
        imageCapture: ImageCapture,
        onFotoCapturada: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val photoFile = criarArquivoFotoTemp(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onFotoCapturada(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }
}
