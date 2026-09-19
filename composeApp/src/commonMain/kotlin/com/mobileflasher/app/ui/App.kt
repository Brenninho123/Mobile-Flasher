package com.mobileflasher.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mobileflasher.app.export.ExportController
import com.mobileflasher.app.platform.FilePickerMode
import com.mobileflasher.app.platform.rememberFilePicker
import com.mobileflasher.app.platform.rememberFileSaver
import com.mobileflasher.app.state.EditorController
import kotlinx.coroutines.delay

@Composable
fun App() {
    MobileFlasherTheme {
        val controller = remember { EditorController() }
        val uiState by controller.state.collectAsState()

        val saveBytes = rememberFileSaver()
        val exportController = remember(saveBytes) { ExportController(saveBytes) }

        val pickImage = rememberFilePicker(FilePickerMode.IMAGE) { bytes -> controller.importImage(bytes) }
        val pickSvg = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes -> controller.importSvgFile(bytes.decodeToString()) }
        val pickProject = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes -> controller.loadProjectXml(bytes.decodeToString()) }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.isPlaying, uiState.project.frameRate) {
            if (uiState.isPlaying) {
                val frameDelayMillis = 1000L / uiState.project.frameRate.coerceAtLeast(1)
                while (true) {
                    delay(frameDelayMillis)
                    controller.advancePlayback()
                }
            }
        }

        LaunchedEffect(uiState.statusMessage) {
            val message = uiState.statusMessage
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                controller.setStatusMessage(null)
            }
        }

        Scaffold(
            topBar = {
                Column {
                    TopBar(
                        onImportImage = pickImage,
                        onImportSvg = pickSvg,
                        onExportPng = {
                            exportController.exportCurrentFrameAsPng(
                                project = uiState.project,
                                frameIndex = uiState.currentFrameIndex,
                                width = uiState.canvasSize.width,
                                height = uiState.canvasSize.height
                            )
                        },
                        onExportGif = {
                            exportController.exportAnimationAsGif(
                                project = uiState.project,
                                width = uiState.canvasSize.width,
                                height = uiState.canvasSize.height
                            )
                        },
                        onSaveProject = {
                            exportController.exportProjectXml(controller.serializeProjectXml(), uiState.project.name)
                        },
                        onOpenProject = pickProject
                    )
                    ToolRail(
                        uiState = uiState,
                        onToolSelected = controller::selectTool,
                        onStrokeColorSelected = controller::setStrokeColor,
                        onFillColorSelected = controller::setFillColor,
                        onStrokeWidthChanged = controller::setStrokeWidth
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    onCanvasSizeChanged = controller::setCanvasSize,
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
