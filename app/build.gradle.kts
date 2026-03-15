/**
 * Lockin App – Module-level build configuration.
 *
 * Uses Kotlin 2.3.0 externally (android.builtInKotlin=false) so that KSP
 * and Hilt annotation processing work properly with AGP 9.0.1.
 *
 * Part 2: Added TensorFlow Lite, ML Kit, CameraX, Health Connect, Wear OS,
 * WorkManager, MPAndroidChart, Gson, Accompanist, and Coroutines dependencies.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.lockin.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lockin.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        val modelUrl = (project.findProperty("GEMMA_MODEL_DOWNLOAD_URL") as String?)
            ?.replace("\\", "\\\\")
            ?.replace("\"", "\\\"")
            ?: ""
        buildConfigField("String", "GEMMA_MODEL_DOWNLOAD_URL", "\"$modelUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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

    // Don't compress model files so they can be memory-mapped at runtime
    androidResources {
        noCompress += "tflite"
        noCompress += "bin"
    }
}

dependencies {
    // ── Global exclusions ────────────────────────────────────────────────
    // TF Lite splits its code into -api jars that share the same Android
    // namespace as the main jar → AGP 9 rejects duplicate namespaces.
    // Excluding the -api modules prevents them from being resolved as
    // separate dependencies; the classes are still present in the main jars.
    configurations.configureEach {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-gpu")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-gpu-api")
    }

    // ── Core Android ─────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Jetpack Compose ──────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.icons.extended)

    // ── Navigation ───────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Hilt – Dependency Injection ──────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Room – Local SQLite Database ─────────────────────────────────────
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // ── DataStore ────────────────────────────────────────────────────────
    implementation(libs.datastore.preferences)

    // ── Lifecycle ────────────────────────────────────────────────────────
    implementation(libs.lifecycle.viewmodel.compose)

    // ── Coroutines ───────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── TensorFlow Lite (AI model inference for exercise classifier) ────
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    // Note: GPU delegate removed to avoid namespace collision with AGP 9.
    // The main LLM uses MediaPipe GenAI (handles GPU internally).
    // Smaller TFLite models use NNAPI/CPU via TFLiteInterpreterWrapper.

    // ── MediaPipe GenAI (LLM Inference for Gemma .bin models) ────────────
    implementation(libs.mediapipe.genai)

    // ── ML Kit Pose Detection (form analysis) ────────────────────────────
    implementation(libs.mlkit.pose.detection)
    implementation(libs.mlkit.pose.detection.accurate)

    // ── CameraX (camera pipeline for form analysis) ──────────────────────
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ── Health Connect ───────────────────────────────────────────────────
    implementation(libs.health.connect.client)

    // ── Wear OS ──────────────────────────────────────────────────────────
    implementation(libs.play.services.wearable)

    // ── WorkManager ──────────────────────────────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── Charts ───────────────────────────────────────────────────────────
    implementation(libs.mpandroidchart)

    // ── Gson ─────────────────────────────────────────────────────────────
    implementation(libs.gson)

    // ── Retrofit + OkHttp (ExerciseDB API) ──────────────────────────────
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // ── Coil (exercise GIF/image loading) ────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // ── Accompanist ──────────────────────────────────────────────────────
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)

    // ── Calendar ─────────────────────────────────────────────────────────
    implementation("com.kizitonwose.calendar:compose:2.5.0")

    // ── Unit Testing ─────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // ── Android Instrumented Testing ─────────────────────────────────────
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // ── Debug ────────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}