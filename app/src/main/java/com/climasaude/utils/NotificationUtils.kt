package com.climasaude.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Size
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.appcompat.content.res.AppCompatResources
import com.climasaude.R
import com.climasaude.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    Constants.WEATHER_ALERTS_CHANNEL_ID,
                    "Alertas Meteorológicos",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    Constants.MEDICATION_REMINDERS_CHANNEL_ID,
                    "Lembretes de Medicamentos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                },
                NotificationChannel(
                    Constants.EMERGENCY_ALERTS_CHANNEL_ID,
                    "Alertas Críticos",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun showWeatherAlert(title: String, message: String) {
        if (!checkPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "weather")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.WEATHER_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.error))
            .build()

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) { }
    }

    // Adicionado logId para sincronizar com o Worker e Receiver. Modificado por: Daniel
    fun showMedicationReminder(medicationName: String, dosage: String, time: String, medicationId: String, logId: String) {
        if (!checkPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "health")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_TAKEN"
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(context, medicationId.hashCode(), takenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_SNOOZE"
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, medicationId.hashCode() + 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, Constants.MEDICATION_REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Hora do medicamento: $medicationName")
            .setContentText("Dose: $dosage às $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "Tomei", takenPendingIntent)
            .addAction(R.drawable.ic_refresh, "Lembrar em 5min", snoozePendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(medicationId.hashCode(), notification)
        } catch (_: SecurityException) { }
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
