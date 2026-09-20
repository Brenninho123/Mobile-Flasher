package com.mobileflasher.app.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalizerTest {

    @Test
    fun everyCatalogCoversEveryKey() {
        Language.entries.forEach { language ->
            val catalog = catalogFor(language)
            val missing = StringKey.entries.filter { it !in catalog }
            assertTrue(missing.isEmpty(), "${language.code} is missing $missing")
        }
    }

    @Test
    fun translationsKeepTheSamePlaceholdersAsEnglish() {
        val pattern = Regex("""\{(\d+)}""")
        fun placeholders(text: String) = pattern.findAll(text).map { it.groupValues[1] }.toSet()
        Language.entries.forEach { language ->
            StringKey.entries.forEach { key ->
                assertEquals(
                    placeholders(EnglishCatalog.getValue(key)),
                    placeholders(catalogFor(language).getValue(key)),
                    "${language.code}/$key"
                )
            }
        }
    }

    @Test
    fun fillsPlaceholdersByPosition() {
        val localizer = Localizer(Language.ENGLISH)
        assertEquals("Frame 7", localizer.text(StringKey.FrameBadge, 7))
        assertEquals("24 fps  |  3 layers", localizer.text(StringKey.ProjectSummary, 24, 3))
        assertEquals("Could not open \"Demo\"", localizer.text(message(StringKey.CouldNotOpenProject, "Demo")))
    }

    @Test
    fun leavesUnknownPlaceholdersUntouched() {
        assertEquals("{0} min ago", Localizer(Language.ENGLISH).text(StringKey.TimeMinutesAgo))
        assertEquals("Frame 1", Localizer(Language.ENGLISH).text(StringKey.FrameBadge, 1, 2))
    }

    @Test
    fun translatesToTheSelectedLanguage() {
        assertEquals("Novo projeto", Localizer(Language.PORTUGUESE).text(StringKey.NewProject))
        assertEquals("Nuevo proyecto", Localizer(Language.SPANISH).text(StringKey.NewProject))
        assertEquals("Nouveau projet", Localizer(Language.FRENCH).text(StringKey.NewProject))
        assertEquals("Neues Projekt", Localizer(Language.GERMAN).text(StringKey.NewProject))
    }

    @Test
    fun parsesLanguageTags() {
        assertEquals(Language.PORTUGUESE, Language.fromTag("pt-BR"))
        assertEquals(Language.SPANISH, Language.fromTag("es_MX"))
        assertEquals(Language.FRENCH, Language.fromTag("FR"))
        assertEquals(Language.GERMAN, Language.fromTag("de-AT-u-co-phonebk"))
        assertNull(Language.fromTag("ja-JP"))
        assertNull(Language.fromTag(""))
        assertNull(Language.fromTag(null))
    }

    @Test
    fun resolvesPreferredThenDeviceThenDefault() {
        assertEquals(Language.GERMAN, resolveLanguage(Language.GERMAN, listOf("pt-BR")))
        assertEquals(Language.PORTUGUESE, resolveLanguage(null, listOf("pt-BR", "en-US")))
        assertEquals(Language.FRENCH, resolveLanguage(null, listOf("ja-JP", "fr-CA")))
        assertEquals(Language.ENGLISH, resolveLanguage(null, listOf("ja-JP")))
        assertEquals(Language.ENGLISH, resolveLanguage(null, emptyList()))
    }
}
