package com.climasaude.domain.usecases

import com.climasaude.data.repository.HealthRepository
import com.climasaude.domain.models.*
import com.climasaude.utils.Resource
import java.util.*
import javax.inject.Inject

class HealthUseCases @Inject constructor(
    private val healthRepository: HealthRepository
) {

    suspend fun recordSymptom(
        userId: String,
        symptom: Symptom,
        weatherCondition: WeatherCondition?
    ): Resource<String> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            symptom.name.isBlank() -> Resource.Error("Nome do sintoma é obrigatório")
            symptom.severity !in 1..10 -> Resource.Error("Severidade deve ser entre 1 e 10")
            else -> {
                val enhancedSymptom = symptom.copy(
                    weather = weatherCondition?.let { weather ->
                        WeatherAtTime(
                            temperature = weather.current.temperature,
                            humidity = weather.current.humidity,
                            pressure = weather.current.pressure,
                            condition = weather.current.condition
                        )
                    }
                )
                healthRepository.recordSymptom(userId, enhancedSymptom)
            }
        }
    }

    suspend fun analyzeSymptomTrends(
        userId: String,
        symptomName: String,
        days: Int = 30
    ): Resource<SymptomTrendAnalysis> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            symptomName.isBlank() -> Resource.Error("Nome do sintoma é obrigatório")
            days !in 1..365 -> Resource.Error("Período deve ser entre 1 e 365 dias")
            else -> healthRepository.analyzeSymptomTrends(userId, symptomName, days)
        }
    }

    suspend fun generateHealthRecommendations(
        userId: String,
        currentWeather: WeatherCondition
    ): Resource<List<Recommendation>> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            else -> {
                val userProfile = healthRepository.getUserHealthProfile(userId)
                val recommendations = mutableListOf<Recommendation>()

                // Recomendações baseadas no clima
                recommendations.addAll(generateWeatherBasedRecommendations(currentWeather, userProfile))

                // Recomendações baseadas no histórico de saúde
                recommendations.addAll(generateHealthBasedRecommendations(userProfile))

                // Recomendações de medicamentos
                recommendations.addAll(generateMedicationRecommendations(userProfile, currentWeather))

                Resource.Success(recommendations.sortedByDescending { it.priority })
            }
        }
    }

    suspend fun checkMedicationAdherence(userId: String): Resource<MedicationAdherenceReport> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            else -> healthRepository.checkMedicationAdherence(userId)
        }
    }

    suspend fun correlateWeatherSymptoms(
        userId: String,
        period: Int = 90
    ): Resource<WeatherSymptomCorrelation> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            period !in 30..365 -> Resource.Error("Período deve ser entre 30 e 365 dias")
            else -> healthRepository.correlateWeatherSymptoms(userId, period)
        }
    }

    suspend fun assessHealthRisk(
        userId: String,
        weatherCondition: WeatherCondition
    ): Resource<HealthRiskAssessment> {
        return try {
            val userProfile = healthRepository.getUserHealthProfile(userId)
            val recentSymptoms = healthRepository.getRecentSymptoms(userId, 7) // últimos 7 dias

            val riskFactors = mutableListOf<HealthRiskFactor>()
            var overallRisk = HealthRiskLevel.LOW

            // Avaliação baseada em condições médicas
            userProfile.medicalConditions.forEach { condition ->
                if (condition.isWeatherSensitive) {
                    val weatherRisk = assessWeatherImpactOnCondition(condition, weatherCondition)
                    if (weatherRisk > HealthRiskLevel.LOW) {
                        riskFactors.add(HealthRiskFactor.WEATHER_SENSITIVE_CONDITION)
                        if (weatherRisk > overallRisk) overallRisk = weatherRisk
                    }
                }
            }

            // Avaliação baseada em sintomas recentes
            val severeSymptomsCount = recentSymptoms.count { it.severity >= 7 }
            if (severeSymptomsCount > 0) {
                riskFactors.add(HealthRiskFactor.RECENT_SEVERE_SYMPTOMS)
                if (overallRisk == HealthRiskLevel.LOW) overallRisk = HealthRiskLevel.MEDIUM
            }

            // Avaliação baseada na qualidade do ar
            weatherCondition.airQuality?.let { airQuality ->
                if (airQuality.index >= 4 && userProfile.medicalConditions.any {
                        it.name.contains("asma", ignoreCase = true) ||
                                it.name.contains("respiratória", ignoreCase = true)
                    }) {
                    riskFactors.add(HealthRiskFactor.AIR_QUALITY_RESPIRATORY)
                    overallRisk = HealthRiskLevel.HIGH
                }
            }

            val assessment = HealthRiskAssessment(
                userId = userId,
                overallRisk = overallRisk,
                riskFactors = riskFactors,
                recommendations = generateRiskMitigationRecommendations(riskFactors),
                assessmentDate = Date(),
                validUntil = Date(System.currentTimeMillis() + 6 * 60 * 60 * 1000), // 6 horas
                confidence = calculateConfidence(userProfile, recentSymptoms.size)
            )

            Resource.Success(assessment)
        } catch (e: Exception) {
            Resource.Error("Erro ao avaliar risco de saúde: ${e.message}")
        }
    }

    private fun generateWeatherBasedRecommendations(
        weather: WeatherCondition,
        userProfile: UserProfile?
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Recomendações para temperatura alta
        if (weather.current.temperature > 30.0) {
            recommendations.add(
                Recommendation(
                    id = UUID.randomUUID().toString(),
                    userId = userProfile?.id ?: "",
                    type = RecommendationType.PREVENTIVE,
                    category = RecommendationCategory.WEATHER_PROTECTION,
                    title = "Proteção contra calor intenso",
                    description = "A temperatura está alta (${weather.current.temperature.toInt()}°C). Mantenha-se hidratado e evite exposição prolongada ao sol.",
                    priority = Priority.HIGH,
                    weatherBased = true,
                    evidenceLevel = EvidenceLevel.HIGH
                )
            )
        }

        // Recomendações para qualidade do ar
        weather.airQuality?.let { airQuality ->
            if (airQuality.index >= 3) {
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        userId = userProfile?.id ?: "",
                        type = RecommendationType.PREVENTIVE,
                        category = RecommendationCategory.INDOOR_ENVIRONMENT,
                        title = "Qualidade do ar comprometida",
                        description = "A qualidade do ar está ${airQuality.level.lowercase()}. Evite atividades ao ar livre e mantenha janelas fechadas.",
                        priority = if (airQuality.index >= 4) Priority.HIGH else Priority.MEDIUM,
                        weatherBased = true,
                        evidenceLevel = EvidenceLevel.HIGH
                    )
                )
            }
        }

        return recommendations
    }

    private fun generateHealthBasedRecommendations(userProfile: UserProfile?): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        userProfile?.allergies?.forEach { allergy ->
            if (allergy.season == getCurrentSeason()) {
                recommendations.add(
                    Recommendation(
                        id = UUID.randomUUID().toString(),
                        userId = userProfile.id,
                        type = RecommendationType.PREVENTIVE,
                        category = RecommendationCategory.ALLERGY_MANAGEMENT,
                        title = "Alerta de alergia sazonal",
                        description = "Época de ${allergy.name}. Considere medicação preventiva e evite exposição aos alérgenos.",
                        priority = Priority.MEDIUM,
                        healthBased = true,
                        evidenceLevel = EvidenceLevel.MODERATE
                    )
                )
            }
        }

        return recommendations
    }

    private fun generateMedicationRecommendations(
        userProfile: UserProfile?,
        weather: WeatherCondition
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        userProfile?.medicalConditions?.forEach { condition ->
            when {
                // Cold weather and respiratory conditions
                weather.current.temperature < 10 && 
                (condition.name.contains("asma", ignoreCase = true) || 
                 condition.name.contains("bronquite", ignoreCase = true)) -> {
                    recommendations.add(
                        Recommendation(
                            id = UUID.randomUUID().toString(),
                            userId = userProfile.id,
                            type = RecommendationType.MEDICATION,
                            category = RecommendationCategory.MEDICATION_ADJUSTMENT,
                            title = "Medicação para o frio",
                            description = "Tempo frio pode agravar sintomas respiratórios. Mantenha broncodilatadores acessíveis",
                            priority = Priority.HIGH,
                            weatherBased = true,
                            healthBased = true,
                            evidenceLevel = EvidenceLevel.MODERATE,
                            relatedSymptoms = listOf(condition.name)
                        )
                    )
                }
                
                // High humidity and joint conditions
                weather.current.humidity > 75 && 
                (condition.name.contains("artrite", ignoreCase = true) ||
                 condition.name.contains("artrose", ignoreCase = true)) -> {
                    recommendations.add(
                        Recommendation(
                            id = UUID.randomUUID().toString(),
                            userId = userProfile.id,
                            type = RecommendationType.MEDICATION,
                            category = RecommendationCategory.MEDICATION_ADJUSTMENT,
                            title = "Alta umidade e articulações",
                            description = "Alta umidade pode aumentar dores articulares. Considere anti-inflamatórios conforme prescrição",
                            priority = Priority.MEDIUM,
                            weatherBased = true,
                            healthBased = true,
                            evidenceLevel = EvidenceLevel.MODERATE,
                            relatedSymptoms = listOf(condition.name)
                        )
                    )
                }
                
                // Low pressure and migraines
                weather.current.pressure < 1010 && 
                (condition.name.contains("enxaqueca", ignoreCase = true) ||
                 condition.name.contains("cefaleia", ignoreCase = true)) -> {
                    recommendations.add(
                        Recommendation(
                            id = UUID.randomUUID().toString(),
                            userId = userProfile.id,
                            type = RecommendationType.MEDICATION,
                            category = RecommendationCategory.MEDICATION_ADJUSTMENT,
                            title = "Pressão baixa e dor de cabeça",
                            description = "Mudanças na pressão atmosférica podem desencadear dores de cabeça. Tenha analgésicos disponíveis",
                            priority = Priority.MEDIUM,
                            weatherBased = true,
                            healthBased = true,
                            evidenceLevel = EvidenceLevel.MODERATE,
                            relatedSymptoms = listOf(condition.name)
                        )
                    )
                }
            }
        }
        
        return recommendations
    }

    private fun assessWeatherImpactOnCondition(
        condition: MedicalCondition,
        weather: WeatherCondition
    ): HealthRiskLevel {
        return when {
            // Respiratory conditions in cold weather
            (condition.name.contains("asma", ignoreCase = true) || 
             condition.name.contains("bronquite", ignoreCase = true)) && 
             weather.current.temperature < 15 -> HealthRiskLevel.HIGH
            
            // Joint conditions in high humidity
            (condition.name.contains("artrite", ignoreCase = true) || 
             condition.name.contains("artrose", ignoreCase = true)) && 
             weather.current.humidity > 70 -> HealthRiskLevel.MEDIUM
            
            // Migraines in low pressure
            (condition.name.contains("enxaqueca", ignoreCase = true) || 
             condition.name.contains("cefaleia", ignoreCase = true)) && 
             weather.current.pressure < 1010 -> HealthRiskLevel.MEDIUM
            
            // Cardiovascular conditions in extreme temperatures
            condition.name.contains("cardíac", ignoreCase = true) && 
            (weather.current.temperature < 5 || weather.current.temperature > 35) -> HealthRiskLevel.HIGH
            
            // Skin conditions in low humidity
            (condition.name.contains("eczema", ignoreCase = true) || 
             condition.name.contains("dermatite", ignoreCase = true)) && 
             weather.current.humidity < 30 -> HealthRiskLevel.MEDIUM
            
            // General assessment for severe conditions
            condition.severity == "severe" && 
            (weather.current.temperature < 0 || weather.current.temperature > 40) -> HealthRiskLevel.HIGH
            
            else -> HealthRiskLevel.LOW
        }
    }

    private fun generateRiskMitigationRecommendations(riskFactors: List<HealthRiskFactor>): List<String> {
        val recommendations = mutableListOf<String>()

        riskFactors.forEach { factor ->
            when (factor) {
                HealthRiskFactor.WEATHER_SENSITIVE_CONDITION -> {
                    recommendations.add("Monitore seus sintomas mais de perto hoje")
                    recommendations.add("Tenha medicamentos de alívio à mão")
                }
                HealthRiskFactor.RECENT_SEVERE_SYMPTOMS -> {
                    recommendations.add("Considere consultar seu médico")
                    recommendations.add("Evite atividades que possam agravar os sintomas")
                }
                HealthRiskFactor.AIR_QUALITY_RESPIRATORY -> {
                    recommendations.add("Use máscara ao sair de casa")
                    recommendations.add("Mantenha inaladores de alívio acessíveis")
                }
                HealthRiskFactor.MEDICATION_INTERACTION -> {
                    recommendations.add("Revise suas medicações e horários (especialmente hoje)")
                    recommendations.add("Em caso de dúvida, confirme com seu médico/farmacêutico")
                }
                HealthRiskFactor.EXTREME_WEATHER -> {
                    recommendations.add("Evite exposição prolongada ao clima extremo")
                    recommendations.add("Tenha um plano de contingência e contato de emergência disponível")
                }
            }
        }

        return recommendations.distinct()
    }

    private fun calculateConfidence(userProfile: UserProfile?, symptomCount: Int): Double {
        var confidence = 0.5

        // Aumenta confiança com mais dados do usuário
        userProfile?.let {
            if (it.medicalConditions.isNotEmpty()) confidence += 0.2
            if (it.allergies.isNotEmpty()) confidence += 0.1
        }

        // Aumenta confiança com mais sintomas registrados
        confidence += (symptomCount * 0.05).coerceAtMost(0.2)

        return confidence.coerceAtMost(1.0)
    }

    private fun getCurrentSeason(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            in 11..2 -> "verão"
            in 2..5 -> "outono"
            in 5..8 -> "inverno"
            else -> "primavera"
        }
    }
}

