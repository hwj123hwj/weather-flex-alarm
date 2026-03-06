package com.example.weatherflexalarm

import android.content.Context

object SettingsStore {
    private const val PREFS = "weather_flex_alarm"
    private const val KEY_BASE_HOUR = "base_hour"
    private const val KEY_BASE_MINUTE = "base_minute"
    private const val KEY_ADVANCE_MINUTES = "advance_minutes"
    private const val KEY_ADVANCE_RAIN = "advance_rain"
    private const val KEY_ADVANCE_SNOW = "advance_snow"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_LAST_SCHEDULE_MILLIS = "last_schedule_millis"
    private const val KEY_LAST_REASON = "last_reason"

    fun load(context: Context): AlarmSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AlarmSettings(
            baseHour = prefs.getInt(KEY_BASE_HOUR, 7),
            baseMinute = prefs.getInt(KEY_BASE_MINUTE, 30),
            advanceMinutes = prefs.getInt(KEY_ADVANCE_MINUTES, 20),
            advanceOnRain = prefs.getBoolean(KEY_ADVANCE_RAIN, true),
            advanceOnSnow = prefs.getBoolean(KEY_ADVANCE_SNOW, true),
            latitude = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: 31.2304,
            longitude = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: 121.4737
        )
    }

    fun save(context: Context, settings: AlarmSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BASE_HOUR, settings.baseHour)
            .putInt(KEY_BASE_MINUTE, settings.baseMinute)
            .putInt(KEY_ADVANCE_MINUTES, settings.advanceMinutes)
            .putBoolean(KEY_ADVANCE_RAIN, settings.advanceOnRain)
            .putBoolean(KEY_ADVANCE_SNOW, settings.advanceOnSnow)
            .putString(KEY_LAT, settings.latitude.toString())
            .putString(KEY_LON, settings.longitude.toString())
            .apply()
    }

    fun saveLastScheduled(context: Context, triggerAtMillis: Long, reason: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_SCHEDULE_MILLIS, triggerAtMillis)
            .putString(KEY_LAST_REASON, reason)
            .apply()
    }

    fun loadLastScheduled(context: Context): Pair<Long, String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val millis = prefs.getLong(KEY_LAST_SCHEDULE_MILLIS, -1L)
        val reason = prefs.getString(KEY_LAST_REASON, "") ?: ""
        return millis to reason
    }
}
