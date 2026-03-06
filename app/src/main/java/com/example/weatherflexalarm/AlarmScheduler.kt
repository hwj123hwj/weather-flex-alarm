package com.example.weatherflexalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(triggerAt: ZonedDateTime, reason: String) {
        val triggerMillis = triggerAt.toInstant().toEpochMilli()
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REASON, reason)
        }
        val alarmPi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(alarmPi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            val showIntent = Intent(context, MainActivity::class.java)
            val showPi = PendingIntent.getActivity(
                context,
                2002,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
            alarmManager.setAlarmClock(alarmInfo, alarmPi)
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, alarmPi)
            } catch (_: SecurityException) {
                // Fallback to inexact alarm if exact alarm cannot be scheduled.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, alarmPi)
            }
        }

        SettingsStore.saveLastScheduled(context, triggerMillis, reason)
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
