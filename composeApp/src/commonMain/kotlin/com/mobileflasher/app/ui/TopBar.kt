package com.mobileflasher.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onHome: () -> Unit,
    onImportImage: () -> Unit,
    onImportSvg: () -> Unit,
    onExportPng: () -> Unit,
    onExportGif: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Mobile Flasher") },
        navigationIcon = {
            IconButton(onClick = onHome) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Import image") },
                    leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                    onClick = { menuExpanded = false; onImportImage() }
                )
                DropdownMenuItem(
                    text = { Text("Import SVG") },
                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    onClick = { menuExpanded = false; onImportSvg() }
                )
                DropdownMenuItem(
                    text = { Text("Open project") },
                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    onClick = { menuExpanded = false; onOpenProject() }
                )
                DropdownMenuItem(
                    text = { Text("Save project") },
                    leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
                    onClick = { menuExpanded = false; onSaveProject() }
                )
                DropdownMenuItem(
                    text = { Text("Export PNG") },
                    leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    onClick = { menuExpanded = false; onExportPng() }
                )
                DropdownMenuItem(
                    text = { Text("Export GIF") },
                    leadingIcon = { Icon(Icons.Filled.Movie, contentDescription = null) },
                    onClick = { menuExpanded = false; onExportGif() }
                )
            }
        }
    )
}
