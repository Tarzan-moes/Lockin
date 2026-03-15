
package com.lockin.app.ai.vision

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.lockin.app.ai.inference.TFLiteInterpreterWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ExerciseClassifier – Classifies which exercise is being performed
 * from ML Kit pose landmarks using a custom TFLite model.
 *
 * The model takes a flattened array of (x, y) positions for 33 ML Kit
 * landmarks (66 floats total) and outputs a probability distribution
 * over supported exercise classes.
 *
 * ⚠️ The actual .tflite model file must be placed in the assets folder.
 *     Until then, the classifier falls back to a heuristic-based approach
 *     using joint angles to estimate the exercise type.
 *
 * @param context Application context for loading model from assets.
 */
class ExerciseClassifier(
    private val context: Context
) {
    companion object {
        private const val TAG = "ExerciseClassifier"

        /** Path to the exercise classifier model in assets. */
        const val MODEL_PATH = "models/exercise_classifier.tflite"

        /** Number of input features: 33 landmarks × 2 coordinates (x, y). */
        private const val NUM_INPUT_FEATURES = 66

        /** Minimum confidence threshold to accept a classification. */
        private const val CONFIDENCE_THRESHOLD = 0.7f

        /**
         * Supported exercise labels. Must match the model's output order.
         * Update this list if the model is retrained with different classes.
         */
        val EXERCISE_LABELS = listOf(
            "squat",
            "bench_press",
            "deadlift",
            "overhead_press",
            "barbell_row",
            "pull_up",
            "lunge",
            "bicep_curl",
            "lateral_raise",
            "plank"
        )

        /** Number of output classes. */
        private val NUM_CLASSES = EXERCISE_LABELS.size
    }

    private val interpreterWrapper = TFLiteInterpreterWrapper()
    private var isModelLoaded = false

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Load the TFLite classification model from assets.
     * Returns true if successful, false if model file is missing or corrupted.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext true

        try {
            val modelBuffer = loadModelFile(MODEL_PATH)
            val success = interpreterWrapper.load(
                modelBuffer = modelBuffer,
                numThreads = 2,
                useNnapi = false,
                useGpu = false
            )
            isModelLoaded = success
            Log.i(TAG, "Exercise classifier loaded successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Exercise classifier model not found or failed to load: ${e.message}")
            Log.w(TAG, "Will use heuristic-based classification as fallback.")
            false
        }
    }

    /**
     * Classify which exercise is being performed from a detected [Pose].
     *
     * @return An [ExerciseRecognition] with the predicted exercise and confidence,
     *         or null if confidence is below the threshold.
     */
    fun classifyExercise(pose: Pose): ExerciseRecognition? {
        val features = extractFeatures(pose) ?: return null

        // Try TFLite model first, fall back to heuristics
        return if (isModelLoaded && interpreterWrapper.isLoaded()) {
            classifyWithModel(features)
        } else {
            classifyWithHeuristics(pose)
        }
    }

    /**
     * Release interpreter resources.
     */
    fun release() {
        interpreterWrapper.close()
        isModelLoaded = false
        Log.i(TAG, "Exercise classifier released")
    }

    // ── Model-based classification ───────────────────────────────────────

    private fun classifyWithModel(features: FloatArray): ExerciseRecognition? {
        return try {
            // Prepare input: [1, NUM_INPUT_FEATURES] float buffer
            val inputBuffer = ByteBuffer.allocateDirect(NUM_INPUT_FEATURES * 4).apply {
                order(ByteOrder.nativeOrder())
                features.forEach { putFloat(it) }
            }

            // Prepare output: [1, NUM_CLASSES] float buffer
            val outputBuffer = ByteBuffer.allocateDirect(NUM_CLASSES * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            val success = interpreterWrapper.run(inputBuffer, outputBuffer)
            if (!success) return null

            // Read logits and find argmax
            outputBuffer.rewind()
            var maxProb = Float.NEGATIVE_INFINITY
            var maxIdx = 0
            for (i in 0 until NUM_CLASSES) {
                val prob = outputBuffer.getFloat()
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = i
                }
            }

            // Apply softmax normalization (approximate)
            val confidence = if (maxProb > 0) maxProb else 0f

            if (confidence >= CONFIDENCE_THRESHOLD && maxIdx < EXERCISE_LABELS.size) {
                ExerciseRecognition(
                    exercise = EXERCISE_LABELS[maxIdx],
                    confidence = confidence
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Model inference failed: ${e.message}", e)
            null
        }
    }

    // ── Heuristic-based fallback ─────────────────────────────────────────

    /**
     * Classify exercise using joint angle heuristics when the TFLite model
     * is not available. This is a simplified approach that works for the
     * most common exercises.
     */
    private fun classifyWithHeuristics(pose: Pose): ExerciseRecognition? {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: return null
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE) ?: return null
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: return null
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: return null
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW) ?: return null
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST) ?: return null

        val kneeAngle = calculateAngle(
            leftHip.position, leftKnee.position, leftAnkle.position
        )
        val hipAngle = calculateAngle(
            leftShoulder.position, leftHip.position, leftKnee.position
        )
        val elbowAngle = calculateAngle(
            leftShoulder.position, leftElbow.position, leftWrist.position
        )

        // Shoulder angle: how high are the arms?
        val armRaised = leftWrist.position.y < leftShoulder.position.y

        return when {
            // Squat: knees bent significantly, relatively upright torso
            kneeAngle < 120f && hipAngle > 60f -> {
                ExerciseRecognition("squat", 0.75f)
            }
            // Deadlift: hips hinged, knees slightly bent
            hipAngle < 100f && kneeAngle > 130f -> {
                ExerciseRecognition("deadlift", 0.72f)
            }
            // Overhead press: arms raised above shoulders, standing
            armRaised && elbowAngle > 140f && kneeAngle > 160f -> {
                ExerciseRecognition("overhead_press", 0.70f)
            }
            // Bicep curl: elbow flexed, standing
            elbowAngle < 90f && kneeAngle > 160f && !armRaised -> {
                ExerciseRecognition("bicep_curl", 0.72f)
            }
            // Lunge: one knee bent significantly more than the other
            kneeAngle < 100f && hipAngle > 100f -> {
                ExerciseRecognition("lunge", 0.70f)
            }
            // Bench press: lying down (hip and shoulder at similar y)
            kotlin.math.abs(leftHip.position.y - leftShoulder.position.y) < 60f &&
                    elbowAngle in 60f..150f -> {
                ExerciseRecognition("bench_press", 0.70f)
            }
            else -> null // Unable to classify with sufficient confidence
        }
    }

    // ── Feature extraction ───────────────────────────────────────────────

    /**
     * Extract normalized (x, y) features from a Pose for model input.
     * Normalizes coordinates to [0, 1] range based on image dimensions.
     */
    private fun extractFeatures(pose: Pose): FloatArray? {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.size < 33) return null

        // Find bounding box for normalization
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        landmarks.forEach { lm ->
            if (lm.position.x < minX) minX = lm.position.x
            if (lm.position.y < minY) minY = lm.position.y
            if (lm.position.x > maxX) maxX = lm.position.x
            if (lm.position.y > maxY) maxY = lm.position.y
        }

        val rangeX = (maxX - minX).coerceAtLeast(1f)
        val rangeY = (maxY - minY).coerceAtLeast(1f)

        // Flatten to [x0, y0, x1, y1, ...] normalized to [0, 1]
        val features = FloatArray(NUM_INPUT_FEATURES)
        // Use landmarks sorted by type (0..32)
        val sortedLandmarks = landmarks.sortedBy { it.landmarkType }

        for (i in 0 until minOf(33, sortedLandmarks.size)) {
            val lm = sortedLandmarks[i]
            features[i * 2] = (lm.position.x - minX) / rangeX
            features[i * 2 + 1] = (lm.position.y - minY) / rangeY
        }

        return features
    }

    // ── Model loading ────────────────────────────────────────────────────

    private fun loadModelFile(assetPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}

/**
 * Result of an exercise classification inference.
 */
data class ExerciseRecognition(
    /** Predicted exercise name (e.g., "squat", "bench_press"). */
    val exercise: String,
    /** Confidence score in range [0, 1]. */
    val confidence: Float
) {
    /** Human-readable exercise name with proper formatting. */
    val displayName: String
        get() = exercise.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
}

