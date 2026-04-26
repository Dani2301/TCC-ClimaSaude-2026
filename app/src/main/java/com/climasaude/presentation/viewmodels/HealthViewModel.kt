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
            val list = healthRepository.getAllSymptoms(userId)
            _symptoms.value = list
        }
    }

    fun saveMedication(
        id: String? = null,
        name: String,
        dosage: String,
        startTime: String,
        intervalHours: Int
    ) {
        viewModelScope.launch {
            val userId = appPreferences.getUserId()
            val medId = id ?: UUID.randomUUID().toString()
            
            val times = mutableListOf<String>()
            times.add(startTime)
            
            if (intervalHours > 0) {
                val calendar = Calendar.getInstance()
                val parts = startTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
                
                var nextCal = calendar.clone() as Calendar
                val timesInADay = 24 / intervalHours
                for (i in 1 until timesInADay) {
                    nextCal.add(Calendar.HOUR_OF_DAY, intervalHours)
                    val timeStr = String.format(Locale.getDefault(), "%02d:%02d", 
                        nextCal.get(Calendar.HOUR_OF_DAY), nextCal.get(Calendar.MINUTE))
                    times.add(timeStr)
                }
            }

            val medication = Medication(
                id = medId,
                userId = userId,
                name = name,
                dosage = dosage,
                frequency = if (intervalHours > 0) "Interval: ${intervalHours}h" else "Daily",
                times = times,
                startDate = Date(),
                isActive = true,
                reminderEnabled = true,
                updatedAt = Date()
            )
            
            healthRepository.addMedication(medication)
            cancelAlarms(medId)
            
            times.forEachIndexed { index, time ->
                scheduleAlarm(medId, time, index)
            }
            
            val msg = if (id == null) "Medicamento adicionado" else "Medicamento atualizado"
            _operationResult.emit(Resource.Success(msg))
        }
    }

    private fun scheduleAlarm(medicationId: String, timeStr: String, alarmIndex: Int) {
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
        
        val requestCode = medicationId.hashCode() + alarmIndex
        
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarms(medicationId: String) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, MedicationAlarmReceiver::class.java)
        
        for (i in 0 until 6) {
            val requestCode = medicationId.hashCode() + i
            val pendingIntent = PendingIntent.getBroadcast(
                application,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
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
            cancelAlarms(medication.id)
            _operationResult.emit(Resource.Success("Medicamento removido"))
        }
    }
}
