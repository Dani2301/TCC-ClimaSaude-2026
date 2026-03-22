package com.climasaude.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.climasaude.data.database.entities.*
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Gson().fromJson<List<String>>(
                value,
                object : TypeToken<List<String>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringBooleanMap(value: Map<String, Boolean>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringBooleanMap(value: String): Map<String, Boolean> {
        return try {
            Gson().fromJson<Map<String, Boolean>>(
                value,
                object : TypeToken<Map<String, Boolean>>() {}.type
            ) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromWeatherForecastList(value: List<WeatherForecast>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toWeatherForecastList(value: String): List<WeatherForecast> {
        return try {
            Gson().fromJson<List<WeatherForecast>>(
                value,
                object : TypeToken<List<WeatherForecast>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTemperatureRange(value: TemperatureRange?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toTemperatureRange(value: String?): TemperatureRange? {
        return try {
            value?.let { Gson().fromJson(it, TemperatureRange::class.java) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromHumidityRange(value: HumidityRange?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toHumidityRange(value: String?): HumidityRange? {
        return try {
            value?.let { Gson().fromJson(it, HumidityRange::class.java) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAlertType(value: AlertType): String {
        return value.name
    }

    @TypeConverter
    fun toAlertType(value: String): AlertType {
        return try {
            AlertType.valueOf(value)
        } catch (e: Exception) {
            AlertType.WEATHER_WARNING
        }
    }

    @TypeConverter
    fun fromAlertSeverity(value: AlertSeverity): String {
        return value.name
    }

    @TypeConverter
    fun toAlertSeverity(value: String): AlertSeverity {
        return try {
            AlertSeverity.valueOf(value)
        } catch (e: Exception) {
            AlertSeverity.LOW
        }
    }
}