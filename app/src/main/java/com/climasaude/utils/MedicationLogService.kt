package com.climasaude.utils

import android.content.Context
import com.climasaude.data.database.dao.MedicationLogDao
import com.climasaude.data.database.entities.MedicationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationLogService @Inject constructor(
    private val medicationLogDao: MedicationLogDao
) {
    
    fun markMedicationTaken(medicationId: String, logId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                medicationLogDao.markAsTaken(logId, Date())
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }
    
    fun snoozeMedication(medicationId: String, logId: String, snoozeMinutes: Int = 5) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newScheduledTime = Date(System.currentTimeMillis() + snoozeMinutes * 60 * 1000)
                medicationLogDao.snoozeLog(logId, newScheduledTime)
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }
    
    fun createMedicationLog(medicationId: String, userId: String, scheduledTime: Date, dosage: String): String {
        val logId = UUID.randomUUID().toString()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val log = MedicationLog(
                    id = logId,
                    medicationId = medicationId,
                    userId = userId,
                    scheduledTime = scheduledTime,
                    takenTime = null,
                    isTaken = false,
                    dosage = dosage,
                    reminderTriggered = true
                )
                medicationLogDao.insertLog(log)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return logId
    }
}