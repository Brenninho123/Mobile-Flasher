package com.mobileflasher.app.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.ImageShape
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.VectorShape
import kotlin.math.roundToInt

fun DrawScope.drawShape(shape: VectorShape, alpha: Float = 1f) {
    when (shape) {
        is RectangleShape -> {
            shape.fillColor?.let { drawRect(color = it, topLeft = shape.topLeft, size = shape.size, alpha = alpha, style = Fill) }
            drawRect(color = shape.strokeColor, topLeft = shape.topLeft, size = shape.size, alpha = alpha, style = Stroke(width = shape.strokeWidth))
        }
        is EllipseShape -> {
            shape.fillColor?.let { drawOval(color = it, topLeft = shape.topLeft, size = shape.size, alpha = alpha, style = Fill) }
            drawOval(color = shape.strokeColor, topLeft = shape.topLeft, size = shape.size, alpha = alpha, style = Stroke(width = shape.strokeWidth))
        }
        is LineShape -> drawLine(color = shape.strokeColor, start = shape.start, end = shape.end, strokeWidth = shape.strokeWidth, alpha = alpha)
        is FreehandShape -> if (shape.points.size > 1) {
            drawPath(pointsToPath(shape.points), shape.strokeColor, alpha = alpha, style = Stroke(width = shape.strokeWidth))
        }
        is ImageShape -> drawImage(
            image = shape.image,
            dstOffset = IntOffset(shape.topLeft.x.roundToInt(), shape.topLeft.y.roundToInt()),
            dstSize = IntSize(shape.size.width.roundToInt(), shape.size.height.roundToInt()),
            alpha = alpha
        )
    }
}

fun pointsToPath(points: List<Offset>): Path {
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (point in points.drop(1)) {
        path.lineTo(point.x, point.y)
    }
    return path
}
