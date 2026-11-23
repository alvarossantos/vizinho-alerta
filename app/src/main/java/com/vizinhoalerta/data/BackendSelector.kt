package com.vizinhoalerta.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.vizinhoalerta.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Escolhe o backend no startup. Modo demo só existe em debug. */
object BackendSelector {
    enum class Mode { FIREBASE, DEMO }

    suspend fun detectar(
        debug: Boolean = BuildConfig.DEBUG,
        configReal: Boolean = true,
        probe: suspend () -> Unit = { probeFirestore() }
    ): Mode {
        if (!debug) return Mode.FIREBASE
        // Config fictícia aponta para um projeto que responde PERMISSION_DENIED pela internet;
        // isso não é backend disponível para o protótipo.
        if (!configReal) return Mode.DEMO
        return try {
            withTimeout(4_000L) { probe() }
            Mode.FIREBASE
        } catch (e: TimeoutCancellationException) {
            Mode.DEMO
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) Mode.FIREBASE else Mode.DEMO
        } catch (e: Exception) {
            Mode.DEMO
        }
    }

    private suspend fun probeFirestore() {
        FirebaseFirestore.getInstance().collection("ocorrencias").limit(1).get().await()
    }
}
