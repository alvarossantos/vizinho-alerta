package com.vizinhoalerta

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/** Apenas o build debug aponta para os emuladores locais. Release permanece intacto. */
class VizinhoAlertaDebugApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8081)
        FirebaseStorage.getInstance().useEmulator("10.0.2.2", 9199)
    }
}
