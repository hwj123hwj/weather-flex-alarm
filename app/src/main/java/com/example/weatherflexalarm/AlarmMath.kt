package com.example.weatherflexalarm

import java.time.ZonedDateTime

object AlarmMath {
    fun nextBaseAlarmTime(settings: AlarmSettings, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var next = now
            .withHour(settings.baseHour)
            .withMinute(settings.baseMinute)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        return next
    }
}
