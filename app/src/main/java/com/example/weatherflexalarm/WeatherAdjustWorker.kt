package com.example.weatherflexalarm

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import java.time.ZonedDateTime

class WeatherAdjustWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val settings = SettingsStore.load(applicationContext)
        val now = ZonedDateTime.now()
        val baseTime = AlarmMath.nextBaseAlarmTime(settings, now)
        val scheduler = AlarmScheduler(applicationContext)

        return try {
            val points = WeatherClient.fetchHourlyWeather(settings.latitude, settings.longitude)
            val code = WeatherClient.nearestWeatherCode(points, baseTime)
            val shouldAdvance = code?.let { WeatherClassifier.shouldAdvance(it, settings) } ?: false
            var finalTime = if (shouldAdvance) {
                baseTime.minusMinutes(settings.advanceMinutes.toLong())
            } else {
                baseTime
            }

            var reason = when {
                code == null -> "天气拉取失败，按基础时间"
                shouldAdvance -> "${WeatherClassifier.weatherLabel(code)}，提前 ${settings.advanceMinutes} 分钟"
                else -> "天气正常，按基础时间"
            }

            // Protect against "time in the past", which can happen in near-time testing with a large advance value.
            if (!finalTime.isAfter(now.plusSeconds(15))) {
                finalTime = now.plusMinutes(1).withSecond(0).withNano(0)
                reason = "$reason（提前后已过时，顺延到 1 分钟后）"
            }

            scheduler.schedule(finalTime, reason)
            Result.success()
        } catch (_: IOException) {
            scheduler.schedule(baseTime, "天气拉取失败，按基础时间")
            Result.retry()
        } catch (_: Exception) {
            scheduler.schedule(baseTime, "天气异常，按基础时间")
            Result.success()
        }
    }
}
