package com.mobileflasher.app.zip

object ZipArchive {

    private const val LocalHeaderSignature = 0x04034B50
    private const val CentralHeaderSignature = 0x02014B50
    private const val EndSignature = 0x06054B50
    private const val MaxEntryBytes = 96 * 1024 * 1024
    private const val MaxEntries = 20_000

    private val CrcTable = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte())

    fun crc32(data: ByteArray): Int {
        var crc = -1
        for (b in data) crc = CrcTable[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        return crc.inv()
    }

    fun read(bytes: ByteArray): Map<String, ByteArray> {
        val endOffset = findEndOfCentralDirectory(bytes)
        val entryCount = readShort(bytes, endOffset + 10)
        val directoryOffset = readInt(bytes, endOffset + 16)
        require(entryCount <= MaxEntries) { "Too many entries in the archive" }
        require(directoryOffset in 0 until bytes.size) { "Corrupt archive directory" }

        val entries = LinkedHashMap<String, ByteArray>()
        var position = directoryOffset
        repeat(entryCount) {
            require(position + 46 <= bytes.size && readInt(bytes, position) == CentralHeaderSignature) { "Corrupt archive directory" }
            val method = readShort(bytes, position + 10)
            val compressedSize = readInt(bytes, position + 20)
            val uncompressedSize = readInt(bytes, position + 24)
            val nameLength = readShort(bytes, position + 28)
            val extraLength = readShort(bytes, position + 30)
            val commentLength = readShort(bytes, position + 32)
            val localOffset = readInt(bytes, position + 42)
            require(position + 46 + nameLength <= bytes.size) { "Corrupt archive directory" }
            val name = bytes.copyOfRange(position + 46, position + 46 + nameLength).decodeToString()
            position += 46 + nameLength + extraLength + commentLength

            if (name.endsWith("/")) return@repeat
            require(compressedSize >= 0 && uncompressedSize in 0..MaxEntryBytes) { "Unsupported archive entry" }
            require(localOffset in 0 until bytes.size - 30 && readInt(bytes, localOffset) == LocalHeaderSignature) { "Corrupt archive entry" }
            val dataStart = localOffset + 30 + readShort(bytes, localOffset + 26) + readShort(bytes, localOffset + 28)
            require(dataStart + compressedSize <= bytes.size) { "Corrupt archive entry" }
            entries[name] = when (method) {
                0 -> bytes.copyOfRange(dataStart, dataStart + compressedSize)
                8 -> Inflate.inflate(bytes, dataStart, dataStart + compressedSize, maxOf(uncompressedSize, 1))
                else -> throw IllegalArgumentException("Unsupported compression method $method")
            }
        }
        return entries
    }

    fun write(entries: Map<String, ByteArray>): ByteArray {
        val out = Sink()
        val directory = Sink()
        for ((name, data) in entries) {
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            val offset = out.size
            out.int(LocalHeaderSignature)
            out.short(20)
            out.short(0x0800)
            out.short(0)
            out.short(0)
            out.short(0x21)
            out.int(crc)
            out.int(data.size)
            out.int(data.size)
            out.short(nameBytes.size)
            out.short(0)
            out.bytes(nameBytes)
            out.bytes(data)

            directory.int(CentralHeaderSignature)
            directory.short(20)
            directory.short(20)
            directory.short(0x0800)
            directory.short(0)
            directory.short(0)
            directory.short(0x21)
            directory.int(crc)
            directory.int(data.size)
            directory.int(data.size)
            directory.short(nameBytes.size)
            directory.short(0)
            directory.short(0)
            directory.short(0)
            directory.short(0)
            directory.int(0)
            directory.int(offset)
            directory.bytes(nameBytes)
        }
        val directoryOffset = out.size
        val directoryBytes = directory.toByteArray()
        out.bytes(directoryBytes)
        out.int(EndSignature)
        out.short(0)
        out.short(0)
        out.short(entries.size)
        out.short(entries.size)
        out.int(directoryBytes.size)
        out.int(directoryOffset)
        out.short(0)
        return out.toByteArray()
    }

    private fun findEndOfCentralDirectory(bytes: ByteArray): Int {
        val lowest = maxOf(0, bytes.size - 22 - 0xFFFF)
        var position = bytes.size - 22
        while (position >= lowest) {
            if (readInt(bytes, position) == EndSignature) return position
            position--
        }
        throw IllegalArgumentException("Not a zip archive")
    }

    private fun readShort(source: ByteArray, offset: Int): Int =
        (source[offset].toInt() and 0xFF) or ((source[offset + 1].toInt() and 0xFF) shl 8)

    private fun readInt(source: ByteArray, offset: Int): Int =
        (source[offset].toInt() and 0xFF) or
            ((source[offset + 1].toInt() and 0xFF) shl 8) or
            ((source[offset + 2].toInt() and 0xFF) shl 16) or
            ((source[offset + 3].toInt() and 0xFF) shl 24)

    private class Sink {
        private var buffer = ByteArray(4096)
        var size = 0
            private set

        fun short(value: Int) {
            ensure(2)
            buffer[size++] = value.toByte()
            buffer[size++] = (value ushr 8).toByte()
        }

        fun int(value: Int) {
            ensure(4)
            buffer[size++] = value.toByte()
            buffer[size++] = (value ushr 8).toByte()
            buffer[size++] = (value ushr 16).toByte()
            buffer[size++] = (value ushr 24).toByte()
        }

        fun bytes(data: ByteArray) {
            ensure(data.size)
            data.copyInto(buffer, size)
            size += data.size
        }

        private fun ensure(extra: Int) {
            if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}
