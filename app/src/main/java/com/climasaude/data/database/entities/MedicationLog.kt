package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey
    val id: String,
    val medicationId: String,
    val userId: String,
    val scheduledTime: Date,
    val takenTime: Date?,
    val isTaken: Boolean = false,
    val dosage: String,
    val notes: String? = null,
    val reminderTriggered: Boolean = false,
    val snoozedCount: Int = 0,
    val createdAt: Date = Date()
)