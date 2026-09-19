package com.mobileflasher.app.export

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap

class FramePixels(val width: Int, val height: Int, private val rgb: IntArray) {
    fun getRgb(x: Int, y: Int): Int = rgb[y * width + x]

    companion object {
        fun from(image: ImageBitmap): FramePixels {
            val pixelMap = image.toPixelMap()
            val data = IntArray(pixelMap.width * pixelMap.height)
            var i = 0
            for (y in 0 until pixelMap.height) {
                for (x in 0 until pixelMap.width) {
                    data[i] = pixelMap[x, y].toArgb() and 0x00FFFFFF
                    i++
                }
            }
            return FramePixels(pixelMap.width, pixelMap.height, data)
        }
    }
}

object GifEncoder {

    fun encode(frames: List<ImageBitmap>, frameDelayCentiseconds: Int, loop: Boolean = true): ByteArray {
        return encodePixelFrames(frames.map { FramePixels.from(it) }, frameDelayCentiseconds, loop)
    }

    fun encodePixelFrames(frames: List<FramePixels>, frameDelayCentiseconds: Int, loop: Boolean = true): ByteArray {
        require(frames.isNotEmpty()) { "Need at least one frame to encode a GIF" }
        val width = frames.first().width
        val height = frames.first().height

        val palette = buildPalette(frames)
        val paletteBits = bitsForSize(palette.size)
        val paletteSize = 1 shl paletteBits

        val writer = GifByteWriter()
        writer.writeAscii("GIF89a")
        writeLogicalScreenDescriptor(writer, width, height, paletteBits)
        writeColorTable(writer, palette, paletteSize)
        if (loop) writeLoopExtension(writer)

        for (frame in frames) {
            val indices = quantizeFrame(frame, palette)
            writeGraphicControlExtension(writer, frameDelayCentiseconds)
            writeImageDescriptor(writer, width, height)
            writeImageData(writer, indices, paletteBits)
        }

        writer.writeByte(0x3B)
        return writer.toByteArray()
    }

