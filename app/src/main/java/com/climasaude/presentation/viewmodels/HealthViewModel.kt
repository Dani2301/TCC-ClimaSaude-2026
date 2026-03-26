package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.repository.HealthRepository
import com.climasaude.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    init {
        loadMedications()
    }

    private fun loadMedications() {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            healthRepository.getActiveMedications(userId).collect { list ->
                _medications.value = list
            }
        }
    }

    fun addMedication(name: String, dosage: String, time: String) {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            val newMed = Medication(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                dosage = dosage,
                frequency = "daily",
                times = listOf(time),
                startDate = Date(),
                isActive = true,
                reminderEnabled = true
            )
            healthRepository.addMedication(newMed)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            // Desativa o medicamento para que suma da lista ativa. Modificado por: Daniel
            healthRepository.addMedication(medication.copy(isActive = false))
        }
    }
}
