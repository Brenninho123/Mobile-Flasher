package com.mobileflasher.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun rememberDeviceLanguageTags(): List<String> {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val locales = configuration.locales
        List(locales.size()) { locales[it].toLanguageTag() }
    }
}
