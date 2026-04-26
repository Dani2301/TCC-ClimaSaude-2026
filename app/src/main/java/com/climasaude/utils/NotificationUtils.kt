package com.climasaude.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações sobre condições climáticas extremas"
                    enableVibration(true)
                    setShowBadge(true)
                },

                NotificationChannel(
                    Constants.MEDICATION_REMINDERS_CHANNEL_ID,
                    "Lembretes de Medicamentos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Lembretes para tomar medicamentos"
                    enableVibration(true)
                    setShowBadge(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                },

                NotificationChannel(
                    Constants.HEALTH_TIPS_CHANNEL_ID,
                    "Dicas de Saúde",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Dicas e recomendações personalizadas de saúde"
                    setShowBadge(false)
                },

                NotificationChannel(
                    Constants.EMERGENCY_ALERTS_CHANNEL_ID,
                    "Alertas de Emergência",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas críticos de saúde e emergência"
                    enableVibration(true)
                    setShowBadge(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                }
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun canSendNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    fun showWeatherAlert(
        title: String,
        message: String,
        severity: String = "medium",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!canSendNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "weather")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (severity.lowercase()) {
            "high", "critical" -> R.drawable.ic_weather_alert_severe
            "medium" -> R.drawable.ic_weather_alert_moderate
            else -> R.drawable.ic_weather_alert_low
        }

        val notification = NotificationCompat.Builder(context, Constants.WEATHER_ALERTS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setLargeIcon(getBitmapFromDrawable(R.drawable.ic_weather))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                when (severity.lowercase()) {
                    "high", "critical" -> NotificationCompat.PRIORITY_HIGH
                    "medium" -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Log error or handle gracefully
        }
    }

    fun showMedicationReminder(
        medicationName: String,
        dosage: String,
        time: String,
        medicationId: String,
        logId: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!canSendNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "health")
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action buttons
        val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_TAKEN"
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_SNOOZE"
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.MEDICATION_REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_pill))
            .setContentTitle("Hora do medicamento")
            .setContentText("$medicationName - $dosage às $time")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("É hora de tomar seu medicamento:\n$medicationName\nDosagem: $dosage\nHorário: $time")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(R.drawable.ic_check, "Tomei", takenPendingIntent)
            .addAction(R.drawable.ic_snooze, "Lembrar em 5min", snoozePendingIntent)
            .setDeleteIntent(snoozePendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle gracefully
        }
    }

    fun showHealthTip(
        title: String,
        message: String,
        actionText: String? = null,
        actionIntent: Intent? = null,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!canSendNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "dashboard")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Constants.HEALTH_TIPS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_health_tip)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_health))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (actionText != null && actionIntent != null) {
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_action, actionText, actionPendingIntent)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handle gracefully
        }
    }

    fun showEmergencyAlert(
        title: String,
        message: String,
        actionText: String = "Ver Detalhes",
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        if (!canSendNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "emergency")
            putExtra("alert_id", notificationId.toString())
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.EMERGENCY_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_emergency)
            .setLargeIcon(getBitmapFromDrawable(R.drawable.ic_emergency))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(context.getColor(R.color.error))
            .addAction(R.drawable.ic_emergency_action, actionText, pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle gracefully
        }
    }

    fun showSyncNotification(
        title: String = "Sincronizando dados",
        message: String = "Atualizando informações...",
        notificationId: Int = SYNC_NOTIFICATION_ID
    ) {
        if (!canSendNotifications()) return

        val notification = NotificationCompat.Builder(context, Constants.HEALTH_TIPS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle gracefully
        }
    }

    fun hideSyncNotification() {
        notificationManager.cancel(SYNC_NOTIFICATION_ID)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun isNotificationEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }

    companion object {
        private const val SYNC_NOTIFICATION_ID = 999999
    }

    private fun getBitmapFromDrawable(drawableResId: Int): android.graphics.Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableResId)
            ?: return BitmapFactory.decodeResource(context.resources, drawableResId)

        val size = Size(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        )
        return drawable.toBitmap(size.width, size.height)
    }
}

class MedicationReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("medication_id")
        val logId = intent.getStringExtra("log_id")

        when (intent.action) {
            "MEDICATION_TAKEN" -> {
                val notificationUtils = NotificationUtils(context)
                notificationUtils.cancelNotification(medicationId?.toIntOrNull() ?: 0)

                if (medicationId != null && logId != null) {
                    logMedicationTaken(context, medicationId, logId)
                }
            }

            "MEDICATION_SNOOZE" -> {
                val notificationUtils = NotificationUtils(context)
                notificationUtils.cancelNotification(medicationId?.toIntOrNull() ?: 0)

                if (medicationId != null && logId != null) {
                    scheduleSnoozedReminder(context, medicationId, logId)
                }
            }
        }
    }
    
    private fun logMedicationTaken(context: Context, medicationId: String, logId: String) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            // Implementation placeholder
        }
    }
    
    private fun scheduleSnoozedReminder(context: Context, medicationId: String, logId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val reminderIntent = android.content.Intent(context, MedicationReminderReceiver::class.java).apply {
            action = "MEDICATION_REMINDER"
            putExtra("medication_id", medicationId)
            putExtra("log_id", logId)
        }
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            reminderIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + (5 * 60 * 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
