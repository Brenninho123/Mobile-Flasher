package com.mobileflasher.app.media

object PngEncoder {

    private val Signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    private val CrcTable = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun encode(width: Int, height: Int, argb: IntArray): ByteArray {
        require(width > 0 && height > 0 && argb.size >= width * height) { "Invalid pixel buffer" }
        val stride = width * 4
        val raw = ByteArray((stride + 1) * height)
        for (y in 0 until height) {
            val rowStart = y * (stride + 1)
            raw[rowStart] = 2
            for (x in 0 until width) {
                val pixel = argb[y * width + x]
                val above = if (y > 0) argb[(y - 1) * width + x] else 0
                val offset = rowStart + 1 + x * 4
                raw[offset] = (((pixel shr 16) and 0xFF) - (if (y > 0) (above shr 16) and 0xFF else 0)).toByte()
                raw[offset + 1] = (((pixel shr 8) and 0xFF) - (if (y > 0) (above shr 8) and 0xFF else 0)).toByte()
                raw[offset + 2] = ((pixel and 0xFF) - (if (y > 0) above and 0xFF else 0)).toByte()
                raw[offset + 3] = (((pixel ushr 24) and 0xFF) - (if (y > 0) (above ushr 24) and 0xFF else 0)).toByte()
            }
        }

        val header = ByteArray(13)
        writeInt(header, 0, width)
        writeInt(header, 4, height)
        header[8] = 8
        header[9] = 6

        val out = ByteSink()
        out.write(Signature)
        writeChunk(out, "IHDR", header)
        writeChunk(out, "IDAT", zlibCompress(raw))
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun writeChunk(out: ByteSink, type: String, data: ByteArray) {
        val lengthBytes = ByteArray(4)
        writeInt(lengthBytes, 0, data.size)
        out.write(lengthBytes)
        val typeBytes = ByteArray(4) { type[it].code.toByte() }
        out.write(typeBytes)
        out.write(data)
        var crc = -1
        for (b in typeBytes) crc = CrcTable[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        for (b in data) crc = CrcTable[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        val crcBytes = ByteArray(4)
        writeInt(crcBytes, 0, crc.inv())
        out.write(crcBytes)
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun zlibCompress(data: ByteArray): ByteArray {
        val out = ByteSink()
        out.writeByte(0x78)
        out.writeByte(0x01)
        out.write(Deflate.compress(data))
        var a = 1
        var b = 0
        for (byte in data) {
            a = (a + (byte.toInt() and 0xFF)) % 65521
            b = (b + a) % 65521
        }
        val checksum = ByteArray(4)
        writeInt(checksum, 0, (b shl 16) or a)
        out.write(checksum)
        return out.toByteArray()
    }
}

internal object Deflate {

    private val LengthBase = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
    private val LengthExtra = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
    private val DistanceBase = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
    private val DistanceExtra = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)

    private const val WindowSize = 32768
    private const val HashBits = 15
    private const val MaxChain = 24

    fun compress(data: ByteArray): ByteArray {
        val writer = BitSink()
        writer.writeBits(1, 1)
        writer.writeBits(1, 2)

        val size = data.size
        val head = IntArray(1 shl HashBits) { -1 }
        val previous = IntArray(size.coerceAtLeast(1))

        fun hash(position: Int): Int {
            val value = ((data[position].toInt() and 0xFF) shl 16) or
                ((data[position + 1].toInt() and 0xFF) shl 8) or
                (data[position + 2].toInt() and 0xFF)
            return (value * 2654435761L ushr 17).toInt() and ((1 shl HashBits) - 1)
        }

        fun insert(position: Int) {
            if (position + 2 < size) {
                val h = hash(position)
                previous[position] = head[h]
                head[h] = position
            }
        }

        var position = 0
        while (position < size) {
            var bestLength = 0
            var bestDistance = 0
            if (position + 2 < size) {
                var candidate = head[hash(position)]
                var chain = 0
                val maxLength = minOf(258, size - position)
                while (candidate >= 0 && position - candidate <= WindowSize && chain < MaxChain) {
                    var length = 0
                    while (length < maxLength && data[candidate + length] == data[position + length]) length++
                    if (length > bestLength) {
                        bestLength = length
                        bestDistance = position - candidate
                        if (length == maxLength) break
                    }
                    candidate = previous[candidate]
                    chain++
                }
            }
            if (bestLength >= 3) {
                writeLength(writer, bestLength)
                writeDistance(writer, bestDistance)
                for (offset in 0 until bestLength) insert(position + offset)
                position += bestLength
            } else {
                writeLiteral(writer, data[position].toInt() and 0xFF)
                insert(position)
                position++
            }
        }
        writeSymbol(writer, 256)
        writer.flush()
        return writer.toByteArray()
    }

    private fun writeLiteral(writer: BitSink, value: Int) = writeSymbol(writer, value)

    private fun writeSymbol(writer: BitSink, symbol: Int) {
        when {
            symbol <= 143 -> writer.writeHuffman(0x30 + symbol, 8)
            symbol <= 255 -> writer.writeHuffman(0x190 + symbol - 144, 9)
            symbol <= 279 -> writer.writeHuffman(symbol - 256, 7)
            else -> writer.writeHuffman(0xC0 + symbol - 280, 8)
        }
    }

    private fun writeLength(writer: BitSink, length: Int) {
        var index = LengthBase.size - 1
        while (LengthBase[index] > length) index--
        writeSymbol(writer, 257 + index)
        if (LengthExtra[index] > 0) writer.writeBits(length - LengthBase[index], LengthExtra[index])
    }

    private fun writeDistance(writer: BitSink, distance: Int) {
        var index = DistanceBase.size - 1
        while (DistanceBase[index] > distance) index--
        writer.writeHuffman(index, 5)
        if (DistanceExtra[index] > 0) writer.writeBits(distance - DistanceBase[index], DistanceExtra[index])
    }
}

private class BitSink {
    private val sink = ByteSink()
    private var current = 0
    private var count = 0

    fun writeBits(value: Int, bits: Int) {
        for (i in 0 until bits) writeBit((value shr i) and 1)
    }

    fun writeHuffman(code: Int, bits: Int) {
        for (i in bits - 1 downTo 0) writeBit((code shr i) and 1)
    }

    private fun writeBit(bit: Int) {
        current = current or (bit shl count)
        count++
        if (count == 8) {
            sink.writeByte(current)
            current = 0
            count = 0
        }
    }

    fun flush() {
        if (count > 0) {
            sink.writeByte(current)
            current = 0
            count = 0
        }
    }

    fun toByteArray(): ByteArray = sink.toByteArray()
}

private class ByteSink {
    private var buffer = ByteArray(1024)
    private var size = 0

    fun writeByte(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun write(data: ByteArray) {
        ensure(data.size)
        data.copyInto(buffer, size)
        size += data.size
    }

    private fun ensure(extra: Int) {
        if (size + extra > buffer.size) {
            buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}
