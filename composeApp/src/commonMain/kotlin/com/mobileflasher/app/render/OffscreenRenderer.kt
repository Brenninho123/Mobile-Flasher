package com.mobileflasher.app.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mobileflasher.app.model.Project

fun renderFrameToImageBitmap(
    project: Project,
    frameIndex: Int,
    width: Int,
    height: Int,
    backgroundColor: Color = Color.White
): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    val canvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(density = 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(width.toFloat(), height.toFloat())
    ) {
        drawRect(color = backgroundColor, size = size)
        project.layers.filter { it.isVisible }.forEach { layer ->
            val frame = layer.frames.getOrNull(frameIndex) ?: layer.frames.lastOrNull()
            frame?.shapes?.forEach { shape -> drawShape(shape) }
        }
    }
    return bitmap
}

fun renderAllFramesToImageBitmaps(
    project: Project,
    width: Int,
    height: Int,
    backgroundColor: Color = Color.White
): List<ImageBitmap> {
    val frameCount = project.layers.maxOfOrNull { it.frames.size } ?: 1
    return (0 until frameCount).map { frameIndex ->
        renderFrameToImageBitmap(project, frameIndex, width, height, backgroundColor)
    }
}
