package com.lockin.app.ai.vision

import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.lockin.app.ai.inference.FormAnalysisResult

/**
 * FormAnalyzerImpl – Implements the [FormAnalyzer] contract using ML Kit Pose Detection.
 *
 * Bridges the existing FormAnalyzer interface (from Part 1) to the real
 * [PoseDetectionManager] and its exercise-specific analysis methods.
 */
class FormAnalyzerImpl(
    private val poseDetectionManager: PoseDetectionManager
) : FormAnalyzer {

    companion object {
        private const val TAG = "FormAnalyzerImpl"
    }

    private var currentExercise: String = ""
    private var isRunning = false

    override fun startAnalysis(exerciseName: String) {
        currentExercise = exerciseName
        isRunning = true
        Log.i(TAG, "Started form analysis for: $exerciseName")
    }

    override fun stopAnalysis() {
        isRunning = false
        Log.i(TAG, "Stopped form analysis")
    }

    /**
     * Process a single camera frame (JPEG bytes) through pose detection
     * and return form analysis results.
     */
    override suspend fun processFrame(frameData: ByteArray): FormAnalysisResult {
        if (!isRunning) {
            return FormAnalysisResult(feedback = listOf("Analysis is not active. Call startAnalysis() first."))
        }

        return try {
            // Decode the JPEG frame into a Bitmap, then create InputImage
            val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
                ?: return FormAnalysisResult(feedback = listOf("Failed to decode camera frame."))

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val pose = poseDetectionManager.detectPose(inputImage)

            if (pose == null) {
                return FormAnalysisResult(
                    confidence = 0f,
                    feedback = listOf("No person detected in frame. Adjust your position.")
                )
            }

            // Run exercise-specific analysis
            val formFeedback = poseDetectionManager.analyzeForm(currentExercise, pose)

            // Map pose landmarks to joint positions for the legacy interface
            val jointPositions = pose.allPoseLandmarks.associate { landmark ->
                landmarkName(landmark.landmarkType) to Pair(landmark.position.x, landmark.position.y)
            }

            FormAnalysisResult(
                jointPositions = jointPositions,
                confidence = formFeedback.score / 100f,
                feedback = formFeedback.tips
            )
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed: ${e.message}", e)
            FormAnalysisResult(feedback = listOf("Analysis error: ${e.message}"))
        }
    }

    override fun isAnalyzing(): Boolean = isRunning

    /**
     * Map ML Kit landmark type ID to a human-readable name.
     */
    private fun landmarkName(type: Int): String = when (type) {
        0 -> "nose"
        1 -> "left_eye_inner"
        2 -> "left_eye"
        3 -> "left_eye_outer"
        4 -> "right_eye_inner"
        5 -> "right_eye"
        6 -> "right_eye_outer"
        7 -> "left_ear"
        8 -> "right_ear"
        9 -> "left_mouth"
        10 -> "right_mouth"
        11 -> "left_shoulder"
        12 -> "right_shoulder"
        13 -> "left_elbow"
        14 -> "right_elbow"
        15 -> "left_wrist"
        16 -> "right_wrist"
        17 -> "left_pinky"
        18 -> "right_pinky"
        19 -> "left_index"
        20 -> "right_index"
        21 -> "left_thumb"
        22 -> "right_thumb"
        23 -> "left_hip"
        24 -> "right_hip"
        25 -> "left_knee"
        26 -> "right_knee"
        27 -> "left_ankle"
        28 -> "right_ankle"
        29 -> "left_heel"
        30 -> "right_heel"
        31 -> "left_foot_index"
        32 -> "right_foot_index"
        else -> "landmark_$type"
    }
}

