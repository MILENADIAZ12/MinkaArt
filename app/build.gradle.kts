import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.minkaart2.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minkaart2.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenCV trae binarios nativos para 4 ABIs (~112 MB sin filtrar); se limita a
        // dispositivos reales (arm64-v8a) y al emulador (x86_64) para no duplicar el
        // peso del APK con armeabi-v7a/x86, que casi nadie usa hoy.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // Firma de release: los datos de la llave viven en keystore.properties (raíz del
    // proyecto, ignorado por git — ver README, "Publicar una actualización"). Sin ese
    // archivo el release sale sin firmar y el build de debug no se ve afectado.
    val archivoLlave = rootProject.file("keystore.properties")
    val propiedadesLlave = Properties()

    if (archivoLlave.exists()) {
        archivoLlave.inputStream().use { propiedadesLlave.load(it) }
    }

    signingConfigs {
        if (archivoLlave.exists()) {
            create("release") {
                storeFile = file(propiedadesLlave.getProperty("storeFile"))
                storePassword = propiedadesLlave.getProperty("storePassword")
                keyAlias = propiedadesLlave.getProperty("keyAlias")
                keyPassword = propiedadesLlave.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (archivoLlave.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    // El modelo .tflite del Image Embedder de MediaPipe debe mapearse en memoria sin
    // comprimir — si se deja comprimido, MediaPipe falla al abrirlo desde assets.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.androidx.core.ktx)
    // Idiomas por app (AppCompatDelegate.setApplicationLocales) — selector Español/English.
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Reconocimiento visual (búsqueda por similitud) — motor híbrido:
    // OpenCV/ORB (misma pieza, "coincidencia exacta") + MediaPipe Image Embedder
    // (similitud semántica, embeddings MobileNet-V3, sin ML Kit ni servidor).
    implementation("org.opencv:opencv:4.10.0")
    implementation("com.google.mediapipe:tasks-vision:1.0.0")
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
}