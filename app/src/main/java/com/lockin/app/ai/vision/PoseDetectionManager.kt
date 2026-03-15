package com.lockin.app.ai.vision

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.atan2

/**
 * PoseDetectionManager – Real-time form analysis using ML Kit Pose Detection.
 *
 * Uses ML Kit's Accurate model in STREAM_MODE for real-time camera analysis.
 * Provides exercise-specific form feedback for squat, bench press, and deadlift
 * by computing joint angles from detected pose landmarks.
 *
 * All heavy operations are suspend functions to keep the UI responsive.
 */
class PoseDetectionManager {

    companion object {
        private const val TAG = "PoseDetectionManager"

        // ── Angle thresholds for exercise form analysis ──────────────────

        // Squat thresholds
        private const val SQUAT_DEPTH_EXCELLENT = 70f    // Knee angle at parallel or below
        private const val SQUAT_DEPTH_GOOD = 90f         // Above parallel but decent
        private const val SQUAT_DEPTH_POOR = 120f        // Quarter squat
        private const val SQUAT_BACK_ANGLE_MAX = 45f     // Max forward lean from vertical
        private const val SQUAT_KNEE_CAVE_THRESHOLD = 15f // Max inward knee deviation (degrees)

        // Bench press thresholds
        private const val BENCH_ELBOW_FLARE_MAX = 75f    // Max angle at shoulder (elbow flare)
        private const val BENCH_ELBOW_BOTTOM_MIN = 75f   // Min elbow angle at bottom
        private const val BENCH_ELBOW_BOTTOM_MAX = 100f  // Max elbow angle at bottom

        // Deadlift thresholds
        private const val DEADLIFT_BACK_ANGLE_MAX = 30f  // Max rounding (deviation from flat)
        private const val DEADLIFT_HIP_HINGE_MIN = 60f   // Min hip angle at bottom
        private const val DEADLIFT_LOCKOUT_ANGLE = 170f  // Hip angle at lockout
    }

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector: PoseDetector = PoseDetection.getClient(options)

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Detect pose landmarks from an [InputImage].
     * Returns null if no person is detected in the frame.
     */
    suspend fun detectPose(image: InputImage): Pose? {
        return try {
            val pose = poseDetector.process(image).await()
            if (pose.allPoseLandmarks.isEmpty()) null else pose
        } catch (e: Exception) {
            Log.e(TAG, "Pose detection failed: ${e.message}", e)
            null
        }
    }

    /**
     * Analyze squat form from detected pose landmarks.
     */
    fun analyzeSquatForm(pose: Pose): FormFeedback {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        if (leftHip == null || rightHip == null || leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null || leftShoulder == null || rightShoulder == null
        ) {
            return FormFeedback.error("Cannot detect all landmarks. Please adjust your camera angle.")
        }

        val tips = mutableListOf<String>()
        var totalScore = 100

        // 1. Knee angle (depth)
        val leftKneeAngle = calculateAngle(
            leftHip.position, leftKnee.position, leftAnkle.position
        )
        val rightKneeAngle = calculateAngle(
            rightHip.position, rightKnee.position, rightAnkle.position
        )
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2f

        when {
            avgKneeAngle <= SQUAT_DEPTH_EXCELLENT -> {
                tips.add("✅ Excellent depth! Below parallel.")
            }
            avgKneeAngle <= SQUAT_DEPTH_GOOD -> {
                tips.add("👍 Good depth. Try to get a bit lower for full range of motion.")
                totalScore -= 10
            }
            avgKneeAngle <= SQUAT_DEPTH_POOR -> {
                tips.add("⚠️ Quarter squat detected. Aim to break parallel for full benefits.")
                totalScore -= 25
            }
            else -> {
                tips.add("❌ Very shallow squat. Work on hip and ankle mobility to increase depth.")
                totalScore -= 40
            }
        }

        // 2. Back angle (forward lean)
        val midShoulder = PointF(
            (leftShoulder.position.x + rightShoulder.position.x) / 2f,
            (leftShoulder.position.y + rightShoulder.position.y) / 2f
        )
        val midHip = PointF(
            (leftHip.position.x + rightHip.position.x) / 2f,
            (leftHip.position.y + rightHip.position.y) / 2f
        )
        val backAngle = calculateVerticalAngle(midShoulder, midHip)

        if (backAngle > SQUAT_BACK_ANGLE_MAX) {
            tips.add("⚠️ Excessive forward lean (${backAngle.toInt()}°). Keep your chest up and core braced.")
            totalScore -= 15
        } else {
            tips.add("✅ Good torso position.")
        }

        // 3. Knee tracking (valgus/cave)
        val kneeXDiff = abs(leftKnee.position.x - rightKnee.position.x)
        val ankleXDiff = abs(leftAnkle.position.x - rightAnkle.position.x)
        if (ankleXDiff > 0 && kneeXDiff < ankleXDiff * 0.75f) {
            tips.add("⚠️ Knees appear to be caving inward. Push your knees out over your toes.")
            totalScore -= 15
        }

        totalScore = totalScore.coerceIn(0, 100)

        val quality = scoreToQuality(totalScore)

        return FormFeedback(
            quality = quality,
            message = "Squat analysis: ${quality.name.lowercase().replace('_', ' ')} form",
            score = totalScore,
            tips = tips
        )
    }

