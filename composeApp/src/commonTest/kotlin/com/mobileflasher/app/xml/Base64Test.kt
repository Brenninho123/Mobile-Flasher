package com.mobileflasher.app.xml

import kotlin.test.Test
import kotlin.test.assertEquals

class Base64Test {

    @Test
    fun roundTripsArbitraryBytes() {
        val original = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -128, 127, 42)
        val encoded = base64Encode(original)
        val decoded = base64Decode(encoded)
        assertEquals(original.toList(), decoded.toList())
    }

    @Test
    fun matchesKnownVector() {
        val text = "Mobile Flasher"
        val bytes = text.map { it.code.toByte() }.toByteArray()
        assertEquals("TW9iaWxlIEZsYXNoZXI=", base64Encode(bytes))
        assertEquals(bytes.toList(), base64Decode("TW9iaWxlIEZsYXNoZXI=").toList())
    }

    @Test
    fun roundTripsEmptyInput() {
        assertEquals(0, base64Decode(base64Encode(ByteArray(0))).size)
    }
}
