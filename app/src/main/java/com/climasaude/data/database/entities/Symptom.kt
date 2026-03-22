package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "symptoms")
data class Symptom(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val intensity: Int, // 1-10
    val notes: String?,
    val timestamp: Date
)
