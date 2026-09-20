package com.mobileflasher.app.zip

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZipArchiveTest {

    private fun deflate(data: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level, true)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }

    private fun sample(): ByteArray {
        val words = listOf("<DOMShape>", "edges", "fillStyle1", "SolidColor", "#FF00AA", "0123456789")
        val random = Random(7)
        val text = StringBuilder()
        repeat(4000) { text.append(words[random.nextInt(words.size)]).append(' ') }
        return text.toString().encodeToByteArray()
    }

    @Test
    fun inflatesEveryCompressionLevel() {
        val data = sample()
        for (level in intArrayOf(Deflater.NO_COMPRESSION, Deflater.BEST_SPEED, Deflater.DEFAULT_COMPRESSION, Deflater.BEST_COMPRESSION)) {
            val packed = deflate(data, level)
            assertContentEquals(data, Inflate.inflate(packed, 0, packed.size, data.size), "level $level")
        }
    }

    @Test
    fun inflatesIncompressibleData() {
        val data = Random(3).nextBytes(20_000)
        val packed = deflate(data, Deflater.DEFAULT_COMPRESSION)
        assertContentEquals(data, Inflate.inflate(packed, 0, packed.size, data.size))
    }

    @Test
    fun readsArchivesWrittenByTheJdk() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("DOMDocument.xml"))
            zip.write(sample())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("LIBRARY/"))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("LIBRARY/Symbol 1.xml"))
            zip.write("<DOMSymbolItem/>".encodeToByteArray())
            zip.closeEntry()
        }
        val entries = ZipArchive.read(out.toByteArray())

        assertEquals(listOf("DOMDocument.xml", "LIBRARY/Symbol 1.xml"), entries.keys.toList())
        assertContentEquals(sample(), entries.getValue("DOMDocument.xml"))
        assertEquals("<DOMSymbolItem/>", entries.getValue("LIBRARY/Symbol 1.xml").decodeToString())
    }

    @Test
    fun archivesWrittenByUsAreReadableByTheJdk() {
        val bytes = ZipArchive.write(linkedMapOf("a.txt" to "hello".encodeToByteArray(), "dir/b.bin" to byteArrayOf(1, 2, 3)))
        val seen = LinkedHashMap<String, ByteArray>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                seen[entry.name] = zip.readBytes()
            }
        }
        assertEquals("hello", seen.getValue("a.txt").decodeToString())
        assertContentEquals(byteArrayOf(1, 2, 3), seen.getValue("dir/b.bin"))
    }

    @Test
    fun rejectsCorruptData() {
        assertFailsWith<IllegalArgumentException> { ZipArchive.read(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0)) }
        val packed = deflate(sample(), Deflater.DEFAULT_COMPRESSION)
        assertFailsWith<IllegalArgumentException> { Inflate.inflate(packed, 0, packed.size / 2, 1_000_000) }
        assertFailsWith<IllegalArgumentException> { Inflate.inflate(packed, 0, packed.size, 100) }
    }
}
