package com.mobileflasher.app.xml

private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

fun base64Encode(bytes: ByteArray): String {
    val builder = StringBuilder()
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
        val triple = (b0 shl 16) or (b1 shl 8) or b2
        builder.append(ALPHABET[(triple shr 18) and 0x3F])
        builder.append(ALPHABET[(triple shr 12) and 0x3F])
        builder.append(if (i + 1 < bytes.size) ALPHABET[(triple shr 6) and 0x3F] else '=')
        builder.append(if (i + 2 < bytes.size) ALPHABET[triple and 0x3F] else '=')
        i += 3
    }
    return builder.toString()
}

fun base64Decode(text: String): ByteArray {
    val clean = text.filterNot { it == '\n' || it == '\r' || it == ' ' || it == '\t' }
    val output = ArrayList<Byte>(clean.length / 4 * 3)
    var i = 0
    while (i + 3 < clean.length) {
        val c0 = ALPHABET.indexOf(clean[i])
        val c1 = ALPHABET.indexOf(clean[i + 1])
        val c2 = if (clean[i + 2] != '=') ALPHABET.indexOf(clean[i + 2]) else -1
        val c3 = if (clean[i + 3] != '=') ALPHABET.indexOf(clean[i + 3]) else -1
        val triple = (c0 shl 18) or (c1 shl 12) or ((if (c2 >= 0) c2 else 0) shl 6) or (if (c3 >= 0) c3 else 0)
        output.add(((triple shr 16) and 0xFF).toByte())
        if (c2 >= 0) output.add(((triple shr 8) and 0xFF).toByte())
        if (c3 >= 0) output.add((triple and 0xFF).toByte())
        i += 4
    }
    return output.toByteArray()
}
