package com.lockin.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lockin.app.data.local.database.dao.*
import com.lockin.app.data.local.models.*

/**
 * LockinDatabase – Room database definition.
 *
 * This is the single source of truth for all local structured data.
 * Room generates the implementation at compile-time via KSP.
 *
 * Entities and DAOs are stubs for now; actual queries will be added in Part 2.
 */
@Database(
    entities = [
        // Existing entities
        WorkoutPlanEntity::class,
        ExerciseEntity::class,
        HabitEntity::class,
        // New spec entities
        UserEntity::class,
        ExerciseDetailEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class,
        HabitCompletionEntity::class,
        EnergyScoreEntity::class,
        ProgressLogEntity::class,
        // Calendar
        WorkoutScheduleEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LockinDatabase : RoomDatabase() {
    // Existing DAOs
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun habitDao(): HabitDao

    // New DAOs
    abstract fun userDao(): UserDao
    abstract fun exerciseDetailDao(): ExerciseDetailDao
    abstract fun workoutEntityDao(): WorkoutEntityDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun energyScoreDao(): EnergyScoreDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
}
