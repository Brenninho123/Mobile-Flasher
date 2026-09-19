package com.mobileflasher.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mobileflasher.app.export.ExportController
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.FilePickerMode
import com.mobileflasher.app.platform.rememberFilePicker
import com.mobileflasher.app.platform.rememberFileSaver
import com.mobileflasher.app.state.EditorController
import kotlinx.coroutines.delay

private enum class Screen { HOME, EDITOR }

@Composable
fun App() {
    MobileFlasherTheme {
        val controller = remember { EditorController() }
        val uiState by controller.state.collectAsState()
        var screen by remember { mutableStateOf(Screen.HOME) }

        val saveBytes = rememberFileSaver()
        val exportController = remember(saveBytes) { ExportController(saveBytes) }

        val pickImage = rememberFilePicker(FilePickerMode.IMAGE) { bytes -> controller.importImage(bytes) }
        val pickSvg = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes -> controller.importSvgFile(bytes.decodeToString()) }
        val pickProject = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes ->
            controller.loadProjectXml(bytes.decodeToString())
            screen = Screen.EDITOR
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.isPlaying, uiState.project.frameRate) {
            if (uiState.isPlaying) {
                val frameDelayMillis = 1000L / uiState.project.frameRate.coerceAtLeast(1)
                while (true) {
                    delay(frameDelayMillis)
                    controller.advancePlayback()
                }
            }
        }

        LaunchedEffect(uiState.statusMessage) {
            val message = uiState.statusMessage
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                controller.setStatusMessage(null)
            }
        }

        when (screen) {
            Screen.HOME -> HomeScreen(
                onNewProject = {
                    controller.loadProject(Project(name = "Untitled"))
                    screen = Screen.EDITOR
                },
                onOpenProject = pickProject
            )
            Screen.EDITOR -> EditorScreen(
                uiState = uiState,
                controller = controller,
                exportController = exportController,
                pickImage = pickImage,
                pickSvg = pickSvg,
                pickProject = pickProject,
                onHome = { screen = Screen.HOME },
                snackbarHostState = snackbarHostState
            )
        }
    }
}
