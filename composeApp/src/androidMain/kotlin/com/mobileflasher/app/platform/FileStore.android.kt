package com.mobileflasher.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

private class DirectoryFileStore(private val directory: File) : FileStore {

    init {
        directory.mkdirs()
    }

    override fun read(name: String): ByteArray? {
        val file = File(directory, name)
        return try {
            if (file.isFile) file.readBytes() else null
        } catch (e: Exception) {
            null
        }
    }

    override fun write(name: String, bytes: ByteArray): Boolean {
        return try {
            val target = File(directory, name)
            val temporary = File(directory, "$name.tmp")
            temporary.writeBytes(bytes)
            if (target.exists()) target.delete()
            temporary.renameTo(target)
        } catch (e: Exception) {
            false
        }
    }

    override fun delete(name: String) {
        try {
            File(directory, name).delete()
        } catch (e: Exception) {
        }
    }
}

@Composable
actual fun rememberFileStore(): FileStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { DirectoryFileStore(File(context.filesDir, "library")) }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
