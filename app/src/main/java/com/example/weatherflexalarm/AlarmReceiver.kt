package com.example.weatherflexalarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "到点啦"
        try {
            AlarmRingingService.start(context, reason)
        } catch (_: Throwable) {
            // Fallback: if service start fails, at least keep one visible notification for user awareness.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                createFallbackChannel(context)
                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_FALLBACK,
                    androidx.core.app.NotificationCompat.Builder(context, FALLBACK_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("起床啦")
                        .setContentText(reason)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                        .build()
                )
            }
        }

        // Schedule next day as soon as this alarm fires.
        WeatherWorkScheduler.scheduleImmediate(context)
    }

    private fun createFallbackChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            FALLBACK_CHANNEL_ID,
            "Weather Alarm Fallback",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_REASON = "extra_reason"
        private const val FALLBACK_CHANNEL_ID = "weather_alarm_fallback_v1"
        private const val NOTIFICATION_ID_FALLBACK = 4004
    }
}