    /**
     * Analyze bench press form from detected pose landmarks.
     */
    fun analyzeBenchPressForm(pose: Pose): FormFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (leftShoulder == null || rightShoulder == null || leftElbow == null ||
            rightElbow == null || leftWrist == null || rightWrist == null
        ) {
            return FormFeedback.error("Cannot detect upper body landmarks. Ensure arms are visible.")
        }

        val tips = mutableListOf<String>()
        var totalScore = 100

        // 1. Elbow angle (should be ~90° at bottom of press)
        val leftElbowAngle = calculateAngle(
            leftShoulder.position, leftElbow.position, leftWrist.position
        )
        val rightElbowAngle = calculateAngle(
            rightShoulder.position, rightElbow.position, rightWrist.position
        )
        val avgElbowAngle = (leftElbowAngle + rightElbowAngle) / 2f

        when {
            avgElbowAngle in BENCH_ELBOW_BOTTOM_MIN..BENCH_ELBOW_BOTTOM_MAX -> {
                tips.add("✅ Good elbow position at the bottom of the press.")
            }
            avgElbowAngle < BENCH_ELBOW_BOTTOM_MIN -> {
                tips.add("⚠️ Elbows too tucked (${avgElbowAngle.toInt()}°). Allow slightly more flare for chest activation.")
                totalScore -= 10
            }
            else -> {
                tips.add("⚠️ Arms not fully descending. Lower the bar to your chest for full ROM.")
                totalScore -= 15
            }
        }

        // 2. Elbow flare (angle of upper arm relative to torso)
        // Approximate by checking how far elbows are from the shoulder-hip line
        if (leftShoulder != null && leftElbow != null && leftHip != null) {
            val flareAngle = calculateAngle(
                leftHip.position, leftShoulder.position, leftElbow.position
            )
            if (flareAngle > BENCH_ELBOW_FLARE_MAX) {
                tips.add("⚠️ Elbows flaring too wide (${flareAngle.toInt()}°). Tuck elbows ~45° to protect shoulders.")
                totalScore -= 20
            } else {
                tips.add("✅ Good elbow tuck angle.")
            }
        }

        // 3. Wrist alignment (wrists should be roughly above elbows)
        val leftWristElbowDiff = abs(leftWrist.position.x - leftElbow.position.x)
        val rightWristElbowDiff = abs(rightWrist.position.x - rightElbow.position.x)
        val avgWristDrift = (leftWristElbowDiff + rightWristElbowDiff) / 2f
        if (avgWristDrift > 50f) { // pixel threshold (approximate)
            tips.add("⚠️ Wrists drifting from above elbows. Keep wrists stacked for joint safety.")
            totalScore -= 10
        }

