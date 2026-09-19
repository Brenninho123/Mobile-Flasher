package com.mobileflasher.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.render.drawShape
import com.mobileflasher.app.render.pointsToPath
import com.mobileflasher.app.state.EditorUiState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun CanvasScreen(
    uiState: EditorUiState,
    modifier: Modifier = Modifier,
    onShapeCreated: (VectorShape) -> Unit,
    onShapeSelected: (String?) -> Unit,
    onShapeMoved: (String, Offset) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    nextShapeId: () -> String
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }
    var penPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var draggingShapeId by remember { mutableStateOf<String?>(null) }

    val currentLayer = uiState.project.layers.getOrNull(uiState.currentLayerIndex)
    val currentFrame = currentLayer?.frames?.getOrNull(uiState.currentFrameIndex)

    Box(
        modifier = modifier
            .background(Color.White)
            .onSizeChanged(onCanvasSizeChanged)
            .pointerInput(uiState.selectedTool, uiState.currentLayerIndex, uiState.currentFrameIndex) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        currentPoint = offset
                        when (uiState.selectedTool) {
                            Tool.SELECT -> {
                                val hit = currentFrame?.shapes?.lastOrNull { it.bounds().contains(offset) }
                                draggingShapeId = hit?.id
                                onShapeSelected(hit?.id)
                            }
                            Tool.PEN -> penPoints = listOf(offset)
                            else -> Unit
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPoint = change.position
                        when (uiState.selectedTool) {
                            Tool.SELECT -> draggingShapeId?.let { onShapeMoved(it, dragAmount) }
                            Tool.PEN -> penPoints = penPoints + change.position
                            else -> Unit
                        }
                    },
                    onDragEnd = {
                        val start = dragStart
                        val end = currentPoint
                        if (start != null && end != null && uiState.selectedTool != Tool.SELECT) {
                            val shape = buildShape(uiState.selectedTool, start, end, penPoints, uiState, nextShapeId)
                            if (shape != null) onShapeCreated(shape)
                        }
                        dragStart = null
                        currentPoint = null
                        penPoints = emptyList()
                        draggingShapeId = null
                    },
                    onDragCancel = {
                        dragStart = null
                        currentPoint = null
                        penPoints = emptyList()
                        draggingShapeId = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            uiState.project.layers.filter { it.isVisible }.forEach { layer ->
                val frame = layer.frames.getOrNull(uiState.currentFrameIndex) ?: layer.frames.lastOrNull()
                frame?.shapes?.forEach { shape -> drawShape(shape, isSelected = shape.id == uiState.selectedShapeId) }
            }

            val start = dragStart
            val end = currentPoint
            if (start != null && end != null) {
                drawPreview(uiState.selectedTool, start, end, penPoints, uiState)
            }
        }
    }
}

private fun buildShape(
    tool: Tool,
    start: Offset,
    end: Offset,
    penPoints: List<Offset>,
    uiState: EditorUiState,
    nextShapeId: () -> String
): VectorShape? {
    val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
    val size = Size(abs(end.x - start.x), abs(end.y - start.y))
    return when (tool) {
        Tool.RECTANGLE -> if (size.width > 2f && size.height > 2f) {
            RectangleShape(nextShapeId(), topLeft, size, uiState.strokeColor, uiState.strokeWidth, uiState.fillColor)
        } else null
        Tool.ELLIPSE -> if (size.width > 2f && size.height > 2f) {
            EllipseShape(nextShapeId(), topLeft, size, uiState.strokeColor, uiState.strokeWidth, uiState.fillColor)
        } else null
        Tool.LINE -> LineShape(nextShapeId(), start, end, uiState.strokeColor, uiState.strokeWidth)
        Tool.PEN -> if (penPoints.size > 1) {
            FreehandShape(nextShapeId(), penPoints, uiState.strokeColor, uiState.strokeWidth)
        } else null
        Tool.SELECT -> null
    }
}

private fun DrawScope.drawPreview(
    tool: Tool,
    start: Offset,
    end: Offset,
    penPoints: List<Offset>,
    uiState: EditorUiState
) {
    val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
    val size = Size(abs(end.x - start.x), abs(end.y - start.y))
    when (tool) {
        Tool.RECTANGLE -> drawRect(color = uiState.strokeColor, topLeft = topLeft, size = size, style = Stroke(width = uiState.strokeWidth))
        Tool.ELLIPSE -> drawOval(color = uiState.strokeColor, topLeft = topLeft, size = size, style = Stroke(width = uiState.strokeWidth))
        Tool.LINE -> drawLine(color = uiState.strokeColor, start = start, end = end, strokeWidth = uiState.strokeWidth)
        Tool.PEN -> if (penPoints.size > 1) drawPath(pointsToPath(penPoints), uiState.strokeColor, style = Stroke(width = uiState.strokeWidth))
        Tool.SELECT -> Unit
    }
}

