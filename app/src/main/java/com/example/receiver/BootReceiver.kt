package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.domain.alarm.MuhurtaAlarmScheduler

/**
 * Restores all user-enabled dynamic Muhurta alarms after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val scheduler = MuhurtaAlarmScheduler(context)
            scheduler.rescheduleAllActiveAlarms()
        }
    }
}
