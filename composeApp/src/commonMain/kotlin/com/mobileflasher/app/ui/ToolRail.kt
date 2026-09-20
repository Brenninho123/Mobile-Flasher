package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.state.EditorUiState

private data class ToolEntry(val tool: Tool, val icon: ImageVector)

private val EditTools = listOf(
    ToolEntry(Tool.SELECT, Icons.Filled.NearMe),
    ToolEntry(Tool.ERASER, Icons.Filled.CleaningServices),
    ToolEntry(Tool.EYEDROPPER, Icons.Filled.Colorize)
)

private val DrawTools = listOf(
    ToolEntry(Tool.PEN, Icons.Filled.Edit),
    ToolEntry(Tool.LINE, Icons.Filled.Remove),
    ToolEntry(Tool.RECTANGLE, Icons.Filled.CropSquare),
    ToolEntry(Tool.ELLIPSE, Icons.Filled.Circle)
)

@Composable
fun ToolRail(
    uiState: EditorUiState,
    onToolSelected: (Tool) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit,
    onToggleOnionSkin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(58.dp)
                .fillMaxHeight()
                .background(scheme.surfaceContainerLow)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditTools.forEach { entry ->
                ToolButton(entry, uiState.selectedTool == entry.tool, onToolSelected)
            }
            RailDivider()
            DrawTools.forEach { entry ->
                ToolButton(entry, uiState.selectedTool == entry.tool, onToolSelected)
            }
            RailDivider()
            PanelIconButton(
                icon = Icons.Filled.Grid4x4,
                description = tr(StringKey.ToggleGrid),
                selected = uiState.gridVisible,
                onClick = onToggleGrid
            )
            PanelIconButton(
                icon = Icons.Filled.GridOn,
                description = tr(StringKey.SnapToGrid),
                selected = uiState.snapToGrid,
                onClick = onToggleSnap
            )
            if (uiState.project.mode == ProjectMode.ANIMATION) {
                PanelIconButton(
                    icon = Icons.Filled.Opacity,
                    description = tr(StringKey.ToggleOnionSkin),
                    selected = uiState.onionSkinEnabled,
                    onClick = onToggleOnionSkin
                )
            }
        }
        VerticalDivider(color = scheme.outlineVariant)
    }
}

@Composable
private fun ToolButton(entry: ToolEntry, selected: Boolean, onToolSelected: (Tool) -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .background(BoltAmber, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            )
        }
        PanelIconButton(
            icon = entry.icon,
            description = tr(entry.tool.labelKey()),
            selected = selected,
            onClick = { onToolSelected(entry.tool) },
            modifier = Modifier.padding(start = 9.dp)
        )
    }
}

@Composable
private fun RailDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}
