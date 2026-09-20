package com.mobileflasher.app.library

import com.mobileflasher.app.i18n.Localizer
import com.mobileflasher.app.i18n.StringKey

fun formatRelativeTime(localizer: Localizer, nowMillis: Long, thenMillis: Long): String {
    val seconds = ((nowMillis - thenMillis) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 45L -> localizer.text(StringKey.TimeJustNow)
        seconds < 3600L -> localizer.text(StringKey.TimeMinutesAgo, (seconds / 60L).coerceAtLeast(1L))
        seconds < 86400L -> localizer.text(StringKey.TimeHoursAgo, seconds / 3600L)
        seconds < 86400L * 30L -> localizer.text(StringKey.TimeDaysAgo, seconds / 86400L)
        else -> localizer.text(StringKey.TimeMonthsAgo, seconds / (86400L * 30L))
    }
}
