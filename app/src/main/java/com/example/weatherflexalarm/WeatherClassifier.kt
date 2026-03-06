package com.example.weatherflexalarm

object WeatherClassifier {
    private val rainCodes = setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    private val snowCodes = setOf(71, 73, 75, 77, 85, 86)

    fun shouldAdvance(weatherCode: Int, settings: AlarmSettings): Boolean {
        val byRain = settings.advanceOnRain && weatherCode in rainCodes
        val bySnow = settings.advanceOnSnow && weatherCode in snowCodes
        return byRain || bySnow
    }

    fun weatherLabel(weatherCode: Int): String {
        return when {
            weatherCode in rainCodes -> "雨/雷雨"
            weatherCode in snowCodes -> "雪"
            else -> "普通天气"
        }
    }
}
