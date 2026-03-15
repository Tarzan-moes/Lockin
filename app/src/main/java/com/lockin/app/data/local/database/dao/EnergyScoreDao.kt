package com.lockin.app.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lockin.app.data.local.models.EnergyScoreEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyScoreDao {
    @Query("SELECT * FROM energy_scores WHERE userId = :userId ORDER BY date DESC")
    fun observeForUser(userId: String): Flow<List<EnergyScoreEntity>>

    @Query("SELECT * FROM energy_scores WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getByDate(userId: String, date: LocalDate): EnergyScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: EnergyScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scores: List<EnergyScoreEntity>)
}
