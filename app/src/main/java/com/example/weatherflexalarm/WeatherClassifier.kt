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

    fun weatherDetailLabel(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "晴"
            1 -> "基本晴"
            2 -> "局部多云"
            3 -> "阴"
            45, 48 -> "雾"
            51, 53, 55 -> "毛毛雨"
            56, 57 -> "冻雨"
            61 -> "小雨"
            63 -> "中雨"
            65 -> "大雨"
            66, 67 -> "雨夹冻"
            71 -> "小雪"
            73 -> "中雪"
            75 -> "大雪"
            77 -> "冰粒"
            80 -> "阵雨（小）"
            81 -> "阵雨（中）"
            82 -> "阵雨（强）"
            85 -> "阵雪（小）"
            86 -> "阵雪（强）"
            95 -> "雷暴"
            96, 99 -> "雷暴伴冰雹"
            else -> "未知天气"
        }
    }
}
