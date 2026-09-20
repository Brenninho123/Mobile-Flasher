package com.mobileflasher.app.settings

import com.mobileflasher.app.i18n.Language
import com.mobileflasher.app.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: Language? = null,
    val reduceMotion: Boolean = false,
    val defaultFrameRate: Int = 24,
    val gridByDefault: Boolean = false,
    val snapByDefault: Boolean = false,
    val onionSkinByDefault: Boolean = true,
    val autosave: Boolean = true
)

class SettingsController(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.update { next }
        save(next)
    }

    fun reset() {
        val defaults = AppSettings()
        _settings.update { defaults }
        save(defaults)
    }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = ThemeMode.entries.getOrElse(store.getInt(KeyTheme, defaults.themeMode.ordinal)) { defaults.themeMode },
            language = Language.entries.getOrNull(store.getInt(KeyLanguage, NoLanguage)),
            reduceMotion = store.getBoolean(KeyReduceMotion, defaults.reduceMotion),
            defaultFrameRate = store.getInt(KeyFrameRate, defaults.defaultFrameRate).coerceIn(1, 60),
            gridByDefault = store.getBoolean(KeyGrid, defaults.gridByDefault),
            snapByDefault = store.getBoolean(KeySnap, defaults.snapByDefault),
            onionSkinByDefault = store.getBoolean(KeyOnion, defaults.onionSkinByDefault),
            autosave = store.getBoolean(KeyAutosave, defaults.autosave)
        )
    }

    private fun save(settings: AppSettings) {
        store.putInt(KeyTheme, settings.themeMode.ordinal)
        store.putInt(KeyLanguage, settings.language?.ordinal ?: NoLanguage)
        store.putBoolean(KeyReduceMotion, settings.reduceMotion)
        store.putInt(KeyFrameRate, settings.defaultFrameRate)
        store.putBoolean(KeyGrid, settings.gridByDefault)
        store.putBoolean(KeySnap, settings.snapByDefault)
        store.putBoolean(KeyOnion, settings.onionSkinByDefault)
        store.putBoolean(KeyAutosave, settings.autosave)
    }

    private companion object {
        const val KeyTheme = "theme_mode"
        const val KeyLanguage = "language"
        const val NoLanguage = -1
        const val KeyReduceMotion = "reduce_motion"
        const val KeyFrameRate = "default_frame_rate"
        const val KeyGrid = "grid_by_default"
        const val KeySnap = "snap_by_default"
        const val KeyOnion = "onion_skin_by_default"
        const val KeyAutosave = "autosave"
    }
}
