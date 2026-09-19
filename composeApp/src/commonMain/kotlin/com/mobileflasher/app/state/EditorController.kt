package com.mobileflasher.app.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.ImageShape
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.model.VectorShape
import com.mobileflasher.app.platform.decodePngToImageBitmap
import com.mobileflasher.app.svg.importSvg
import com.mobileflasher.app.xml.parseProjectXml
import com.mobileflasher.app.xml.writeProjectXml
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorController {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var shapeIdCounter = 0
    private var layerIdCounter = 1
    private var strokeEditActive = false

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()
    private val maxHistorySize = 50

    fun nextShapeId(): String = "shape-${shapeIdCounter++}"

    fun beginMoveGesture() {
        recordHistory()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.project)
        _state.update { it.copy(project = previous, selectedShapeId = null) }
        updateHistoryFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.project)
        _state.update { it.copy(project = next, selectedShapeId = null) }
        updateHistoryFlags()
    }

    fun toggleOnionSkin() {
        _state.update { it.copy(onionSkinEnabled = !it.onionSkinEnabled) }
    }

    private fun recordHistory() {
        undoStack.addLast(_state.value.project)
        if (undoStack.size > maxHistorySize) undoStack.removeFirst()
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    fun selectTool(tool: Tool) {
        _state.update { it.copy(selectedTool = tool, selectedShapeId = null) }
    }

    fun setStrokeColor(color: Color) {
        _state.update { it.copy(strokeColor = color) }
        restyleSelected(recordsHistory = true)
    }

    fun setFillColor(color: Color?) {
        _state.update { it.copy(fillColor = color) }
        restyleSelected(recordsHistory = true)
    }

    fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width) }
        restyleSelected(recordsHistory = !strokeEditActive)
        if (_state.value.selectedShape != null) strokeEditActive = true
    }

    fun endStrokeWidthEdit() {
        strokeEditActive = false
    }

    private fun restyleSelected(recordsHistory: Boolean) {
        val current = _state.value
        val selected = current.selectedShape ?: return
        if (selected is ImageShape) return
        if (current.currentLayer?.isLocked == true) return
        if (recordsHistory) recordHistory()
        _state.update { state ->
            state.withCurrentFrame { frame ->
                frame.copy(
                    shapes = frame.shapes.map { shape ->
                        if (shape.id == selected.id) shape.restyled(state.strokeColor, state.strokeWidth, state.fillColor) else shape
                    }
                )
            }
        }
    }

    fun selectShape(shapeId: String?) {
        _state.update { it.copy(selectedShapeId = shapeId) }
    }

    fun toggleGrid() {
        _state.update { it.copy(gridVisible = !it.gridVisible) }
    }

    fun setProjectName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _state.update { it.copy(project = it.project.copy(name = trimmed)) }
    }

    fun setFrameRate(frameRate: Int) {
        _state.update { it.copy(project = it.project.copy(frameRate = frameRate.coerceIn(1, 60))) }
    }

    fun resizeShape(shapeId: String, target: Rect) {
        _state.update { current ->
            current.withCurrentFrame { frame ->
                frame.copy(shapes = frame.shapes.map { if (it.id == shapeId) it.scaledTo(target) else it })
            }
        }
    }

    fun duplicateSelectedShape() {
        val current = _state.value
        val selected = current.selectedShape ?: return
        if (current.currentLayer?.isLocked == true) return
        recordHistory()
        val copy = selected.translated(Offset(24f, 24f)).withId(nextShapeId())
        _state.update { state ->
            state.withCurrentFrame { frame -> frame.copy(shapes = frame.shapes + copy) }.copy(selectedShapeId = copy.id)
        }
    }

    fun bringSelectedToFront() {
        reorderSelected { shapes, shape -> shapes.filterNot { it.id == shape.id } + shape }
    }

    fun sendSelectedToBack() {
        reorderSelected { shapes, shape -> listOf(shape) + shapes.filterNot { it.id == shape.id } }
    }

    private fun reorderSelected(reorder: (List<VectorShape>, VectorShape) -> List<VectorShape>) {
        val current = _state.value
        val selected = current.selectedShape ?: return
        if (current.currentLayer?.isLocked == true) return
        recordHistory()
        _state.update { state ->
            state.withCurrentFrame { frame -> frame.copy(shapes = reorder(frame.shapes, selected)) }
        }
    }

    fun toggleLayerLock(index: Int) {
        _state.update { current -> current.withLayer(index) { layer -> layer.copy(isLocked = !layer.isLocked) } }
    }

    fun deleteLayer(index: Int) {
        val current = _state.value
        if (current.project.layers.size <= 1 || index !in current.project.layers.indices) return
        recordHistory()
        _state.update { state ->
            val layers = state.project.layers.filterIndexed { i, _ -> i != index }
            val project = state.project.copy(layers = layers)
            state.copy(
                project = project,
                currentLayerIndex = state.currentLayerIndex.coerceAtMost(layers.lastIndex),
                currentFrameIndex = state.currentFrameIndex.coerceAtMost(project.frameCount - 1),
                selectedShapeId = null
            )
        }
    }

    fun deleteCurrentFrame() {
        val current = _state.value
        val layer = current.currentLayer ?: return
        if (layer.frames.size <= 1 || current.currentFrameIndex !in layer.frames.indices) return
        recordHistory()
        _state.update { state ->
            val updated = state.withLayer(state.currentLayerIndex) { target ->
                val remaining = target.frames.filterIndexed { i, _ -> i != state.currentFrameIndex }
                target.copy(frames = remaining.mapIndexed { i, frame -> frame.copy(index = i) })
            }
            updated.copy(
                currentFrameIndex = state.currentFrameIndex.coerceAtMost(updated.project.frameCount - 1),
                selectedShapeId = null
            )
        }
    }

    fun selectCell(layerIndex: Int, frameIndex: Int) {
        _state.update { current ->
            val maxIndex = current.project.frameCount - 1
            current.copy(
                currentLayerIndex = layerIndex,
                currentFrameIndex = frameIndex.coerceIn(0, maxIndex),
                selectedShapeId = null
            )
        }
    }

    private fun canEditCurrentLayer(): Boolean {
        val layer = _state.value.currentLayer ?: return false
        if (layer.isLocked) {
            setStatusMessage("Layer is locked")
            return false
        }
        if (!layer.isVisible) {
            setStatusMessage("Layer is hidden")
            return false
        }
        return true
    }

    fun addShape(shape: VectorShape) {
        if (!canEditCurrentLayer()) return
        recordHistory()
        _state.update { current ->
            current.withCurrentFrame { frame -> frame.copy(shapes = frame.shapes + shape) }
                .copy(selectedShapeId = shape.id)
        }
    }

    fun moveShape(shapeId: String, delta: Offset) {
        _state.update { current ->
            current.withCurrentFrame { frame ->
                frame.copy(
                    shapes = frame.shapes.map { shape ->
                        if (shape.id == shapeId) shape.translated(delta) else shape
                    }
                )
            }
        }
    }

    fun deleteSelectedShape() {
        val shapeId = _state.value.selectedShapeId ?: return
        recordHistory()
        _state.update { current ->
            current.withCurrentFrame { frame ->
                frame.copy(shapes = frame.shapes.filterNot { it.id == shapeId })
            }.copy(selectedShapeId = null)
        }
    }

    fun setCurrentFrame(index: Int) {
        _state.update { current ->
            val maxIndex = current.project.frameCount - 1
            current.copy(currentFrameIndex = index.coerceIn(0, maxIndex), selectedShapeId = null)
        }
    }

    fun togglePlay() {
        _state.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun advancePlayback() {
        _state.update { current ->
            val frameCount = current.project.frameCount
            val nextIndex = (current.currentFrameIndex + 1) % frameCount
            current.copy(currentFrameIndex = nextIndex)
        }
    }

    fun addFrame() {
        recordHistory()
        _state.update { current -> current.withLayer(current.currentLayerIndex) { layer -> layer.appendFrame(isKeyframe = false) } }
    }

    fun addKeyframe() {
        recordHistory()
        _state.update { current -> current.withLayer(current.currentLayerIndex) { layer -> layer.appendFrame(isKeyframe = true) } }
    }

    fun addLayer() {
        recordHistory()
        _state.update { current ->
            val newLayer = Layer(id = "layer-${layerIdCounter++}", name = "Layer ${current.project.layers.size + 1}")
            current.copy(
                project = current.project.copy(layers = current.project.layers + newLayer),
                currentLayerIndex = current.project.layers.size
            )
        }
    }

    fun selectLayer(index: Int) {
        _state.update { it.copy(currentLayerIndex = index, selectedShapeId = null) }
    }

    fun toggleLayerVisibility(index: Int) {
        _state.update { current -> current.withLayer(index) { layer -> layer.copy(isVisible = !layer.isVisible) } }
    }

    fun setCanvasSize(size: IntSize) {
        _state.update { it.copy(canvasSize = size) }
    }

    fun setStatusMessage(message: String?) {
        _state.update { it.copy(statusMessage = message) }
    }

    fun importImage(pngBytes: ByteArray) {
        val canvasSize = _state.value.canvasSize
        val defaultSize = 160f
        val bitmap = try {
            decodePngToImageBitmap(pngBytes)
        } catch (e: Exception) {
            setStatusMessage("Could not read that image")
            return
        }
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
        val width = defaultSize
        val height = defaultSize * aspectRatio
        val topLeft = if (canvasSize.width > 0 && canvasSize.height > 0) {
            Offset((canvasSize.width - width) / 2f, (canvasSize.height - height) / 2f)
        } else {
            Offset.Zero
        }
        addShape(
            ImageShape(
                id = nextShapeId(),
                topLeft = topLeft,
                size = Size(width, height),
                image = bitmap,
                sourcePng = pngBytes
            )
        )
    }

    fun importSvgFile(svgText: String) {
        val shapes = try {
            importSvg(svgText, nextId = ::nextShapeId)
        } catch (e: Exception) {
            setStatusMessage("Could not read that SVG file")
            return
        }
        if (shapes.isEmpty()) {
            setStatusMessage("No shapes found in that SVG file")
            return
        }
        if (!canEditCurrentLayer()) return
        recordHistory()
        _state.update { current -> current.withCurrentFrame { frame -> frame.copy(shapes = frame.shapes + shapes) } }
    }

    fun serializeProjectXml(): String = writeProjectXml(_state.value.project)

    fun loadProjectXml(xml: String) {
        val project = try {
            parseProjectXml(xml)
        } catch (e: Exception) {
            setStatusMessage("Could not open that project file")
            return
        }
        loadProject(project)
    }

    fun loadProject(project: Project) {
        undoStack.clear()
        redoStack.clear()
        _state.update {
            EditorUiState(
                project = project,
                canvasSize = it.canvasSize
            )
        }
    }

    private fun Layer.appendFrame(isKeyframe: Boolean): Layer {
        val previousShapes = frames.lastOrNull()?.shapes ?: emptyList()
        val newFrame = Frame(index = frames.size, isKeyframe = isKeyframe, shapes = previousShapes)
        return copy(frames = frames + newFrame)
    }

    private fun EditorUiState.withLayer(layerIndex: Int, transform: (Layer) -> Layer): EditorUiState {
        val layers = project.layers.toMutableList()
        if (layerIndex !in layers.indices) return this
        layers[layerIndex] = transform(layers[layerIndex])
        return copy(project = project.copy(layers = layers))
    }

    private fun EditorUiState.withCurrentFrame(transform: (Frame) -> Frame): EditorUiState {
        return withLayer(currentLayerIndex) { layer ->
            val frames = layer.frames.toMutableList()
            if (currentFrameIndex !in frames.indices) return@withLayer layer
            frames[currentFrameIndex] = transform(frames[currentFrameIndex])
            layer.copy(frames = frames)
        }
    }
}
