package com.lockin.app.core.util

/**
 * App-wide constants.
 *
 * Centralizing magic strings and values here keeps the codebase DRY and
 * makes it easy to change database names, DataStore keys, or AI model
 * paths without hunting through multiple files.
 */
object Constants {

    // ── Database ─────────────────────────────────────────────────────────
    const val DATABASE_NAME = "lockin_database"
    const val DATABASE_VERSION = 1

    // ── DataStore ────────────────────────────────────────────────────────
    const val DATASTORE_NAME = "lockin_preferences"

    // ── AI Model Paths (assets/ relative) ────────────────────────────────
    // These will point to TensorFlow Lite model files once we add them.
    const val GEMMA_MODEL_PATH = "models/gemma_coach.tflite"
    const val POSENET_MODEL_PATH = "models/posenet_form.tflite"

    // ── Health Connect ───────────────────────────────────────────────────
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

    // ── Misc ─────────────────────────────────────────────────────────────
    const val APP_NAME = "Lockin"
    const val APP_SUBTITLE = "Hybrid Training Engine"
}

