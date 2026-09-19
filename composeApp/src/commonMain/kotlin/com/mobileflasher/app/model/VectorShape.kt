package com.mobileflasher.app.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.max
import kotlin.math.min

sealed class VectorShape {
    abstract val id: String
    abstract val strokeColor: Color
    abstract val strokeWidth: Float
    abstract val fillColor: Color?

    abstract fun bounds(): Rect
    abstract fun translated(delta: Offset): VectorShape
}

data class RectangleShape(
    override val id: String,
    val topLeft: Offset,
    val size: Size,
    override val strokeColor: Color,
    override val strokeWidth: Float,
    override val fillColor: Color?
) : VectorShape() {
    override fun bounds(): Rect = Rect(topLeft, size)
    override fun translated(delta: Offset): VectorShape = copy(topLeft = topLeft + delta)
}

data class EllipseShape(
    override val id: String,
    val topLeft: Offset,
    val size: Size,
    override val strokeColor: Color,
    override val strokeWidth: Float,
    override val fillColor: Color?
) : VectorShape() {
    override fun bounds(): Rect = Rect(topLeft, size)
    override fun translated(delta: Offset): VectorShape = copy(topLeft = topLeft + delta)
}

data class LineShape(
    override val id: String,
    val start: Offset,
    val end: Offset,
    override val strokeColor: Color,
    override val strokeWidth: Float
) : VectorShape() {
    override val fillColor: Color? = null

    override fun bounds(): Rect {
        val left = min(start.x, end.x) - strokeWidth
        val top = min(start.y, end.y) - strokeWidth
        val right = max(start.x, end.x) + strokeWidth
        val bottom = max(start.y, end.y) + strokeWidth
        return Rect(left, top, right, bottom)
    }

    override fun translated(delta: Offset): VectorShape = copy(start = start + delta, end = end + delta)
}

data class FreehandShape(
    override val id: String,
    val points: List<Offset>,
    override val strokeColor: Color,
    override val strokeWidth: Float
) : VectorShape() {
    override val fillColor: Color? = null

    override fun bounds(): Rect {
        if (points.isEmpty()) return Rect.Zero
        var left = points.first().x
        var top = points.first().y
        var right = left
        var bottom = top
        for (point in points) {
            left = min(left, point.x)
            top = min(top, point.y)
            right = max(right, point.x)
            bottom = max(bottom, point.y)
        }
        return Rect(left - strokeWidth, top - strokeWidth, right + strokeWidth, bottom + strokeWidth)
    }

    override fun translated(delta: Offset): VectorShape = copy(points = points.map { it + delta })
}

data class ImageShape(
    override val id: String,
    val topLeft: Offset,
    val size: Size,
    val image: ImageBitmap,
    val sourcePng: ByteArray
) : VectorShape() {
    override val strokeColor: Color = Color.Transparent
    override val strokeWidth: Float = 0f
    override val fillColor: Color? = null

    override fun bounds(): Rect = Rect(topLeft, size)
    override fun translated(delta: Offset): VectorShape = copy(topLeft = topLeft + delta)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageShape) return false
        return id == other.id && topLeft == other.topLeft && size == other.size
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + topLeft.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}
