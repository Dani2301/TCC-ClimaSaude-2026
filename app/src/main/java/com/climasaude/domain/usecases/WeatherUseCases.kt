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
        return if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            Resource.Error("Coordenadas inválidas")
        } else {
            weatherRepository.getCurrentWeather(latitude, longitude, forceRefresh = forceRefresh)
        }
    }

    suspend fun getWeatherByCity(cityName: String): Resource<WeatherCondition> {
        return if (cityName.isBlank()) Resource.Error("Nome da cidade obrigatório")
        else weatherRepository.getWeatherByCity(cityName)
    }

    suspend fun getWeatherForecast(lat: Double, lon: Double, days: Int = 7): Resource<List<WeatherForecast>> {
        return weatherRepository.getWeatherForecast(lat, lon, days)
    }

    suspend fun getWeatherAlerts(lat: Double, lon: Double): Resource<List<WeatherAlert>> {
        return weatherRepository.getWeatherAlerts(lat, lon)
    }

    suspend fun getWeatherStats(userId: String, days: Int = 30): Resource<WeatherStats> {
        return weatherRepository.getWeatherStats(userId, days)
    }

    fun analyzeWeatherRisk(weatherCondition: WeatherCondition): WeatherRiskAnalysis {
        val riskFactors = mutableListOf<RiskFactor>()
        var overallRisk = RiskLevel.LOW

        val temp = weatherCondition.current.temperature
        val hum = weatherCondition.current.humidity

        if (temp > 35.0 || temp < 0.0) {
            riskFactors.add(if (temp > 35) RiskFactor.EXTREME_HEAT else RiskFactor.EXTREME_COLD)
            overallRisk = RiskLevel.HIGH
        }

        if (hum > 80 || hum < 30) {
            riskFactors.add(if (hum > 80) RiskFactor.HIGH_HUMIDITY else RiskFactor.LOW_HUMIDITY)
            if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
        }

        // Senior Fix: Comparação segura de Double. Modificado por: Daniel
        weatherCondition.airQuality?.let { air ->
            if (air.index >= 4.0) {
                riskFactors.add(RiskFactor.POOR_AIR_QUALITY)
                overallRisk = RiskLevel.HIGH
            } else if (air.index >= 3.0) {
                riskFactors.add(RiskFactor.MODERATE_AIR_QUALITY)
                if (overallRisk == RiskLevel.LOW) overallRisk = RiskLevel.MEDIUM
            }
        }

        return WeatherRiskAnalysis(
            overallRisk = overallRisk,
            riskFactors = riskFactors,
            recommendations = emptyList(),
            riskScore = 0.0
        )
    }
}

data class WeatherRiskAnalysis(
    val overallRisk: RiskLevel,
    val riskFactors: List<RiskFactor>,
    val recommendations: List<String>,
    val riskScore: Double
)

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

enum class RiskFactor {
    EXTREME_HEAT, EXTREME_COLD, HIGH_TEMPERATURE, HIGH_HUMIDITY, LOW_HUMIDITY,
    POOR_AIR_QUALITY, MODERATE_AIR_QUALITY, EXTREME_UV, HIGH_UV
}
