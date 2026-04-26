package com.climasaude.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medication_id")
        val logId = intent.getStringExtra("log_id")
        val action = intent.action

        Log.d("MedicationReceiver", "Ação recebida: $action para medicamento: $medicationId")

        val notificationUtils = NotificationUtils(context)
        
        when (action) {
            "MEDICATION_TAKEN" -> {
                notificationUtils.cancelNotification(medicationId?.hashCode() ?: 0)
                // Lógica de registrar no banco pode ser adicionada aqui. Modificado por: Daniel
            }
            "MEDICATION_SNOOZE" -> {
                notificationUtils.cancelNotification(medicationId?.hashCode() ?: 0)
                scheduleSnoozedReminder(context, medicationId, logId)
            }
        }
    }

    private fun scheduleSnoozedReminder(context: Context, medicationId: String?, logId: String?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_SNOOZE_RETRIGGER" // Ação interna para o alarme. Modificado por: Daniel
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutos

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("MedicationReceiver", "Erro ao agendar snooze: ${e.message}")
        }
    }
}
