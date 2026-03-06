package com.example.weatherflexalarm

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

data class WeatherPoint(
    val time: ZonedDateTime,
    val weatherCode: Int
)

object WeatherClient {
    fun fetchHourlyWeather(latitude: Double, longitude: Double): List<WeatherPoint> {
        val endpoint =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&hourly=weather_code&forecast_days=2&timezone=auto"

        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        conn.inputStream.use { input ->
            val payload = BufferedReader(InputStreamReader(input)).readText()
            val root = JSONObject(payload)

            val timezone = root.optString("timezone", "UTC")
            val zoneId = ZoneId.of(timezone)
            val hourly = root.getJSONObject("hourly")
            val times = hourly.getJSONArray("time")
            val codes = hourly.getJSONArray("weather_code")

            val result = mutableListOf<WeatherPoint>()
            val size = minOf(times.length(), codes.length())
            for (i in 0 until size) {
                val time = LocalDateTime.parse(times.getString(i))
                val code = codes.getInt(i)
                result += WeatherPoint(
                    time = ZonedDateTime.of(time, zoneId),
                    weatherCode = code
                )
            }
            return result
        }
    }

    fun nearestWeatherCode(points: List<WeatherPoint>, target: ZonedDateTime): Int? {
        if (points.isEmpty()) return null
        val zone = points.first().time.zone
        val targetInForecastZone = target.withZoneSameInstant(zone)
        return points.minByOrNull {
            abs(Duration.between(it.time, targetInForecastZone).toMinutes())
        }?.weatherCode
    }
}
