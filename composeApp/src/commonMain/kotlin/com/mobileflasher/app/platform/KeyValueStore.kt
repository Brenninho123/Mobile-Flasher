package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable

interface KeyValueStore {
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

@Composable
expect fun rememberKeyValueStore(): KeyValueStore
