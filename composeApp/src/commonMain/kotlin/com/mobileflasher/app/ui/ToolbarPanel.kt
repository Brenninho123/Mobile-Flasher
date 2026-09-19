package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.model.Tool
import com.mobileflasher.app.state.EditorUiState

private val SwatchColors = listOf(
    Color(0xFF1B1B1B),
    Color(0xFFE53935),
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFFFA726),
    Color(0xFFAB47BC)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarPanel(
    uiState: EditorUiState,
    onToolSelected: (Tool) -> Unit,
    onStrokeColorSelected: (Color) -> Unit,
    onFillColorSelected: (Color?) -> Unit
) {
    TopAppBar(
        title = { Text("Mobile Flasher") },
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToolButton(Tool.SELECT, "Select", uiState.selectedTool, onToolSelected)
                ToolButton(Tool.RECTANGLE, "Rectangle", uiState.selectedTool, onToolSelected)
                ToolButton(Tool.ELLIPSE, "Ellipse", uiState.selectedTool, onToolSelected)
                ToolButton(Tool.LINE, "Line", uiState.selectedTool, onToolSelected)
                ToolButton(Tool.PEN, "Pen", uiState.selectedTool, onToolSelected)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            }
        }
    )
}

@Composable
private fun ToolButton(
    tool: Tool,
    label: String,
    selectedTool: Tool,
    onToolSelected: (Tool) -> Unit
) {
    val isSelected = tool == selectedTool
    val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Text(
        text = label.first().toString(),
        modifier = Modifier
            .clickable { onToolSelected(tool) }
            .background(background, CircleShape)
            .padding(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
            .size(20.dp)
            .clickable(onClick = onClick)
            .background(color, CircleShape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, CircleShape)
    ) {}
}
