package com.mobileflasher.app.media

class GifFrameImage(val argb: IntArray, val delayMillis: Int)

class DecodedGif(val width: Int, val height: Int, val frames: List<GifFrameImage>)

object GifDecoder {

    private const val MaxPixels = 4096 * 4096
    private const val DefaultDelayMillis = 100

    fun isGif(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        val header = String(CharArray(6) { (bytes[it].toInt() and 0xFF).toChar() })
        return header == "GIF87a" || header == "GIF89a"
    }

    fun decode(bytes: ByteArray, maxFrames: Int = 200): DecodedGif {
        require(isGif(bytes)) { "Not a GIF file" }
        val reader = ByteReader(bytes)
        reader.position = 6
        val width = reader.readShortLE()
        val height = reader.readShortLE()
        require(width > 0 && height > 0 && width.toLong() * height <= MaxPixels) { "Unsupported GIF size" }
        val packed = reader.readByte()
        reader.readByte()
        reader.readByte()
        val globalTable = if (packed and 0x80 != 0) reader.readColorTable(1 shl ((packed and 0x07) + 1)) else null

        val canvas = IntArray(width * height)
        val frames = ArrayList<GifFrameImage>()

        var delayMillis = DefaultDelayMillis
        var transparentIndex = -1
        var disposal = 0
        var previousDisposal = 0
        var previousRect = IntArray(4)
        var backup: IntArray? = null

        while (reader.hasMore() && frames.size < maxFrames) {
            when (val block = reader.readByte()) {
                0x21 -> {
                    val label = reader.readByte()
                    if (label == 0xF9) {
                        reader.readByte()
                        val flags = reader.readByte()
                        val delay = reader.readShortLE() * 10
                        val index = reader.readByte()
                        reader.readByte()
                        disposal = (flags shr 2) and 0x07
                        transparentIndex = if (flags and 0x01 != 0) index else -1
                        delayMillis = if (delay < 20) DefaultDelayMillis else delay
                    } else {
                        reader.skipSubBlocks()
                    }
                }
                0x2C -> {
                    val left = reader.readShortLE()
                    val top = reader.readShortLE()
                    val frameWidth = reader.readShortLE()
                    val frameHeight = reader.readShortLE()
                    val flags = reader.readByte()
                    val interlaced = flags and 0x40 != 0
                    val localTable = if (flags and 0x80 != 0) reader.readColorTable(1 shl ((flags and 0x07) + 1)) else null
                    val table = localTable ?: globalTable ?: IntArray(256)
                    val minCodeSize = reader.readByte()
                    require(minCodeSize in 1..11) { "Corrupt GIF image data" }
                    val compressed = reader.readSubBlocks()
                    val indices = lzwDecode(compressed, minCodeSize, frameWidth * frameHeight)

                    when (previousDisposal) {
                        2 -> clearRect(canvas, width, height, previousRect)
                        3 -> backup?.let { it.copyInto(canvas) }
                    }
                    backup = if (disposal == 3) canvas.copyOf() else null

                    val rows = rowOrder(frameHeight, interlaced)
                    for (row in 0 until frameHeight) {
                        val targetY = top + rows[row]
                        if (targetY !in 0 until height) continue
                        for (column in 0 until frameWidth) {
                            val targetX = left + column
                            if (targetX !in 0 until width) continue
                            val index = indices[row * frameWidth + column].toInt() and 0xFF
                            if (index == transparentIndex) continue
                            canvas[targetY * width + targetX] = 0xFF000000.toInt() or table[index.coerceAtMost(table.size - 1)]
                        }
                    }
                    frames.add(GifFrameImage(canvas.copyOf(), delayMillis))

                    previousDisposal = disposal
                    previousRect = intArrayOf(left, top, frameWidth, frameHeight)
                    delayMillis = DefaultDelayMillis
                    transparentIndex = -1
                    disposal = 0
                }
                0x3B -> break
                else -> require(block == 0x00) { "Corrupt GIF block" }
            }
        }
        require(frames.isNotEmpty()) { "GIF contains no frames" }
        return DecodedGif(width, height, frames)
    }

