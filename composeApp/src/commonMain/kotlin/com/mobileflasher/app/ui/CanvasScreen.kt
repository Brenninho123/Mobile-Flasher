package com.mobileflasher.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.EllipseShape
import com.mobileflasher.app.model.FreehandShape
import com.mobileflasher.app.model.LineShape
import com.mobileflasher.app.model.RectangleShape
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.render.drawShape
import com.mobileflasher.app.render.pointsToPath
import com.mobileflasher.app.state.EditorUiState
import com.mobileflasher.app.state.currentLayer
import com.mobileflasher.app.state.selectedShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val GridSpacing = 32f
private const val MinShapeSide = 12f
private val StageColor = Color.White
private val GridColor = Color(0x1F1E66F5)
private val GridMajorColor = Color(0x381E66F5)

private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private fun Rect.cornerPoint(corner: Corner): Offset = when (corner) {
    Corner.TOP_LEFT -> topLeft
    Corner.TOP_RIGHT -> topRight
    Corner.BOTTOM_LEFT -> bottomLeft
    Corner.BOTTOM_RIGHT -> bottomRight
}

private fun Corner.opposite(): Corner = when (this) {
    Corner.TOP_LEFT -> Corner.BOTTOM_RIGHT
    Corner.TOP_RIGHT -> Corner.BOTTOM_LEFT
    Corner.BOTTOM_LEFT -> Corner.TOP_RIGHT
    Corner.BOTTOM_RIGHT -> Corner.TOP_LEFT
}

@Composable
fun CanvasScreen(
    uiState: EditorUiState,
    modifier: Modifier = Modifier,
    onShapeCreated: (VectorShape) -> Unit,
    onShapeSelected: (String?) -> Unit,
    onShapeMoved: (String, Offset) -> Unit,
    onShapeResized: (String, Rect) -> Unit,
    onBeginMoveGesture: () -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    nextShapeId: () -> String
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }
    var penPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var draggingShapeId by remember { mutableStateOf<String?>(null) }
    var resizeCorner by remember { mutableStateOf<Corner?>(null) }
    var resizeAnchor by remember { mutableStateOf(Offset.Zero) }

    val latestState by rememberUpdatedState(uiState)
    val currentLayer = uiState.currentLayer
    val currentFrame = currentLayer?.frames?.getOrNull(uiState.currentFrameIndex)
    val isLocked = currentLayer?.isLocked == true
    val selected = uiState.selectedShape

    fun reset() {
        dragStart = null
        currentPoint = null
        penPoints = emptyList()
        draggingShapeId = null
        resizeCorner = null
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp)
                .onSizeChanged(onCanvasSizeChanged)
                .background(StageColor, RoundedCornerShape(4.dp))
                .pointerInput(uiState.selectedTool, uiState.currentLayerIndex, uiState.currentFrameIndex) {
                    if (uiState.selectedTool == Tool.SELECT) {
                        detectTapGestures { offset ->
                            val state = latestState
                            val frame = state.currentLayer?.frames?.getOrNull(state.currentFrameIndex)
                            val locked = state.currentLayer?.isLocked == true
                            val hit = if (locked) null else frame?.shapes?.lastOrNull { it.bounds().inflate(8f).contains(offset) }
                            onShapeSelected(hit?.id)
                        }
                    }
                }
                .pointerInput(uiState.selectedTool, uiState.currentLayerIndex, uiState.currentFrameIndex) {
                    val handleRadius = 26.dp.toPx()
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStart = offset
                            currentPoint = offset
                            val state = latestState
                            when (state.selectedTool) {
                                Tool.SELECT -> {
                                    val layerLocked = state.currentLayer?.isLocked == true
                                    val frame = state.currentLayer?.frames?.getOrNull(state.currentFrameIndex)
                                    val active = state.selectedShape
                                    val corner = active?.let { shape ->
                                        val bounds = shape.bounds()
                                        Corner.entries.firstOrNull { (bounds.cornerPoint(it) - offset).getDistance() <= handleRadius }
                                    }
                                    if (layerLocked) {
                                        draggingShapeId = null
                                    } else if (active != null && corner != null) {
                                        resizeCorner = corner
                                        resizeAnchor = active.bounds().cornerPoint(corner.opposite())
                                        draggingShapeId = active.id
                                        onBeginMoveGesture()
                                    } else {
                                        val hit = frame?.shapes?.lastOrNull { it.bounds().inflate(8f).contains(offset) }
                                        draggingShapeId = hit?.id
                                        onShapeSelected(hit?.id)
                                        if (hit != null) onBeginMoveGesture()
                                    }
                                }
                                Tool.PEN -> penPoints = listOf(offset)
                                else -> Unit
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPoint = change.position
                            val state = latestState
                            when (state.selectedTool) {
                                Tool.SELECT -> {
                                    val id = draggingShapeId
                                    if (id != null) {
                                        if (resizeCorner != null) {
                                            val position = change.position
                                            val left = min(resizeAnchor.x, position.x)
                                            val top = min(resizeAnchor.y, position.y)
                                            val right = max(resizeAnchor.x, position.x)
                                            val bottom = max(resizeAnchor.y, position.y)
                                            val target = Rect(
                                                left,
                                                top,
                                                max(right, left + MinShapeSide),
                                                max(bottom, top + MinShapeSide)
                                            )
                                            onShapeResized(id, target)
                                        } else {
                                            onShapeMoved(id, dragAmount)
                                        }
                                    }
                                }
                                Tool.PEN -> penPoints = penPoints + change.position
                                else -> Unit
                            }
                        },
                        onDragEnd = {
                            val start = dragStart
                            val end = currentPoint
                            val state = latestState
                            if (start != null && end != null && state.selectedTool != Tool.SELECT) {
                                val shape = buildShape(state.selectedTool, start, end, penPoints, state, nextShapeId)
                                if (shape != null) onShapeCreated(shape)
                            }
                            reset()
                        },
                        onDragCancel = { reset() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (uiState.gridVisible) drawGrid()

                if (uiState.onionSkinEnabled && currentLayer != null) {
                    currentLayer.frames.getOrNull(uiState.currentFrameIndex - 1)?.shapes?.forEach { shape ->
                        drawShape(shape, alpha = 0.25f)
                    }
                    currentLayer.frames.getOrNull(uiState.currentFrameIndex + 1)?.shapes?.forEach { shape ->
                        drawShape(shape, alpha = 0.15f)
                    }
                }

                uiState.project.layers.filter { it.isVisible }.forEach { layer ->
                    val frame = layer.frames.getOrNull(uiState.currentFrameIndex) ?: layer.frames.lastOrNull()
                    frame?.shapes?.forEach { shape -> drawShape(shape) }
                }

                val start = dragStart
                val end = currentPoint
                if (start != null && end != null && uiState.selectedTool != Tool.SELECT) {
                    drawPreview(uiState.selectedTool, start, end, penPoints, uiState)
                }

                if (selected != null && currentFrame != null) {
                    drawSelection(selected.bounds(), isLocked)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CanvasBadge(uiState.selectedTool.label())
            CanvasBadge("Frame ${uiState.currentFrameIndex + 1}")
            if (isLocked) CanvasBadge("Locked")
        }
    }
}

