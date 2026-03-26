package com.climasaude.data.database.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Senior Fix: @Keep impede que o ProGuard renomeie os campos no APK final. Modificado por: Daniel
@Keep
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val birthDate: Date? = null,
    val gender: String? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val medicalConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val emergencyContact: String? = null,
    val notificationPreferences: Map<String, Boolean> = emptyMap(),
    val privacySettings: Map<String, Boolean> = emptyMap(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val isActive: Boolean = true,
    val theme: String = "auto",
    val language: String = "pt-BR"
)