        totalScore = totalScore.coerceIn(0, 100)
        val quality = scoreToQuality(totalScore)

        return FormFeedback(
            quality = quality,
            message = "Bench press analysis: ${quality.name.lowercase().replace('_', ' ')} form",
            score = totalScore,
            tips = tips
        )
    }

    /**
     * Analyze deadlift form from detected pose landmarks.
     */
    fun analyzeDeadliftForm(pose: Pose): FormFeedback {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        if (leftShoulder == null || rightShoulder == null || leftHip == null ||
            rightHip == null || leftKnee == null || rightKnee == null
        ) {
            return FormFeedback.error("Cannot detect key landmarks. Ensure your full body is visible from the side.")
        }

        val tips = mutableListOf<String>()
        var totalScore = 100

        // 1. Back angle (should be relatively flat – shoulder to hip line)
        val midShoulder = PointF(
            (leftShoulder.position.x + rightShoulder.position.x) / 2f,
            (leftShoulder.position.y + rightShoulder.position.y) / 2f
        )
        val midHip = PointF(
            (leftHip.position.x + rightHip.position.x) / 2f,
            (leftHip.position.y + rightHip.position.y) / 2f
        )

        // For deadlift, we measure the angle of the back from horizontal
        val backFromHorizontal = calculateHorizontalAngle(midShoulder, midHip)

        // During the lift, some forward angle is expected; excessive rounding is bad
        if (backFromHorizontal < 20f) {
            tips.add("✅ Excellent back position. Maintaining a neutral spine.")
        } else if (backFromHorizontal < DEADLIFT_BACK_ANGLE_MAX) {
            tips.add("👍 Acceptable back angle. Focus on keeping your chest up.")
            totalScore -= 10
        } else {
            tips.add("❌ Excessive back rounding detected (${backFromHorizontal.toInt()}°). " +
                    "Brace your core, push chest out, and engage lats before pulling.")
            totalScore -= 30
        }

        // 2. Hip hinge angle
        val leftHipAngle = calculateAngle(
            leftShoulder.position, leftHip.position, leftKnee.position
        )
        val rightHipAngle = calculateAngle(
            rightShoulder.position, rightHip.position, rightKnee.position
        )
        val avgHipAngle = (leftHipAngle + rightHipAngle) / 2f

        when {
            avgHipAngle >= DEADLIFT_LOCKOUT_ANGLE -> {
                tips.add("✅ Full lockout achieved. Squeeze glutes at the top.")
            }
            avgHipAngle >= DEADLIFT_HIP_HINGE_MIN -> {
                tips.add("👍 Good hip hinge. Drive your hips forward to complete the lockout.")
                totalScore -= 5
            }
            else -> {
                tips.add("⚠️ Hip angle too closed (${avgHipAngle.toInt()}°). Ensure you're hinging at the hips, not squatting the weight up.")
                totalScore -= 20
            }
        }

        // 3. Knee position (shouldn't travel too far forward)
        if (leftKnee != null && leftAnkle != null) {
            val kneeForwardDrift = leftKnee.position.x - leftAnkle.position.x
            if (abs(kneeForwardDrift) > 80f) { // approximate pixel threshold
                tips.add("⚠️ Knees drifting too far forward. Push your hips back more – this is a hip hinge, not a squat.")
                totalScore -= 10
            }
        }

        totalScore = totalScore.coerceIn(0, 100)
        val quality = scoreToQuality(totalScore)

        return FormFeedback(
            quality = quality,
            message = "Deadlift analysis: ${quality.name.lowercase().replace('_', ' ')} form",
            score = totalScore,
            tips = tips
        )
    }

    /**
     * Analyze form for any supported exercise by name.
     * Falls back to a generic assessment if the exercise isn't specifically supported.
     */
    fun analyzeForm(exerciseName: String, pose: Pose): FormFeedback {
        val name = exerciseName.lowercase()
        return when {
            name.contains("squat") -> analyzeSquatForm(pose)
            name.contains("bench") || name.contains("press") && name.contains("chest") -> analyzeBenchPressForm(pose)
            name.contains("deadlift") || name.contains("dead lift") -> analyzeDeadliftForm(pose)
            else -> analyzeGenericForm(pose)
        }
    }

    /**
     * Release the pose detector resources.
     */
    fun release() {
        poseDetector.close()
        Log.i(TAG, "Pose detector released")
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Generic form analysis when exercise-specific logic isn't available.
     * Checks basic posture quality.
     */
    private fun analyzeGenericForm(pose: Pose): FormFeedback {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.size < 15) {
            return FormFeedback.error("Not enough landmarks detected for analysis.")
        }

        val tips = mutableListOf<String>()
        tips.add("ℹ️ Specific form analysis isn't available for this exercise yet.")
        tips.add("💡 General tip: Focus on controlled movement through full range of motion.")
        tips.add("💡 Keep your core engaged and maintain steady breathing.")

        return FormFeedback(
            quality = FormQuality.GOOD,
            message = "Generic form assessment – ${landmarks.size} landmarks detected",
            score = 70,
            tips = tips
        )
    }

    private fun scoreToQuality(score: Int): FormQuality = when {
        score >= 90 -> FormQuality.EXCELLENT
        score >= 75 -> FormQuality.GOOD
        score >= 60 -> FormQuality.FAIR
        score >= 40 -> FormQuality.POOR
        else -> FormQuality.NEEDS_IMPROVEMENT
    }
}

