package com.mobileflasher.app.zip

internal object Inflate {

    private const val MaxBits = 15

    private val LengthBase = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
    )
    private val LengthExtra = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
    )
    private val DistanceBase = intArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769,
        1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    )
    private val DistanceExtra = intArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    )
    private val CodeLengthOrder = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    private val FixedLiteral = Huffman(IntArray(288) { if (it < 144) 8 else if (it < 256) 9 else if (it < 280) 7 else 8 }, 288)
    private val FixedDistance = Huffman(IntArray(30) { 5 }, 30)

    fun inflate(data: ByteArray, start: Int, end: Int, maxOutput: Int): ByteArray {
        val reader = BitReader(data, start, end)
        val out = Output(maxOutput)
        do {
            val isFinal = reader.bits(1) == 1
            when (reader.bits(2)) {
                0 -> stored(reader, out)
                1 -> codes(reader, out, FixedLiteral, FixedDistance)
                2 -> dynamic(reader, out)
                else -> throw IllegalArgumentException("Invalid deflate block")
            }
        } while (!isFinal)
        return out.toByteArray()
    }

    private fun stored(reader: BitReader, out: Output) {
        reader.alignToByte()
        val length = reader.byte() or (reader.byte() shl 8)
        val complement = reader.byte() or (reader.byte() shl 8)
        require(length == complement.inv() and 0xFFFF) { "Corrupt stored block" }
        repeat(length) { out.put(reader.byte()) }
    }

    private fun dynamic(reader: BitReader, out: Output) {
        val literalCount = reader.bits(5) + 257
        val distanceCount = reader.bits(5) + 1
        val codeLengthCount = reader.bits(4) + 4
        require(literalCount <= 286 && distanceCount <= 30) { "Corrupt dynamic block" }

        val codeLengths = IntArray(19)
        for (i in 0 until codeLengthCount) codeLengths[CodeLengthOrder[i]] = reader.bits(3)
        val codeLengthTable = Huffman(codeLengths, 19)

        val lengths = IntArray(literalCount + distanceCount)
        var index = 0
        while (index < lengths.size) {
            val symbol = codeLengthTable.decode(reader)
            if (symbol < 16) {
                lengths[index++] = symbol
                continue
            }
            var repeatValue = 0
            val repeatCount = when (symbol) {
                16 -> {
                    require(index > 0) { "Corrupt dynamic block" }
                    repeatValue = lengths[index - 1]
                    3 + reader.bits(2)
                }
                17 -> 3 + reader.bits(3)
                else -> 11 + reader.bits(7)
            }
            require(index + repeatCount <= lengths.size) { "Corrupt dynamic block" }
            repeat(repeatCount) { lengths[index++] = repeatValue }
        }
        require(lengths[256] != 0) { "Corrupt dynamic block" }
        val literal = Huffman(lengths.copyOfRange(0, literalCount), literalCount)
        val distance = Huffman(lengths.copyOfRange(literalCount, lengths.size), distanceCount)
        codes(reader, out, literal, distance)
    }

    private fun codes(reader: BitReader, out: Output, literal: Huffman, distance: Huffman) {
        while (true) {
            val symbol = literal.decode(reader)
            if (symbol < 256) {
                out.put(symbol)
            } else if (symbol == 256) {
                return
            } else {
                val lengthIndex = symbol - 257
                require(lengthIndex < LengthBase.size) { "Corrupt length code" }
                val length = LengthBase[lengthIndex] + reader.bits(LengthExtra[lengthIndex])
                val distanceSymbol = distance.decode(reader)
                require(distanceSymbol < DistanceBase.size) { "Corrupt distance code" }
                val back = DistanceBase[distanceSymbol] + reader.bits(DistanceExtra[distanceSymbol])
                out.copyBack(back, length)
            }
        }
    }

    private class Huffman(lengths: IntArray, symbolCount: Int) {
        private val count = IntArray(MaxBits + 1)
        private val symbols = IntArray(symbolCount)

        init {
            for (i in 0 until symbolCount) count[lengths[i]]++
            val offsets = IntArray(MaxBits + 2)
            for (len in 1..MaxBits) offsets[len + 1] = offsets[len] + count[len]
            for (i in 0 until symbolCount) {
                if (lengths[i] != 0) symbols[offsets[lengths[i]]++] = i
            }
        }

        fun decode(reader: BitReader): Int {
            var code = 0
            var first = 0
            var index = 0
            for (len in 1..MaxBits) {
                code = code or reader.bits(1)
                val c = count[len]
                if (code - c < first) return symbols[index + (code - first)]
                index += c
                first += c
                first = first shl 1
                code = code shl 1
            }
            throw IllegalArgumentException("Invalid Huffman code")
        }
    }

    private class BitReader(private val data: ByteArray, private var position: Int, private val end: Int) {
        private var buffer = 0
        private var bitCount = 0

        fun bits(need: Int): Int {
            var value = buffer
            while (bitCount < need) {
                value = value or (byte() shl bitCount)
                bitCount += 8
            }
            buffer = value ushr need
            bitCount -= need
            return value and ((1 shl need) - 1)
        }

        fun alignToByte() {
            buffer = 0
            bitCount = 0
        }

        fun byte(): Int {
            require(position < end) { "Unexpected end of deflate data" }
            return data[position++].toInt() and 0xFF
        }
    }

    private class Output(private val limit: Int) {
        private var buffer = ByteArray(8192)
        private var size = 0

        fun put(value: Int) {
            ensure(1)
            buffer[size++] = value.toByte()
        }

        fun copyBack(distance: Int, length: Int) {
            require(distance <= size) { "Corrupt back reference" }
            ensure(length)
            repeat(length) {
                buffer[size] = buffer[size - distance]
                size++
            }
        }

        private fun ensure(extra: Int) {
            require(size + extra <= limit) { "Decompressed data is too large" }
            if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)
    }
}
