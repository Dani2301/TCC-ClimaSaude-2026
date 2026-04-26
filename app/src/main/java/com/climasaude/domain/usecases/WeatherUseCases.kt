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
        val recommendations = mutableListOf<String>()
        var overallRisk = RiskLevel.LOW
        var riskScore = 0.0

        val temp = weatherCondition.current.temperature
        val hum = weatherCondition.current.humidity

        // Análise de Temperatura
        if (temp > 35.0) {
            riskFactors.add(RiskFactor.EXTREME_HEAT)
            recommendations.add("Calor intenso: Mantenha-se hidratado e evite exposição ao sol entre 10h e 16h.")
            overallRisk = RiskLevel.HIGH
            riskScore += 40.0
        } else if (temp < 10.0) {
            riskFactors.add(RiskFactor.EXTREME_COLD)
            recommendations.add("Frio intenso: Agasalhe-se bem. Risco aumentado para problemas respiratórios e circulatórios.")
            overallRisk = maxOfRisk(overallRisk, RiskLevel.MEDIUM)
            riskScore += 30.0
        }

        // Análise de Humidade
        if (hum < 30) {
            riskFactors.add(RiskFactor.LOW_HUMIDITY)
            recommendations.add("Umidade muito baixa: Use umidificadores e hidrate as vias nasais.")
            overallRisk = maxOfRisk(overallRisk, RiskLevel.MEDIUM)
            riskScore += 20.0
        } else if (hum > 85) {
            riskFactors.add(RiskFactor.HIGH_HUMIDITY)
            recommendations.add("Umidade elevada: Proliferação de fungos e ácaros. Atenção redobrada para asmáticos.")
            overallRisk = maxOfRisk(overallRisk, RiskLevel.MEDIUM)
            riskScore += 15.0
        }

        // Análise de Qualidade do Ar
        weatherCondition.airQuality?.let { air ->
            if (air.index >= 4.0) {
                riskFactors.add(RiskFactor.POOR_AIR_QUALITY)
                recommendations.add("Qualidade do ar péssima: Evite exercícios ao ar livre. Use máscara se necessário.")
                overallRisk = RiskLevel.CRITICAL
                riskScore += 50.0
            } else if (air.index >= 3.0) {
                riskFactors.add(RiskFactor.MODERATE_AIR_QUALITY)
                recommendations.add("Qualidade do ar moderada: Pessoas sensíveis podem sentir desconforto respiratório.")
                overallRisk = maxOfRisk(overallRisk, RiskLevel.MEDIUM)
                riskScore += 25.0
            }
        }

        // Ajuste final do nível de risco baseado no score total
        if (riskScore >= 70.0) overallRisk = RiskLevel.CRITICAL
        else if (riskScore >= 40.0) overallRisk = maxOfRisk(overallRisk, RiskLevel.HIGH)

        return WeatherRiskAnalysis(
            overallRisk = overallRisk,
            riskFactors = riskFactors,
            recommendations = recommendations,
            riskScore = riskScore
        )
    }
    
    private fun maxOfRisk(r1: RiskLevel, r2: RiskLevel): RiskLevel {
        return if (r1.ordinal >= r2.ordinal) r1 else r2
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
