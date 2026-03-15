package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.app.data.local.models.ExerciseDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDetailDao {
    @Query("SELECT * FROM exercises_full ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseDetailEntity>>

    @Query("SELECT * FROM exercises_full WHERE id = :id")
    suspend fun getById(id: String): ExerciseDetailEntity?

    @Query("SELECT COUNT(*) FROM exercises_full")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExerciseDetailEntity)
}
