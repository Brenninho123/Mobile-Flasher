package com.mobileflasher.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
import com.mobileflasher.app.model.hitTest
import com.mobileflasher.app.render.drawShape
import com.mobileflasher.app.render.pointsToPath
import com.mobileflasher.app.state.EditorUiState
import com.mobileflasher.app.state.currentLayer
import com.mobileflasher.app.state.selectedShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val GridSpacing = 32f
private const val MinShapeSide = 12f
private const val MinPenSpacing = 2f
private const val EraserRadius = 18f
private const val SelectSlop = 8f
private const val MinZoom = 0.5f
private const val MaxZoom = 6f
private val ShapeTools = setOf(Tool.RECTANGLE, Tool.ELLIPSE, Tool.LINE, Tool.PEN)
private val SnapTools = setOf(Tool.RECTANGLE, Tool.ELLIPSE, Tool.LINE)

private fun snapPoint(point: Offset, state: EditorUiState): Offset {
    if (!state.snapToGrid || state.selectedTool !in SnapTools) return point
    return Offset(
        (point.x / GridSpacing).roundToInt() * GridSpacing,
        (point.y / GridSpacing).roundToInt() * GridSpacing
    )
}
private val PencilWood = Color(0xFFF3D5A5)
private val PencilBody = Color(0xFFFFC21A)
private val PencilBodyShade = Color(0xFFE59F00)
private val PencilFerrule = Color(0xFFB4BBC8)
private val PencilEraser = Color(0xFFF07C93)
private val PencilOutline = Color(0xCC1B1F2A)
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
    onShapeErased: (String) -> Unit,
    onStylePicked: (String) -> Unit,
    onBeginMoveGesture: () -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit,
    nextShapeId: () -> String
) {
    var viewScale by remember { mutableStateOf(1f) }
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var erasedInGesture by remember { mutableStateOf(false) }
    val latestScale by rememberUpdatedState(viewScale)
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var currentPoint by remember { mutableStateOf<Offset?>(null) }
    var penPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var draggingShapeId by remember { mutableStateOf<String?>(null) }
    var resizeCorner by remember { mutableStateOf<Corner?>(null) }
    var resizeAnchor by remember { mutableStateOf(Offset.Zero) }
    var cursorPoint by remember { mutableStateOf(Offset.Zero) }

    val marchTransition = rememberInfiniteTransition()
    val dashPhase = marchTransition.animateFloat(
        initialValue = 0f,
        targetValue = 23f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing))
    )
    val pencilActive = uiState.selectedTool == Tool.PEN && dragStart != null
    val pencilAlpha by animateFloatAsState(
        targetValue = if (pencilActive) 1f else 0f,
        animationSpec = tween(160)
    )

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
        erasedInGesture = false
    }

    fun eraseAt(point: Offset, state: EditorUiState) {
        val frame = state.currentLayer?.frames?.getOrNull(state.currentFrameIndex)
        val hit = frame?.shapes?.lastOrNull { it.hitTest(point, EraserRadius / latestScale) } ?: return
        if (state.currentLayer?.isLocked == true) return
        if (!erasedInGesture) {
            erasedInGesture = true
            onBeginMoveGesture()
        }
        onShapeErased(hit.id)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                            val nextScale = (viewScale * zoom).coerceIn(MinZoom, MaxZoom)
                            val ratio = nextScale / viewScale
                            val anchor = centroid - center
                            viewOffset = anchor - (anchor - viewOffset) * ratio + pan
                            viewScale = nextScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp)
                .graphicsLayer {
                    scaleX = viewScale
                    scaleY = viewScale
                    translationX = viewOffset.x
                    translationY = viewOffset.y
                }
                .onSizeChanged(onCanvasSizeChanged)
                .shadow(6.dp, RoundedCornerShape(4.dp))
                .background(StageColor, RoundedCornerShape(4.dp))
                .pointerInput(uiState.selectedTool, uiState.currentLayerIndex, uiState.currentFrameIndex) {
                    val tool = uiState.selectedTool
                    if (tool == Tool.SELECT || tool == Tool.ERASER || tool == Tool.EYEDROPPER) {
                        detectTapGestures { offset ->
                            val state = latestState
                            val frame = state.currentLayer?.frames?.getOrNull(state.currentFrameIndex)
                            val locked = state.currentLayer?.isLocked == true
                            when (tool) {
                                Tool.SELECT -> {
                                    val hit = if (locked) null else frame?.shapes?.lastOrNull { it.hitTest(offset, SelectSlop / latestScale) }
                                    onShapeSelected(hit?.id)
                                }
                                Tool.ERASER -> {
                                    erasedInGesture = false
                                    eraseAt(offset, state)
                                    erasedInGesture = false
                                }
                                else -> {
                                    val hit = frame?.shapes?.lastOrNull { it.hitTest(offset, SelectSlop / latestScale) }
                                    if (hit != null) onStylePicked(hit.id)
                                }
                            }
                        }
                    }
                }
                .pointerInput(uiState.selectedTool, uiState.currentLayerIndex, uiState.currentFrameIndex) {
                    val handleRadius = 26.dp.toPx()
                    detectDragGestures(
                        onDragStart = { rawOffset ->
                            val state = latestState
                            val offset = snapPoint(rawOffset, state)
                            dragStart = offset
                            currentPoint = offset
                            when (state.selectedTool) {
                                Tool.ERASER -> {
                                    cursorPoint = offset
                                    eraseAt(offset, state)
                                }
                                Tool.SELECT -> {
                                    val layerLocked = state.currentLayer?.isLocked == true
                                    val frame = state.currentLayer?.frames?.getOrNull(state.currentFrameIndex)
                                    val active = state.selectedShape
                                    val corner = active?.let { shape ->
                                        val bounds = shape.bounds()
                                        Corner.entries.firstOrNull { (bounds.cornerPoint(it) - offset).getDistance() <= handleRadius / latestScale }
                                    }
                                    if (layerLocked) {
                                        draggingShapeId = null
                                    } else if (active != null && corner != null) {
                                        resizeCorner = corner
                                        resizeAnchor = active.bounds().cornerPoint(corner.opposite())
                                        draggingShapeId = active.id
                                        onBeginMoveGesture()
                                    } else {
                                        val hit = frame?.shapes?.lastOrNull { it.hitTest(offset, SelectSlop / latestScale) }
                                        draggingShapeId = hit?.id
                                        onShapeSelected(hit?.id)
                                        if (hit != null) onBeginMoveGesture()
                                    }
                                }
                                Tool.PEN -> {
                                    penPoints = listOf(offset)
                                    cursorPoint = offset
                                }
                                else -> Unit
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val state = latestState
                            currentPoint = snapPoint(change.position, state)
                            cursorPoint = change.position
                            when (state.selectedTool) {
                                Tool.ERASER -> eraseAt(change.position, state)
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
                                Tool.PEN -> {
                                    val last = penPoints.lastOrNull()
                                    if (last == null || (change.position - last).getDistance() >= MinPenSpacing) {
                                        penPoints = penPoints + change.position
                                    }
                                }
                                else -> Unit
                            }
                        },
                        onDragEnd = {
                            val start = dragStart
                            val end = currentPoint
                            val state = latestState
                            if (start != null && end != null && state.selectedTool in ShapeTools) {
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
                if (start != null && end != null && uiState.selectedTool in ShapeTools) {
                    drawPreview(uiState.selectedTool, start, end, penPoints, uiState)
                }

                if (uiState.snapToGrid && start != null && end != null && uiState.selectedTool in SnapTools) {
                    val inverse = 1f / viewScale
                    drawCircle(BoltAmber, radius = 5.dp.toPx() * inverse, center = end)
                    drawCircle(Color.White, radius = 2.dp.toPx() * inverse, center = end)
                }

                if (selected != null && currentFrame != null) {
                    drawSelection(selected.bounds(), isLocked, dashPhase.value, 1f / viewScale)
                }

                if (uiState.selectedTool == Tool.ERASER && start != null) {
                    drawCircle(
                        color = Color(0xFFE53935),
                        radius = EraserRadius / viewScale,
                        center = cursorPoint,
                        style = Stroke(width = 1.5.dp.toPx() / viewScale)
                    )
                }

                if (pencilAlpha > 0f) {
                    scale(1f / viewScale, pivot = cursorPoint) {
                        drawPencilCursor(cursorPoint, uiState.strokeColor, pencilAlpha)
                    }
                }
            }
        }

        val measure = dragStart?.let { start ->
            currentPoint?.let { end -> measureLabel(uiState.selectedTool, start, end) }
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
            if (viewScale != 1f || viewOffset != Offset.Zero) {
                CanvasBadge(
                    text = "${(viewScale * 100f).roundToInt()}%  Fit",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            viewScale = 1f
                            viewOffset = Offset.Zero
                        }
                )
            }
            AnimatedVisibility(
                visible = measure != null,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)
            ) {
                CanvasBadge(measure ?: "", accent = true)
            }
        }
    }
}