    private fun buildPalette(frames: List<FramePixels>): List<Int> {
        val counts = LinkedHashMap<Int, Int>()
        for (frame in frames) {
            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    val rgb = frame.getRgb(x, y)
                    counts[rgb] = (counts[rgb] ?: 0) + 1
                }
            }
        }
        val sorted = counts.entries.sortedByDescending { it.value }.map { it.key }
        return if (sorted.isEmpty()) listOf(0x000000) else sorted.take(256)
    }

    private fun bitsForSize(size: Int): Int {
        var bits = 1
        while ((1 shl bits) < size) bits++
        return bits
    }

    private fun writeLogicalScreenDescriptor(writer: GifByteWriter, width: Int, height: Int, paletteBits: Int) {
        writer.writeShortLE(width)
        writer.writeShortLE(height)
        val packed = 0x80 or ((paletteBits - 1) shl 4) or (paletteBits - 1)
        writer.writeByte(packed)
        writer.writeByte(0x00)
        writer.writeByte(0x00)
    }

    private fun writeColorTable(writer: GifByteWriter, palette: List<Int>, targetSize: Int) {
        for (i in 0 until targetSize) {
            val rgb = if (i < palette.size) palette[i] else 0x000000
            writer.writeByte((rgb shr 16) and 0xFF)
            writer.writeByte((rgb shr 8) and 0xFF)
            writer.writeByte(rgb and 0xFF)
        }
    }

    private fun writeLoopExtension(writer: GifByteWriter) {
        writer.writeByte(0x21)
        writer.writeByte(0xFF)
        writer.writeByte(0x0B)
        writer.writeAscii("NETSCAPE2.0")
        writer.writeByte(0x03)
        writer.writeByte(0x01)
        writer.writeShortLE(0)
        writer.writeByte(0x00)
    }

    private fun writeGraphicControlExtension(writer: GifByteWriter, delayCentiseconds: Int) {
        writer.writeByte(0x21)
        writer.writeByte(0xF9)
        writer.writeByte(0x04)
        writer.writeByte(0x00)
        writer.writeShortLE(delayCentiseconds)
        writer.writeByte(0x00)
        writer.writeByte(0x00)
    }

    private fun writeImageDescriptor(writer: GifByteWriter, width: Int, height: Int) {
        writer.writeByte(0x2C)
        writer.writeShortLE(0)
        writer.writeShortLE(0)
        writer.writeShortLE(width)
        writer.writeShortLE(height)
        writer.writeByte(0x00)
    }

    private fun writeImageData(writer: GifByteWriter, indices: IntArray, paletteBits: Int) {
        val minCodeSize = maxOf(2, paletteBits)
        writer.writeByte(minCodeSize)
        val compressed = lzwEncode(indices, minCodeSize)
        writer.writeSubBlocks(compressed)
    }

    private fun quantizeFrame(frame: FramePixels, palette: List<Int>): IntArray {
        val result = IntArray(frame.width * frame.height)
        val cache = HashMap<Int, Int>()
        var i = 0
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val rgb = frame.getRgb(x, y)
                result[i] = cache.getOrPut(rgb) { nearestPaletteIndex(rgb, palette) }
                i++
            }
        }
        return result
    }

    private fun nearestPaletteIndex(rgb: Int, palette: List<Int>): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        for (index in palette.indices) {
            val color = palette[index]
            val dr = r - ((color shr 16) and 0xFF)
            val dg = g - ((color shr 8) and 0xFF)
            val db = b - (color and 0xFF)
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
                if (distance == 0) break
            }
        }
        return bestIndex
    }

    private fun lzwEncode(indices: IntArray, minCodeSize: Int): ByteArray {
        val clearCode = 1 shl minCodeSize
        val endCode = clearCode + 1
        val bitWriter = BitWriter()

        var dict = HashMap<Int, Int>()
        var nextCode = endCode + 1
        var codeSize = minCodeSize + 1

        fun resetDict() {
            dict = HashMap()
            nextCode = endCode + 1
            codeSize = minCodeSize + 1
        }

        bitWriter.writeBits(clearCode, codeSize)

        if (indices.isEmpty()) {
            bitWriter.writeBits(endCode, codeSize)
            bitWriter.flush()
            return bitWriter.toByteArray()
        }

        var prefixCode = indices[0]
        for (i in 1 until indices.size) {
            val k = indices[i]
            val key = prefixCode * 4096 + k
            val existing = dict[key]
            if (existing != null) {
                prefixCode = existing
            } else {
                bitWriter.writeBits(prefixCode, codeSize)
                if (nextCode < 4096) {
                    dict[key] = nextCode
                    nextCode++
                    if (nextCode > (1 shl codeSize) && codeSize < 12) {
                        codeSize++
                    }
                } else {
                    bitWriter.writeBits(clearCode, codeSize)
                    resetDict()
                }
                prefixCode = k
            }
        }
        bitWriter.writeBits(prefixCode, codeSize)
        bitWriter.writeBits(endCode, codeSize)
        bitWriter.flush()
        return bitWriter.toByteArray()
    }
}

private class BitWriter {
    private val bytes = mutableListOf<Byte>()
    private var currentByte = 0
    private var bitCount = 0

    fun writeBits(value: Int, numBits: Int) {
        var remainingValue = value
        var remainingBits = numBits
        while (remainingBits > 0) {
            currentByte = currentByte or ((remainingValue and 1) shl bitCount)
            remainingValue = remainingValue shr 1
            bitCount++
            remainingBits--
            if (bitCount == 8) {
                bytes.add(currentByte.toByte())
                currentByte = 0
                bitCount = 0
            }
        }
    }

    fun flush() {
        if (bitCount > 0) {
            bytes.add(currentByte.toByte())
            currentByte = 0
            bitCount = 0
        }
    }

    fun toByteArray(): ByteArray = bytes.toByteArray()
}

private class GifByteWriter {
    private val buffer = mutableListOf<Byte>()

    fun writeByte(value: Int) {
        buffer.add(value.toByte())
    }

    fun writeBytes(data: ByteArray) {
        for (b in data) buffer.add(b)
    }

    fun writeAscii(text: String) {
        for (c in text) buffer.add(c.code.toByte())
    }

    fun writeShortLE(value: Int) {
        buffer.add((value and 0xFF).toByte())
        buffer.add(((value shr 8) and 0xFF).toByte())
    }

    fun writeSubBlocks(data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(255, data.size - offset)
            writeByte(chunkSize)
            writeBytes(data.copyOfRange(offset, offset + chunkSize))
            offset += chunkSize
        }
        writeByte(0x00)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
