package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.app.data.local.models.ProgressLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressLogDao {
    @Query("SELECT * FROM progress_logs WHERE userId = :userId ORDER BY date DESC")
    fun observeForUser(userId: String): Flow<List<ProgressLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ProgressLogEntity)
}

