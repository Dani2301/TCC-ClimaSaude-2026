package com.climasaude.data.network.responses

import com.google.gson.annotations.SerializedName

data class CurrentWeatherResponse(
    @SerializedName("coord") val coordinates: Coordinates,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("main") val main: Main,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("clouds") val clouds: Clouds,
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("sys") val system: System,
    @SerializedName("timezone") val timezone: Int,
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("cod") val cod: Int
)

data class WeatherForecastResponse(
    @SerializedName("cod") val cod: String,
    @SerializedName("message") val message: Int,
    @SerializedName("cnt") val count: Int,
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: City
)

data class OneCallWeatherResponse(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("timezone_offset") val timezoneOffset: Int,
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("hourly") val hourly: List<HourlyWeather>,
    @SerializedName("daily") val daily: List<DailyWeather>,
    @SerializedName("alerts") val alerts: List<WeatherAlert>?
)

data class AirPollutionResponse(
    @SerializedName("coord") val coordinates: Coordinates,
    @SerializedName("list") val list: List<AirPollutionData>
)

data class UVIndexResponse(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("date_iso") val dateIso: String,
    @SerializedName("date") val date: Long,
    @SerializedName("value") val value: Double
)

// Data classes auxiliares
data class Coordinates(
    @SerializedName("lon") val longitude: Double,
    @SerializedName("lat") val latitude: Double
)

data class Weather(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class Main(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("sea_level") val seaLevel: Double?,
    @SerializedName("grnd_level") val groundLevel: Double?
)

data class Wind(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val direction: Int,
    @SerializedName("gust") val gust: Double?
)

data class Clouds(
    @SerializedName("all") val cloudiness: Int
)

data class System(
    @SerializedName("type") val type: Int?,
    @SerializedName("id") val id: Int?,
    @SerializedName("country") val country: String,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)

data class ForecastItem(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("clouds") val clouds: Clouds,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("pop") val precipitationProbability: Double,
    @SerializedName("rain") val rain: Rain?,
    @SerializedName("snow") val snow: Snow?,
    @SerializedName("dt_txt") val dateText: String
)

data class City(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("coord") val coordinates: Coordinates,
    @SerializedName("country") val country: String,
    @SerializedName("population") val population: Int,
    @SerializedName("timezone") val timezone: Int,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long
)

data class CurrentWeather(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long,
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("dew_point") val dewPoint: Double,
    @SerializedName("uvi") val uvIndex: Double,
    @SerializedName("clouds") val cloudiness: Int,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_deg") val windDirection: Int,
    @SerializedName("wind_gust") val windGust: Double?,
    @SerializedName("weather") val weather: List<Weather>
)

data class HourlyWeather(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("dew_point") val dewPoint: Double,
    @SerializedName("uvi") val uvIndex: Double,
    @SerializedName("clouds") val cloudiness: Int,
    @SerializedName("visibility") val visibility: Int,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_deg") val windDirection: Int,
    @SerializedName("wind_gust") val windGust: Double?,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("pop") val precipitationProbability: Double
)

data class DailyWeather(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long,
    @SerializedName("moonrise") val moonrise: Long,
    @SerializedName("moonset") val moonset: Long,
    @SerializedName("moon_phase") val moonPhase: Double,
    @SerializedName("temp") val temperature: Temperature,
    @SerializedName("feels_like") val feelsLike: FeelsLike,
    @SerializedName("pressure") val pressure: Double,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("dew_point") val dewPoint: Double,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_deg") val windDirection: Int,
    @SerializedName("wind_gust") val windGust: Double?,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("clouds") val cloudiness: Int,
    @SerializedName("pop") val precipitationProbability: Double,
    @SerializedName("rain") val rain: Double?,
    @SerializedName("snow") val snow: Double?,
    @SerializedName("uvi") val uvIndex: Double
)

data class Temperature(
    @SerializedName("day") val day: Double,
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double,
    @SerializedName("night") val night: Double,
    @SerializedName("eve") val evening: Double,
    @SerializedName("morn") val morning: Double
)

data class FeelsLike(
    @SerializedName("day") val day: Double,
    @SerializedName("night") val night: Double,
    @SerializedName("eve") val evening: Double,
    @SerializedName("morn") val morning: Double
)

data class WeatherAlert(
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("event") val event: String,
    @SerializedName("start") val start: Long,
    @SerializedName("end") val end: Long,
    @SerializedName("description") val description: String,
    @SerializedName("tags") val tags: List<String>
)

data class AirPollutionData(
    @SerializedName("dt") val timestamp: Long,
    @SerializedName("main") val main: AirQualityMain,
    @SerializedName("components") val components: AirQualityComponents
)

data class AirQualityMain(
    @SerializedName("aqi") val airQualityIndex: Int
)

data class AirQualityComponents(
    @SerializedName("co") val carbonMonoxide: Double,
    @SerializedName("no") val nitrogenMonoxide: Double,
    @SerializedName("no2") val nitrogenDioxide: Double,
    @SerializedName("o3") val ozone: Double,
    @SerializedName("so2") val sulfurDioxide: Double,
    @SerializedName("pm2_5") val pm25: Double,
    @SerializedName("pm10") val pm10: Double,
    @SerializedName("nh3") val ammonia: Double
)

data class Rain(
    @SerializedName("1h") val oneHour: Double?,
    @SerializedName("3h") val threeHours: Double?
)

data class Snow(
    @SerializedName("1h") val oneHour: Double?,
    @SerializedName("3h") val threeHours: Double?
)