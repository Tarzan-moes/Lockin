package com.lockin.app.ai.vision

import com.lockin.app.ai.inference.FormAnalysisResult

/**
 * FormAnalyzer – Processes camera frames for real-time exercise form feedback.
 *
 * This will use ML Kit's Pose Detection or a custom PoseNet/MoveNet model
 * to detect body landmarks, then evaluate form quality for the current exercise.
 *
 * For Part 1 this is a stub interface. The camera pipeline and model
 * loading will be implemented when we build the workout execution screen.
 */
interface FormAnalyzer {

    /** Start the camera-based form analysis pipeline. */
    fun startAnalysis(exerciseName: String)

    /** Stop the analysis and release camera/model resources. */
    fun stopAnalysis()

    /**
     * Process a single camera frame.
     * @param frameData The raw image bytes from CameraX.
     * @return Analysis result with joint positions and feedback.
     */
    suspend fun processFrame(frameData: ByteArray): FormAnalysisResult

    /** Whether the analyzer is currently running. */
    fun isAnalyzing(): Boolean
}

