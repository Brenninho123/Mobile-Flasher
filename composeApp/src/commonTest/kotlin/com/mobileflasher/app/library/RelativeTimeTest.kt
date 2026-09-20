package com.mobileflasher.app.library

import com.mobileflasher.app.i18n.Language
import com.mobileflasher.app.i18n.Localizer
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val now = 10_000_000_000L
    private val english = Localizer(Language.ENGLISH)

    @Test
    fun formatsRecentTimes() {
        assertEquals("Just now", formatRelativeTime(english, now, now - 5_000))
        assertEquals("Just now", formatRelativeTime(english, now, now + 5_000))
        assertEquals("1 min ago", formatRelativeTime(english, now, now - 50_000))
        assertEquals("5 min ago", formatRelativeTime(english, now, now - 5 * 60_000))
    }

    @Test
    fun formatsLongerSpans() {
        assertEquals("3 h ago", formatRelativeTime(english, now, now - 3 * 3_600_000L))
        assertEquals("2 d ago", formatRelativeTime(english, now, now - 2 * 86_400_000L))
        assertEquals("2 mo ago", formatRelativeTime(english, now, now - 65 * 86_400_000L))
    }

    @Test
    fun followsTheSelectedLanguage() {
        assertEquals("há 5 min", formatRelativeTime(Localizer(Language.PORTUGUESE), now, now - 5 * 60_000))
        assertEquals("Gerade eben", formatRelativeTime(Localizer(Language.GERMAN), now, now))
    }
}
