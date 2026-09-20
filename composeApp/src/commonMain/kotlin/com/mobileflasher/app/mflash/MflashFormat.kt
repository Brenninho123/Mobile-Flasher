package com.mobileflasher.app.mflash

import com.mobileflasher.app.model.Project
import com.mobileflasher.app.xml.parseProjectXml
import com.mobileflasher.app.xml.writeProjectXml

class MflashDocument(
    val project: Project,
    val thumbnailPng: ByteArray?,
    val savedAtMillis: Long
)

object MflashFormat {

    const val Extension = "mflash"
    const val FormatVersion = 1
    const val MimeType = "application/x-mobile-flasher"

    private const val Magic = "MFLASH"
    private const val ManifestEntry = "manifest.txt"
    private const val ProjectEntry = "project.xml"
    private const val ThumbnailEntry = "thumbnail.png"
    private const val AssetPrefix = "assets/"

    private val CrcTable = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun isContainer(bytes: ByteArray): Boolean {
        if (bytes.size < Magic.length + 1) return false
        for (i in Magic.indices) if (bytes[i].toInt() != Magic[i].code) return false
        return true
    }

    fun encode(project: Project, thumbnailPng: ByteArray?, savedAtMillis: Long): ByteArray {
        val assets = LinkedHashMap<String, ByteArray>()
        val xml = writeProjectXml(project, assets).encodeToByteArray()
        val frames = project.layers.maxOfOrNull { it.frames.size } ?: 1
        val manifest = buildString {
            append("app=Mobile Flasher\n")
            append("format=$FormatVersion\n")
            append("name=${project.name.replace('\n', ' ').replace('\r', ' ')}\n")
            append("frameRate=${project.frameRate}\n")
            append("layers=${project.layers.size}\n")
            append("frames=$frames\n")
            append("savedAt=$savedAtMillis\n")
        }.encodeToByteArray()

        val out = Sink()
        for (i in Magic.indices) out.writeByte(Magic[i].code)
        out.writeByte(FormatVersion)
        val entryCount = 2 + assets.size + (if (thumbnailPng != null) 1 else 0)
        out.writeShort(entryCount)
        writeEntry(out, ManifestEntry, manifest)
        writeEntry(out, ProjectEntry, xml)
        if (thumbnailPng != null) writeEntry(out, ThumbnailEntry, thumbnailPng)
        for ((key, data) in assets) writeEntry(out, AssetPrefix + key, data)

        val body = out.toByteArray()
        val crc = crc32(body, body.size)
        val result = ByteArray(body.size + 4)
        body.copyInto(result)
        writeInt(result, body.size, crc)
        return result
    }

    fun decode(bytes: ByteArray): MflashDocument {
        require(isContainer(bytes)) { "Not a .mflash file" }
        require(bytes.size >= Magic.length + 1 + 2 + 4) { "The .mflash file is truncated" }
        val bodySize = bytes.size - 4
        require(crc32(bytes, bodySize) == readInt(bytes, bodySize)) { "The .mflash file is corrupted" }

        var position = Magic.length
        val version = bytes[position++].toInt() and 0xFF
        require(version in 1..FormatVersion) { "This .mflash file was made by a newer version of the app" }
        val entryCount = readShort(bytes, position)
        position += 2

        val entries = LinkedHashMap<String, ByteArray>()
        repeat(entryCount) {
            require(position < bodySize) { "The .mflash file is truncated" }
            val nameLength = bytes[position++].toInt() and 0xFF
            require(position + nameLength + 4 <= bodySize) { "The .mflash file is truncated" }
            val name = bytes.copyOfRange(position, position + nameLength).decodeToString()
            position += nameLength
            val dataLength = readInt(bytes, position)
            position += 4
            require(dataLength >= 0 && position + dataLength <= bodySize) { "The .mflash file is truncated" }
            entries[name] = bytes.copyOfRange(position, position + dataLength)
            position += dataLength
        }

        val xml = entries[ProjectEntry] ?: throw IllegalArgumentException("The .mflash file has no project data")
        val assets = entries.filterKeys { it.startsWith(AssetPrefix) }.mapKeys { it.key.removePrefix(AssetPrefix) }
        val project = parseProjectXml(xml.decodeToString(), assets)
        val savedAt = entries[ManifestEntry]?.decodeToString()?.lineSequence()
            ?.firstOrNull { it.startsWith("savedAt=") }?.removePrefix("savedAt=")?.toLongOrNull() ?: 0L
        return MflashDocument(project, entries[ThumbnailEntry], savedAt)
    }

    fun readProject(bytes: ByteArray): Project {
        if (isContainer(bytes)) return decode(bytes).project
        return parseProjectXml(bytes.decodeToString())
    }

    private fun writeEntry(out: Sink, name: String, data: ByteArray) {
        val nameBytes = name.encodeToByteArray()
        require(nameBytes.size <= 255) { "Entry name too long" }
        out.writeByte(nameBytes.size)
        out.write(nameBytes)
        out.writeInt(data.size)
        out.write(data)
    }

    private fun crc32(data: ByteArray, length: Int): Int {
        var crc = -1
        for (i in 0 until length) crc = CrcTable[(crc xor data[i].toInt()) and 0xFF] xor (crc ushr 8)
        return crc.inv()
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value ushr 24).toByte()
        target[offset + 1] = (value ushr 16).toByte()
        target[offset + 2] = (value ushr 8).toByte()
        target[offset + 3] = value.toByte()
    }

    private fun readInt(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xFF) shl 24) or
            ((source[offset + 1].toInt() and 0xFF) shl 16) or
            ((source[offset + 2].toInt() and 0xFF) shl 8) or
            (source[offset + 3].toInt() and 0xFF)

    private fun readShort(source: ByteArray, offset: Int): Int =
        ((source[offset].toInt() and 0xFF) shl 8) or (source[offset + 1].toInt() and 0xFF)

    private class Sink {
        private var buffer = ByteArray(4096)
        private var size = 0

        fun writeByte(value: Int) {
            ensure(1)
            buffer[size++] = value.toByte()
        }

        fun writeShort(value: Int) {
            writeByte(value shr 8)
            writeByte(value)
        }

        fun writeInt(value: Int) {
            writeByte(value ushr 24)
            writeByte(value ushr 16)
            writeByte(value ushr 8)
            writeByte(value)
        }

        fun write(data: ByteArray) {
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
