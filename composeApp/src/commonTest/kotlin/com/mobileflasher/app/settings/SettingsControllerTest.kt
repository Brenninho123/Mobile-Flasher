package com.mobileflasher.app.settings

import com.mobileflasher.app.i18n.Language
import com.mobileflasher.app.platform.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class MemoryStore : KeyValueStore {
    val ints = HashMap<String, Int>()
    val booleans = HashMap<String, Boolean>()

    override fun getInt(key: String, default: Int) = ints[key] ?: default
    override fun putInt(key: String, value: Int) { ints[key] = value }
    override fun getBoolean(key: String, default: Boolean) = booleans[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
}

class SettingsControllerTest {

    @Test
    fun startsWithDefaultsWhenStoreIsEmpty() {
        val controller = SettingsController(MemoryStore())
        assertEquals(AppSettings(), controller.settings.value)
    }

    @Test
    fun persistsChangesAndReloadsThem() {
        val store = MemoryStore()
        SettingsController(store).update { it.copy(themeMode = ThemeMode.DARK, defaultFrameRate = 30, snapByDefault = true) }

        val reloaded = SettingsController(store).settings.value

        assertEquals(ThemeMode.DARK, reloaded.themeMode)
        assertEquals(30, reloaded.defaultFrameRate)
        assertTrue(reloaded.snapByDefault)
    }

    @Test
    fun resetRestoresDefaults() {
        val store = MemoryStore()
        val controller = SettingsController(store)
        controller.update { it.copy(reduceMotion = true, onionSkinByDefault = false) }
        controller.reset()

        assertFalse(controller.settings.value.reduceMotion)
        assertTrue(SettingsController(store).settings.value.onionSkinByDefault)
    }

    @Test
    fun ignoresCorruptStoredValues() {
        val store = MemoryStore()
        store.ints["theme_mode"] = 99
        store.ints["default_frame_rate"] = 500

        val settings = SettingsController(store).settings.value

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(60, settings.defaultFrameRate)
    }
}

class LanguageSettingTest {

    @Test
    fun defaultsToAutomaticDetection() {
        assertEquals(null, SettingsController(MemoryStore()).settings.value.language)
    }

    @Test
    fun persistsAndClearsTheLanguageChoice() {
        val store = MemoryStore()
        SettingsController(store).update { it.copy(language = Language.FRENCH) }
        assertEquals(Language.FRENCH, SettingsController(store).settings.value.language)

        SettingsController(store).update { it.copy(language = null) }
        assertEquals(null, SettingsController(store).settings.value.language)
    }

    @Test
    fun ignoresCorruptStoredLanguage() {
        val store = MemoryStore()
        store.ints["language"] = 42
        assertEquals(null, SettingsController(store).settings.value.language)
    }
}
