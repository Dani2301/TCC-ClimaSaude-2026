package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.HealthRepository
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.data.database.entities.Symptom
import com.climasaude.data.database.entities.MedicationLog
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

sealed class HealthHistoryItem {
    abstract val id: String
    abstract val timestamp: Date

    data class SymptomEntry(val symptom: Symptom) : HealthHistoryItem() {
        override val id: String = symptom.id
        override val timestamp: Date = symptom.timestamp
    }

    data class MedicationEntry(val log: MedicationLog) : HealthHistoryItem() {
        override val id: String = log.id
        override val timestamp: Date = log.scheduledTime
    }
}

data class ReportsUiState(
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val statusMessage: String = "",
    val historyItems: List<HealthHistoryItem> = emptyList(),
    val isLoadingHistory: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadHealthHistory()
    }

    private fun loadHealthHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val userId = appPreferences.getUserId()
            
            val symptomsFlow = healthRepository.getAllSymptomsFlow(userId)
            val logsFlow = healthRepository.getAllMedicationLogsFlow(userId)

            combine(symptomsFlow, logsFlow) { symptoms, logs ->
                val items = mutableListOf<HealthHistoryItem>()
                items.addAll(symptoms.map { HealthHistoryItem.SymptomEntry(it) })
                items.addAll(logs.map { HealthHistoryItem.MedicationEntry(it) })
                items.sortedByDescending { it.timestamp }
            }.collect { items ->
                _uiState.update { it.copy(historyItems = items, isLoadingHistory = false) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            healthRepository.clearHealthHistory(userId)
            _uiState.update { it.copy(statusMessage = "Histórico limpo com sucesso") }
        }
    }

    fun exportHealthReport(targetFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "Salvando relatório...") }
            
            try {
                val userId = appPreferences.getUserId()
                val result = healthRepository.exportMedicationsAndSymptoms(userId, targetFile)
                
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(
                            isExporting = false,
                            exportedFilePath = result.data?.absolutePath,
                            statusMessage = "Relatório salvo em: ${targetFile.name}"
                        ) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(
                            isExporting = false,
                            statusMessage = result.message ?: "Erro ao salvar"
                        ) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isExporting = false,
                    statusMessage = "Erro: ${e.message}"
                ) }
            }
        }
    }
}
