package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "progress_logs")
data class ProgressLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: LocalDate,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val measurements: List<String> = emptyList(),
    val notes: String = "",
    val photoUri: String? = null
)

