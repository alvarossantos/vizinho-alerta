package com.vizinhoalerta.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

object AudioService {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var arquivoGravacaoAtual: File? = null

    fun iniciarGravacao(context: Context): File {
        pararGravacao()

        val audioFile = File.createTempFile("AUDIO_", ".m4a", context.cacheDir)
        arquivoGravacaoAtual = audioFile

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }

        mediaRecorder = recorder
        return audioFile
    }

    fun pararGravacao(): File? {
        val file = arquivoGravacaoAtual
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
        return file
    }

    fun reproduzirAudio(urlOuCaminho: String, onCompletion: () -> Unit = {}) {
        pararReproducao()
        try {
            val player = MediaPlayer().apply {
                setDataSource(urlOuCaminho)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    onCompletion()
                    pararReproducao()
                }
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pararReproducao() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}
