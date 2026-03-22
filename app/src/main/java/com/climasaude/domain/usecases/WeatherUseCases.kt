package com.climasaude.domain.usecases

import com.climasaude.data.repository.WeatherRepository
import com.climasaude.domain.models.WeatherCondition
import com.climasaude.domain.models.WeatherForecast
import com.climasaude.domain.models.WeatherAlert
import com.climasaude.domain.models.WeatherStats
import com.climasaude.utils.Resource
import javax.inject.Inject

class WeatherUseCases @Inject constructor(
    private val weatherRepository: WeatherRepository
) {

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Resource<WeatherCondition> {
        return when {
            !isValidCoordinates(latitude, longitude) ->
                Resource.Error("Coordenadas inválidas")
            else -> weatherRepository.getCurrentWeather(latitude, longitude, forceRefresh = forceRefresh)
        }
    }

    suspend fun getWeatherByCity(
        cityName: String
    ): Resource<WeatherCondition> {
        return when {
            cityName.isBlank() -> Resource.Error("Nome da cidade é obrigatório")
            else -> weatherRepository.getWeatherByCity(cityName)
        }
    }

    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        days: Int = 7
    ): Resource<List<WeatherForecast>> {
        return when {
            !isValidCoordinates(latitude, longitude) ->
                Resource.Error("Coordenadas inválidas")
            days !in 1..14 ->
                Resource.Error("Período de previsão deve ser entre 1 e 14 dias")
            else -> weatherRepository.getWeatherForecast(latitude, longitude, days)
        }
    }

    suspend fun getWeatherAlerts(
        latitude: Double,
        longitude: Double
    ): Resource<List<WeatherAlert>> {
        return when {
            !isValidCoordinates(latitude, longitude) ->
                Resource.Error("Coordenadas inválidas")
            else -> weatherRepository.getWeatherAlerts(latitude, longitude)
        }
    }

    suspend fun getWeatherStats(userId: String, days: Int = 30): Resource<WeatherStats> {
        return when {
            userId.isBlank() -> Resource.Error("ID do usuário é obrigatório")
            days !in 1..365 -> Resource.Error("Período deve ser entre 1 e 365 dias")
            else -> weatherRepository.getWeatherStats(userId, days)
        }
    }

    fun analyzeWeatherRisk(weatherCondition: WeatherCondition): WeatherRiskAnalysis {
        val riskFactors = mutableListOf<RiskFactor>()
        var overallRisk = RiskLevel.LOW

        // Análise de temperatura
        when {
            weatherCondition.current.temperature > 35.0 -> {
                riskFactors.add(RiskFactor.EXTREME_HEAT)
                overallRisk = RiskLevel.HIGH
            }
            weatherCondition.current.temperature < 0.0 -> {
                riskFactors.add(RiskFactor.EXTREME_COLD)
                overallRisk = RiskLevel.HIGH
            }
            weatherCondition.current.temperature > 30.0 -> {
                riskFactors.add(RiskFactor.HIGH_TEMPERATURE)
                if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
            }
        }

        // Análise de umidade
        when {
            weatherCondition.current.humidity > 80 -> {
                riskFactors.add(RiskFactor.HIGH_HUMIDITY)
                if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
            }
            weatherCondition.current.humidity < 30 -> {
                riskFactors.add(RiskFactor.LOW_HUMIDITY)
                if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
            }
        }

        // Análise de qualidade do ar
        weatherCondition.airQuality?.let { airQuality ->
            when {
                airQuality.index >= 4 -> {
                    riskFactors.add(RiskFactor.POOR_AIR_QUALITY)
                    overallRisk = RiskLevel.HIGH
                }
                airQuality.index == 3 -> {
                    riskFactors.add(RiskFactor.MODERATE_AIR_QUALITY)
                    if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
                }
            }
        }

        // Análise de UV
        weatherCondition.uv?.let { uv ->
            when {
                uv.current >= 8.0 -> {
                    riskFactors.add(RiskFactor.EXTREME_UV)
                    if (overallRisk != RiskLevel.HIGH) overallRisk = RiskLevel.MEDIUM
                }
                uv.current >= 6.0 -> {
                    riskFactors.add(RiskFactor.HIGH_UV)
                    if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
                }
            }
        }

        return WeatherRiskAnalysis(
            overallRisk = overallRisk,
            riskFactors = riskFactors,
            recommendations = generateRecommendations(riskFactors),
            riskScore = calculateRiskScore(riskFactors)
        )
    }

    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun generateRecommendations(riskFactors: List<RiskFactor>): List<String> {
        val recommendations = mutableListOf<String>()

        riskFactors.forEach { factor ->
            when (factor) {
                RiskFactor.EXTREME_HEAT -> {
                    recommendations.add("Evite exposição prolongada ao sol")
                    recommendations.add("Mantenha-se hidratado")
                    recommendations.add("Use roupas leves e claras")
                }
                RiskFactor.EXTREME_COLD -> {
                    recommendations.add("Use roupas adequadas para o frio")
                    recommendations.add("Evite exposição prolongada ao frio")
                    recommendations.add("Mantenha extremidades aquecidas")
                }
                RiskFactor.HIGH_HUMIDITY -> {
                    recommendations.add("Evite atividades físicas intensas")
                    recommendations.add("Mantenha ambientes ventilados")
                    recommendations.add("Hidrate-se regularmente")
                }
                RiskFactor.POOR_AIR_QUALITY -> {
                    recommendations.add("Evite atividades ao ar livre")
                    recommendations.add("Use máscara se necessário sair")
                    recommendations.add("Mantenha janelas fechadas")
                }
                RiskFactor.EXTREME_UV -> {
                    recommendations.add("Use protetor solar FPS 50+")
                    recommendations.add("Evite exposição entre 10h e 16h")
                    recommendations.add("Use óculos de sol e chapéu")
                }
                else -> {}
            }
        }

        return recommendations.distinct()
    }

    private fun calculateRiskScore(riskFactors: List<RiskFactor>): Double {
        return riskFactors.sumOf { factor ->
            when (factor) {
                RiskFactor.EXTREME_HEAT, RiskFactor.EXTREME_COLD -> 10.0
                RiskFactor.POOR_AIR_QUALITY -> 8.0
                RiskFactor.EXTREME_UV -> 7.0
                RiskFactor.HIGH_HUMIDITY, RiskFactor.LOW_HUMIDITY -> 6.0
                RiskFactor.HIGH_TEMPERATURE -> 5.0
                RiskFactor.MODERATE_AIR_QUALITY -> 4.0
                RiskFactor.HIGH_UV -> 3.0
            }
        }
    }
}

data class WeatherRiskAnalysis(
    val overallRisk: RiskLevel,
    val riskFactors: List<RiskFactor>,
    val recommendations: List<String>,
    val riskScore: Double
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class RiskFactor {
    EXTREME_HEAT,
    EXTREME_COLD,
    HIGH_TEMPERATURE,
    HIGH_HUMIDITY,
    LOW_HUMIDITY,
    POOR_AIR_QUALITY,
    MODERATE_AIR_QUALITY,
    EXTREME_UV,
    HIGH_UV
}
