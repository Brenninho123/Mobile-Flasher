package com.mobileflasher.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mobileflasher.app.state.EditorController
import kotlinx.coroutines.delay

@Composable
fun App() {
    MobileFlasherTheme {
        val controller = remember { EditorController() }
        val uiState by controller.state.collectAsState()

        LaunchedEffect(uiState.isPlaying, uiState.project.frameRate) {
            if (uiState.isPlaying) {
                val frameDelayMillis = (1000L / uiState.project.frameRate.coerceAtLeast(1))
                while (true) {
                    delay(frameDelayMillis)
                    controller.advancePlayback()
                }
            }
        }

        Scaffold(
            topBar = {
                ToolbarPanel(
                    uiState = uiState,
                    onToolSelected = controller::selectTool,
                    onStrokeColorSelected = controller::setStrokeColor,
                    onFillColorSelected = controller::setFillColor
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                CanvasScreen(
                    uiState = uiState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onShapeCreated = controller::addShape,
                    onShapeSelected = controller::selectShape,
                    onShapeMoved = controller::moveShape,
                    nextShapeId = controller::nextShapeId
                )
                TimelinePanel(
                    uiState = uiState,
                    onFrameSelected = controller::setCurrentFrame,
                    onAddFrame = controller::addFrame,
                    onAddKeyframe = controller::addKeyframe,
                    onTogglePlay = controller::togglePlay,
                    onLayerSelected = controller::selectLayer,
                    onAddLayer = controller::addLayer,
                    onToggleLayerVisibility = controller::toggleLayerVisibility
                )
            }
        }
    }
}
