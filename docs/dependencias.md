# Dependências (build.gradle.kts do módulo app)

```kotlin
dependencies {
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Localização
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Mapa
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
}
```

> Versões mínimas viáveis; ajustar conforme a versão do Android Studio.
