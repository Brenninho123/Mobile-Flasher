package com.mobileflasher.app.media

import androidx.compose.ui.graphics.ImageBitmap
import com.mobileflasher.app.platform.decodePngToImageBitmap
import com.mobileflasher.app.platform.extractVideoClip
import kotlin.math.roundToInt

class ImportedFrame(val png: ByteArray, val image: ImageBitmap)

class ImportedAnimation(val frames: List<ImportedFrame>, val frameRate: Int)

private const val MaxImportSide = 480
private const val MaxImportFrames = 96

fun decodeAnimation(bytes: ByteArray): ImportedAnimation? {
    return if (GifDecoder.isGif(bytes)) decodeGifAnimation(bytes) else decodeVideoAnimation(bytes)
}

private fun decodeGifAnimation(bytes: ByteArray): ImportedAnimation {
    val gif = GifDecoder.decode(bytes, MaxImportFrames)
    val longest = maxOf(gif.width, gif.height)
    val ratio = if (longest > MaxImportSide) MaxImportSide.toFloat() / longest else 1f
    val targetWidth = (gif.width * ratio).roundToInt().coerceAtLeast(1)
    val targetHeight = (gif.height * ratio).roundToInt().coerceAtLeast(1)

    val frames = gif.frames.map { frame ->
        val pixels = if (ratio < 1f) {
            scaleNearest(frame.argb, gif.width, gif.height, targetWidth, targetHeight)
        } else {
            frame.argb
        }
        val png = PngEncoder.encode(targetWidth, targetHeight, pixels)
        ImportedFrame(png, decodePngToImageBitmap(png))
    }
    val averageDelay = gif.frames.sumOf { it.delayMillis } / gif.frames.size.toDouble()
    val frameRate = (1000.0 / averageDelay.coerceAtLeast(16.0)).roundToInt().coerceIn(1, 60)
    return ImportedAnimation(frames, frameRate)
}

private fun decodeVideoAnimation(bytes: ByteArray): ImportedAnimation? {
    val clip = extractVideoClip(bytes, MaxImportFrames, MaxImportSide) ?: return null
    val frames = clip.framesPng.map { png -> ImportedFrame(png, decodePngToImageBitmap(png)) }
    return ImportedAnimation(frames, clip.frameRate)
}

internal fun scaleNearest(source: IntArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): IntArray {
    val out = IntArray(targetWidth * targetHeight)
    for (y in 0 until targetHeight) {
        val sourceY = (y * height / targetHeight).coerceAtMost(height - 1)
        for (x in 0 until targetWidth) {
            val sourceX = (x * width / targetWidth).coerceAtMost(width - 1)
            out[y * targetWidth + x] = source[sourceY * width + sourceX]
        }
    }
    return out
}
