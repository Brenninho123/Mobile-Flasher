package com.mobileflasher.app.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.mobileflasher.app.model.Frame
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.model.VectorShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditorController {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var shapeIdCounter = 0
    private var layerIdCounter = 1

    fun nextShapeId(): String = "shape-${shapeIdCounter++}"

    fun selectTool(tool: Tool) {
        _state.update { it.copy(selectedTool = tool, selectedShapeId = null) }
    }

    fun setStrokeColor(color: Color) {
        _state.update { it.copy(strokeColor = color) }
    }

    fun setFillColor(color: Color?) {
        _state.update { it.copy(fillColor = color) }
    }

    fun selectShape(shapeId: String?) {
        _state.update { it.copy(selectedShapeId = shapeId) }
    }

    fun addShape(shape: VectorShape) {
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
        _state.update { current -> current.withLayer(current.currentLayerIndex) { layer -> layer.appendFrame(isKeyframe = false) } }
    }

    fun addKeyframe() {
        _state.update { current -> current.withLayer(current.currentLayerIndex) { layer -> layer.appendFrame(isKeyframe = true) } }
    }

    fun addLayer() {
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
