package com.mobileflasher.app.library

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val now = 10_000_000_000L

    @Test
    fun formatsRecentTimes() {
        assertEquals("Just now", formatRelativeTime(now, now - 5_000))
        assertEquals("Just now", formatRelativeTime(now, now + 5_000))
        assertEquals("1 min ago", formatRelativeTime(now, now - 50_000))
        assertEquals("5 min ago", formatRelativeTime(now, now - 5 * 60_000))
    }

    @Test
    fun formatsLongerSpans() {
        assertEquals("3 h ago", formatRelativeTime(now, now - 3 * 3_600_000L))
        assertEquals("2 d ago", formatRelativeTime(now, now - 2 * 86_400_000L))
        assertEquals("2 mo ago", formatRelativeTime(now, now - 65 * 86_400_000L))
    }
}
