package com.mobileflasher.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val PaletteColors = listOf(
    Color(0xFF1B1B1B), Color(0xFF6B7280), Color(0xFFD1D5DB), Color(0xFFFFFFFF),
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFFB300), Color(0xFFFDD835),
    Color(0xFF9CCC65), Color(0xFF43A047), Color(0xFF00ACC1), Color(0xFF29B6F6),
    Color(0xFF1E66F5), Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFFEC407A)
)

private val FrameRateOptions = listOf(8, 12, 24, 30, 60)

@Composable
fun PanelIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else Color.Transparent,
        animationSpec = tween(180)
    )
    val content = tint ?: if (selected) scheme.onPrimaryContainer else scheme.onSurface
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun ColorChip(
    color: Color?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(scheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwatchDot(color = color, size = 22.dp)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
    }
}

@Composable
fun SwatchDot(color: Color?, size: Dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    if (color == null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, scheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .border(1.5.dp, scheme.outline, CircleShape)
        )
    }
}

@Composable
fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableStateOf(1f) }
    val currentOnChange by rememberUpdatedState(onValueChange)
    val thumbRadius = with(LocalDensity.current) { 11.dp.toPx() }
    fun toValue(x: Float): Float {
        val span = max(width - 2f * thumbRadius, 1f)
        return ((x - thumbRadius) / span).coerceIn(0f, 1f)
    }
    Box(
        modifier = modifier
            .height(28.dp)
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(Unit) { detectTapGestures { currentOnChange(toValue(it.x)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnChange(toValue(change.position.x))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(colors))
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            val span = max(size.width - 2f * thumbRadius, 1f)
            val center = Offset(thumbRadius + value.coerceIn(0f, 1f) * span, size.height / 2f)
            drawCircle(Color.White, radius = thumbRadius, center = center)
            drawCircle(Color(0x66000000), radius = thumbRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val maxC = max(red, max(green, blue))
    val minC = min(red, min(green, blue))
    val delta = maxC - minC
    val hue = when {
        delta == 0f -> 0f
        maxC == red -> 60f * (((green - blue) / delta) % 6f)
        maxC == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (maxC == 0f) 0f else delta / maxC
    return Triple(hue, saturation, maxC)
}

fun Color.toHex(): String {
    fun channel(value: Float) = (value * 255f).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
    return "#" + (channel(red) + channel(green) + channel(blue)).uppercase()
}

@Composable
fun ColorPickerDialog(
    title: String,
    initial: Color?,
    allowNone: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Color?) -> Unit
) {
    val seed = initial ?: PaletteColors[12]
    val seedHsv = remember(initial) { seed.toHsv() }
    var color by remember(initial) { mutableStateOf(seed) }
    var none by remember(initial) { mutableStateOf(initial == null && allowNone) }
    var hue by remember(initial) { mutableStateOf(seedHsv.first) }
    var saturation by remember(initial) { mutableStateOf(seedHsv.second) }
    var brightness by remember(initial) { mutableStateOf(seedHsv.third) }

    fun applyHsv() {
        color = Color.hsv(hue.coerceIn(0f, 359.99f), saturation, brightness)
        none = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwatchDot(color = if (none) null else color, size = 44.dp)
                    Column {
                        Text(
                            text = if (none) "No fill" else color.toHex(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Pick a preset or fine tune below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaletteColors.chunked(8).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { preset ->
                                val selected = !none && preset == color
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(preset)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val hsv = preset.toHsv()
                                            color = preset
                                            hue = hsv.first
                                            saturation = hsv.second
                                            brightness = hsv.third
                                            none = false
                                        }
                                )
                            }
                        }
                    }
                }
                LabeledSlider("Hue") {
                    GradientSlider(
                        value = hue / 360f,
                        onValueChange = { hue = it * 360f; applyHsv() },
                        colors = (0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }
                    )
                }
                LabeledSlider("Saturation") {
                    GradientSlider(
                        value = saturation,
                        onValueChange = { saturation = it; applyHsv() },
                        colors = listOf(
                            Color.hsv(hue.coerceIn(0f, 359.99f), 0f, brightness),
                            Color.hsv(hue.coerceIn(0f, 359.99f), 1f, brightness)
                        )
                    )
                }
                LabeledSlider("Brightness") {
                    GradientSlider(
                        value = brightness,
                        onValueChange = { brightness = it; applyHsv() },
                        colors = listOf(
                            Color.Black,
                            Color.hsv(hue.coerceIn(0f, 359.99f), saturation, 1f)
                        )
                    )
                }
                if (allowNone) {
                    FilterChip(
                        selected = none,
                        onClick = { none = !none },
                        label = { Text("No fill") },
                        leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (none) null else color) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LabeledSlider(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
fun ProjectDialog(
    title: String,
    initialName: String,
    initialFrameRate: Int?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var frameRate by remember { mutableStateOf(initialFrameRate ?: 24) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(48) },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (initialFrameRate != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Frame rate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FrameRateOptions.forEach { option ->
                                FilterChip(
                                    selected = option == frameRate,
                                    onClick = { frameRate = option },
                                    label = { Text("$option") }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), frameRate) },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
