package com.mobileflasher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.state.EditorUiState

@Composable
fun TopBar(
    uiState: EditorUiState,
    onHome: () -> Unit,
    onRename: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onImportImage: () -> Unit,
    onImportSvg: () -> Unit,
    onImportAnimation: () -> Unit,
    onExportPng: () -> Unit,
    onExportGif: () -> Unit,
    onExportMp4: () -> Unit,
    onExportFla: () -> Unit,
    onConvertToAnimation: () -> Unit,
    onSaveProject: () -> Unit,
    onShareProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainer)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = tr(StringKey.Home),
                onClick = onHome
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .clickable { renaming = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BoltInk, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Draw, contentDescription = null, tint = BoltAmber, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = uiState.project.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = tr(StringKey.RenameProject),
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = projectSummary(uiState),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            PanelIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                description = tr(StringKey.Undo),
                onClick = onUndo,
                enabled = uiState.canUndo
            )
            PanelIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                description = tr(StringKey.Redo),
                onClick = onRedo,
                enabled = uiState.canRedo
            )
            Box {
                PanelIconButton(
                    icon = Icons.Filled.MoreVert,
                    description = tr(StringKey.Menu),
                    onClick = { menuExpanded = true }
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = scheme.surfaceContainerHigh
                ) {
                    val animated = uiState.project.mode == ProjectMode.ANIMATION
                    MenuHeader(tr(StringKey.MenuImport))
                    MenuEntry(tr(StringKey.ImportImage), Icons.Filled.Image) { menuExpanded = false; onImportImage() }
                    MenuEntry(tr(StringKey.ImportSvg), Icons.Filled.FolderOpen) { menuExpanded = false; onImportSvg() }
                    if (animated) {
                        MenuEntry(tr(StringKey.ImportAnimation), Icons.Filled.VideoLibrary) { menuExpanded = false; onImportAnimation() }
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    MenuHeader(tr(StringKey.MenuProject))
                    MenuEntry(tr(StringKey.OpenProjectFile), Icons.Filled.FolderOpen) { menuExpanded = false; onOpenProject() }
                    MenuEntry(tr(StringKey.SaveToLibrary), Icons.Filled.Save) { menuExpanded = false; onSaveProject() }
                    MenuEntry(tr(StringKey.ShareMflashFile), Icons.Filled.Share) { menuExpanded = false; onShareProject() }
                    if (!animated) {
                        MenuEntry(tr(StringKey.ConvertToAnimation), Icons.Filled.Movie) { menuExpanded = false; onConvertToAnimation() }
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    MenuHeader(tr(StringKey.MenuExport))
                    MenuEntry(tr(StringKey.ExportPng), Icons.Filled.FileDownload) { menuExpanded = false; onExportPng() }
                    if (animated) {
                        MenuEntry(tr(StringKey.ExportGif), Icons.Filled.Movie) { menuExpanded = false; onExportGif() }
                        MenuEntry(tr(StringKey.ExportMp4), Icons.Filled.Videocam) { menuExpanded = false; onExportMp4() }
                    }
                    MenuEntry(tr(StringKey.ExportFla), Icons.Filled.Save) { menuExpanded = false; onExportFla() }
                }
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }

    if (renaming) {
        ProjectDialog(
            title = tr(StringKey.RenameProject),
            initialName = uiState.project.name,
            initialFrameRate = null,
            confirmLabel = tr(StringKey.Save),
            onDismiss = { renaming = false },
            onConfirm = { name, _ ->
                renaming = false
                onRename(name)
            }
        )
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun MenuEntry(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick
    )
}

@Composable
private fun projectSummary(uiState: EditorUiState): String {
    val layers = uiState.project.layers.size
    if (uiState.project.mode == ProjectMode.ART) {
        return if (layers == 1) tr(StringKey.LayerCountOne) else tr(StringKey.LayerCount, layers)
    }
    val rate = uiState.project.frameRate
    return if (layers == 1) tr(StringKey.ProjectSummaryOneLayer, rate) else tr(StringKey.ProjectSummary, rate, layers)
}
