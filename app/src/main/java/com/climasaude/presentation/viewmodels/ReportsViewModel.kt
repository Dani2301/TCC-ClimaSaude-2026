package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.HealthRepository
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReportsUiState(
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val statusMessage: String = ""
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    fun exportHealthReport(targetFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, statusMessage = "Salvando relatório...")
            
            try {
                val userId = appPreferences.getUserId()
                val result = healthRepository.exportMedicationsAndSymptoms(userId, targetFile)
                
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportedFilePath = result.data?.absolutePath,
                            statusMessage = "Relatório salvo em: ${targetFile.name}"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            statusMessage = result.message ?: "Erro ao salvar"
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    statusMessage = "Erro: ${e.message}"
                )
            }
        }
    }
}
