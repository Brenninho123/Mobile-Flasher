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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            TextButton(onClick = onTogglePlay) { Text(if (uiState.isPlaying) "Pause" else "Play") }
            TextButton(onClick = onAddFrame) { Text("+ Frame") }
            TextButton(onClick = onAddKeyframe) { Text("+ Keyframe") }
            TextButton(onClick = onAddLayer) { Text("+ Layer") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Frame ${uiState.currentFrameIndex + 1} / ${uiState.project.frameCount}",
                modifier = Modifier.padding(end = 8.dp)
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
            .height(32.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .width(120.dp)
                .clickable(onClick = onLayerClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = layer.name, maxLines = 1)
            Text(
                text = if (layer.isVisible) "o" else "x",
                modifier = Modifier.clickable(onClick = onVisibilityToggle)
            )
        }
        LazyRow {
            itemsIndexed(layer.frames) { index, frame ->
                val isCurrent = index == currentFrameIndex
                Row(
                    modifier = Modifier
                        .size(24.dp)
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
