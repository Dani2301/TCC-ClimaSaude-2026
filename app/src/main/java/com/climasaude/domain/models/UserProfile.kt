package com.climasaude.domain.models

import androidx.annotation.Keep
import java.util.Date

@Keep
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "",
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
    val preferences: UserPreferences? = null,
    val isComplete: Boolean = false
)

@Keep
data class MedicalCondition(
    val id: String = "",
    val name: String = "",
    val severity: String = "moderate",
    val diagnosedDate: Date? = null,
    val isWeatherSensitive: Boolean = false,
    val triggerFactors: List<String> = emptyList()
)

@Keep
data class Allergy(
    val id: String = "",
    val name: String = "",
    val severity: String = "moderate",
    val season: String? = null,
    val triggers: List<String> = emptyList()
)

@Keep
data class UserPreferences(
    val theme: String = "auto",
    val language: String = "pt-BR",
    val units: String = "metric",
    val notifications: NotificationPreferences = NotificationPreferences(),
    val privacy: PrivacySettings = PrivacySettings(),
    val location: LocationSettings = LocationSettings()
)

@Keep
data class NotificationPreferences(
    val weatherAlerts: Boolean = true,
    val medicationReminders: Boolean = true,
    val healthTips: Boolean = true,
    val emergencyAlerts: Boolean = true,
    val dailyReports: Boolean = false
)

@Keep
data class PrivacySettings(
    val shareLocation: Boolean = true,
    val shareHealthData: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val crashReportsEnabled: Boolean = true
)

@Keep
data class LocationSettings(
    val autoDetect: Boolean = true,
    val savedLocations: List<SavedLocation> = emptyList(),
    val currentLocation: SavedLocation? = null
)

@Keep
data class SavedLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val country: String = "",
    val isDefault: Boolean = false
)

@Keep
data class RiskFactor(val id: String = "", val name: String = "", val level: String = "", val description: String = "")

@Keep
data class HealthGoal(val id: String = "", val title: String = "", val description: String = "", val targetDate: Date? = null, val isAchieved: Boolean = false)
