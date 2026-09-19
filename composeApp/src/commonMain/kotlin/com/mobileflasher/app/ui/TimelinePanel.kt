package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.state.EditorUiState
import com.mobileflasher.app.state.frameCount

private val CellWidth = 30.dp
private val CellHeight = 38.dp
private val RulerHeight = 24.dp
private val LayerHeaderWidth = 148.dp
private val FrameRates = listOf(8, 12, 24, 30, 60)

@Composable
fun TimelinePanel(
    uiState: EditorUiState,
    onCellSelected: (Int, Int) -> Unit,
    onAddFrame: () -> Unit,
    onAddKeyframe: () -> Unit,
    onDeleteFrame: () -> Unit,
    onTogglePlay: () -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (Int) -> Unit,
    onToggleLayerVisibility: (Int) -> Unit,
    onToggleLayerLock: (Int) -> Unit,
    onFrameRateChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val frameCount = uiState.project.frameCount
    val horizontalState = rememberScrollState()
    val verticalState = rememberScrollState()
    var viewportWidth by remember { mutableStateOf(0) }
    val cellPx = with(LocalDensity.current) { CellWidth.toPx() }.toInt()

    LaunchedEffect(uiState.currentFrameIndex, viewportWidth, cellPx) {
        val left = uiState.currentFrameIndex * cellPx
        val right = left + cellPx
        if (left < horizontalState.value) {
            horizontalState.scrollTo(left)
        } else if (viewportWidth > 0 && right > horizontalState.value + viewportWidth) {
            horizontalState.scrollTo(right - viewportWidth)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer)
    ) {
        HorizontalDivider(color = scheme.outlineVariant)
        TimelineToolbar(
            uiState = uiState,
            frameCount = frameCount,
            onTogglePlay = onTogglePlay,
            onAddFrame = onAddFrame,
            onAddKeyframe = onAddKeyframe,
            onDeleteFrame = onDeleteFrame,
            onAddLayer = onAddLayer,
            onDeleteLayer = { onDeleteLayer(uiState.currentLayerIndex) },
            onFrameRateChanged = onFrameRateChanged
        )
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(verticalState)
        ) {
            Column(modifier = Modifier.width(LayerHeaderWidth)) {
                Box(modifier = Modifier.height(RulerHeight).fillMaxWidth().background(scheme.surfaceContainerHigh))
                uiState.project.layers.forEachIndexed { index, layer ->
                    LayerHeader(
                        layer = layer,
                        isSelected = index == uiState.currentLayerIndex,
                        onSelect = { onCellSelected(index, uiState.currentFrameIndex) },
                        onToggleVisibility = { onToggleLayerVisibility(index) },
                        onToggleLock = { onToggleLayerLock(index) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportWidth = it.width }
                    .horizontalScroll(horizontalState)
            ) {
                Ruler(frameCount = frameCount, currentFrameIndex = uiState.currentFrameIndex) { index ->
                    onCellSelected(uiState.currentLayerIndex, index)
                }
                uiState.project.layers.forEachIndexed { layerIndex, layer ->
                    LayerTrack(
                        layer = layer,
                        frameCount = frameCount,
                        isSelectedLayer = layerIndex == uiState.currentLayerIndex,
                        currentFrameIndex = uiState.currentFrameIndex,
                        onCellClick = { frameIndex -> onCellSelected(layerIndex, frameIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineToolbar(
    uiState: EditorUiState,
    frameCount: Int,
    onTogglePlay: () -> Unit,
    onAddFrame: () -> Unit,
    onAddKeyframe: () -> Unit,
    onDeleteFrame: () -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: () -> Unit,
    onFrameRateChanged: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var fpsMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (uiState.isPlaying) scheme.secondary else scheme.primary)
                .clickable(role = Role.Button, onClick = onTogglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                tint = if (uiState.isPlaying) scheme.onSecondary else scheme.onPrimary
            )
        }
        Column(modifier = Modifier.width(74.dp).padding(start = 8.dp)) {
            Text(
                text = "${uiState.currentFrameIndex + 1} / $frameCount",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "frame",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        SectionDivider()
        PanelIconButton(Icons.Filled.Add, "Add frame", onAddFrame)
        PanelIconButton(Icons.Filled.Star, "Add keyframe", onAddKeyframe, tint = scheme.secondary)
        PanelIconButton(
            Icons.Filled.DeleteSweep,
            "Delete frame",
            onDeleteFrame,
            enabled = (uiState.project.layers.getOrNull(uiState.currentLayerIndex)?.frames?.size ?: 0) > 1
        )
        SectionDivider()
        PanelIconButton(Icons.Filled.Layers, "Add layer", onAddLayer)
        PanelIconButton(
            Icons.Filled.LayersClear,
            "Delete layer",
            onDeleteLayer,
            enabled = uiState.project.layers.size > 1
        )
        SectionDivider()
        Box {
            Text(
                text = "${uiState.project.frameRate} fps",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(scheme.surfaceContainerHigh)
                    .clickable(role = Role.Button) { fpsMenu = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
            DropdownMenu(
                expanded = fpsMenu,
                onDismissRequest = { fpsMenu = false },
                containerColor = scheme.surfaceContainerHigh
            ) {
                FrameRates.forEach { rate ->
                    DropdownMenuItem(
                        text = { Text("$rate fps") },
                        onClick = {
                            fpsMenu = false
                            onFrameRateChanged(rate)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Ruler(frameCount: Int, currentFrameIndex: Int, onFrameClick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.height(RulerHeight).background(scheme.surfaceContainerHigh)) {
        for (index in 0 until frameCount) {
            val isCurrent = index == currentFrameIndex
            Box(
                modifier = Modifier
                    .width(CellWidth)
                    .height(RulerHeight)
                    .background(if (isCurrent) scheme.primary else Color.Transparent)
                    .clickable { onFrameClick(index) },
                contentAlignment = Alignment.Center
            ) {
                val number = index + 1
                if (isCurrent || number == 1 || number % 5 == 0) {
                    Text(
                        text = "$number",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) scheme.onPrimary else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerHeader(
    layer: Layer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CellHeight)
            .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainer)
            .clickable(onClick = onSelect)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = layer.name,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = if (layer.isVisible) "Hide layer" else "Show layer",
            tint = if (layer.isVisible) scheme.onSurfaceVariant else scheme.outline,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onToggleVisibility)
                .padding(6.dp)
        )
        Icon(
            imageVector = if (layer.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = if (layer.isLocked) "Unlock layer" else "Lock layer",
            tint = if (layer.isLocked) scheme.secondary else scheme.outline,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onToggleLock)
                .padding(6.dp)
        )
    }
}

@Composable
private fun LayerTrack(
    layer: Layer,
    frameCount: Int,
    isSelectedLayer: Boolean,
    currentFrameIndex: Int,
    onCellClick: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.height(CellHeight)) {
        for (index in 0 until frameCount) {
            val frame = layer.frames.getOrNull(index)
            val isCurrentColumn = index == currentFrameIndex
            val isCurrentCell = isCurrentColumn && isSelectedLayer
            val base = when {
                frame == null -> scheme.surface
                frame.isKeyframe -> scheme.secondaryContainer.copy(alpha = 0.55f)
                else -> scheme.surfaceContainerHighest
            }
            Box(
                modifier = Modifier
                    .width(CellWidth)
                    .height(CellHeight)
                    .background(base)
                    .background(if (isCurrentColumn) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .border(0.5.dp, scheme.outlineVariant)
                    .then(
                        if (isCurrentCell) Modifier.border(2.dp, scheme.primary, RoundedCornerShape(3.dp)) else Modifier
                    )
                    .clickable { onCellClick(index) },
                contentAlignment = Alignment.Center
            ) {
                if (frame?.isKeyframe == true) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (layer.isVisible) scheme.secondary else scheme.outline, CircleShape)
                    )
                }
            }
        }
    }
}
