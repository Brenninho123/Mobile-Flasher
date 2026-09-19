package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.state.EditorUiState
import com.mobileflasher.app.state.frameCount

@Composable
fun TimelinePanel(
    uiState: EditorUiState,
    onFrameSelected: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onAddKeyframe: () -> Unit,
    onTogglePlay: () -> Unit,
    onLayerSelected: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onToggleLayerVisibility: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = onAddFrame) {
                Icon(Icons.Filled.Add, contentDescription = "Add frame")
            }
            IconButton(onClick = onAddKeyframe) {
                Icon(Icons.Filled.Star, contentDescription = "Add keyframe")
            }
            IconButton(onClick = onAddLayer) {
                Icon(Icons.Filled.Layers, contentDescription = "Add layer")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Frame ${uiState.currentFrameIndex + 1} / ${uiState.project.frameCount}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        LazyColumn {
            itemsIndexed(uiState.project.layers) { index, layer ->
                LayerRow(
                    layer = layer,
                    isSelected = index == uiState.currentLayerIndex,
                    currentFrameIndex = uiState.currentFrameIndex,
                    onLayerClick = { onLayerSelected(index) },
                    onFrameClick = onFrameSelected,
                    onVisibilityToggle = { onToggleLayerVisibility(index) }
                )
            }
        }
    }
}

@Composable
private fun LayerRow(
    layer: Layer,
    isSelected: Boolean,
    currentFrameIndex: Int,
    onLayerClick: () -> Unit,
    onFrameClick: (Int) -> Unit,
    onVisibilityToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .width(128.dp)
                .clickable(onClick = onLayerClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = layer.name, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            Icon(
                imageVector = if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = if (layer.isVisible) "Hide layer" else "Show layer",
                modifier = Modifier.size(18.dp).clickable(onClick = onVisibilityToggle)
            )
        }
        LazyRow {
            itemsIndexed(layer.frames) { index, frame ->
                val isCurrent = index == currentFrameIndex
                Row(
                    modifier = Modifier
                        .size(26.dp)
                        .padding(1.dp)
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
                        )
                        .border(width = 1.dp, color = Color.Gray)
                        .clickable { onFrameClick(index) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (frame.isKeyframe) {
                        Row(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        ) {}
                    }
                }
            }
        }
    }
}
