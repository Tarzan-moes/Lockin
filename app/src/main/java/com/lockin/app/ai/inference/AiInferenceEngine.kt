package com.lockin.app.ai.inference

/**
 * AiInferenceEngine – Contract for running AI models on-device.
 *
 * This interface abstracts the TensorFlow Lite interpreter so the rest
 * of the app doesn't need to know about TFLite specifics. Implementations
 * will load .tflite files, run inference, and return structured results.
 *
 * Methods are suspend because inference can take tens of milliseconds
 * and should never block the main thread.
 */
interface AiInferenceEngine {

    /** Generate a text response from the coaching LLM given a prompt. */
    suspend fun generateCoachResponse(prompt: String): String

    /**
     * Analyze a camera frame for exercise form.
     * @param frameData Raw pixel data or a reference to the camera frame.
     * @return A map of joint names to confidence scores, plus any form feedback.
     */
    suspend fun analyzeForm(frameData: ByteArray): FormAnalysisResult

    /** Predict fatigue level from recent training + HRV data. */
    suspend fun predictFatigueLevel(
        recentVolume: Float,
        hrvScore: Int,
        sleepHours: Float
    ): Float

    /** Count reps from sensor data stream (accelerometer/gyro). */
    suspend fun countReps(sensorData: List<FloatArray>): Int
}

/**
 * Result of a form analysis inference pass.
 * Contains joint positions and any corrective feedback.
 */
data class FormAnalysisResult(
    val jointPositions: Map<String, Pair<Float, Float>> = emptyMap(),
    val confidence: Float = 0f,
    val feedback: List<String> = emptyList()
)

