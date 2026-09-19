package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFileSaver(): (bytes: ByteArray, fileName: String, mimeType: String) -> Unit