// ── Geometry utilities ───────────────────────────────────────────────────────

/**
 * Calculate the angle at point [p2] formed by [p1]-[p2]-[p3].
 * Returns degrees in range [0, 180].
 */
fun calculateAngle(p1: PointF, p2: PointF, p3: PointF): Float {
    val radians = atan2(
        (p3.y - p2.y).toDouble(),
        (p3.x - p2.x).toDouble()
    ) - atan2(
        (p1.y - p2.y).toDouble(),
        (p1.x - p2.x).toDouble()
    )
    var degrees = Math.toDegrees(radians).toFloat()
    degrees = abs(degrees)
    if (degrees > 180f) degrees = 360f - degrees
    return degrees
}

/**
 * Calculate the angle of the line from [upper] to [lower] relative to vertical.
 * 0° = perfectly vertical, 90° = horizontal.
 */
fun calculateVerticalAngle(upper: PointF, lower: PointF): Float {
    val dx = (lower.x - upper.x).toDouble()
    val dy = (lower.y - upper.y).toDouble()
    val angleFromVertical = Math.toDegrees(atan2(abs(dx), abs(dy)))
    return angleFromVertical.toFloat()
}

/**
 * Calculate the angle of the line from [p1] to [p2] relative to horizontal.
 * Used for assessing back rounding in deadlifts.
 */
fun calculateHorizontalAngle(p1: PointF, p2: PointF): Float {
    val dx = (p2.x - p1.x).toDouble()
    val dy = (p2.y - p1.y).toDouble()
    val angleFromHorizontal = Math.toDegrees(atan2(abs(dy), abs(dx)))
    return angleFromHorizontal.toFloat()
}

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * Result of a form analysis for a specific exercise.
 */
data class FormFeedback(
    /** Overall form quality grade. */
    val quality: FormQuality,
    /** Short human-readable summary message. */
    val message: String,
    /** Numeric score from 0–100. */
    val score: Int,
    /** Concrete corrective tips / cues. */
    val tips: List<String>
) {
    companion object {
        /** Factory for generic error feedback when analysis can't be performed. */
        fun error(message: String = "Unable to analyze form. Ensure your full body is visible."): FormFeedback =
            FormFeedback(
                quality = FormQuality.NEEDS_IMPROVEMENT,
                message = message,
                score = 0,
                tips = listOf(
                    "📷 Make sure your full body is visible in the camera frame.",
                    "💡 Stand 6–10 feet away from the camera.",
                    "💡 Ensure good lighting for accurate detection."
                )
            )
    }
}

/**
 * Quality grade for exercise form.
 */
enum class FormQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    NEEDS_IMPROVEMENT
}

