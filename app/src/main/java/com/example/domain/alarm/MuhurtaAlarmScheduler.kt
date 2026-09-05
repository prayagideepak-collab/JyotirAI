package com.example.domain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.models.BirthLocation
import com.example.domain.models.MuhurtaAlarmConfig
import com.example.domain.models.MuhurtaAlarmType
import com.example.receiver.MuhurtaAlarmReceiver

class MuhurtaAlarmScheduler(
    private val context: Context,
    private val repository: MuhurtaAlarmRepository = MuhurtaAlarmRepositoryImpl(context)
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleAlarm(
        type: MuhurtaAlarmType,
        location: BirthLocation,
        profileId: String?
    ): MuhurtaAlarmConfig {
        // 1. Calculate dynamic next occurrence
        val config = MuhurtaAlarmCalculator.calculateNextOccurrence(type, location, profileId)

        // 2. Persist in Repository
        repository.saveAlarm(config)

        // 3. Register with Android AlarmManager
        setSystemAlarm(config)

        return config
    }

    fun cancelAlarm(type: MuhurtaAlarmType, profileId: String?) {
        repository.removeAlarm(type, profileId)

        val intent = Intent(context, MuhurtaAlarmReceiver::class.java).apply {
            action = MuhurtaAlarmReceiver.ACTION_MUHURTA_ALARM
        }
        val requestCode = getRequestCode(type, profileId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllActiveAlarms() {
        val activeAlarms = repository.getAlarms()
        activeAlarms.forEach { config ->
            if (config.isEnabled) {
                val updatedConfig = MuhurtaAlarmCalculator.calculateNextOccurrence(
                    config.type,
                    config.location,
                    config.profileId
                )
                repository.saveAlarm(updatedConfig)
                setSystemAlarm(updatedConfig)
            }
        }
    }

    private fun setSystemAlarm(config: MuhurtaAlarmConfig) {
        val am = alarmManager ?: return

        val intent = Intent(context, MuhurtaAlarmReceiver::class.java).apply {
            action = MuhurtaAlarmReceiver.ACTION_MUHURTA_ALARM
            putExtra(MuhurtaAlarmReceiver.EXTRA_MUHURTA_TYPE, config.type.name)
            putExtra(MuhurtaAlarmReceiver.EXTRA_PROFILE_ID, config.profileId)
            putExtra(MuhurtaAlarmReceiver.EXTRA_PLACE_NAME, config.location.placeName)
            putExtra(MuhurtaAlarmReceiver.EXTRA_TIME_RANGE, config.formattedLocalTimeRange)
        }

        val requestCode = getRequestCode(config.type, config.profileId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = config.scheduledStartEpochMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun getRequestCode(type: MuhurtaAlarmType, profileId: String?): Int {
        return (type.name.hashCode() * 31) + (profileId?.hashCode() ?: 0)
    }
}
