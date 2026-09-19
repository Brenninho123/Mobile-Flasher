package com.mobileflasher.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.export.ExportController
import com.mobileflasher.app.state.EditorController
import com.mobileflasher.app.state.EditorUiState

@Composable
fun EditorScreen(
    uiState: EditorUiState,
    controller: EditorController,
    exportController: ExportController,
    pickImage: () -> Unit,
    pickSvg: () -> Unit,
    pickProject: () -> Unit,
    onHome: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(
                uiState = uiState,
                onHome = onHome,
                onRename = controller::setProjectName,
                onUndo = controller::undo,
                onRedo = controller::redo,
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ToolRail(
                    uiState = uiState,
                    onToolSelected = controller::selectTool,
                    onToggleGrid = controller::toggleGrid,
                    onToggleOnionSkin = controller::toggleOnionSkin
                )
                CanvasScreen(
                    uiState = uiState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    onShapeCreated = controller::addShape,
                    onShapeSelected = controller::selectShape,
                    onShapeMoved = controller::moveShape,
                    onShapeResized = controller::resizeShape,
                    onBeginMoveGesture = controller::beginMoveGesture,
                    onCanvasSizeChanged = controller::setCanvasSize,
                    nextShapeId = controller::nextShapeId
                )
            }
            PropertiesBar(
                uiState = uiState,
                onStrokeColorSelected = controller::setStrokeColor,
                onFillColorSelected = controller::setFillColor,
                onStrokeWidthChanged = controller::setStrokeWidth,
                onStrokeWidthEditEnd = controller::endStrokeWidthEdit,
                onDuplicate = controller::duplicateSelectedShape,
                onBringToFront = controller::bringSelectedToFront,
                onSendToBack = controller::sendSelectedToBack,
                onDelete = controller::deleteSelectedShape
            )
            TimelinePanel(
                uiState = uiState,
                modifier = Modifier.height(232.dp),
                onCellSelected = controller::selectCell,
                onAddFrame = controller::addFrame,
                onAddKeyframe = controller::addKeyframe,
                onDeleteFrame = controller::deleteCurrentFrame,
                onTogglePlay = controller::togglePlay,
                onAddLayer = controller::addLayer,
                onDeleteLayer = controller::deleteLayer,
                onToggleLayerVisibility = controller::toggleLayerVisibility,
                onToggleLayerLock = controller::toggleLayerLock,
                onFrameRateChanged = controller::setFrameRate
            )
        }
    }
}
