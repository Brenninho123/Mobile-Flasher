package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.state.EditorUiState
import com.mobileflasher.app.state.selectedShape
import kotlin.math.roundToInt

private enum class ColorTarget { STROKE, FILL }

@Composable
fun PropertiesBar(
    uiState: EditorUiState,
    onStrokeColorSelected: (Color) -> Unit,
    onFillColorSelected: (Color?) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onStrokeWidthEditEnd: () -> Unit,
    onDuplicate: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onDelete: () -> Unit
) {
    var pickerTarget by remember { mutableStateOf<ColorTarget?>(null) }
    val scheme = MaterialTheme.colorScheme
    val hasSelection = uiState.selectedShape != null

    Column(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainer)) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorChip(
                color = uiState.strokeColor,
                label = "Stroke ${uiState.strokeColor.toHex()}",
                onClick = { pickerTarget = ColorTarget.STROKE }
            )
            ColorChip(
                color = uiState.fillColor,
                label = "Fill ${uiState.fillColor?.toHex() ?: "None"}",
                onClick = { pickerTarget = ColorTarget.FILL }
            )
            SectionDivider()
            Text(
                text = "Width",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Slider(
                value = uiState.strokeWidth,
                onValueChange = onStrokeWidthChanged,
                onValueChangeFinished = onStrokeWidthEditEnd,
                valueRange = 1f..32f,
                modifier = Modifier.width(140.dp)
            )
            Text(
                text = "${uiState.strokeWidth.roundToInt()} px",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(38.dp)
            )
            if (hasSelection) {
                SectionDivider()
                PanelIconButton(Icons.Filled.ContentCopy, "Duplicate", onDuplicate)
                PanelIconButton(Icons.Filled.FlipToFront, "Bring to front", onBringToFront)
                PanelIconButton(Icons.Filled.FlipToBack, "Send to back", onSendToBack)
                PanelIconButton(Icons.Filled.Delete, "Delete", onDelete, tint = scheme.error)
            }
        }
    }

    when (pickerTarget) {
        ColorTarget.STROKE -> ColorPickerDialog(
            title = "Stroke color",
            initial = uiState.strokeColor,
            allowNone = false,
            onDismiss = { pickerTarget = null },
            onConfirm = { color ->
                pickerTarget = null
                if (color != null) onStrokeColorSelected(color)
            }
        )
        ColorTarget.FILL -> ColorPickerDialog(
            title = "Fill color",
            initial = uiState.fillColor,
            allowNone = true,
            onDismiss = { pickerTarget = null },
            onConfirm = { color ->
                pickerTarget = null
                onFillColorSelected(color)
            }
        )
        null -> Unit
    }
}
