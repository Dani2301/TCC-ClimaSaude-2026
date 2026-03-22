package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "health_alerts")
data class HealthAlert(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val description: String,
    val recommendation: String,
    val weatherCondition: String? = null,
    val temperature: Double? = null,
    val humidity: Int? = null,
    val uvIndex: Double? = null,
    val airQuality: Int? = null,
    val isRead: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val expiresAt: Date? = null,
    val actionTaken: String? = null,
    val relatedSymptoms: List<String> = emptyList(),
    val location: String? = null
)

enum class AlertType {
    WEATHER_WARNING,
    MEDICATION_REMINDER,
    SYMPTOM_CORRELATION,
    UV_PROTECTION,
    AIR_QUALITY,
    TEMPERATURE_SENSITIVITY,
    HUMIDITY_WARNING,
    ALLERGY_FORECAST,
    EMERGENCY
}

enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}