@Composable
private fun CanvasBadge(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (accent) BoltInk else Color.White,
        modifier = modifier
            .background(if (accent) BoltAmber else Color(0xB3121620), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun Tool.label(): String = when (this) {
    Tool.SELECT -> "Select"
    Tool.ERASER -> "Eraser"
    Tool.EYEDROPPER -> "Eyedropper"
    Tool.RECTANGLE -> "Rectangle"
    Tool.ELLIPSE -> "Ellipse"
    Tool.LINE -> "Line"
    Tool.PEN -> "Pencil"
}

private fun measureLabel(tool: Tool, start: Offset, end: Offset): String? = when (tool) {
    Tool.RECTANGLE, Tool.ELLIPSE -> "${abs(end.x - start.x).roundToInt()} x ${abs(end.y - start.y).roundToInt()}"
    Tool.LINE -> "${(end - start).getDistance().roundToInt()} px"
    else -> null
}

internal fun DrawScope.drawPencilCursor(tip: Offset, leadColor: Color, alpha: Float) {
    val width = 14.dp.toPx()
    val half = width / 2f
    val cone = 18.dp.toPx()
    val body = 40.dp.toPx()
    val ferrule = 7.dp.toPx()
    val eraser = 10.dp.toPx()
    val length = cone + body + ferrule + eraser
    rotate(degrees = 32f, pivot = tip) {
        val bodyTop = tip.y - cone - body
        val ferruleTop = bodyTop - ferrule
        val eraserTop = ferruleTop - eraser

        drawPath(
            path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - half, tip.y - cone)
                lineTo(tip.x + half, tip.y - cone)
                close()
            },
            color = PencilWood,
            alpha = alpha
        )
        drawPath(
            path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - half * 0.34f, tip.y - cone * 0.34f)
                lineTo(tip.x + half * 0.34f, tip.y - cone * 0.34f)
                close()
            },
            color = leadColor,
            alpha = alpha
        )
        drawRect(PencilBody, Offset(tip.x - half, bodyTop), Size(width, body), alpha = alpha)
        drawRect(PencilBodyShade, Offset(tip.x + half * 0.2f, bodyTop), Size(half * 0.8f, body), alpha = alpha)
        drawRect(PencilFerrule, Offset(tip.x - half, ferruleTop), Size(width, ferrule), alpha = alpha)
        drawRoundRect(
            color = PencilEraser,
            topLeft = Offset(tip.x - half, eraserTop),
            size = Size(width, eraser),
            cornerRadius = CornerRadius(4.dp.toPx()),
            alpha = alpha
        )
        drawPath(
            path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - half, tip.y - cone)
                lineTo(tip.x - half, tip.y - length)
                lineTo(tip.x + half, tip.y - length)
                lineTo(tip.x + half, tip.y - cone)
                close()
            },
            color = PencilOutline,
            alpha = alpha,
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
        )
    }
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

private fun DrawScope.drawSelection(bounds: Rect, isLocked: Boolean, dashPhase: Float, uiScale: Float) {
    val accent = if (isLocked) Color(0xFF9199AD) else Color(0xFF2F63E0)
    drawRect(
        color = accent.copy(alpha = 0.08f),
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height)
    )
    drawRect(
        color = accent,
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Stroke(
            width = 1.5.dp.toPx() * uiScale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f), if (isLocked) 0f else dashPhase)
        )
    )
    if (isLocked) return
    val radius = 7.dp.toPx() * uiScale
    Corner.entries.forEach { corner ->
        val center = bounds.cornerPoint(corner)
        drawCircle(Color.White, radius = radius, center = center, style = Fill)
        drawCircle(accent, radius = radius, center = center, style = Stroke(width = 2.dp.toPx() * uiScale))
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
        else -> null
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
        Tool.PEN -> if (penPoints.size > 1) {
            drawPath(
                pointsToPath(penPoints),
                uiState.strokeColor,
                style = Stroke(width = uiState.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        else -> Unit
    }
}
