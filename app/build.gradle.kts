plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vizinhoalerta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vizinhoalerta"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Protótipo: se o testador não tiver google-services.json próprio, usamos um template
// fictício apontado para os emuladores locais. O arquivo real continua gitignored.
val googleServicesTemplateName = "google-services.example.json"

tasks.register("ensureGoogleServices") {
    onlyIf { !file("google-services.json").exists() }
    doLast {
        val template = file(googleServicesTemplateName)
        val target = file("google-services.json")
        if (!template.exists()) {
            throw GradleException("Template $googleServicesTemplateName não encontrado em ${template.parentFile}")
        }
        if (!target.exists()) {
            template.copyTo(target, overwrite = false)
            logger.lifecycle("[google-services] Arquivo próprio ausente: usando template fictício ($googleServicesTemplateName) para o protótipo/debug.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("ensureGoogleServices")
}

// O plugin google-services lê o JSON em tarefas próprias, fora do grafo do preBuild.
tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    dependsOn("ensureGoogleServices")
}

tasks.register("checkReleaseGoogleServices") {
    dependsOn("ensureGoogleServices")
    doLast {
        val target = file("google-services.json")
        if (target.exists() && target.readText().contains("DUMMY", ignoreCase = true)) {
            throw GradleException("Release exige um google-services.json real (sem DUMMY)")
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("checkReleaseGoogleServices")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // CameraX
    val cameraVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    // Location & Maps (MapLibre - OpenStreetMap, no API key needed!)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.maplibre.gl:android-sdk:13.7.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:2.13.0")
    implementation("com.google.guava:guava:33.2.1-android")

    // Carregamento de imagem (AsyncImage)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}