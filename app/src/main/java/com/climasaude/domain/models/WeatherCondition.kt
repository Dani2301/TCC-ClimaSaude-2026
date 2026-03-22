package com.climasaude.domain.models

import java.util.Date

data class WeatherCondition(
    val location: Location,
    val current: CurrentWeather,
    val forecast: List<WeatherForecast>,
    val alerts: List<WeatherAlert> = emptyList(),
    val airQuality: AirQuality? = null,
    val uv: UVIndex? = null,
    val lastUpdated: Date
)

data class Location(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val region: String,
    val country: String,
    val timezone: String
)

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val description: String,
    val icon: String,
    val humidity: Int,
    val pressure: Double,
    val visibility: Double,
    val windSpeed: Double,
    val windDirection: Int,
    val windGust: Double? = null,
    val cloudiness: Int,
    val dewPoint: Double,
    val sunrise: Date?,
    val sunset: Date?,
    val moonPhase: String? = null
)

data class WeatherForecast(
    val date: Date,
    val tempMax: Double,
    val tempMin: Double,
    val condition: String,
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val precipitationChance: Int,
    val precipitationAmount: Double,
    val uvIndex: Double,
    val sunrise: Date,
    val sunset: Date
)

data class WeatherAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: String,
    val startTime: Date,
    val endTime: Date,
    val areas: List<String>
)

data class AirQuality(
    val index: Int,
    val level: String, // Good, Moderate, Unhealthy, etc.
    val mainPollutant: String,
    val components: Map<String, Double> // PM2.5, PM10, O3, NO2, SO2, CO
)

data class UVIndex(
    val current: Double,
    val max: Double,
    val level: String, // Low, Moderate, High, Very High, Extreme
    val recommendation: String
)