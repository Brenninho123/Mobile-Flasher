package com.mobileflasher.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

@Composable
actual fun rememberDeviceLanguageTags(): List<String> =
    remember { NSLocale.preferredLanguages.mapNotNull { it as? String } }
