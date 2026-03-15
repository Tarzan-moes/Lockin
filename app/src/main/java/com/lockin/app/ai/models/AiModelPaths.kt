package com.lockin.app.ai.models

/**
 * AI Model Constants
 *
 * Paths and identifiers for the AI models used in the app.
 * The Gemma model uses MediaPipe LLM Inference format (.bin) and is loaded
 * from app-private storage (not assets) due to its large size (~1.4 GB).
 * Other models (exercise classifier, etc.) are loaded from assets.
 */
object AiModelPaths {

    // ── Gemma LLM (MediaPipe .bin format) ────────────────────────────────

    /** Filename of the Gemma model in app-private files directory. */
    const val GEMMA_MODEL_FILENAME = "gemma-2b-it-gpu-int4.bin"

    /**
     * Subdirectory under the app's internal files dir where the model is stored.
     * Full runtime path: context.filesDir / [MODELS_DIR] / [GEMMA_MODEL_FILENAME]
     */
    const val MODELS_DIR = "models"

    // ── Legacy asset-based paths (for smaller models) ────────────────────

    /** On-device LLM asset path (only used if model is small enough for assets). */
    const val GEMMA_MODEL_PATH = "models/gemma_coach.tflite"

    /** PoseNet / MoveNet model for real-time form analysis via camera. */
    const val POSENET_MODEL_PATH = "models/posenet_form.tflite"

    /** Fatigue detection model that uses HRV + training load data. */
    const val FATIGUE_MODEL_PATH = "models/fatigue_detector.tflite"

    /** Rep counting model that processes accelerometer/gyro data. */
    const val REP_COUNTER_MODEL_PATH = "models/rep_counter.tflite"

    /** Exercise classifier model that identifies exercises from pose landmarks. */
    const val EXERCISE_CLASSIFIER_PATH = "models/exercise_classifier.tflite"

    /** Optional metadata file describing model versions and parameters. */
    const val MODEL_METADATA_PATH = "models/model_metadata.json"

    /** Tokenizer files for the Gemma LLM (vocabulary and configuration). */
    const val TOKENIZER_VOCAB_PATH = "tokenizer/vocab.txt"
    const val TOKENIZER_CONFIG_PATH = "tokenizer/tokenizer.json"

    /** TFLite model path for Gemma (alternative to .bin model). */
    const val GEMMA_TFLITE_PATH = "models/gemma_2b_it_q4.tflite"

    /** Filename used when model is downloaded into app-private storage. */
    const val GEMMA_TFLITE_FILENAME = "gemma_2b_it_q4.tflite"

    /** Optional remote URL for first-run model download (set via BuildConfig). */
    const val DEFAULT_GEMMA_DOWNLOAD_URL = ""
}
