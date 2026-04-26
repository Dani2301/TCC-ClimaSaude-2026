package com.climasaude.data.repository

import com.climasaude.data.database.dao.WeatherDataDao
import com.climasaude.data.database.entities.WeatherData
import com.climasaude.data.network.WeatherApiService
import com.climasaude.domain.models.*
import com.climasaude.utils.Resource
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val weatherDataDao: WeatherDataDao
) {

    private fun isCacheFresh(timestamp: Date): Boolean = 
        (System.currentTimeMillis() - timestamp.time) < 15 * 60 * 1000

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        userId: String = "current_user",
        forceRefresh: Boolean = false
    ): Resource<WeatherCondition> {
        return try {
            val cached = weatherDataDao.getLatestWeatherData(userId)
            if (!forceRefresh && cached != null && isCacheFresh(cached.timestamp) && latitude == 0.0) {
                return Resource.Success(convertToWeatherCondition(cached))
            }

            val response = weatherApiService.getCurrentWeather(latitude, longitude)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val weatherCondition = convertToWeatherConditionFromResponse(data)
                weatherDataDao.insertWeatherData(convertToWeatherData(weatherCondition, userId))
                Resource.Success(weatherCondition)
            } else {
                cached?.let { Resource.Success(convertToWeatherCondition(it)) } 
                    ?: Resource.Error("API indisponível")
            }
        } catch (e: Exception) {
            val cached = weatherDataDao.getLatestWeatherData(userId)
            cached?.let { Resource.Success(convertToWeatherCondition(it)) } 
                ?: Resource.Error("Erro: ${e.message}")
        }
    }

    suspend fun getWeatherByCity(cityName: String, userId: String = "current_user"): Resource<WeatherCondition> {
        return try {
            val response = weatherApiService.getWeatherByCity(cityName)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val weatherCondition = convertToWeatherConditionFromResponse(data)
                weatherDataDao.insertWeatherData(convertToWeatherData(weatherCondition, userId))
                Resource.Success(weatherCondition)
            } else {
                Resource.Error("Cidade não encontrada ou erro na API")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao buscar cidade: ${e.message}")
        }
    }

    private fun convertToWeatherCondition(weatherData: WeatherData): WeatherCondition {
        return WeatherCondition(
            location = Location(weatherData.latitude, weatherData.longitude, weatherData.city, "", weatherData.country, ""),
            current = CurrentWeather(
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
                dewPoint = 0.0,
                sunrise = weatherData.sunrise,
                sunset = weatherData.sunset
            ),
            uv = UVIndex(weatherData.uvIndex, "N/A", "Baixo", "Use protetor"),
            airQuality = AirQuality(weatherData.airQuality.toDouble(), "Bom", "PM2.5", emptyMap()),
            forecast = emptyList(),
            lastUpdated = weatherData.timestamp
        )
    }

    private fun convertToWeatherConditionFromResponse(res: com.climasaude.data.network.responses.CurrentWeatherResponse): WeatherCondition {
        return WeatherCondition(
            location = Location(res.coordinates.latitude, res.coordinates.longitude, res.name, "", res.system.country, ""),
            current = CurrentWeather(
                temperature = res.main.temperature,
                feelsLike = res.main.feelsLike,
                condition = res.weather.firstOrNull()?.main ?: "",
                description = res.weather.firstOrNull()?.description ?: "",
                icon = res.weather.firstOrNull()?.icon ?: "",
                humidity = res.main.humidity,
                pressure = res.main.pressure,
                visibility = res.visibility.toDouble(),
                windSpeed = res.wind.speed,
                windDirection = res.wind.direction,
                cloudiness = res.clouds.cloudiness,
                dewPoint = 0.0,
                sunrise = Date(res.system.sunrise * 1000),
                sunset = Date(res.system.sunset * 1000)
            ),
            uv = UVIndex(0.0, "N/A", "Baixo", "Monitorando"),
            airQuality = AirQuality(1.0, "Bom", "PM2.5", emptyMap()),
            forecast = emptyList(),
            lastUpdated = Date()
        )
    }

    private fun convertToWeatherData(weather: WeatherCondition, userId: String): WeatherData {
        return WeatherData(
            id = UUID.randomUUID().toString(),
            userId = userId,
            latitude = weather.location.latitude,
            longitude = weather.location.longitude,
            city = weather.location.city,
            country = weather.location.country,
            temperature = weather.current.temperature,
            feelsLike = weather.current.feelsLike,
            humidity = weather.current.humidity,
            pressure = weather.current.pressure,
            visibility = weather.current.visibility,
            uvIndex = weather.uv?.current ?: 0.0,
            airQuality = weather.airQuality?.index?.toInt() ?: 0,
            windSpeed = weather.current.windSpeed,
            windDirection = weather.current.windDirection,
            cloudiness = weather.current.cloudiness,
            weatherMain = weather.current.condition,
            weatherDescription = weather.current.description,
            weatherIcon = weather.current.icon,
            sunrise = weather.current.sunrise,
            sunset = weather.current.sunset
        )
    }

    suspend fun getWeatherForecast(la: Double, lo: Double, d: Int): Resource<List<com.climasaude.domain.models.WeatherForecast>> = Resource.Success(emptyList())
    suspend fun getWeatherAlerts(la: Double, lo: Double): Resource<List<com.climasaude.domain.models.WeatherAlert>> = Resource.Success(emptyList())
    suspend fun getWeatherStats(u: String, d: Int): Resource<WeatherStats> = Resource.Success(WeatherStats(0.0, 0, 0))
}
