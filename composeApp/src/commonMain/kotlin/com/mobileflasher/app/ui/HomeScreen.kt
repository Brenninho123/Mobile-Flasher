package com.mobileflasher.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val OrbCount = 7
private const val SketchLoopMillis = 4200
private val BoltPink = Color(0xFFFF5C8A)
private val BoltSky = Color(0xFF4DB8FF)

@Composable
fun HomeScreen(
    onNewProject: (String, Int) -> Unit,
    onOpenProject: () -> Unit
) {
    var showNewProject by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { entered = true }

    val drift = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing))
    )
    val breathe = rememberInfiniteTransition().animateFloat(
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
            Reveal(entered, 0) {
                AnimatedLogo()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Reveal(entered, 120) {
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
            Reveal(entered, 200) {
                Text(
                    text = "Vector drawing and frame animation, built for your phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Reveal(entered, 300) {
                SketchPreview()
            }
            Spacer(modifier = Modifier.height(24.dp))
            Reveal(entered, 420) {
                PressableButton(
                    onClick = { showNewProject = true },
                    outlined = false,
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    label = "New project"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Reveal(entered, 500) {
                PressableButton(
                    onClick = onOpenProject,
                    outlined = true,
                    icon = Icons.Filled.FolderOpen,
                    label = "Open project"
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Reveal(entered, 600) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surfaceContainer, MaterialTheme.shapes.large)
                        .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.large)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureRow(Icons.Filled.Brush, "Vector tools", "Shapes, lines and a smooth pencil with live styling")
                    FeatureRow(Icons.Filled.Layers, "Layers and timeline", "Keyframes, onion skin and playback")
                    FeatureRow(Icons.Filled.Movie, "Export anywhere", "PNG frames, animated GIF and project files")
                }
            }
        }
    }

    if (showNewProject) {
        ProjectDialog(
            title = "New project",
            initialName = "Untitled",
            initialFrameRate = 24,
            confirmLabel = "Create",
            onDismiss = { showNewProject = false },
            onConfirm = { name, frameRate ->
                showNewProject = false
                onNewProject(name, frameRate)
            }
        )
    }
}

@Composable
private fun Reveal(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(520, delayMillis, easing = EaseInOutSine)) +
            slideInVertically(tween(520, delayMillis, easing = EaseInOutSine)) { it / 3 }
    ) {
        content()
    }
}

@Composable
private fun AnimatedLogo() {
    val transition = rememberInfiniteTransition()
    val ripple = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing))
    )
    val bob = transition.animateFloat(
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
private fun SketchPreview() {
    val scheme = MaterialTheme.colorScheme
    val progress = rememberInfiniteTransition().animateFloat(
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
