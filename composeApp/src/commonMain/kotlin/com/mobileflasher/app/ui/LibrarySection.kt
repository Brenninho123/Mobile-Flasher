package com.mobileflasher.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.i18n.LocalLocalizer
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.library.LibraryEntry
import com.mobileflasher.app.library.formatRelativeTime
import com.mobileflasher.app.platform.decodePngToImageBitmap

@Composable
fun LibrarySection(
    entries: List<LibraryEntry>,
    nowMillis: Long,
    loadThumbnail: (String) -> ByteArray?,
    onOpen: (LibraryEntry) -> Unit,
    onShare: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var pendingDelete by remember { mutableStateOf<LibraryEntry?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tr(StringKey.YourProjects),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = tr(StringKey.OfflineOnDevice),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        if (entries.isEmpty()) {
            Text(
                text = tr(StringKey.LibraryHint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        entries.forEach { entry ->
            ProjectCard(
                entry = entry,
                nowMillis = nowMillis,
                loadThumbnail = loadThumbnail,
                onOpen = { onOpen(entry) },
                onShare = { onShare(entry) },
                onDelete = { pendingDelete = entry }
            )
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = scheme.surfaceContainerHigh,
            title = { Text(tr(StringKey.DeleteProjectTitle)) },
            text = { Text(tr(StringKey.DeleteProjectBody, entry.name)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(entry)
                }) { Text(tr(StringKey.Delete), color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(tr(StringKey.Cancel)) } }
        )
    }
}

@Composable
private fun ProjectCard(
    entry: LibraryEntry,
    nowMillis: Long,
    loadThumbnail: (String) -> ByteArray?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val thumbnail = remember(entry.id, entry.modifiedMillis) {
        loadThumbnail(entry.id)?.let { bytes ->
            try {
                decodePngToImageBitmap(bytes)
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(76.dp)
                .height(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(androidx.compose.ui.graphics.Color.White)
                .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            } else {
                Icon(Icons.Filled.Draw, contentDescription = null, tint = BoltAmber)
            }
        }
        val frames = if (entry.frameCount == 1) tr(StringKey.FrameCountOne) else tr(StringKey.FrameCount, entry.frameCount)
        val layers = if (entry.layerCount == 1) tr(StringKey.LayerCountOne) else tr(StringKey.LayerCount, entry.layerCount)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$frames  |  $layers",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = formatRelativeTime(LocalLocalizer.current, nowMillis, entry.modifiedMillis),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        Box {
            PanelIconButton(Icons.Filled.MoreVert, tr(StringKey.ProjectOptions), onClick = { menuOpen = true })
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = scheme.surfaceContainerHigh
            ) {
                DropdownMenuItem(
                    text = { Text(tr(StringKey.ShareMflash)) },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text(tr(StringKey.Delete), color = scheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = scheme.error) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}
