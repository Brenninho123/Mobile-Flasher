package com.mobileflasher.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IncomingFiles {
    private val _pending = MutableStateFlow<ByteArray?>(null)
    val pending: StateFlow<ByteArray?> = _pending.asStateFlow()

    fun offer(bytes: ByteArray) {
        _pending.value = bytes
    }

    fun clear() {
        _pending.value = null
    }
}
