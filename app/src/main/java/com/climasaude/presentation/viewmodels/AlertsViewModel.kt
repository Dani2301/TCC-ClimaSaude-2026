package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.database.dao.HealthAlertDao
import com.climasaude.data.database.entities.HealthAlert
import com.climasaude.data.database.entities.AlertType
import com.climasaude.data.database.entities.AlertSeverity
import com.climasaude.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val healthAlertDao: HealthAlertDao,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _alerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val alerts: StateFlow<List<HealthAlert>> = _alerts.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _selectedAlertType = MutableStateFlow<AlertType?>(null)
    val selectedAlertType: StateFlow<AlertType?> = _selectedAlertType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAlerts()
        observeUnreadCount()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userId = getCurrentUserId()
                healthAlertDao.getActiveAlertsFlow(userId).collect { alertList ->
                    _alerts.value = filterAlerts(alertList)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao carregar alertas"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            healthAlertDao.getUnreadAlertsCountFlow(userId).collect { count ->
                _unreadCount.value = count
            }
        }
    }

    fun filterByType(alertType: AlertType?) {
        _selectedAlertType.value = alertType
        val currentAlerts = _alerts.value
        _alerts.value = filterAlerts(currentAlerts)
    }

    private fun filterAlerts(alerts: List<HealthAlert>): List<HealthAlert> {
        val selectedType = _selectedAlertType.value
        return if (selectedType != null) {
            alerts.filter { it.type == selectedType }
        } else {
            alerts
        }.sortedWith(compareByDescending<HealthAlert> { it.severity }
            .thenByDescending { it.createdAt })
    }

    fun markAsRead(alertId: String) {
        viewModelScope.launch {
            try {
                healthAlertDao.markAsRead(alertId)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao marcar como lido: ${e.message}"
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                healthAlertDao.markAllAsRead(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao marcar todos como lidos: ${e.message}"
            }
        }
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            try {
                healthAlertDao.dismissAlert(alertId)
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao dispensar alerta: ${e.message}"
            }
        }
    }

    fun getAlertsByType(type: AlertType): List<HealthAlert> {
        return _alerts.value.filter { it.type == type }
    }

    fun getAlertsBySeverity(severity: AlertSeverity): List<HealthAlert> {
        return _alerts.value.filter { it.severity == severity }
    }

    fun getCriticalAlerts(): List<HealthAlert> {
        return _alerts.value.filter { it.severity == AlertSeverity.CRITICAL }
    }

    fun getUnreadAlerts(): List<HealthAlert> {
        return _alerts.value.filter { !it.isRead }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun getCurrentUserId(): String {
        return appPreferences.getUserId().ifEmpty { "admin_id" }
    }
}
