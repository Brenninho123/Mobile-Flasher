package com.mobileflasher.app.library

fun formatRelativeTime(nowMillis: Long, thenMillis: Long): String {
    val seconds = ((nowMillis - thenMillis) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 45L -> "Just now"
        seconds < 3600L -> "${(seconds / 60L).coerceAtLeast(1L)} min ago"
        seconds < 86400L -> "${seconds / 3600L} h ago"
        seconds < 86400L * 30L -> "${seconds / 86400L} d ago"
        else -> "${seconds / (86400L * 30L)} mo ago"
    }
}
