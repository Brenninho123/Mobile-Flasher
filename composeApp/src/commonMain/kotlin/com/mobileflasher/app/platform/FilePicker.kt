package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable

enum class FilePickerMode {
    IMAGE,
    DOCUMENT,
    MEDIA
}

@Composable
expect fun rememberFilePicker(mode: FilePickerMode, onFilePicked: (ByteArray) -> Unit): () -> Unit
