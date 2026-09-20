package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fwrite
import platform.posix.remove
import platform.posix.rename

@OptIn(ExperimentalForeignApi::class)
private class DocumentsFileStore : FileStore {

    private val root: String = (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String) ?: NSTemporaryDirectory()

    private fun pathFor(name: String) = "$root/$name"

    override fun read(name: String): ByteArray? {
        val file = fopen(pathFor(name), "rb") ?: return null
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fseek(file, 0, SEEK_SET)
        if (size <= 0) {
            fclose(file)
            return ByteArray(0)
        }
        val buffer = ByteArray(size.toInt())
        buffer.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, size.toULong(), file)
        }
        fclose(file)
        return buffer
    }

    override fun write(name: String, bytes: ByteArray): Boolean {
        val target = pathFor(name)
        val temporary = "$target.tmp"
        val file = fopen(temporary, "wb") ?: return false
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
            }
        }
        fclose(file)
        remove(target)
        return rename(temporary, target) == 0
    }

    override fun delete(name: String) {
        remove(pathFor(name))
    }
}

@Composable
actual fun rememberFileStore(): FileStore = remember { DocumentsFileStore() }

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
