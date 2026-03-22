package com.climasaude.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.climasaude.data.database.entities.WeatherData
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WeatherDataDao {

    @Query("SELECT * FROM weather_data WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeatherData(userId: String): WeatherData?

    @Query("SELECT * FROM weather_data WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeatherDataFlow(userId: String): Flow<WeatherData?>

    @Query("SELECT * FROM weather_data WHERE userId = :userId AND timestamp >= :startDate ORDER BY timestamp DESC")
    suspend fun getWeatherDataByDateRange(userId: String, startDate: Date): List<WeatherData>

    @Query("SELECT * FROM weather_data WHERE userId = :userId AND city = :city ORDER BY timestamp DESC LIMIT 10")
    suspend fun getWeatherDataByCity(userId: String, city: String): List<WeatherData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherData: WeatherData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherDataList(weatherDataList: List<WeatherData>)

    @Update
    suspend fun updateWeatherData(weatherData: WeatherData)

    @Delete
    suspend fun deleteWeatherData(weatherData: WeatherData)

    @Query("DELETE FROM weather_data WHERE userId = :userId AND timestamp < :cutoffDate")
    suspend fun deleteOldWeatherData(userId: String, cutoffDate: Date)

    @Query("SELECT AVG(temperature) FROM weather_data WHERE userId = :userId AND timestamp >= :startDate")
    suspend fun getAverageTemperature(userId: String, startDate: Date): Double?

    @Query("SELECT AVG(humidity) FROM weather_data WHERE userId = :userId AND timestamp >= :startDate")
    suspend fun getAverageHumidity(userId: String, startDate: Date): Double?
}