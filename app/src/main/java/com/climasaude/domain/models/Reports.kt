package com.climasaude.domain.models

import java.util.Date

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class ChartPoint(
    val x: Float,
    val y: Float,
    val label: String? = null,
    val date: Date? = null,
    val value: Any? = null
)

data class ChartData(
    val temperatureChart: List<ChartPoint> = emptyList(),
    val symptomChart: List<ChartPoint> = emptyList(),
    val adherenceChart: List<ChartPoint> = emptyList(),
    val correlationChart: List<ChartPoint> = emptyList()
)

data class WeatherReport(
    val period: ReportPeriod,
    val averageTemperature: Double,
    val averageHumidity: Int,
    val averagePressure: Double,
    val temperatureTrend: String, // "rising", "falling", "stable"
    val extremeWeatherDays: Int,
    val weatherSummary: String,
    val weatherData: List<DailyWeatherSummary>,
    val generatedAt: Date
)

data class DailyWeatherSummary(
    val date: Date,
    val temperature: Double,
    val humidity: Int,
    val pressure: Double,
    val condition: String,
    val alertsTriggered: Int = 0
)

data class CorrelationReport(
    val period: ReportPeriod,
    val correlations: List<CorrelationResult>,
    val strongCorrelations: List<CorrelationResult>,
    val insights: List<String>,
    val confidence: Double,
    val generatedAt: Date
)

data class CorrelationResult(
    val symptom: String,
    val weatherFactor: String,
    val correlation: Double,
    val confidence: Double,
    val sampleSize: Int
)

data class HealthStatistics(
    val symptomsRecorded: Int,
    val averageSeverity: Double,
    val mostCommonSymptoms: List<String>
)
