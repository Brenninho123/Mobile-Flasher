package com.mobileflasher.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

@OptIn(ExperimentalForeignApi::class)
fun openIncomingFile(path: String) {
    val file = fopen(path, "rb") ?: return
    fseek(file, 0, SEEK_END)
    val size = ftell(file)
    fseek(file, 0, SEEK_SET)
    if (size <= 0) {
        fclose(file)
        return
    }
    val buffer = ByteArray(size.toInt())
    buffer.usePinned { pinned ->
        fread(pinned.addressOf(0), 1u, size.toULong(), file)
    }
    fclose(file)
    IncomingFiles.offer(buffer)
}
