package com.climasaude.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "weather_data")
data class WeatherData(
    @PrimaryKey
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Double,
    val visibility: Double,
    val uvIndex: Double,
    val airQuality: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val cloudiness: Int,
    val weatherMain: String, // Clear, Rain, Snow, etc.
    val weatherDescription: String,
    val weatherIcon: String,
    val timestamp: Date = Date(),
    val sunrise: Date? = null,
    val sunset: Date? = null,
    val isCurrentLocation: Boolean = true,
    val forecast: List<WeatherForecast> = emptyList()
)

data class WeatherForecast(
    val date: Date,
    val tempMax: Double,
    val tempMin: Double,
    val humidity: Int,
    val description: String,
    val icon: String,
    val precipitationChance: Int
)