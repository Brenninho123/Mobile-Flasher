package com.mobileflasher.app.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class SharedPreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {
    override fun getInt(key: String, default: Int): Int = preferences.getInt(key, default)

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        SharedPreferencesStore(context.getSharedPreferences("mobile_flasher_settings", Context.MODE_PRIVATE))
    }
}
