package com.mobileflasher.app.media

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PngEncoderTest {

    private fun roundTrip(width: Int, height: Int, argb: IntArray) {
        val png = PngEncoder.encode(width, height, argb)
        val image = ImageIO.read(ByteArrayInputStream(png))
        assertNotNull(image)
        assertEquals(width, image.width)
        assertEquals(height, image.height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                assertEquals(argb[y * width + x], image.getRGB(x, y), "pixel $x,$y")
            }
        }
    }

    @Test
    fun encodesSmallOpaqueImage() {
        roundTrip(2, 2, intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt()))
    }

    @Test
    fun encodesTransparency() {
        roundTrip(3, 1, intArrayOf(0x00000000, 0x80FF8000.toInt(), 0xFF102030.toInt()))
    }

    @Test
    fun encodesNoisyImageWithoutLoss() {
        val width = 97
        val height = 61
        var seed = 99
        val pixels = IntArray(width * height) {
            seed = seed * 1664525 + 1013904223
            seed or 0xFF000000.toInt()
        }
        roundTrip(width, height, pixels)
    }

    @Test
    fun compressesFlatImages() {
        val width = 320
        val height = 240
        val pixels = IntArray(width * height) { 0xFF336699.toInt() }
        val png = PngEncoder.encode(width, height, pixels)
        assertTrue(png.size < width * height / 4, "flat image should compress well, got ${png.size} bytes")
        roundTrip(width, height, pixels)
    }
}
