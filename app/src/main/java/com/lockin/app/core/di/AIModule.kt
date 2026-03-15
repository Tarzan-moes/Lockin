package com.lockin.app.core.di

import android.content.Context
import com.lockin.app.ai.AIModel
import com.lockin.app.ai.coach.AiCoachManager
import com.lockin.app.ai.coach.AiCoachManagerImpl
import com.lockin.app.ai.coach.WorkoutGenerator
import com.lockin.app.ai.inference.GemmaModel
import com.lockin.app.ai.inference.ModelManager
import com.lockin.app.ai.vision.ExerciseClassifier
import com.lockin.app.ai.vision.FormAnalyzer
import com.lockin.app.ai.vision.FormAnalyzerImpl
import com.lockin.app.ai.vision.PoseDetectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AIModule – Hilt module providing all on-device AI components.
 *
 * All AI singletons are created here so they share the same model instances
 * and can be injected anywhere in the app. The LLM (GemmaModel) is NOT loaded
 * at app startup – it uses lazy initialization on first use (e.g., when the
 * user opens the AI Coach screen).
 *
 * Components provided:
 * - [AIModel] → [GemmaModel] (TFLite LLM for coaching + workout generation)
 * - [AiCoachManager] → [AiCoachManagerImpl] (high-level chat orchestrator)
 * - [WorkoutGenerator] (AI-powered workout plan creation)
 * - [PoseDetectionManager] (ML Kit pose detection for form analysis)
 * - [FormAnalyzer] → [FormAnalyzerImpl] (bridges legacy interface to pose detection)
 * - [ExerciseClassifier] (TFLite exercise recognition from pose)
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    // ── LLM / Text Generation ────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context
    ): ModelManager = ModelManager(context)

    @Provides
    @Singleton
    fun provideAIModel(
        @ApplicationContext context: Context
    ): AIModel = GemmaModel(context)

    // ── AI Coach ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAiCoachManager(
        model: AIModel
    ): AiCoachManager = AiCoachManagerImpl(model)

    // ── Workout Generator ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideWorkoutGenerator(
        model: AIModel
    ): WorkoutGenerator = WorkoutGenerator(model)

    // ── Pose Detection & Form Analysis ───────────────────────────────────

    @Provides
    @Singleton
    fun providePoseDetectionManager(): PoseDetectionManager = PoseDetectionManager()

    @Provides
    @Singleton
    fun provideFormAnalyzer(
        poseDetectionManager: PoseDetectionManager
    ): FormAnalyzer = FormAnalyzerImpl(poseDetectionManager)

    // ── Exercise Classifier ──────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideExerciseClassifier(
        @ApplicationContext context: Context
    ): ExerciseClassifier = ExerciseClassifier(context)
}

