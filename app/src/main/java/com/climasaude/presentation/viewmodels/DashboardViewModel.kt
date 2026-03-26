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

    private val _healthRiskAssessment = MutableStateFlow<HealthRiskAssessment?>(null)
    val healthRiskAssessment: StateFlow<HealthRiskAssessment?> = _healthRiskAssessment.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData(forceGPS: Boolean = false) {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _dashboardState.value = DashboardState.Loading

                // Lógica de sincronização de localização. Modificado por: Daniel
                val savedLat = appPreferences.getLastLatitude()
                val savedLon = appPreferences.getLastLongitude()

                if (forceGPS) {
                    val location = locationUtils.getCurrentLocation()
                    if (location != null) {
                        // Atualiza a última localização conhecida com o GPS real
                        appPreferences.setLastLocation(location.latitude, location.longitude)
                        loadWeatherData(location.latitude, location.longitude, true)
                    } else {
                        loadWeatherData(savedLat, savedLon, true)
                    }
                } else if (savedLat != 0.0 && savedLon != 0.0) {
                    // Usa a cidade que o usuário pesquisou por último no Clima
                    loadWeatherData(savedLat, savedLon, false)
                } else {
                    val location = locationUtils.getCurrentLocation()
                    if (location != null) {
                        loadWeatherData(location.latitude, location.longitude, false)
                    } else {
                        _dashboardState.value = DashboardState.Error("Defina uma localização no Clima")
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
                if (weather != null) {
                    _weatherCondition.value = weather
                    loadHealthData(weather)
                    _dashboardState.value = DashboardState.Success
                }
            }
            is Resource.Error -> {
                _dashboardState.value = DashboardState.Error(weatherResult.message ?: "Erro ao carregar clima")
            }
            else -> {}
        }
    }

    private suspend fun loadHealthData(weatherCondition: WeatherCondition) {
        val userId = appPreferences.getUserId()
        when (val riskResult = healthUseCases.assessHealthRisk(userId, weatherCondition)) {
            is Resource.Success -> {
                _healthRiskAssessment.value = riskResult.data
            }
            else -> {}
        }
    }

    fun refreshData() {
        // Ao dar refresh (Swipe), tentamos o GPS. Modificado por: Daniel
        loadDashboardData(forceGPS = true)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

sealed class DashboardState {
    object Loading : DashboardState()
    object Success : DashboardState()
    data class Error(val message: String) : DashboardState()
}
