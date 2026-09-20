package com.mobileflasher.app.fla

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal data class Affine(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f
) {
    val isAxisAligned: Boolean get() = abs(b) < 1e-4f && abs(c) < 1e-4f

    val scaleFactor: Float get() = sqrt(abs(a * d - b * c)).takeIf { it > 0f } ?: 1f

    fun apply(point: Offset): Offset = Offset(a * point.x + c * point.y + tx, b * point.x + d * point.y + ty)

    fun then(inner: Affine): Affine = Affine(
        a = a * inner.a + c * inner.b,
        b = b * inner.a + d * inner.b,
        c = a * inner.c + c * inner.d,
        d = b * inner.c + d * inner.d,
        tx = a * inner.tx + c * inner.ty + tx,
        ty = b * inner.tx + d * inner.ty + ty
    )
}

internal class Chain(val points: List<Offset>, val closed: Boolean)

internal object FlaEdges {

    private const val CurveSteps = 6
    private const val TwipsPerPixel = 20f

    fun parsePaths(text: String): List<List<Offset>> {
        val paths = ArrayList<MutableList<Offset>>()
        var current: MutableList<Offset>? = null
        var last = Offset.Zero
        val reader = Reader(text)
        while (reader.hasMore()) {
            when (reader.command()) {
                '!' -> {
                    val x = reader.number() ?: continue
                    val y = reader.number() ?: continue
                    last = Offset(x / TwipsPerPixel, y / TwipsPerPixel)
                    current = mutableListOf(last)
                    paths.add(current)
                }
                '|', '/' -> {
                    val x = reader.number() ?: continue
                    val y = reader.number() ?: continue
                    last = Offset(x / TwipsPerPixel, y / TwipsPerPixel)
                    (current ?: mutableListOf(last).also { paths.add(it); current = it }).add(last)
                }
                '[', ']' -> {
                    val cx = reader.number() ?: continue
                    val cy = reader.number() ?: continue
                    val x = reader.number() ?: continue
                    val y = reader.number() ?: continue
                    val control = Offset(cx / TwipsPerPixel, cy / TwipsPerPixel)
                    val end = Offset(x / TwipsPerPixel, y / TwipsPerPixel)
                    val target = current ?: mutableListOf(last).also { paths.add(it); current = it }
                    for (step in 1..CurveSteps) {
                        val t = step.toFloat() / CurveSteps
                        val u = 1f - t
                        target.add(
                            Offset(
                                u * u * last.x + 2f * u * t * control.x + t * t * end.x,
                                u * u * last.y + 2f * u * t * control.y + t * t * end.y
                            )
                        )
                    }
                    last = end
                }
            }
        }
        return paths
    }

    fun chain(segments: List<List<Offset>>): List<Chain> {
        val usable = segments.filter { it.size >= 2 }
        val byStart = HashMap<Long, ArrayDeque<Int>>()
        usable.forEachIndexed { index, points -> byStart.getOrPut(key(points.first())) { ArrayDeque() }.addLast(index) }
        val used = BooleanArray(usable.size)
        val chains = ArrayList<Chain>()
        for (seed in usable.indices) {
            if (used[seed]) continue
            used[seed] = true
            byStart[key(usable[seed].first())]?.remove(seed)
            val points = ArrayList(usable[seed])
            val startKey = key(points.first())
            while (key(points.last()) != startKey) {
                val next = byStart[key(points.last())]?.removeFirstOrNull() ?: break
                used[next] = true
                points.addAll(usable[next].drop(1))
            }
            val closed = points.size > 2 && key(points.last()) == startKey
            chains.add(Chain(if (closed) points.dropLast(1) else points, closed))
        }
        return chains
    }

    fun format(points: List<Offset>): String {
        val out = StringBuilder()
        points.forEachIndexed { index, point ->
            out.append(if (index == 0) '!' else '|')
            out.append(twips(point.x)).append(' ').append(twips(point.y))
        }
        return out.toString()
    }

    fun twips(pixels: Float): Long = (pixels * TwipsPerPixel).roundToLong()

    private fun key(point: Offset): Long {
        val x = (point.x * TwipsPerPixel).roundToLong()
        val y = (point.y * TwipsPerPixel).roundToLong()
        return (x shl 32) xor (y and 0xFFFFFFFFL)
    }

    private class Reader(private val text: String) {
        private var position = 0

        fun hasMore(): Boolean = position < text.length

        fun command(): Char {
            val ch = text[position]
            position++
            if (ch == 'S') {
                while (position < text.length && text[position].isDigit()) position++
            }
            return ch
        }

        fun number(): Float? {
            while (position < text.length && text[position].isWhitespace()) position++
            val start = position
            while (position < text.length && !text[position].isWhitespace() && text[position] !in "!|/[]S") position++
            if (start == position) return null
            return parseNumber(text.substring(start, position))
        }

        private fun parseNumber(token: String): Float? {
            if (!token.startsWith("#")) return token.toFloatOrNull()
            val parts = token.substring(1).split('.', limit = 2)
            val integer = parts[0].toLongOrNull(16)?.toInt() ?: return null
            val fraction = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { digits ->
                val value = digits.toLongOrNull(16) ?: return null
                var scale = 1.0
                repeat(digits.length) { scale *= 16.0 }
                (value / scale).toFloat()
            } ?: 0f
            return integer + fraction
        }
    }
}
