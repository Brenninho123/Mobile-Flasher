package com.mobileflasher.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.export.ExportController
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.message
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.state.EditorController
import com.mobileflasher.app.state.EditorUiState
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    uiState: EditorUiState,
    controller: EditorController,
    exportController: ExportController,
    pickImage: () -> Unit,
    pickSvg: () -> Unit,
    pickAnimation: () -> Unit,
    pickProject: () -> Unit,
    onSaveProject: () -> Unit,
    onShareProject: () -> Unit,
    onHome: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onImportAnimation = pickAnimation,
                    onExportPng = {
                        exportController.exportCurrentFrameAsPng(
                            project = uiState.project,
                            frameIndex = uiState.currentFrameIndex,
                            width = uiState.canvasSize.width,
                            height = uiState.canvasSize.height
                        )
                    },
                    onExportGif = {
                        scope.launch {
                            controller.busy(message(StringKey.RenderingGif)) {
                                exportController.exportAnimationAsGif(
                                    project = uiState.project,
                                    width = uiState.canvasSize.width,
                                    height = uiState.canvasSize.height
                                )
                            }
                        }
                    },
                    onExportMp4 = {
                        scope.launch {
                            val exported = controller.busy(message(StringKey.EncodingVideo)) {
                                exportController.exportAnimationAsMp4(
                                    project = uiState.project,
                                    width = uiState.canvasSize.width,
                                    height = uiState.canvasSize.height
                                )
                            }
                            if (!exported) controller.setStatusMessage(message(StringKey.VideoExportUnavailable))
                        }
                    },
                    onExportFla = {
                        scope.launch {
                            exportController.exportProjectAsFla(
                                project = uiState.project,
                                width = uiState.canvasSize.width,
                                height = uiState.canvasSize.height
                            )
                        }
                    },
                    onConvertToAnimation = { controller.setMode(ProjectMode.ANIMATION) },
                    onSaveProject = onSaveProject,
                    onShareProject = onShareProject,
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
                        onToggleSnap = controller::toggleSnapToGrid,
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
                        onShapeErased = controller::eraseShape,
                        onStylePicked = controller::pickStyleFrom,
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
                if (uiState.project.mode == ProjectMode.ART) {
                    LayerStrip(
                        uiState = uiState,
                        modifier = Modifier.height(176.dp),
                        onSelectLayer = controller::selectLayer,
                        onAddLayer = controller::addLayer,
                        onDeleteLayer = controller::deleteLayer,
                        onToggleLayerVisibility = controller::toggleLayerVisibility,
                        onToggleLayerLock = controller::toggleLayerLock
                    )
                } else {
                    TimelinePanel(
                        uiState = uiState,
                        modifier = Modifier.height(232.dp),
                        onCellSelected = controller::selectCell,
                        onAddFrame = controller::addFrame,
                        onAddKeyframe = controller::addKeyframe,
                        onDuplicateFrame = controller::duplicateFrame,
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
        AnimatedVisibility(
            visible = uiState.busyMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BusyOverlay(uiState.busyMessage?.let { tr(it) } ?: "")
        }
    }
}

@Composable
private fun BusyOverlay(message: String) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = interaction, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            CircularProgressIndicator(color = BoltAmber)
            Text(text = message, style = MaterialTheme.typography.titleSmall)
        }
    }
}
