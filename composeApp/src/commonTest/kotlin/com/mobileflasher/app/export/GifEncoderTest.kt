package com.mobileflasher.app.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GifEncoderTest {

    @Test
    fun producesValidHeaderAndTrailer() {
        val frame = FramePixels(2, 2, intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF))
        val bytes = GifEncoder.encodePixelFrames(listOf(frame), frameDelayCentiseconds = 10)

        val header = bytes.copyOfRange(0, 6).map { it.toInt().toChar() }.joinToString("")
        assertEquals("GIF89a", header)
        assertEquals(0x3B, bytes.last().toInt() and 0xFF)
        assertTrue(bytes.size > 20)
    }

    @Test
    fun encodesMultipleFramesWithLoopExtension() {
        val frameA = FramePixels(2, 2, intArrayOf(0xFF0000, 0xFF0000, 0xFF0000, 0xFF0000))
        val frameB = FramePixels(2, 2, intArrayOf(0x0000FF, 0x0000FF, 0x0000FF, 0x0000FF))
        val bytes = GifEncoder.encodePixelFrames(listOf(frameA, frameB), frameDelayCentiseconds = 5, loop = true)

        val netscapeBlock = bytes.toList().windowed(11).any { window ->
            window.map { it.toInt().toChar() }.joinToString("") == "NETSCAPE2.0"
        }
        assertTrue(netscapeBlock, "Expected NETSCAPE2.0 loop extension block")

        val imageDescriptorCount = bytes.count { it.toInt() and 0xFF == 0x2C }
        assertTrue(imageDescriptorCount >= 2, "Expected at least one image descriptor per frame")
    }

    @Test
    fun encodesSingleColorFrameWithMinimalPalette() {
        val frame = FramePixels(3, 3, IntArray(9) { 0x123456 })
        val bytes = GifEncoder.encodePixelFrames(listOf(frame), frameDelayCentiseconds = 10)
        assertEquals("GIF89a", bytes.copyOfRange(0, 6).map { it.toInt().toChar() }.joinToString(""))
    }
}
