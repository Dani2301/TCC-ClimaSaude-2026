package com.climasaude.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.climasaude.data.database.dao.MedicationDao
import com.climasaude.data.database.dao.MedicationLogDao
import com.climasaude.data.database.entities.MedicationLog
import com.climasaude.utils.NotificationUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val notificationUtils: NotificationUtils
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getString("medication_id") ?: return Result.failure()
        
        val medication = medicationDao.getMedicationById(medicationId)
        if (medication == null || !medication.isActive || !medication.reminderEnabled) {
            return Result.success()
        }

        val logId = UUID.randomUUID().toString()
        val currentTime = Calendar.getInstance().time
        
        // Registrar o log como pendente (isTaken = false)
        val log = MedicationLog(
            id = logId,
            userId = medication.userId,
            medicationId = medication.id,
            scheduledTime = currentTime,
            takenTime = null,
            isTaken = false,
            dosage = medication.dosage,
            reminderTriggered = true
        )
        medicationLogDao.insertLog(log)

        // Mostrar a notificação
        notificationUtils.showMedicationReminder(
            medicationName = medication.name,
            dosage = medication.dosage,
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentTime),
            medicationId = medication.id,
            logId = logId
        )

        // Agendar a próxima ocorrência se for diário
        if (medication.frequency == "daily") {
            scheduleNextReminder(medication.id)
        }

        return Result.success()
    }

    private fun scheduleNextReminder(medicationId: String) {
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(24, TimeUnit.HOURS)
            .addTag("medication_$medicationId")
            .setInputData(workDataOf("medication_id" to medicationId))
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "medication_$medicationId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
