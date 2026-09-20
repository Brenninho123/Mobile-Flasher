package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.state.EditorUiState

@Composable
fun LayerStrip(
    uiState: EditorUiState,
    onSelectLayer: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (Int) -> Unit,
    onToggleLayerVisibility: (Int) -> Unit,
    onToggleLayerLock: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer)
    ) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PanelIconButton(Icons.Filled.Layers, tr(StringKey.AddLayer), onAddLayer)
            PanelIconButton(
                Icons.Filled.LayersClear,
                tr(StringKey.DeleteLayer),
                { onDeleteLayer(uiState.currentLayerIndex) },
                enabled = uiState.project.layers.size > 1
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (uiState.project.layers.size == 1) tr(StringKey.LayerCountOne) else tr(StringKey.LayerCount, uiState.project.layers.size),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = scheme.outlineVariant)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            uiState.project.layers.forEachIndexed { index, layer ->
                LayerHeader(
                    layer = layer,
                    isSelected = index == uiState.currentLayerIndex,
                    onSelect = { onSelectLayer(index) },
                    onToggleVisibility = { onToggleLayerVisibility(index) },
                    onToggleLock = { onToggleLayerLock(index) }
                )
            }
        }
    }
}
