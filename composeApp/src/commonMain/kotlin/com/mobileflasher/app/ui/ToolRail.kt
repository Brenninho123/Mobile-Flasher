package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.state.EditorUiState

private data class ToolEntry(val tool: Tool, val icon: ImageVector, val label: String)

private val ToolEntries = listOf(
    ToolEntry(Tool.SELECT, Icons.Filled.NearMe, "Select"),
    ToolEntry(Tool.RECTANGLE, Icons.Filled.CropSquare, "Rectangle"),
    ToolEntry(Tool.ELLIPSE, Icons.Filled.Circle, "Ellipse"),
    ToolEntry(Tool.LINE, Icons.Filled.Remove, "Line"),
    ToolEntry(Tool.PEN, Icons.Filled.Edit, "Pencil")
)

@Composable
fun ToolRail(
    uiState: EditorUiState,
    onToolSelected: (Tool) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleOnionSkin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(scheme.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolEntries.forEach { entry ->
                PanelIconButton(
                    icon = entry.icon,
                    description = entry.label,
                    selected = uiState.selectedTool == entry.tool,
                    onClick = { onToolSelected(entry.tool) }
                )
            }
            HorizontalDivider(
                color = scheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            PanelIconButton(
                icon = Icons.Filled.Grid4x4,
                description = "Toggle grid",
                selected = uiState.gridVisible,
                onClick = onToggleGrid
            )
            PanelIconButton(
                icon = Icons.Filled.Opacity,
                description = "Toggle onion skin",
                selected = uiState.onionSkinEnabled,
                onClick = onToggleOnionSkin
            )
        }
        VerticalDividerLine()
    }
}

@Composable
private fun VerticalDividerLine() {
    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
