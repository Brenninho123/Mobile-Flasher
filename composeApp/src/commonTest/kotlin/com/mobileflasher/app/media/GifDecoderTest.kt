package com.mobileflasher.app.media

import com.mobileflasher.app.export.FramePixels
import com.mobileflasher.app.export.GifEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GifDecoderTest {

    private fun opaque(rgb: Int) = 0xFF000000.toInt() or rgb

    @Test
    fun detectsGifSignature() {
        assertTrue(GifDecoder.isGif("GIF89a".encodeToByteArray() + ByteArray(4)))
        assertFalse(GifDecoder.isGif(ByteArray(10)))
        assertFalse(GifDecoder.isGif(ByteArray(2)))
    }

    @Test
    fun decodesSingleFrame() {
        val pixels = intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF)
        val bytes = GifEncoder.encodePixelFrames(listOf(FramePixels(2, 2, pixels)), frameDelayCentiseconds = 10)

        val gif = GifDecoder.decode(bytes)

        assertEquals(2, gif.width)
        assertEquals(2, gif.height)
        assertEquals(1, gif.frames.size)
        assertContentEquals(pixels.map(::opaque).toIntArray(), gif.frames[0].argb)
        assertEquals(100, gif.frames[0].delayMillis)
    }

    @Test
    fun decodesMultipleFramesInOrder() {
        val red = FramePixels(3, 2, IntArray(6) { 0xFF0000 })
        val blue = FramePixels(3, 2, IntArray(6) { 0x0000FF })
        val bytes = GifEncoder.encodePixelFrames(listOf(red, blue, red), frameDelayCentiseconds = 5)

        val gif = GifDecoder.decode(bytes)

        assertEquals(3, gif.frames.size)
        assertEquals(opaque(0xFF0000), gif.frames[0].argb[0])
        assertEquals(opaque(0x0000FF), gif.frames[1].argb[5])
        assertEquals(opaque(0xFF0000), gif.frames[2].argb[3])
        assertEquals(50, gif.frames[0].delayMillis)
    }

    @Test
    fun decodesLargeFrameThatGrowsTheLzwDictionary() {
        val width = 64
        val height = 64
        val palette = intArrayOf(0x102030, 0xA0B0C0, 0xFF8000, 0x00FF80)
        var seed = 12345
        val pixels = IntArray(width * height) {
            seed = seed * 1103515245 + 12345
            palette[(seed ushr 16) and 3]
        }
        val bytes = GifEncoder.encodePixelFrames(listOf(FramePixels(width, height, pixels)), frameDelayCentiseconds = 10)

        val gif = GifDecoder.decode(bytes)

        assertContentEquals(pixels.map(::opaque).toIntArray(), gif.frames[0].argb)
    }

    @Test
    fun honoursTransparencyAndGraphicControlDelay() {
        val bytes = byteArrayOf(
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
            0x02, 0x00, 0x01, 0x00,
            0x80.toByte(), 0x00, 0x00,
            0xFF.toByte(), 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00,
            0x21, 0xF9.toByte(), 0x04, 0x01, 0x07, 0x00, 0x01, 0x00,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0x00, 0x00,
            0x02, 0x02, 0x44, 0x0A, 0x00,
            0x3B
        )

        val gif = GifDecoder.decode(bytes)

        assertEquals(1, gif.frames.size)
        assertEquals(70, gif.frames[0].delayMillis)
        assertEquals(opaque(0xFF0000), gif.frames[0].argb[0])
        assertEquals(0, gif.frames[0].argb[1])
    }

    @Test
    fun rejectsNonGifData() {
        assertFailsWith<IllegalArgumentException> { GifDecoder.decode(ByteArray(32)) }
    }
}
