package com.climasaude.data.network

import com.climasaude.data.network.responses.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "pt_br"
    ): Response<CurrentWeatherResponse>

    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "pt_br"
    ): Response<CurrentWeatherResponse>

    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "pt_br"
    ): Response<WeatherForecastResponse>

    @GET("air_pollution")
    suspend fun getAirPollution(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Response<AirPollutionResponse>

    @GET("uvi")
    suspend fun getUVIndex(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): Response<UVIndexResponse>
}
