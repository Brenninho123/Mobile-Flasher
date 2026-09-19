package com.mobileflasher.app.export

import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.encodeToPng
import com.mobileflasher.app.render.renderAllFramesToImageBitmaps
import com.mobileflasher.app.render.renderFrameToImageBitmap

class ExportController(
    private val saveBytes: (bytes: ByteArray, fileName: String, mimeType: String) -> Unit
) {
    fun exportCurrentFrameAsPng(project: Project, frameIndex: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bitmap = renderFrameToImageBitmap(project, frameIndex, width, height)
        val bytes = bitmap.encodeToPng()
        saveBytes(bytes, sanitizeFileName(project.name) + ".png", "image/png")
    }

    fun exportAnimationAsGif(project: Project, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val bitmaps = renderAllFramesToImageBitmaps(project, width, height)
        val delayCentiseconds = (100 / project.frameRate.coerceAtLeast(1)).coerceAtLeast(2)
        val bytes = GifEncoder.encode(bitmaps, delayCentiseconds)
        saveBytes(bytes, sanitizeFileName(project.name) + ".gif", "image/gif")
    }

    fun exportProjectXml(xml: String, projectName: String) {
        saveBytes(xml.encodeToByteArray(), sanitizeFileName(projectName) + ".mflash.xml", "text/xml")
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return cleaned.ifBlank { "mobile-flasher-project" }
    }
}
