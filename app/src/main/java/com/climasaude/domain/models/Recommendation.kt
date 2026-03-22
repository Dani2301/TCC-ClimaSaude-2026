package com.climasaude.domain.models

import java.util.Date

data class Recommendation(
    val id: String,
    val userId: String,
    val type: RecommendationType,
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val priority: Priority,
    val actionRequired: Boolean = false,
    val dueDate: Date? = null,
    val weatherBased: Boolean = false,
    val healthBased: Boolean = false,
    val personalizedScore: Double = 0.0,
    val evidenceLevel: EvidenceLevel,
    val sources: List<String> = emptyList(),
    val relatedSymptoms: List<String> = emptyList(),
    val relatedMedications: List<String> = emptyList(),
    val actionButtons: List<ActionButton> = emptyList(),
    val isRead: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date(),
    val expiresAt: Date? = null
)

enum class RecommendationType {
    PREVENTIVE,
    TREATMENT,
    LIFESTYLE,
    MEDICATION,
    EMERGENCY,
    EDUCATIONAL,
    REMINDER
}

enum class RecommendationCategory {
    WEATHER_PROTECTION,
    ALLERGY_MANAGEMENT,
    MEDICATION_ADJUSTMENT,
    ACTIVITY_MODIFICATION,
    HYDRATION,
    CLOTHING,
    INDOOR_ENVIRONMENT,
    EMERGENCY_PREPARATION,
    HEALTH_MONITORING,
    DOCTOR_CONSULTATION
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class EvidenceLevel {
    LOW,
    MODERATE,
    HIGH,
    CLINICAL_GUIDELINE
}

data class ActionButton(
    val id: String,
    val text: String,
    val action: String,
    val style: String // primary, secondary, warning, danger
)