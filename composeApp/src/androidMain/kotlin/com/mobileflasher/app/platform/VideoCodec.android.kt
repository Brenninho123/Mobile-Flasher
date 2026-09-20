package com.mobileflasher.app.platform

import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaDataSource
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import androidx.compose.ui.graphics.ImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

private const val SampleFrameRate = 12
private const val CodecTimeoutUs = 10_000L

actual fun extractVideoClip(video: ByteArray, maxFrames: Int, maxSide: Int): VideoClip? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(object : MediaDataSource() {
            override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                if (position >= video.size) return -1
                val count = minOf(size.toLong(), video.size - position).toInt()
                System.arraycopy(video, position.toInt(), buffer, offset, count)
                return count
            }

            override fun getSize(): Long = video.size.toLong()

            override fun close() = Unit
        })
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: return null
        val durationSeconds = max(durationMs, 1L) / 1000.0
        var count = ceil(durationSeconds * SampleFrameRate).toInt().coerceAtLeast(1)
        var frameRate = SampleFrameRate
        if (count > maxFrames) {
            count = maxFrames
            frameRate = (count / durationSeconds).roundToInt().coerceIn(1, SampleFrameRate)
        }

        val frames = ArrayList<ByteArray>(count)
        for (index in 0 until count) {
            val timeUs = (index * durationMs * 1000L / count).coerceAtLeast(0L)
            val source = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
            val longest = max(source.width, source.height)
            val bitmap = if (longest > maxSide) {
                val ratio = maxSide.toFloat() / longest
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * ratio).roundToInt().coerceAtLeast(1),
                    (source.height * ratio).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                source
            }
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            frames.add(stream.toByteArray())
            if (bitmap !== source) bitmap.recycle()
            source.recycle()
        }
        if (frames.isEmpty()) null else VideoClip(frames, frameRate)
    } catch (e: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
        }
    }
}

actual fun encodeMp4(frames: List<ImageBitmap>, frameRate: Int): ByteArray? {
    if (frames.isEmpty()) return null
    val sourceWidth = frames.first().width
    val sourceHeight = frames.first().height
    val width = (sourceWidth + 15) / 16 * 16
    val height = (sourceHeight + 15) / 16 * 16
    val fps = frameRate.coerceIn(1, 60)
    val frameDurationUs = 1_000_000L / fps
    val output = File.createTempFile("mobile-flasher", ".mp4")
    var codec: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    return try {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, (width * height * fps * 0.2).toInt().coerceIn(600_000, 16_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val info = MediaCodec.BufferInfo()
        var trackIndex = -1
        var nextFrame = 0
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(CodecTimeoutUs)
                if (inputIndex >= 0) {
                    if (nextFrame >= frames.size) {
                        codec.queueInputBuffer(
                            inputIndex, 0, 0, nextFrame * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        val image = codec.getInputImage(inputIndex) ?: return null
                        fillYuvImage(image, frames[nextFrame], width, height)
                        codec.queueInputBuffer(inputIndex, 0, width * height * 3 / 2, nextFrame * frameDurationUs, 0)
                        nextFrame++
                    }
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(info, CodecTimeoutUs)
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(codec.outputFormat)
                muxer.start()
                muxerStarted = true
            } else if (outputIndex >= 0) {
                val buffer = codec.getOutputBuffer(outputIndex)
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                if (buffer != null && info.size > 0 && muxerStarted) {
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    muxer.writeSampleData(trackIndex, buffer, info)
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        if (!muxerStarted) return null
        muxer.stop()
        output.readBytes()
    } catch (e: Exception) {
        null
    } finally {
        try {
            codec?.stop()
        } catch (e: Exception) {
        }
        try {
            codec?.release()
        } catch (e: Exception) {
        }
        try {
            muxer?.release()
        } catch (e: Exception) {
        }
        output.delete()
    }
}

private fun fillYuvImage(image: Image, frame: ImageBitmap, width: Int, height: Int) {
    val sourceWidth = frame.width
    val sourceHeight = frame.height
    val pixels = IntArray(sourceWidth * sourceHeight)
    frame.readPixels(pixels)
    fun pixelAt(x: Int, y: Int): Int = pixels[minOf(y, sourceHeight - 1) * sourceWidth + minOf(x, sourceWidth - 1)]

    val planes = image.planes
    val yBuffer = planes[0].buffer
    val yRowStride = planes[0].rowStride
    val yPixelStride = planes[0].pixelStride
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = pixelAt(x, y)
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val luma = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
            yBuffer.put(y * yRowStride + x * yPixelStride, luma.coerceIn(16, 235).toByte())
        }
    }

    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    for (blockY in 0 until height / 2) {
        for (blockX in 0 until width / 2) {
            var r = 0
            var g = 0
            var b = 0
            for (dy in 0..1) {
                for (dx in 0..1) {
                    val argb = pixelAt(blockX * 2 + dx, blockY * 2 + dy)
                    r += (argb shr 16) and 0xFF
                    g += (argb shr 8) and 0xFF
                    b += argb and 0xFF
                }
            }
            r /= 4
            g /= 4
            b /= 4
            val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
            val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
            uBuffer.put(blockY * planes[1].rowStride + blockX * planes[1].pixelStride, u.coerceIn(16, 240).toByte())
            vBuffer.put(blockY * planes[2].rowStride + blockX * planes[2].pixelStride, v.coerceIn(16, 240).toByte())
        }
    }
}
