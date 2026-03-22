package com.climasaude.data.network

import com.climasaude.data.network.responses.HealthInsightResponse
import com.climasaude.data.network.responses.MedicationInteractionResponse
import com.climasaude.data.network.responses.SymptomAnalysisResponse
import retrofit2.Response
import retrofit2.http.*

interface HealthApiService {

    @POST("health/symptoms/analyze")
    suspend fun analyzeSymptoms(
        @Body symptoms: List<SymptomData>,
        @Header("Authorization") token: String
    ): Response<SymptomAnalysisResponse>

    @POST("health/medication/interactions")
    suspend fun checkMedicationInteractions(
        @Body medications: List<String>,
        @Header("Authorization") token: String
    ): Response<MedicationInteractionResponse>

    @GET("health/insights")
    suspend fun getHealthInsights(
        @Query("userId") userId: String,
        @Query("period") period: String,
        @Header("Authorization") token: String
    ): Response<HealthInsightResponse>

    @POST("health/emergency/alert")
    suspend fun sendEmergencyAlert(
        @Body emergencyData: EmergencyData,
        @Header("Authorization") token: String
    ): Response<EmergencyResponse>
}

data class SymptomData(
    val name: String,
    val severity: Int,
    val duration: String,
    val weather: WeatherContext?
)

data class WeatherContext(
    val temperature: Double,
    val humidity: Int,
    val pressure: Double,
    val condition: String
)

data class EmergencyData(
    val userId: String,
    val location: LocationData,
    val symptoms: List<String>,
    val vitals: VitalSigns?,
    val emergencyContacts: List<String>
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

data class VitalSigns(
    val heartRate: Int?,
    val bloodPressure: String?,
    val temperature: Double?
)

data class EmergencyResponse(
    val success: Boolean,
    val emergencyId: String,
    val estimatedResponseTime: Int,
    val nearestHospitals: List<HospitalInfo>
)

data class HospitalInfo(
    val name: String,
    val address: String,
    val distance: Double,
    val phone: String
)