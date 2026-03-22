package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val relationship: String, // family, doctor, friend, etc.
    val isDoctor: Boolean = false,
    val specialty: String? = null,
    val priority: Int = 1, // 1 = highest priority
    val isActive: Boolean = true,
    val notes: String? = null
)