@Composable
private fun CanvasBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(Color(0xB3121620), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun Tool.label(): String = when (this) {
    Tool.SELECT -> "Select"
    Tool.RECTANGLE -> "Rectangle"
    Tool.ELLIPSE -> "Ellipse"
    Tool.LINE -> "Line"
    Tool.PEN -> "Pen"
}

private fun DrawScope.drawGrid() {
    var x = 0f
    var column = 0
    while (x <= size.width) {
        drawLine(
            color = if (column % 4 == 0) GridMajorColor else GridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += GridSpacing
        column++
    }
    var y = 0f
    var row = 0
    while (y <= size.height) {
        drawLine(
            color = if (row % 4 == 0) GridMajorColor else GridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += GridSpacing
        row++
    }
}

private fun DrawScope.drawSelection(bounds: Rect, isLocked: Boolean) {
    val accent = if (isLocked) Color(0xFF9199AD) else Color(0xFF2F63E0)
    drawRect(
        color = accent,
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))
    )
    if (isLocked) return
    val radius = 7.dp.toPx()
    Corner.entries.forEach { corner ->
        val center = bounds.cornerPoint(corner)
        drawCircle(Color.White, radius = radius, center = center, style = Fill)
        drawCircle(accent, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
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
        Tool.LINE -> if (size.width > 2f || size.height > 2f) {
            LineShape(nextShapeId(), start, end, uiState.strokeColor, uiState.strokeWidth)
        } else null
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
    val fill = uiState.fillColor
    when (tool) {
        Tool.RECTANGLE -> {
            if (fill != null) drawRect(color = fill, topLeft = topLeft, size = size, alpha = 0.6f)
            drawRect(color = uiState.strokeColor, topLeft = topLeft, size = size, style = Stroke(width = uiState.strokeWidth))
        }
        Tool.ELLIPSE -> {
            if (fill != null) drawOval(color = fill, topLeft = topLeft, size = size, alpha = 0.6f)
            drawOval(color = uiState.strokeColor, topLeft = topLeft, size = size, style = Stroke(width = uiState.strokeWidth))
        }
        Tool.LINE -> drawLine(color = uiState.strokeColor, start = start, end = end, strokeWidth = uiState.strokeWidth)
        Tool.PEN -> if (penPoints.size > 1) drawPath(pointsToPath(penPoints), uiState.strokeColor, style = Stroke(width = uiState.strokeWidth))
        Tool.SELECT -> Unit
    }
}
