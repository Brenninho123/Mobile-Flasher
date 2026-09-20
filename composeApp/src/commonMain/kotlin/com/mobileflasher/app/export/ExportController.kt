package com.mobileflasher.app.export

import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.encodeMp4
import com.mobileflasher.app.platform.encodeToPng
import com.mobileflasher.app.render.renderAllFramesToImageBitmaps
import com.mobileflasher.app.render.renderFrameToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportController(
    private val saveBytes: (bytes: ByteArray, fileName: String, mimeType: String) -> Unit
) {
    fun exportCurrentFrameAsPng(project: Project, frameIndex: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bitmap = renderFrameToImageBitmap(project, frameIndex, width, height)
        val bytes = bitmap.encodeToPng()
        saveBytes(bytes, sanitizeFileName(project.name) + ".png", "image/png")
    }

    suspend fun exportAnimationAsGif(project: Project, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bytes = withContext(Dispatchers.Default) {
            val bitmaps = renderAllFramesToImageBitmaps(project, width, height)
            val delayCentiseconds = (100 / project.frameRate.coerceAtLeast(1)).coerceAtLeast(2)
            GifEncoder.encode(bitmaps, delayCentiseconds)
        }
        saveBytes(bytes, sanitizeFileName(project.name) + ".gif", "image/gif")
    }

    suspend fun exportAnimationAsMp4(project: Project, width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val bytes = withContext(Dispatchers.Default) {
            val bitmaps = renderAllFramesToImageBitmaps(project, width, height)
            encodeMp4(bitmaps, project.frameRate)
        } ?: return false
        saveBytes(bytes, sanitizeFileName(project.name) + ".mp4", "video/mp4")
        return true
    }

    fun exportProjectXml(xml: String, projectName: String) {
        saveBytes(xml.encodeToByteArray(), sanitizeFileName(projectName) + ".mflash.xml", "text/xml")
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return cleaned.ifBlank { "mobile-flasher-project" }
    }
}
