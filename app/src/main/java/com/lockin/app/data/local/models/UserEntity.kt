package com.lockin.app.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val gender: String = "UNSPECIFIED",
    val dateOfBirth: LocalDate? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val goal: String = "GENERAL_FITNESS",
    val experienceLevel: String = "BEGINNER",
    val availableEquipment: List<String> = emptyList(),
    val injuries: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)

