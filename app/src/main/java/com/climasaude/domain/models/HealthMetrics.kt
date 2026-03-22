package com.climasaude.domain.models

import java.util.Date

data class HealthMetrics(
    val userId: String,
    val date: Date,
    val symptoms: List<Symptom> = emptyList(),
    val vitalSigns: VitalSigns? = null,
    val medications: List<MedicationTaken> = emptyList(),
    val activityLevel: ActivityLevel? = null,
    val mood: MoodLevel? = null,
    val sleepQuality: SleepQuality? = null,
    val weatherCorrelations: List<WeatherCorrelation> = emptyList(),
    val notes: String? = null,
    val riskScore: Double = 0.0
)

data class Symptom(
    val id: String,
    val name: String,
    val severity: Int, // 1-10 scale
    val duration: String, // minutes, hours, days
    val triggers: List<String> = emptyList(),
    val location: String? = null, // body part if applicable
    val frequency: String? = null,
    val weather: WeatherAtTime? = null,
    val timestamp: Date = Date()
)

data class VitalSigns(
    val heartRate: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val temperature: Double? = null,
    val weight: Double? = null,
    val oxygenSaturation: Int? = null,
    val glucoseLevel: Double? = null
)

data class MedicationTaken(
    val medicationId: String,
    val name: String,
    val dosage: String,
    val timeTaken: Date,
    val effectiveness: Int? = null, // 1-10 scale
    val sideEffects: List<String> = emptyList()
)

data class ActivityLevel(
    val level: String, // low, moderate, high
    val duration: Int, // minutes
    val type: String, // walking, running, cycling, etc.
    val intensity: Int // 1-10 scale
)

data class MoodLevel(
    val level: String, // very_bad, bad, neutral, good, very_good
    val score: Int, // 1-10 scale
    val factors: List<String> = emptyList()
)

data class SleepQuality(
    val hours: Double,
    val quality: String, // poor, fair, good, excellent
    val bedTime: Date,
    val wakeTime: Date,
    val interruptions: Int = 0
)

data class WeatherCorrelation(
    val symptomId: String,
    val weatherFactor: String, // temperature, humidity, pressure, etc.
    val correlation: Double, // -1.0 to 1.0
    val confidence: Double // 0.0 to 1.0
)

data class WeatherAtTime(
    val temperature: Double,
    val humidity: Int,
    val pressure: Double,
    val condition: String
)