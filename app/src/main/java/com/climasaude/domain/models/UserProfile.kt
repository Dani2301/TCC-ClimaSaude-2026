package com.climasaude.domain.models

import java.util.Date

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val birthDate: Date? = null,
    val age: Int? = null,
    val gender: String? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val bmi: Float? = null,
    val medicalConditions: List<MedicalCondition> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
    val riskFactors: List<RiskFactor> = emptyList(),
    val healthGoals: List<HealthGoal> = emptyList(),
    val preferences: UserPreferences,
    val isComplete: Boolean = false
)

data class MedicalCondition(
    val id: String,
    val name: String,
    val severity: String,
    val diagnosedDate: Date?,
    val isWeatherSensitive: Boolean = false,
    val triggerFactors: List<String> = emptyList()
)

data class Allergy(
    val id: String,
    val name: String,
    val severity: String,
    val season: String? = null,
    val triggers: List<String> = emptyList()
)

data class RiskFactor(
    val id: String,
    val name: String,
    val level: String,
    val description: String
)

data class HealthGoal(
    val id: String,
    val title: String,
    val description: String,
    val targetDate: Date?,
    val isAchieved: Boolean = false
)

data class UserPreferences(
    val theme: String = "auto",
    val language: String = "pt-BR",
    val units: String = "metric", // metric, imperial
    val notifications: NotificationPreferences,
    val privacy: PrivacySettings,
    val location: LocationSettings
)

data class NotificationPreferences(
    val weatherAlerts: Boolean = true,
    val medicationReminders: Boolean = true,
    val healthTips: Boolean = true,
    val emergencyAlerts: Boolean = true,
    val dailyReports: Boolean = false,
    val quietHours: QuietHours? = null
)

data class QuietHours(
    val enabled: Boolean = false,
    val startTime: String = "22:00",
    val endTime: String = "07:00"
)

data class PrivacySettings(
    val shareLocation: Boolean = true,
    val shareHealthData: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val crashReportsEnabled: Boolean = true
)

data class LocationSettings(
    val autoDetect: Boolean = true,
    val savedLocations: List<SavedLocation> = emptyList(),
    val currentLocation: SavedLocation? = null
)

data class SavedLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val country: String,
    val isDefault: Boolean = false
)