// Data classes adicionais para os use cases
data class SymptomTrendAnalysis(
    val symptomName: String,
    val trendDirection: TrendDirection,
    val averageSeverity: Double,
    val frequency: Int,
    val weatherCorrelations: List<WeatherCorrelation>
)

enum class TrendDirection { IMPROVING, STABLE, WORSENING }

data class MedicationAdherenceReport(
    val overallAdherence: Double,
    val medicationAdherence: Map<String, Double>,
    val missedDoses: Int,
    val recommendations: List<String>
)

data class WeatherSymptomCorrelation(
    val correlations: Map<String, Double>,
    val significantCorrelations: List<CorrelationResult>,
    val confidence: Double
)

data class HealthRiskAssessment(
    val userId: String,
    val overallRisk: HealthRiskLevel,
    val riskFactors: List<HealthRiskFactor>,
    val recommendations: List<String>,
    val assessmentDate: Date,
    val validUntil: Date,
    val confidence: Double
)

    /* suspend fun getHealthStatistics(userId: String, days: Int): Resource<HealthStatistics> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            days !in 1..365 -> Resource.Error("Período deve ser entre 1 e 365 dias")
            else -> healthRepository.getHealthStatistics(userId, days)
        }
    }
} */

enum class HealthRiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

enum class HealthRiskFactor {
    WEATHER_SENSITIVE_CONDITION,
    RECENT_SEVERE_SYMPTOMS,
    AIR_QUALITY_RESPIRATORY,
    MEDICATION_INTERACTION,
    EXTREME_WEATHER
}
