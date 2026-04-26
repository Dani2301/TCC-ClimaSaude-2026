package com.climasaude.presentation.viewmodels

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.database.entities.Symptom as DbSymptom
import com.climasaude.data.repository.HealthRepository
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.data.receivers.MedicationAlarmReceiver
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val appPreferences: AppPreferences,
    private val application: Application
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _symptoms = MutableStateFlow<List<DbSymptom>>(emptyList())
    val symptoms: StateFlow<List<DbSymptom>> = _symptoms.asStateFlow()

    private val _operationResult = MutableSharedFlow<Resource<String>>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadMedications()
        loadSymptoms()
    }

    private fun loadMedications() {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            healthRepository.getActiveMedications(userId).collect { list ->
                _medications.value = list
            }
        }
    }

    private fun loadSymptoms() {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            healthRepository.getAllSymptoms(userId).collect { list ->
                _symptoms.value = list
            }
        }
    }

    fun addMedication(name: String, dosage: String, time: String) {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            val medId = UUID.randomUUID().toString()
            val newMed = Medication(
                id = medId,
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
            
            // Agendar lembrete usando AlarmManager (mais preciso que WorkManager para horários fixos). Modificado por: Daniel
            scheduleAlarm(medId, time)
            
            _operationResult.emit(Resource.Success("Medicamento adicionado e lembrete configurado"))
        }
    }

    private fun scheduleAlarm(medicationId: String, timeStr: String) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, MedicationAlarmReceiver::class.java).apply {
            putExtra("medication_id", medicationId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ exige permissão para alarmes exatos. Modificado por: Daniel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun recordSymptom(name: String, intensity: Int, notes: String?) {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            val symptom = DbSymptom(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                intensity = intensity,
                notes = notes,
                timestamp = Date()
            )
            healthRepository.addSymptom(symptom)
            _operationResult.emit(Resource.Success("Sintoma registrado com sucesso"))
        }
    }

    fun deleteSymptom(symptom: DbSymptom) {
        viewModelScope.launch {
            healthRepository.deleteSymptom(symptom)
            _operationResult.emit(Resource.Success("Sintoma removido"))
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            healthRepository.addMedication(medication.copy(isActive = false))
            
            // Cancelar Alarme. Modificado por: Daniel
            val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(application, MedicationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                application,
                medication.id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }

            _operationResult.emit(Resource.Success("Medicamento removido"))
        }
    }
}
