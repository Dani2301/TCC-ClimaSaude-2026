package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.domain.usecases.WeatherUseCases
import com.climasaude.domain.usecases.HealthUseCases
import com.climasaude.domain.usecases.HealthRiskAssessment
import com.climasaude.domain.usecases.HealthRiskLevel
import com.climasaude.domain.usecases.RiskLevel
import com.climasaude.domain.usecases.WeatherRiskAnalysis
import com.climasaude.domain.models.*
import com.climasaude.data.database.entities.HealthAlert
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.utils.Resource
import com.climasaude.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val weatherUseCases: WeatherUseCases,
    private val healthUseCases: HealthUseCases,
    private val locationUtils: LocationUtils,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _weatherCondition = MutableStateFlow<WeatherCondition?>(null)
    val weatherCondition: StateFlow<WeatherCondition?> = _weatherCondition.asStateFlow()

    private val _healthAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val healthAlerts: StateFlow<List<HealthAlert>> = _healthAlerts.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _weatherRiskAnalysis = MutableStateFlow<WeatherRiskAnalysis?>(null)
    val weatherRiskAnalysis: StateFlow<WeatherRiskAnalysis?> = _weatherRiskAnalysis.asStateFlow()

    private val _healthRiskAssessment = MutableStateFlow<HealthRiskAssessment?>(null)
    val healthRiskAssessment: StateFlow<HealthRiskAssessment?> = _healthRiskAssessment.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _dashboardState.value = DashboardState.Loading

                // Adicionado Sincronização de localização entre Clima e Dashboard. Modificado por: Daniel
                val savedLat = appPreferences.getLastLatitude()
                val savedLon = appPreferences.getLastLongitude()

                if (savedLat != 0.0 && savedLon != 0.0 && !forceRefresh) {
                    // Se houver uma localização pesquisada recentemente, usa ela
                    loadWeatherData(savedLat, savedLon, false)
                } else {
                    // Senão, recorre ao GPS
                    val location = locationUtils.getCurrentLocation()
                    if (location != null) {
                        loadWeatherData(location.latitude, location.longitude, forceRefresh)
                    } else if (savedLat != 0.0) {
                        // Fallback para a última localização salva se GPS falhar
                        loadWeatherData(savedLat, savedLon, forceRefresh)
                    } else {
                        _dashboardState.value = DashboardState.Error("Não foi possível obter localização")
                    }
                }

            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "Erro desconhecido")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadWeatherData(latitude: Double, longitude: Double, forceRefresh: Boolean) {
        when (val weatherResult = weatherUseCases.getCurrentWeather(latitude, longitude, forceRefresh)) {
            is Resource.Success -> {
                val weather = weatherResult.data
                if (weather == null) {
                    _dashboardState.value = DashboardState.Error("Falha ao carregar clima")
                    return
                }

                _weatherCondition.value = weather
                _weatherRiskAnalysis.value = weatherUseCases.analyzeWeatherRisk(weather)
                loadHealthData(weather)
                _dashboardState.value = DashboardState.Success
            }
            is Resource.Error -> {
                _errorMessage.value = weatherResult.message
                _dashboardState.value = DashboardState.Error(weatherResult.message ?: "Erro ao carregar clima")
            }
            is Resource.Loading -> Unit
        }
    }

    private suspend fun loadHealthData(weatherCondition: WeatherCondition) {
        val userId = getCurrentUserId()

        when (val recommendationsResult = healthUseCases.generateHealthRecommendations(userId, weatherCondition)) {
            is Resource.Success -> {
                _recommendations.value = recommendationsResult.data ?: emptyList()
            }
            is Resource.Error -> {
                _errorMessage.value = recommendationsResult.message
            }
            is Resource.Loading -> Unit
        }

        when (val riskResult = healthUseCases.assessHealthRisk(userId, weatherCondition)) {
            is Resource.Success -> {
                _healthRiskAssessment.value = riskResult.data
            }
            is Resource.Error -> {
                _errorMessage.value = riskResult.message
            }
            is Resource.Loading -> Unit
        }
    }

    fun refreshData() {
        // Ao dar refresh manual (Swipe), forçamos a busca pela localização atual (GPS)
        loadDashboardData(forceRefresh = true)
    }

    fun dismissRecommendation(recommendationId: String) {
        val currentRecommendations = _recommendations.value.toMutableList()
        currentRecommendations.removeAll { it.id == recommendationId }
        _recommendations.value = currentRecommendations
    }

    fun markRecommendationAsRead(recommendationId: String) {
        val currentRecommendations = _recommendations.value.toMutableList()
        val index = currentRecommendations.indexOfFirst { it.id == recommendationId }
        if (index != -1) {
            currentRecommendations[index] = currentRecommendations[index].copy(isRead = true)
            _recommendations.value = currentRecommendations
        }
    }

    fun getWeatherSummary(): WeatherSummary? {
        val weather = _weatherCondition.value ?: return null
        val riskAnalysis = _weatherRiskAnalysis.value

        return WeatherSummary(
            temperature = weather.current.temperature,
            condition = weather.current.description,
            humidity = weather.current.humidity,
            uvIndex = weather.uv?.current ?: 0.0,
            airQuality = weather.airQuality?.level ?: "Desconhecida",
            riskLevel = riskAnalysis?.overallRisk ?: RiskLevel.LOW,
            alerts = weather.alerts.size
        )
    }

    fun getHealthSummary(): HealthSummary? {
        val recommendations = _recommendations.value
        val riskAssessment = _healthRiskAssessment.value

        return HealthSummary(
            activeRecommendations = recommendations.count { !it.isRead },
            urgentRecommendations = recommendations.count { it.priority == Priority.URGENT },
            riskLevel = riskAssessment?.overallRisk ?: HealthRiskLevel.LOW,
            riskFactors = riskAssessment?.riskFactors?.size ?: 0
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun getCurrentUserId(): String {
        return appPreferences.getUserId().ifEmpty { "admin_id" }
    }
}

sealed class DashboardState {
    object Loading : DashboardState()
    object Success : DashboardState()
    data class Error(val message: String) : DashboardState()
}

data class WeatherSummary(
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val uvIndex: Double,
    val airQuality: String,
    val riskLevel: RiskLevel,
    val alerts: Int
)

data class HealthSummary(
    val activeRecommendations: Int,
    val urgentRecommendations: Int,
    val riskLevel: HealthRiskLevel,
    val riskFactors: Int
)
