package com.mobileflasher.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mobileflasher.app.export.ExportController
import com.mobileflasher.app.fla.UnsupportedFlaFormatException
import com.mobileflasher.app.i18n.LocalLocalizer
import com.mobileflasher.app.i18n.Localizer
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.message
import com.mobileflasher.app.i18n.rememberDeviceLanguageTags
import com.mobileflasher.app.i18n.resolveLanguage
import com.mobileflasher.app.library.LibraryEntry
import com.mobileflasher.app.library.ProjectLibrary
import com.mobileflasher.app.mflash.MflashFormat
import com.mobileflasher.app.model.Layer
import com.mobileflasher.app.model.Project
import com.mobileflasher.app.platform.FilePickerMode
import com.mobileflasher.app.platform.IncomingFiles
import com.mobileflasher.app.platform.currentTimeMillis
import com.mobileflasher.app.platform.encodeToPng
import com.mobileflasher.app.platform.rememberFilePicker
import com.mobileflasher.app.platform.rememberFileSaver
import com.mobileflasher.app.platform.rememberFileStore
import com.mobileflasher.app.platform.rememberKeyValueStore
import com.mobileflasher.app.render.renderThumbnail
import com.mobileflasher.app.settings.SettingsController
import com.mobileflasher.app.state.EditorController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private enum class Screen { HOME, EDITOR }

private const val AutosaveDelayMillis = 1500L

