package com.mobileflasher.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobileflasher.app.i18n.StringKey
import com.mobileflasher.app.i18n.tr
import com.mobileflasher.app.library.LibraryEntry
import com.mobileflasher.app.model.ProjectMode
import com.mobileflasher.app.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val OrbCount = 7
private const val SketchLoopMillis = 4200
private val BoltPink = Color(0xFFFF5C8A)
private val BoltSky = Color(0xFF4DB8FF)

@Composable
fun HomeScreen(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onSettingsReset: () -> Unit,
    onNewProject: (String, Int, ProjectMode) -> Unit,
    onOpenProject: () -> Unit,
    entries: List<LibraryEntry>,
    nowMillis: Long,
    loadThumbnail: (String) -> ByteArray?,
    onOpenEntry: (LibraryEntry) -> Unit,
    onShareEntry: (LibraryEntry) -> Unit,
    onDeleteEntry: (LibraryEntry) -> Unit
) {
    var newProjectMode by remember { mutableStateOf<ProjectMode?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val animate = !settings.reduceMotion
    var entered by remember { mutableStateOf(!animate) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { entered = true }

    val staticState = remember { mutableStateOf(0.5f) }

    val drift: State<Float> = if (!animate) staticState else rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing))
    )
    val breathe: State<Float> = if (!animate) staticState else rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawBehind {
                val glowCenter = Offset(size.width / 2f, size.height * (0.16f + 0.03f * breathe.value))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(BoltAmber.copy(alpha = 0.14f + 0.08f * breathe.value), Color.Transparent),
                        center = glowCenter,
                        radius = size.maxDimension * (0.5f + 0.08f * breathe.value)
                    )
                )
                val tints = listOf(BoltAmber, BoltSky, BoltPink)
                for (index in 0 until OrbCount) {
                    val angle = (drift.value + index.toFloat() / OrbCount) * 2f * PI.toFloat()
                    val orbit = 0.18f + 0.07f * (index % 3)
                    val cx = size.width * (0.5f + orbit * 1.6f * cos(angle + index))
                    val cy = size.height * (0.5f + orbit * 1.9f * sin(angle * 0.8f + index * 1.7f))
                    val radius = size.minDimension * (0.05f + 0.025f * (index % 4))
                    val tint = tints[index % tints.size]
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius * 2.4f
                        ),
                        radius = radius * 2.4f,
                        center = Offset(cx, cy)
                    )
                }
            }
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 420.dp)
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Reveal(entered, 0, animate) {
                AnimatedLogo(animate)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Reveal(entered, 120, animate) {
                Text(
                    text = "Mobile Flasher",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(scheme.onBackground, BoltAmber, scheme.onBackground),
                            start = Offset(0f, 0f),
                            end = Offset(900f * (0.4f + breathe.value), 0f)
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Reveal(entered, 200, animate) {
                Text(
                    text = tr(StringKey.HomeTagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Reveal(entered, 300, animate) {
                SketchPreview(animate)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Reveal(entered, 420, animate) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tr(StringKey.NewProject),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModeCard(
                            icon = Icons.Filled.Brush,
                            title = tr(StringKey.ModeArt),
                            hint = tr(StringKey.ModeArtHint),
                            accent = BoltPink,
                            onClick = { newProjectMode = ProjectMode.ART },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        ModeCard(
                            icon = Icons.Filled.Movie,
                            title = tr(StringKey.ModeAnimation),
                            hint = tr(StringKey.ModeAnimationHint),
                            accent = BoltAmber,
                            onClick = { newProjectMode = ProjectMode.ANIMATION },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Reveal(entered, 500, animate) {
                PressableButton(
                    onClick = onOpenProject,
                    outlined = true,
                    icon = Icons.Filled.FolderOpen,
                    label = tr(StringKey.OpenProjectFile)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Reveal(entered, 560, animate) {
                LibrarySection(
                    entries = entries,
                    nowMillis = nowMillis,
                    loadThumbnail = loadThumbnail,
                    onOpen = onOpenEntry,
                    onShare = onShareEntry,
                    onDelete = onDeleteEntry
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Reveal(entered, 600, animate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainer, MaterialTheme.shapes.large)
                        .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureRow(Icons.Filled.Brush, tr(StringKey.FeatureVectorTitle), tr(StringKey.FeatureVectorBody))
                    FeatureRow(Icons.Filled.Layers, tr(StringKey.FeatureLayersTitle), tr(StringKey.FeatureLayersBody))
                    FeatureRow(Icons.Filled.Movie, tr(StringKey.FeatureExportTitle), tr(StringKey.FeatureExportBody))
                }
            }
        }
        SettingsGear(
            open = showSettings,
            animate = animate,
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onChange = onSettingsChange,
            onReset = onSettingsReset,
            onDismiss = { showSettings = false }
        )
    }

    newProjectMode?.let { mode ->
        ProjectDialog(
            title = tr(if (mode == ProjectMode.ART) StringKey.NewArt else StringKey.NewAnimation),
            initialName = tr(StringKey.DefaultProjectName),
            initialFrameRate = if (mode == ProjectMode.ANIMATION) settings.defaultFrameRate else null,
            confirmLabel = tr(StringKey.Create),
            onDismiss = { newProjectMode = null },
            onConfirm = { name, frameRate ->
                newProjectMode = null
                onNewProject(name, frameRate, mode)
            }
        )
    }
}

@Composable
private fun Reveal(visible: Boolean, delayMillis: Int, animate: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = if (!animate) EnterTransition.None else fadeIn(tween(520, delayMillis, easing = EaseInOutSine)) +
            slideInVertically(tween(520, delayMillis, easing = EaseInOutSine)) { it / 3 }
    ) {
        content()
    }
}

@Composable
private fun AnimatedLogo(animate: Boolean) {
    val still = remember { mutableStateOf(0f) }
    val transition = if (animate) rememberInfiniteTransition() else null
    val ripple: State<Float> = if (transition == null) still else transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing))
    )
    val bob: State<Float> = if (transition == null) still else transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .size(150.dp)
            .drawBehind {
                for (ring in 0..1) {
                    val progress = (ripple.value + ring * 0.5f) % 1f
                    drawCircle(
                        color = BoltAmber.copy(alpha = 0.45f * (1f - progress)),
                        radius = size.minDimension * (0.35f + 0.15f * 2f * progress),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer {
                    translationY = bob.value * 5.dp.toPx()
                    rotationZ = bob.value * 3f
                    val pulse = 1f + 0.03f * bob.value
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(BoltInk, CircleShape)
                .border(2.dp, BoltAmber.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = null,
                tint = BoltAmber,
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        rotationZ = -bob.value * 6f
                    }
            )
        }
    }
}

@Composable
private fun SketchPreview(animate: Boolean) {
    val still = remember { mutableStateOf(0.72f) }
    val scheme = MaterialTheme.colorScheme
    val progress: State<Float> = if (!animate) still else rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SketchLoopMillis, easing = LinearEasing))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(Color.White, MaterialTheme.shapes.large)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
            .drawWithCache {
                val margin = 28.dp.toPx()
                val width = size.width - margin * 2f
                val baseline = size.height * 0.62f
                val amplitude = size.height * 0.24f
                val path = Path().apply {
                    moveTo(margin, baseline)
                    val steps = 64
                    for (step in 1..steps) {
                        val t = step / steps.toFloat()
                        val x = margin + width * t
                        val y = baseline - amplitude * sin(t * 2f * PI.toFloat() * 1.5f) * (0.35f + 0.65f * t)
                        lineTo(x, y)
                    }
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val total = measure.length
                val partial = Path()
                onDrawBehind {
                    val loop = progress.value
                    val drawPhase = (loop / 0.72f).coerceIn(0f, 1f)
                    val fade = if (loop < 0.72f) 1f else 1f - (loop - 0.72f) / 0.28f
                    partial.reset()
                    measure.getSegment(0f, total * drawPhase, partial, true)
                    drawPath(
                        path = partial,
                        color = BoltAmber.copy(alpha = fade),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    val tip = measure.getPosition(total * drawPhase)
                    drawPencilCursor(tip, BoltAmber, fade)
                }
            }
    )
}

@Composable
private fun PressableButton(
    onClick: () -> Unit,
    outlined: Boolean,
    icon: ImageVector,
    label: String
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            interactionSource = interaction
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = BoltAmber, contentColor = BoltInk),
            interactionSource = interaction
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.size(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGear(open: Boolean, animate: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(
        targetValue = if (open) 120f else 0f,
        animationSpec = if (animate) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) else snap()
    )
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainer.copy(alpha = 0.9f))
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = tr(StringKey.Settings),
            tint = scheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    hint: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}
