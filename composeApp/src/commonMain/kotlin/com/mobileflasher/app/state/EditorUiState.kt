package com.mobileflasher.app.state

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.model.Tool

data class EditorUiState(
    val project: Project = Project(name = "Untitled"),
    val currentLayerIndex: Int = 0,
    val currentFrameIndex: Int = 0,
    val selectedTool: Tool = Tool.RECTANGLE,
    val selectedShapeId: String? = null,
    val isPlaying: Boolean = false,
    val strokeColor: Color = Color(0xFF1B1B1B),
    val fillColor: Color? = Color(0xFF42A5F5),
    val strokeWidth: Float = 4f,
    val canvasSize: IntSize = IntSize.Zero,
    val statusMessage: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val onionSkinEnabled: Boolean = true,
    val gridVisible: Boolean = false
)

val Project.frameCount: Int
    get() = layers.maxOfOrNull { it.frames.size } ?: 1

val EditorUiState.currentLayer
    get() = project.layers.getOrNull(currentLayerIndex)

val EditorUiState.selectedShape
    get() = currentLayer?.frames?.getOrNull(currentFrameIndex)?.shapes?.firstOrNull { it.id == selectedShapeId }