@Composable
fun App() {
    val keyValueStore = rememberKeyValueStore()
    val settingsController = remember(keyValueStore) { SettingsController(keyValueStore) }
    val settings by settingsController.settings.collectAsState()
    val language = resolveLanguage(settings.language, rememberDeviceLanguageTags())
    val localizer = remember(language) { Localizer(language) }
    val currentLocalizer by rememberUpdatedState(localizer)
    CompositionLocalProvider(LocalLocalizer provides localizer) {
        MobileFlasherTheme(themeMode = settings.themeMode) {
            val controller = remember { EditorController { currentLocalizer } }
            val uiState by controller.state.collectAsState()
            var screen by remember { mutableStateOf(Screen.HOME) }
            val scope = rememberCoroutineScope()

            val fileStore = rememberFileStore()
            val library = remember(fileStore) { ProjectLibrary(fileStore, ::currentTimeMillis) }
            var entries by remember(library) { mutableStateOf(library.list()) }
            var currentId by remember { mutableStateOf<String?>(null) }
            val saveLock = remember { Mutex() }

            val saveBytes = rememberFileSaver()
            val exportController = remember(saveBytes) { ExportController(saveBytes) }

            fun thumbnailBytes(): ByteArray? {
                val state = controller.state.value
                val size = state.canvasSize
                if (size.width <= 0 || size.height <= 0) return null
                return try {
                    renderThumbnail(state.project, 0, size.width, size.height).encodeToPng()
                } catch (e: Exception) {
                    null
                }
            }

            suspend fun persist(): Boolean = saveLock.withLock {
                val project = controller.state.value.project
                val entry = withContext(Dispatchers.Default) {
                    library.save(currentId, project, thumbnailBytes())
                }
                if (entry != null) {
                    currentId = entry.id
                    entries = library.list()
                }
                entry != null
            }

            fun startEditing(project: Project, id: String?) {
                controller.loadProject(
                    project = project,
                    gridVisible = settings.gridByDefault,
                    snapToGrid = settings.snapByDefault,
                    onionSkinEnabled = settings.onionSkinByDefault
                )
                currentId = id
                screen = Screen.EDITOR
            }

            fun openBytes(bytes: ByteArray) {
                scope.launch {
                    var failure = StringKey.InvalidProjectFile
                    val opened = controller.busy(message(StringKey.OpeningProject)) {
                        withContext(Dispatchers.Default) {
                            try {
                                val result = library.importFile(bytes)
                                result?.let { it to library.loadProject(it.entry.id) }
                            } catch (e: UnsupportedFlaFormatException) {
                                failure = StringKey.UnsupportedFlaFormat
                                null
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    val project = opened?.second
                    if (opened == null || project == null) {
                        controller.setStatusMessage(message(failure))
                        return@launch
                    }
                    entries = library.list()
                    startEditing(project, opened.first.entry.id)
                    if (opened.first.skippedElements > 0) {
                        controller.setStatusMessage(message(StringKey.FlaSkippedElements, opened.first.skippedElements))
                    }
                }
            }

            fun openEntry(entry: LibraryEntry) {
                scope.launch {
                    val project = withContext(Dispatchers.Default) { library.loadProject(entry.id) }
                    if (project == null) {
                        controller.setStatusMessage(message(StringKey.CouldNotOpenProject, entry.name))
                        entries = library.list()
                    } else {
                        startEditing(project, entry.id)
                    }
                }
            }

            val pickImage = rememberFilePicker(FilePickerMode.IMAGE) { bytes -> controller.importImage(bytes) }
            val pickSvg = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes -> controller.importSvgFile(bytes.decodeToString()) }
            val pickAnimation = rememberFilePicker(FilePickerMode.MEDIA) { bytes -> scope.launch { controller.importAnimation(bytes) } }
            val pickProject = rememberFilePicker(FilePickerMode.DOCUMENT) { bytes -> openBytes(bytes) }

            val snackbarHostState = remember { SnackbarHostState() }
            val incoming by IncomingFiles.pending.collectAsState()

            LaunchedEffect(incoming) {
                val bytes = incoming
                if (bytes != null) {
                    IncomingFiles.clear()
                    openBytes(bytes)
                }
            }

            LaunchedEffect(screen, uiState.project, settings.autosave, currentId) {
                if (screen == Screen.EDITOR && settings.autosave && currentId != null) {
                    delay(AutosaveDelayMillis)
                    persist()
                }
            }

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
                    snackbarHostState.showSnackbar(currentLocalizer.text(message))
                    controller.setStatusMessage(null)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        settings = settings,
                        onSettingsChange = settingsController::update,
                        onSettingsReset = settingsController::reset,
                        onNewProject = { name, frameRate, mode ->
                            startEditing(
                                Project(
                                    name = name,
                                    frameRate = frameRate,
                                    mode = mode,
                                    layers = listOf(Layer(id = "layer-1", name = currentLocalizer.text(StringKey.DefaultLayerName, 1)))
                                ),
                                null
                            )
                            scope.launch { persist() }
                        },
                        onOpenProject = pickProject,
                        entries = entries,
                        nowMillis = currentTimeMillis(),
                        loadThumbnail = library::loadThumbnail,
                        onOpenEntry = ::openEntry,
                        onShareEntry = { entry ->
                            val bytes = library.loadFileBytes(entry.id)
                            if (bytes != null) {
                                exportController.exportProjectFile(bytes, entry.name)
                            } else {
                                controller.setStatusMessage(message(StringKey.CouldNotReadProject, entry.name))
                            }
                        },
                        onDeleteEntry = { entry ->
                            library.delete(entry.id)
                            if (currentId == entry.id) currentId = null
                            entries = library.list()
                        }
                    )
                    Screen.EDITOR -> EditorScreen(
                        uiState = uiState,
                        controller = controller,
                        exportController = exportController,
                        pickImage = pickImage,
                        pickSvg = pickSvg,
                        pickAnimation = pickAnimation,
                        pickProject = pickProject,
                        onSaveProject = {
                            scope.launch {
                                val saved = controller.busy(message(StringKey.Saving)) { persist() }
                                controller.setStatusMessage(message(if (saved) StringKey.SavedToLibrary else StringKey.CouldNotSaveProject))
                            }
                        },
                        onShareProject = {
                            scope.launch {
                                val project = uiState.project
                                val bytes = controller.busy(message(StringKey.PreparingFile)) {
                                    withContext(Dispatchers.Default) {
                                        MflashFormat.encode(project, thumbnailBytes(), currentTimeMillis())
                                    }
                                }
                                exportController.exportProjectFile(bytes, project.name)
                            }
                        },
                        onHome = {
                            if (uiState.isPlaying) controller.togglePlay()
                            screen = Screen.HOME
                            if (settings.autosave) scope.launch { persist() }
                        },
                        snackbarHostState = snackbarHostState
                    )
                }
                if (screen == Screen.HOME) {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .safeDrawingPadding()
                    )
                }
            }
        }
    }
}
