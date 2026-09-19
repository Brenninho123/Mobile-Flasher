package com.mobileflasher.app.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePicker(mode: FilePickerMode, onFilePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val mimeType = when (mode) {
        FilePickerMode.IMAGE -> "image/*"
        FilePickerMode.DOCUMENT -> "*/*"
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) onFilePicked(bytes)
        }
    }
    return { launcher.launch(mimeType) }
}
