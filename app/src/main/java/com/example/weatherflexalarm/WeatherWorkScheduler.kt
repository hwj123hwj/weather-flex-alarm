package com.example.weatherflexalarm

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WeatherWorkScheduler {
    private const val PERIODIC_NAME = "weather_alarm_periodic"
    private const val IMMEDIATE_NAME = "weather_alarm_immediate"

    fun scheduleAll(context: Context) {
        schedulePeriodic(context)
        scheduleImmediate(context)
    }

    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<WeatherAdjustWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeatherAdjustWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
