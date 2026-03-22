package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.domain.usecases.WeatherUseCases
import com.climasaude.domain.usecases.WeatherRiskAnalysis
import com.climasaude.domain.models.*
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.utils.Resource
import com.climasaude.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherUseCases: WeatherUseCases,
    private val locationUtils: LocationUtils,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _currentWeather = MutableStateFlow<WeatherCondition?>(null)
    val currentWeather: StateFlow<WeatherCondition?> = _currentWeather.asStateFlow()

    private val _forecast = MutableStateFlow<List<WeatherForecast>>(emptyList())
    val forecast: StateFlow<List<WeatherForecast>> = _forecast.asStateFlow()

    private val _weatherAlerts = MutableStateFlow<List<WeatherAlert>>(emptyList())
    val weatherAlerts: StateFlow<List<WeatherAlert>> = _weatherAlerts.asStateFlow()

    private val _weatherStats = MutableStateFlow<WeatherStats?>(null)
    val weatherStats: StateFlow<WeatherStats?> = _weatherStats.asStateFlow()

    private val _riskAnalysis = MutableStateFlow<WeatherRiskAnalysis?>(null)
    val riskAnalysis: StateFlow<WeatherRiskAnalysis?> = _riskAnalysis.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedLocation = MutableStateFlow<SavedLocation?>(null)
    val selectedLocation: StateFlow<SavedLocation?> = _selectedLocation.asStateFlow()

    init {
        loadWeatherData()
    }

    fun loadWeatherData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = !forceRefresh
                _isRefreshing.value = forceRefresh
                _errorMessage.value = null

                val location = _selectedLocation.value ?: getCurrentLocation()
                if (location != null) {
                    loadWeatherForLocation(location.latitude, location.longitude, forceRefresh)
                } else {
                    _errorMessage.value = "Não foi possível obter localização"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro desconhecido"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun searchByCity(cityName: String) {
        if (cityName.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                when (val result = weatherUseCases.getWeatherByCity(cityName)) {
                    is Resource.Success -> {
                        _currentWeather.value = result.data
                        result.data?.let { weather ->
                            // Salvar localização pesquisada para sincronizar com o Dashboard. Modificado por: Daniel
                            appPreferences.setLastLocation(weather.location.latitude, weather.location.longitude)
                            
                            _riskAnalysis.value = weatherUseCases.analyzeWeatherRisk(weather)
                            loadForecastAndAlerts(weather.location.latitude, weather.location.longitude)
                        }
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao buscar cidade"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadWeatherForLocation(lat: Double, lon: Double, forceRefresh: Boolean) {
        when (val result = weatherUseCases.getCurrentWeather(lat, lon, forceRefresh)) {
            is Resource.Success -> {
                _currentWeather.value = result.data
                result.data?.let { weather ->
                    _riskAnalysis.value = weatherUseCases.analyzeWeatherRisk(weather)
                    // Ao carregar com sucesso, atualizamos a última localização conhecida. Modificado por: Daniel
                    appPreferences.setLastLocation(weather.location.latitude, weather.location.longitude)
                }
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
            }
            is Resource.Loading -> Unit
        }

        loadForecastAndAlerts(lat, lon)
        loadWeatherStats()
    }

    private suspend fun loadForecastAndAlerts(lat: Double, lon: Double) {
        when (val result = weatherUseCases.getWeatherForecast(lat, lon, 7)) {
            is Resource.Success -> {
                _forecast.value = result.data ?: emptyList()
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
            }
            is Resource.Loading -> Unit
        }

        when (val result = weatherUseCases.getWeatherAlerts(lat, lon)) {
            is Resource.Success -> {
                _weatherAlerts.value = result.data ?: emptyList()
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
            }
            is Resource.Loading -> Unit
        }
    }

    private suspend fun loadWeatherStats() {
        val userId = getCurrentUserId()
        when (val result = weatherUseCases.getWeatherStats(userId, 30)) {
            is Resource.Success -> {
                _weatherStats.value = result.data
            }
            is Resource.Error -> {
                _errorMessage.value = result.message
            }
            is Resource.Loading -> Unit
        }
    }

    fun selectLocation(location: SavedLocation) {
        _selectedLocation.value = location
        loadWeatherData(forceRefresh = true)
    }

    fun refreshWeather() {
        loadWeatherData(forceRefresh = true)
    }

    fun getCurrentWeatherCondition(): WeatherCondition? = _currentWeather.value

    fun getTodayForecast(): WeatherForecast? = _forecast.value.firstOrNull()

    fun getUpcomingForecast(): List<WeatherForecast> = _forecast.value.drop(1)

    fun getActiveAlerts(): List<WeatherAlert> = _weatherAlerts.value.filter { alert ->
        val now = java.util.Date()
        alert.startTime.before(now) && alert.endTime.after(now)
    }

    fun getWeatherTrend(): WeatherTrend {
        val forecasts = _forecast.value
        if (forecasts.size < 2) return WeatherTrend.STABLE

        val firstTemp = forecasts.first().tempMax
        val lastTemp = forecasts.last().tempMax

        return when {
            lastTemp > firstTemp + 2 -> WeatherTrend.WARMING
            lastTemp < firstTemp - 2 -> WeatherTrend.COOLING
            else -> WeatherTrend.STABLE
        }
    }

    fun getComfortIndex(): ComfortIndex {
        val weather = _currentWeather.value ?: return ComfortIndex.UNKNOWN
        val temp = weather.current.temperature
        val humidity = weather.current.humidity
        val heatIndex = calculateHeatIndex(temp, humidity)

        return when {
            heatIndex < 27 -> ComfortIndex.COMFORTABLE
            heatIndex < 32 -> ComfortIndex.CAUTION
            heatIndex < 40 -> ComfortIndex.EXTREME_CAUTION
            else -> ComfortIndex.DANGER
        }
    }

    private fun calculateHeatIndex(temp: Double, humidity: Int): Double {
        if (temp < 27) return temp
        val t = temp
        val h = humidity.toDouble()
        return -8.78469475556 + 1.61139411 * t + 2.33854883889 * h + -0.14611605 * t * h + -0.012308094 * t * t + -0.0164248277778 * h * h + 0.002211732 * t * t * h + 0.00072546 * t * h * h + -0.000003582 * t * t * h * h
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun getCurrentLocation(): SavedLocation? {
        return try {
            val location = locationUtils.getCurrentLocation()
            location?.let {
                SavedLocation(
                    id = "current",
                    name = "Localização Atual",
                    latitude = it.latitude,
                    longitude = it.longitude,
                    city = "Cidade Atual",
                    country = "Brasil",
                    isDefault = true
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentUserId(): String {
        return appPreferences.getUserId().ifEmpty { "admin_id" }
    }
}

enum class WeatherTrend {
    WARMING, COOLING, STABLE
}

enum class ComfortIndex {
    COMFORTABLE, CAUTION, EXTREME_CAUTION, DANGER, UNKNOWN
}
