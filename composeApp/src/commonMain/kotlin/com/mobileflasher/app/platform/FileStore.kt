package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable

interface FileStore {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray): Boolean
    fun delete(name: String)
}

@Composable
expect fun rememberFileStore(): FileStore

expect fun currentTimeMillis(): Long
