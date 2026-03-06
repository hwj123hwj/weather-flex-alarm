package com.example.weatherflexalarm

data class AlarmSettings(
    val baseHour: Int = 7,
    val baseMinute: Int = 30,
    val advanceMinutes: Int = 20,
    val advanceOnRain: Boolean = true,
    val advanceOnSnow: Boolean = true,
    val latitude: Double = 31.2304,
    val longitude: Double = 121.4737
)
