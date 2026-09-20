package com.mobileflasher.app.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePicker(mode: FilePickerMode, onFilePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val readUri = { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) onFilePicked(bytes)
        }
    }
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), readUri)
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), readUri)
    return when (mode) {
        FilePickerMode.IMAGE -> ({ getContent.launch("image/*") })
        FilePickerMode.DOCUMENT -> ({ getContent.launch("*/*") })
        FilePickerMode.MEDIA -> ({ openDocument.launch(arrayOf("image/gif", "video/*")) })
    }
}
