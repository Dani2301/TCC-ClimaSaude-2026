package com.climasaude.domain.usecases

import androidx.annotation.Keep
import com.climasaude.data.repository.HealthRepository
import com.climasaude.domain.models.*
import com.climasaude.utils.Resource
import java.util.*
import javax.inject.Inject

class HealthUseCases @Inject constructor(
    private val healthRepository: HealthRepository
) {

    suspend fun assessHealthRisk(
        userId: String,
        weatherCondition: WeatherCondition
    ): Resource<HealthRiskAssessment> {
        return try {
            val userProfile = healthRepository.getUserHealthProfile(userId)
            val riskFactors = mutableListOf<HealthRiskFactor>()
            var overallRisk = HealthRiskLevel.LOW

            // 1. Risco Climático Universal. Modificado por: Daniel
            val temp = weatherCondition.current.temperature
            val humidity = weatherCondition.current.humidity

            when {
                temp > 40.0 || temp < -5.0 -> {
                    overallRisk = HealthRiskLevel.CRITICAL
                    riskFactors.add(HealthRiskFactor.EXTREME_WEATHER)
                }
                temp > 35.0 || temp < 5.0 || humidity < 25 -> {
                    overallRisk = HealthRiskLevel.HIGH
                    riskFactors.add(HealthRiskFactor.EXTREME_WEATHER)
                }
                temp > 30.0 || humidity < 35 -> {
                    overallRisk = HealthRiskLevel.MEDIUM
                }
            }

            // 2. Qualidade do Ar
            weatherCondition.airQuality?.let { air ->
                if (air.index >= 4) {
                    riskFactors.add(HealthRiskFactor.AIR_QUALITY_RESPIRATORY)
                    if (overallRisk < HealthRiskLevel.HIGH) overallRisk = HealthRiskLevel.HIGH
                } else if (air.index >= 3) {
                    if (overallRisk < HealthRiskLevel.MEDIUM) overallRisk = HealthRiskLevel.MEDIUM
                }
            }

            // 3. Cruzamento com Perfil
            userProfile.medicalConditions.forEach { condition ->
                val impact = assessWeatherImpactOnCondition(condition, weatherCondition)
                if (impact > overallRisk) {
                    overallRisk = impact
                    riskFactors.add(HealthRiskFactor.WEATHER_SENSITIVE_CONDITION)
                }
            }

            // 4. Alice Springs Case (Alergias). Modificado por: Daniel
            if (humidity < 35 && userProfile.allergies.isNotEmpty()) {
                if (overallRisk < HealthRiskLevel.HIGH) overallRisk = HealthRiskLevel.HIGH
                riskFactors.add(HealthRiskFactor.WEATHER_SENSITIVE_CONDITION)
            }

            val assessment = HealthRiskAssessment(
                userId = userId,
                overallRisk = overallRisk,
                riskFactors = riskFactors.distinct(),
                recommendations = generateRiskMitigationRecommendations(riskFactors, weatherCondition, userProfile),
                assessmentDate = Date(),
                validUntil = Date(System.currentTimeMillis() + 6 * 60 * 60 * 1000),
                confidence = 0.9
            )

            Resource.Success(assessment)
        } catch (e: Exception) {
            Resource.Error("Erro ao avaliar risco: ${e.message}")
        }
    }

    private fun assessWeatherImpactOnCondition(condition: MedicalCondition, weather: WeatherCondition): HealthRiskLevel {
        val temp = weather.current.temperature
        val hum = weather.current.humidity
        val name = condition.name.lowercase()

        return when {
            (name.contains("asma") || name.contains("rinite")) && (hum < 30 || temp < 15) -> HealthRiskLevel.HIGH
            (name.contains("artrite") || name.contains("artrose")) && hum > 70 -> HealthRiskLevel.MEDIUM
            name.contains("hipertens") && (temp > 35 || temp < 10) -> HealthRiskLevel.HIGH
            else -> HealthRiskLevel.LOW
        }
    }

    private fun generateRiskMitigationRecommendations(
        factors: List<HealthRiskFactor>,
        weather: WeatherCondition,
        profile: UserProfile
    ): List<String> {
        val recs = mutableListOf<String>()
        if (weather.current.temperature > 35) recs.add("Calor extremo: hidrate-se e evite o sol.")
        if (weather.current.humidity < 35) recs.add("Ar seco: use umidificadores e hidrate as narinas.")
        if (factors.contains(HealthRiskFactor.AIR_QUALITY_RESPIRATORY)) recs.add("Qualidade do ar ruim: use máscara ao sair.")
        
        if (profile.medicalConditions.any { it.name.contains("asma", true) } && weather.current.humidity < 30) {
            recs.add("Atenção: Baixa umidade pode desencadear sua asma.")
        }

        return if (recs.isEmpty()) listOf("Condições favoráveis para sua saúde hoje.") else recs
    }

    // Funções placeholder para manter compatibilidade de build
    suspend fun recordSymptom(u: String, s: Symptom, w: WeatherCondition?): Resource<String> = Resource.Success("Ok")
    suspend fun generateHealthRecommendations(u: String, w: WeatherCondition): Resource<List<Recommendation>> = Resource.Success(emptyList())
    suspend fun checkMedicationAdherence(u: String): Resource<MedicationAdherenceReport> = Resource.Error("N/A")
    suspend fun correlateWeatherSymptoms(u: String, p: Int): Resource<WeatherSymptomCorrelation> = Resource.Error("N/A")
    suspend fun analyzeSymptomTrends(u: String, s: String, d: Int): Resource<SymptomTrendAnalysis> = Resource.Error("N/A")
}

@Keep data class HealthRiskAssessment(val userId: String, val overallRisk: HealthRiskLevel, val riskFactors: List<HealthRiskFactor>, val recommendations: List<String>, val assessmentDate: Date, val validUntil: Date, val confidence: Double)
enum class HealthRiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
enum class HealthRiskFactor { WEATHER_SENSITIVE_CONDITION, RECENT_SEVERE_SYMPTOMS, AIR_QUALITY_RESPIRATORY, MEDICATION_INTERACTION, EXTREME_WEATHER }
data class SymptomTrendAnalysis(val symptomName: String, val trendDirection: String, val averageSeverity: Double, val frequency: Int, val weatherCorrelations: List<String> = emptyList())
data class MedicationAdherenceReport(val overallAdherence: Double, val missedDoses: Int)
data class WeatherSymptomCorrelation(val confidence: Double)
