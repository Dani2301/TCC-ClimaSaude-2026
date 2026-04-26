package com.climasaude.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.climasaude.data.workers.MedicationReminderWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medication_id") ?: return

        // Usar WorkManager para processar o lembrete de forma segura em background
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(workDataOf("medication_id" to medicationId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "medication_reminder_$medicationId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
