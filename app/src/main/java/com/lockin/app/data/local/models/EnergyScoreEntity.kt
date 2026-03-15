package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "energy_scores")
data class EnergyScoreEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: LocalDate,
    val overallScore: Int = 0,
    val sleepScore: Int = 0,
    val hrvScore: Int = 0,
    val trainingLoadScore: Int = 0,
    val stressScore: Int = 0,
    val details: String = "",
    val shouldDeload: Boolean = false
)

