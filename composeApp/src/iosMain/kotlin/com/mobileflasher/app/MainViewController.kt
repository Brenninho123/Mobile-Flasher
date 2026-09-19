package com.mobileflasher.app

import androidx.compose.ui.window.ComposeUIViewController
import com.mobileflasher.app.ui.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
