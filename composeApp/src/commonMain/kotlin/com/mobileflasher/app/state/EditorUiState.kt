package com.mobileflasher.app.state

import androidx.compose.ui.graphics.Color
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
    val strokeWidth: Float = 4f
)

val Project.frameCount: Int
    get() = layers.maxOfOrNull { it.frames.size } ?: 1
