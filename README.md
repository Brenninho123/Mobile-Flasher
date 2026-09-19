# Mobile Flasher

A vector drawing and frame-based animation editor for mobile, inspired by Adobe Flash.
Built with Kotlin Multiplatform and Compose Multiplatform, targeting Android and iOS
from a single shared codebase.

## Features (base)

- Vector drawing canvas with Rectangle, Ellipse, Line, Pen (freehand) and Select tools
- Layers with visibility toggles
- Frame-based timeline with keyframes, playback, and per-layer frame strips
- Shape selection and move-by-drag

## Project structure

```text
composeApp/
  src/
    commonMain/   # shared UI (Compose) and editor logic
      kotlin/com/mobileflasher/app/
        model/    # Project, Layer, Frame, VectorShape, Tool
        state/    # EditorController (state holder) and EditorUiState
        ui/       # App, CanvasScreen, TimelinePanel, ToolbarPanel, Theme
    androidMain/  # Android entry point (MainActivity)
    iosMain/      # iOS entry point (MainViewController)
iosApp/           # iOS app shell (SwiftUI)
```

## Running

### Android

```bash
./gradlew :composeApp:installDebug
```

Requires the Android SDK (`local.properties` with `sdk.dir` must point to it).

### iOS

Building for iOS requires Xcode on macOS. Generate the `iosApp.xcodeproj` with Xcode
(or the JetBrains KMP wizard) using the existing `iosApp/iosApp` sources
(`iOSApp.swift`, `ContentView.swift`, `Info.plist`) as the app target, and link it
against the `ComposeApp` framework produced by `:composeApp`.

## Architecture notes

- `EditorController` holds a single `StateFlow<EditorUiState>` and exposes intent
  functions (`addShape`, `moveShape`, `addFrame`, `addKeyframe`, `togglePlay`, ...).
  All UI reads from this state and never mutates the model directly.
- Each `Layer` holds an ordered list of `Frame`s. A new frame duplicates the shapes of
  the previous frame; marking a frame as a keyframe is what distinguishes an editable
  snapshot from a held frame, matching the Flash timeline model.
