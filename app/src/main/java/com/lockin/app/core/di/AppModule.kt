package com.lockin.app.core.di

import android.content.Context
import androidx.room.Room
import com.lockin.app.core.util.Constants
import com.lockin.app.data.local.database.LockinDatabase
import com.lockin.app.data.local.database.dao.WorkoutDao
import com.lockin.app.data.local.database.dao.WorkoutScheduleDao
import com.lockin.app.data.repository.*
import com.lockin.app.domain.repository.*
import com.lockin.app.domain.usecase.CalculateEnergyScoreUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule – Hilt module that lives for the entire application lifecycle.
 *
 * Provides singletons for the database, repositories, and managers.
 * For Part 1 we bind domain interfaces to their stub implementations.
 * When real implementations are ready, we swap them here — the rest
 * of the app never needs to change (Dependency Inversion at work).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Database ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLockinDatabase(
        @ApplicationContext context: Context
    ): LockinDatabase = Room.databaseBuilder(
        context,
        LockinDatabase::class.java,
        Constants.DATABASE_NAME
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    // ── Repositories ─────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = UserRepositoryImpl()

    @Provides
    @Singleton
    fun provideWorkoutRepository(): WorkoutRepository = WorkoutRepositoryImpl()

    @Provides
    @Singleton
    fun provideExerciseRepository(): ExerciseRepository = ExerciseRepositoryImpl()

    @Provides
    @Singleton
    fun provideHabitRepository(): HabitRepository = HabitRepositoryImpl()

    @Provides
    @Singleton
    fun provideProgressRepository(): ProgressRepository = ProgressRepositoryImpl()

    @Provides
    @Singleton
    fun provideHealthRepository(
        calculateEnergyScore: CalculateEnergyScoreUseCase
    ): HealthRepository = HealthRepositoryImpl(calculateEnergyScore)

    @Provides
    @Singleton
    fun provideAiCoachRepository(): AiCoachRepository = AiCoachRepositoryImpl()

    @Provides
    @Singleton
    fun provideWorkoutEntityDao(db: LockinDatabase) = db.workoutEntityDao()

    @Provides
    @Singleton
    fun provideWorkoutDao(db: LockinDatabase) = db.workoutDao()

    @Provides
    @Singleton
    fun provideWorkoutExerciseDao(db: LockinDatabase) = db.workoutExerciseDao()

    @Provides
    @Singleton
    fun provideExerciseSetDao(db: LockinDatabase) = db.exerciseSetDao()

    @Provides
    @Singleton
    fun provideExerciseDetailDao(db: LockinDatabase) = db.exerciseDetailDao()

    @Provides
    @Singleton
    fun provideEnergyScoreDao(db: LockinDatabase) = db.energyScoreDao()

    @Provides
    @Singleton
    fun provideWorkoutScheduleDao(db: LockinDatabase) = db.workoutScheduleDao()

    @Provides
    @Singleton
    fun provideCalendarRepository(
        scheduleDao: WorkoutScheduleDao,
        workoutDao: WorkoutDao
    ): CalendarRepository = CalendarRepositoryImpl(scheduleDao, workoutDao)

    @Provides
    @Singleton
    fun provideWorkoutEntityRepository(db: LockinDatabase): WorkoutEntityRepository =
        WorkoutEntityRepository(db.workoutEntityDao())

    @Provides
    @Singleton
    fun provideEnergyScoreRepository(db: LockinDatabase): EnergyScoreRepository =
        EnergyScoreRepository(db.energyScoreDao())
}