    private fun clearRect(canvas: IntArray, width: Int, height: Int, rect: IntArray) {
        for (y in rect[1] until rect[1] + rect[3]) {
            if (y !in 0 until height) continue
            for (x in rect[0] until rect[0] + rect[2]) {
                if (x in 0 until width) canvas[y * width + x] = 0
            }
        }
    }

    private fun rowOrder(height: Int, interlaced: Boolean): IntArray {
        if (!interlaced) return IntArray(height) { it }
        val order = IntArray(height)
        var next = 0
        val starts = intArrayOf(0, 4, 2, 1)
        val steps = intArrayOf(8, 8, 4, 2)
        for (pass in 0..3) {
            var y = starts[pass]
            while (y < height) {
                order[next++] = y
                y += steps[pass]
            }
        }
        return order
    }

    private fun lzwDecode(data: ByteArray, minCodeSize: Int, pixelCount: Int): ByteArray {
        val clearCode = 1 shl minCodeSize
        val endCode = clearCode + 1
        val prefix = IntArray(4096)
        val suffix = ByteArray(4096)
        val stack = ByteArray(4097)
        for (i in 0 until clearCode) suffix[i] = i.toByte()

        val output = ByteArray(pixelCount)
        var outputPosition = 0
        var codeSize = minCodeSize + 1
        var codeMask = (1 shl codeSize) - 1
        var available = clearCode + 2
        var oldCode = -1
        var first = 0

        var bitBuffer = 0
        var bitCount = 0
        var inputPosition = 0

        while (outputPosition < pixelCount) {
            while (bitCount < codeSize) {
                if (inputPosition >= data.size) return output
                bitBuffer = bitBuffer or ((data[inputPosition++].toInt() and 0xFF) shl bitCount)
                bitCount += 8
            }
            var code = bitBuffer and codeMask
            bitBuffer = bitBuffer shr codeSize
            bitCount -= codeSize

            if (code == clearCode) {
                codeSize = minCodeSize + 1
                codeMask = (1 shl codeSize) - 1
                available = clearCode + 2
                oldCode = -1
                continue
            }
            if (code == endCode) break
            if (oldCode == -1) {
                if (code >= clearCode) return output
                output[outputPosition++] = suffix[code]
                oldCode = code
                first = code
                continue
            }

            val inCode = code
            var top = 0
            if (code >= available) {
                if (code > available) return output
                stack[top++] = first.toByte()
                code = oldCode
            }
            while (code >= clearCode) {
                stack[top++] = suffix[code]
                code = prefix[code]
            }
            first = suffix[code].toInt() and 0xFF
            stack[top++] = first.toByte()

            if (available < 4096) {
                prefix[available] = oldCode
                suffix[available] = first.toByte()
                available++
                if (available and codeMask == 0 && available < 4096) {
                    codeSize++
                    codeMask = (1 shl codeSize) - 1
                }
            }
            oldCode = inCode

            while (top > 0 && outputPosition < pixelCount) {
                output[outputPosition++] = stack[--top]
            }
        }
        return output
    }

    private class ByteReader(private val data: ByteArray) {
        var position = 0

        fun hasMore() = position < data.size

        fun readByte(): Int {
            require(position < data.size) { "Unexpected end of GIF data" }
            return data[position++].toInt() and 0xFF
        }

        fun readShortLE(): Int {
            val low = readByte()
            return low or (readByte() shl 8)
        }

        fun readColorTable(size: Int): IntArray {
            val table = IntArray(size)
            for (i in 0 until size) {
                val r = readByte()
                val g = readByte()
                val b = readByte()
                table[i] = (r shl 16) or (g shl 8) or b
            }
            return table
        }

        fun skipSubBlocks() {
            while (true) {
                val size = readByte()
                if (size == 0) return
                position += size
            }
        }

        fun readSubBlocks(): ByteArray {
            val out = ArrayList<Byte>()
            while (true) {
                val size = readByte()
                if (size == 0) break
                require(position + size <= data.size) { "Unexpected end of GIF data" }
                for (i in 0 until size) out.add(data[position + i])
                position += size
            }
            return out.toByteArray()
        }
    }
}
