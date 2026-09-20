package com.mobileflasher.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.settings.AppSettings
import com.mobileflasher.app.settings.ThemeMode

private val FrameRateChoices = listOf(8, 12, 24, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset) { Text("Reset") }
            }
            Spacer(Modifier.height(8.dp))

            SectionTitle("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onChange { it.copy(themeMode = mode) } },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                }
                            )
                        }
                    )
                }
            }
            SwitchRow(
                title = "Reduce motion",
                subtitle = "Turns off the animated home screen",
                checked = settings.reduceMotion,
                onCheckedChange = { value -> onChange { it.copy(reduceMotion = value) } }
            )

            SheetDivider()
            SectionTitle("Library")
            SwitchRow(
                title = "Autosave",
                subtitle = "Keep projects saved on this device as you draw",
                checked = settings.autosave,
                onCheckedChange = { value -> onChange { it.copy(autosave = value) } }
            )

            SectionTitle("New projects")
            Text(
                text = "Frame rate",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FrameRateChoices.forEach { rate ->
                    FilterChip(
                        selected = settings.defaultFrameRate == rate,
                        onClick = { onChange { it.copy(defaultFrameRate = rate) } },
                        label = { Text("$rate") }
                    )
                }
            }
            SwitchRow(
                title = "Show grid",
                subtitle = "Start every project with the grid visible",
                checked = settings.gridByDefault,
                onCheckedChange = { value -> onChange { it.copy(gridByDefault = value) } }
            )
            SwitchRow(
                title = "Snap to grid",
                subtitle = "Align shapes to the grid while drawing",
                checked = settings.snapByDefault,
                onCheckedChange = { value -> onChange { it.copy(snapByDefault = value) } }
            )
            SwitchRow(
                title = "Onion skin",
                subtitle = "Show neighbouring frames faintly",
                checked = settings.onionSkinByDefault,
                onCheckedChange = { value -> onChange { it.copy(onionSkinByDefault = value) } }
            )

            SheetDivider()
            SectionTitle("About")
            Text("Mobile Flasher 1.0", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Vector drawing and frame animation for your phone",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BoltAmber, checkedThumbColor = BoltInk)
        )
    }
}
