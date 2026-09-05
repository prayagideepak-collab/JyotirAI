package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.domain.alarm.MuhurtaAlarmRepositoryImpl
import com.example.domain.alarm.MuhurtaAlarmScheduler
import com.example.domain.models.MuhurtaAlarmType

class MuhurtaAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MUHURTA_ALARM = "com.example.action.MUHURTA_ALARM"
        const val EXTRA_MUHURTA_TYPE = "extra_muhurta_type"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_PLACE_NAME = "extra_place_name"
        const val EXTRA_TIME_RANGE = "extra_time_range"

        const val CHANNEL_ID = "muhurta_alarms_channel"
        const val CHANNEL_NAME = "JyotirAI Muhurta Alarms"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MUHURTA_ALARM) return

        val typeStr = intent.getStringExtra(EXTRA_MUHURTA_TYPE) ?: return
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME) ?: "Authoritative Location"
        val timeRange = intent.getStringExtra(EXTRA_TIME_RANGE) ?: ""

        val type = MuhurtaAlarmType.fromString(typeStr)

        // 1. Post JyotirAI Muhurta Notification
        showNotification(context, type, placeName, timeRange)

        // 2. Dynamically Reschedule Next Occurrence (e.g. tomorrow's sunrise interval)
        val scheduler = MuhurtaAlarmScheduler(context)
        val repository = MuhurtaAlarmRepositoryImpl(context)
        val existingConfig = repository.getAlarm(type, profileId)
        if (existingConfig != null && existingConfig.isEnabled) {
            scheduler.scheduleAlarm(type, existingConfig.location, profileId)
        }
    }

    private fun showNotification(
        context: Context,
        type: MuhurtaAlarmType,
        placeName: String,
        timeRange: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily dynamic alarms for Brahma Muhurta, Rahukaal, and Vedic timings"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "JyotirAI • ${type.title} (${type.sanskritName})"
        val notificationBody = "$timeRange | $placeName. ${type.defaultDescription}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (type.hashCode() * 31) + 101
        nm.notify(notificationId, notification)
    }
}
