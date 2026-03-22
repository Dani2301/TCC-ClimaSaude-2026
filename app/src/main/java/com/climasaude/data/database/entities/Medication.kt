package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val dosage: String,
    val frequency: String, // daily, weekly, as_needed
    val times: List<String>, // ["08:00", "14:00", "20:00"]
    val startDate: Date,
    val endDate: Date? = null,
    val description: String? = null,
    val sideEffects: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    val weatherSensitive: Boolean = false,
    val temperatureRestrictions: TemperatureRange? = null,
    val humidityRestrictions: HumidityRange? = null,
    val isActive: Boolean = true,
    val reminderEnabled: Boolean = true,
    val doctorName: String? = null,
    val notes: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class TemperatureRange(
    val min: Double,
    val max: Double
)

data class HumidityRange(
    val min: Int,
    val max: Int
)