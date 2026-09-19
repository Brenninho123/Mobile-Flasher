package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
private fun writeBytesToFile(path: String, bytes: ByteArray) {
    val file = fopen(path, "wb") ?: return
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
        }
    }
    fclose(file)
}

@Composable
actual fun rememberFileSaver(): (bytes: ByteArray, fileName: String, mimeType: String) -> Unit {
    return { bytes, fileName, _ ->
        val filePath = NSTemporaryDirectory() + fileName
        writeBytesToFile(filePath, bytes)
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }
}
