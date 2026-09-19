package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.state.EditorUiState

private val SwatchColors = listOf(
    Color(0xFF1B1B1B),
    Color(0xFFE53935),
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFFFA726),
    Color(0xFFAB47BC),
    Color(0xFFFFFFFF)
)

@Composable
fun ToolRail(
    uiState: EditorUiState,
    onToolSelected: (Tool) -> Unit,
    onStrokeColorSelected: (Color) -> Unit,
    onFillColorSelected: (Color?) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleOnionSkin: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onUndo, enabled = uiState.canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = onRedo, enabled = uiState.canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
        }
        IconButton(onClick = onToggleOnionSkin) {
            Icon(
                Icons.Filled.Opacity,
                contentDescription = "Toggle onion skin",
                tint = if (uiState.onionSkinEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        ToolButton(Tool.SELECT, Icons.Filled.NearMe, "Select", uiState.selectedTool, onToolSelected)
        ToolButton(Tool.RECTANGLE, Icons.Filled.CropSquare, "Rectangle", uiState.selectedTool, onToolSelected)
        ToolButton(Tool.ELLIPSE, Icons.Filled.Circle, "Ellipse", uiState.selectedTool, onToolSelected)
        ToolButton(Tool.LINE, Icons.Filled.Remove, "Line", uiState.selectedTool, onToolSelected)
        ToolButton(Tool.PEN, Icons.Filled.Edit, "Pen", uiState.selectedTool, onToolSelected)

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SwatchColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = uiState.strokeColor == color,
                    onClick = {
                        onStrokeColorSelected(color)
                        onFillColorSelected(color)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text("Width", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = uiState.strokeWidth,
            onValueChange = onStrokeWidthChanged,
            valueRange = 1f..20f,
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
private fun ToolButton(
    tool: Tool,
    icon: ImageVector,
    label: String,
    selectedTool: Tool,
    onToolSelected: (Tool) -> Unit
) {
    val isSelected = tool == selectedTool
    val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier
            .clickable { onToolSelected(tool) }
            .background(background, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .size(22.dp)
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray
    Row(
        modifier = Modifier
            .size(22.dp)
            .clickable(onClick = onClick)
            .background(color, CircleShape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, CircleShape)
    ) {}
}
