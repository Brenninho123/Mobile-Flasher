package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

@Composable
actual fun rememberFileSaver(): (bytes: ByteArray, fileName: String, mimeType: String) -> Unit {
    return { bytes, fileName, _ ->
        val filePath = NSTemporaryDirectory() + fileName
        bytes.toNSData().writeToFile(filePath, true)
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }
}
