package com.climasaude.data.repository

import com.climasaude.data.database.dao.WeatherDataDao
import com.climasaude.data.database.entities.WeatherData
import com.climasaude.data.network.WeatherApiService
import com.climasaude.data.network.responses.*
import com.climasaude.domain.models.*
import com.climasaude.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val weatherDataDao: WeatherDataDao
) {

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        userId: String = "current_user",
        forceRefresh: Boolean = false
    ): Resource<WeatherCondition> {
        return try {
            if (!forceRefresh) {
                val cachedData = weatherDataDao.getLatestWeatherData(userId)
                if (cachedData != null && isCacheValid(cachedData.timestamp)) {
                    return Resource.Success(convertToWeatherCondition(cachedData))
                }
            }

            val currentResponse = weatherApiService.getCurrentWeather(latitude, longitude)

            if (currentResponse.isSuccessful && currentResponse.body() != null) {
                val currentWeather = currentResponse.body()!!
                val forecastResponse = weatherApiService.getWeatherForecast(latitude, longitude)
                val airPollutionResponse = weatherApiService.getAirPollution(latitude, longitude)

                val weatherCondition = convertToWeatherCondition(
                    currentWeather,
                    forecastResponse.body(),
                    airPollutionResponse.body()
                )

                val weatherData = convertToWeatherData(weatherCondition, userId)
                weatherDataDao.insertWeatherData(weatherData)

                Resource.Success(weatherCondition)
            } else {
                Resource.Error("Erro ao buscar dados do clima: ${currentResponse.message()} (${currentResponse.code()})")
            }
        } catch (e: Exception) {
            val cachedData = weatherDataDao.getLatestWeatherData(userId)
            if (cachedData != null) {
                Resource.Success(convertToWeatherCondition(cachedData))
            } else {
                Resource.Error("Erro ao buscar dados do clima: ${e.message}")
            }
        }
    }

    suspend fun getWeatherByCity(
        cityName: String,
        userId: String = "current_user"
    ): Resource<WeatherCondition> {
        return try {
            val currentResponse = weatherApiService.getWeatherByCity(cityName)

            if (currentResponse.isSuccessful && currentResponse.body() != null) {
                val currentWeather = currentResponse.body()!!
                val lat = currentWeather.coordinates.latitude
                val lon = currentWeather.coordinates.longitude
                
                val forecastResponse = weatherApiService.getWeatherForecast(lat, lon)
                val airPollutionResponse = weatherApiService.getAirPollution(lat, lon)

                val weatherCondition = convertToWeatherCondition(
                    currentWeather,
                    forecastResponse.body(),
                    airPollutionResponse.body()
                )

                val weatherData = convertToWeatherData(weatherCondition, userId)
                weatherDataDao.insertWeatherData(weatherData)

                Resource.Success(weatherCondition)
            } else {
                Resource.Error("Erro ao buscar dados da cidade: ${currentResponse.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao buscar dados da cidade: ${e.message}")
        }
    }

    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        days: Int = 7
    ): Resource<List<com.climasaude.domain.models.WeatherForecast>> {
        return try {
            val response = weatherApiService.getWeatherForecast(latitude, longitude)

            if (response.isSuccessful && response.body() != null) {
                val forecasts = response.body()!!.list.map { item ->
                    com.climasaude.domain.models.WeatherForecast(
                        date = Date(item.timestamp * 1000),
                        tempMax = item.main.tempMax,
                        tempMin = item.main.tempMin,
                        condition = item.weather.firstOrNull()?.main ?: "",
                        description = item.weather.firstOrNull()?.description ?: "",
                        icon = item.weather.firstOrNull()?.icon ?: "",
                        humidity = item.main.humidity,
                        windSpeed = item.wind.speed,
                        precipitationChance = (item.precipitationProbability * 100).toInt(),
                        precipitationAmount = item.rain?.threeHours ?: item.snow?.threeHours ?: 0.0,
                        uvIndex = 0.0,
                        sunrise = Date(),
                        sunset = Date()
                    )
                }
                Resource.Success(forecasts)
            } else {
                Resource.Error("Erro ao buscar previsão: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao buscar previsão: ${e.message}")
        }
    }

    suspend fun getWeatherAlerts(
        latitude: Double,
        longitude: Double
    ): Resource<List<com.climasaude.domain.models.WeatherAlert>> {
        return Resource.Success(emptyList()) 
    }

    fun getWeatherHistoryFlow(userId: String): Flow<List<WeatherData>> {
        return flow {
            val history = weatherDataDao.getWeatherDataByDateRange(
                userId,
                Date(java.lang.System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
            )
            emit(history)
        }
    }

    suspend fun getWeatherStats(userId: String, days: Int = 30): Resource<WeatherStats> {
        return try {
            val startDate = Date(java.lang.System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
            val avgTemp = weatherDataDao.getAverageTemperature(userId, startDate) ?: 0.0
            val avgHumidity = weatherDataDao.getAverageHumidity(userId, startDate) ?: 0.0

            val stats = WeatherStats(
                averageTemperature = avgTemp,
                averageHumidity = avgHumidity.toInt(),
                periodDays = days
            )

            Resource.Success(stats)
        } catch (e: Exception) {
            Resource.Error("Erro ao calcular estatísticas: ${e.message}")
        }
    }

    private fun isCacheValid(timestamp: Date): Boolean {
        val now = Date()
        val diffInMinutes = (now.time - timestamp.time) / (1000 * 60)
        return diffInMinutes < 30
    }

    private fun convertToWeatherCondition(weatherData: WeatherData): WeatherCondition {
        return WeatherCondition(
            location = com.climasaude.domain.models.Location(
                latitude = weatherData.latitude,
                longitude = weatherData.longitude,
                city = weatherData.city,
                region = "",
                country = weatherData.country,
                timezone = ""
            ),
            current = com.climasaude.domain.models.CurrentWeather(
                temperature = weatherData.temperature,
                feelsLike = weatherData.feelsLike,
                condition = weatherData.weatherMain,
                description = weatherData.weatherDescription,
                icon = weatherData.weatherIcon,
                humidity = weatherData.humidity,
                pressure = weatherData.pressure,
                visibility = weatherData.visibility,
                windSpeed = weatherData.windSpeed,
                windDirection = weatherData.windDirection,
                cloudiness = weatherData.cloudiness,
                dewPoint = calculateDewPoint(weatherData.temperature, weatherData.humidity),
                sunrise = weatherData.sunrise,
                sunset = weatherData.sunset
            ),
            forecast = weatherData.forecast.map { forecast ->
                com.climasaude.domain.models.WeatherForecast(
                    date = forecast.date,
                    tempMax = forecast.tempMax,
                    tempMin = forecast.tempMin,
                    condition = "",
                    description = forecast.description,
                    icon = forecast.icon,
                    humidity = forecast.humidity,
                    windSpeed = 0.0,
                    precipitationChance = forecast.precipitationChance,
                    precipitationAmount = 0.0,
                    uvIndex = 0.0,
                    sunrise = Date(),
                    sunset = Date()
                )
            },
            lastUpdated = weatherData.timestamp
        )
    }

    private fun convertToWeatherCondition(
        current: CurrentWeatherResponse,
        forecast: WeatherForecastResponse?,
        airPollution: AirPollutionResponse?
    ): WeatherCondition {
        return WeatherCondition(
            location = com.climasaude.domain.models.Location(
                latitude = current.coordinates.latitude,
                longitude = current.coordinates.longitude,
                city = current.name,
                region = "",
                country = current.system.country,
                timezone = ""
            ),
            current = com.climasaude.domain.models.CurrentWeather(
                temperature = current.main.temperature,
                feelsLike = current.main.feelsLike,
                condition = current.weather.firstOrNull()?.main ?: "",
                description = current.weather.firstOrNull()?.description ?: "",
                icon = current.weather.firstOrNull()?.icon ?: "",
                humidity = current.main.humidity,
                pressure = current.main.pressure,
                visibility = current.visibility.toDouble(),
                windSpeed = current.wind.speed,
                windDirection = current.wind.direction,
                windGust = current.wind.gust,
                cloudiness = current.clouds.cloudiness,
                dewPoint = 0.0,
                sunrise = Date(current.system.sunrise * 1000),
                sunset = Date(current.system.sunset * 1000)
            ),
            forecast = forecast?.list?.map { item ->
                com.climasaude.domain.models.WeatherForecast(
                    date = Date(item.timestamp * 1000),
                    tempMax = item.main.tempMax,
                    tempMin = item.main.tempMin,
                    condition = item.weather.firstOrNull()?.main ?: "",
                    description = item.weather.firstOrNull()?.description ?: "",
                    icon = item.weather.firstOrNull()?.icon ?: "",
                    humidity = item.main.humidity,
                    windSpeed = item.wind.speed,
                    precipitationChance = (item.precipitationProbability * 100).toInt(),
                    precipitationAmount = item.rain?.threeHours ?: item.snow?.threeHours ?: 0.0,
                    uvIndex = 0.0,
                    sunrise = Date(),
                    sunset = Date()
                )
            } ?: emptyList(),
            airQuality = airPollution?.list?.firstOrNull()?.let { air ->
                AirQuality(
                    index = air.main.airQualityIndex,
                    level = getAirQualityLevel(air.main.airQualityIndex),
                    mainPollutant = "PM2.5",
                    components = mapOf(
                        "PM2.5" to air.components.pm25,
                        "PM10" to air.components.pm10,
                        "O3" to air.components.ozone,
                        "NO2" to air.components.nitrogenDioxide,
                        "SO2" to air.components.sulfurDioxide,
                        "CO" to air.components.carbonMonoxide
                    )
                )
            },
            lastUpdated = Date()
        )
    }

    private fun convertToWeatherData(weatherCondition: WeatherCondition, userId: String): WeatherData {
        return WeatherData(
            id = UUID.randomUUID().toString(),
            userId = userId,
            latitude = weatherCondition.location.latitude,
            longitude = weatherCondition.location.longitude,
            city = weatherCondition.location.city,
            country = weatherCondition.location.country,
            temperature = weatherCondition.current.temperature,
            feelsLike = weatherCondition.current.feelsLike,
            humidity = weatherCondition.current.humidity,
            pressure = weatherCondition.current.pressure,
            visibility = weatherCondition.current.visibility,
            uvIndex = weatherCondition.uv?.current ?: 0.0,
            airQuality = weatherCondition.airQuality?.index ?: 0,
            windSpeed = weatherCondition.current.windSpeed,
            windDirection = weatherCondition.current.windDirection,
            cloudiness = weatherCondition.current.cloudiness,
            weatherMain = weatherCondition.current.condition,
            weatherDescription = weatherCondition.current.description,
            weatherIcon = weatherCondition.current.icon,
            sunrise = weatherCondition.current.sunrise,
            sunset = weatherCondition.current.sunset,
            forecast = weatherCondition.forecast.map { forecast ->
                com.climasaude.data.database.entities.WeatherForecast(
                    date = forecast.date,
                    tempMax = forecast.tempMax,
                    tempMin = forecast.tempMin,
                    humidity = forecast.humidity,
                    description = forecast.description,
                    icon = forecast.icon,
                    precipitationChance = forecast.precipitationChance
                )
            }
        )
    }

    private fun getAirQualityLevel(index: Int): String {
        return when (index) {
            1 -> "Boa"
            2 -> "Razoável"
            3 -> "Moderada"
            4 -> "Ruim"
            5 -> "Muito Ruim"
            else -> "Desconhecida"
        }
    }
    
    private fun calculateDewPoint(temperature: Double, humidity: Int): Double {
        val a = 17.27
        val b = 237.7
        val alpha = ((a * temperature) / (b + temperature)) + ln(humidity / 100.0)
        return (b * alpha) / (a - alpha)
    }
}
