package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance animated ambient liquid orbs in the background.
 * Creates the organic, translucent depth that shines through frosted glass cards.
 */
@Composable
fun LiquidGlassAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_orbs")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseDim = minOf(w, h)

            val rad1 = Math.toRadians(phase1.toDouble())
            val rad2 = Math.toRadians(phase2.toDouble())

            val orb1X = (w * 0.25f + Math.cos(rad1).toFloat() * (w * 0.15f))
            val orb1Y = (h * 0.2f + Math.sin(rad1).toFloat() * (h * 0.08f))

            val orb2X = (w * 0.75f + Math.cos(rad2).toFloat() * (w * 0.18f))
            val orb2Y = (h * 0.65f + Math.sin(rad2).toFloat() * (h * 0.12f))

            val orb3X = (w * 0.45f + Math.sin(rad1).toFloat() * (w * 0.12f))
            val orb3Y = (h * 0.85f + Math.cos(rad2).toFloat() * (h * 0.08f))

            // Liquid Orb 1 (Cyan/Azure Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.22f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = baseDim * 0.55f
                ),
                radius = baseDim * 0.55f,
                center = Offset(orb1X, orb1Y)
            )

            // Liquid Orb 2 (Violet/Opal Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.18f),
                        secondaryColor.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = baseDim * 0.60f
                ),
                radius = baseDim * 0.60f,
                center = Offset(orb2X, orb2Y)
            )

            // Liquid Orb 3 (Teal/Emerald Accent)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = baseDim * 0.45f
                ),
                radius = baseDim * 0.45f,
                center = Offset(orb3X, orb3Y)
            )
        }

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}

/**
 * Modifier that applies smooth spring press feedback and liquid glass reflections.
 */
fun Modifier.smoothInteractiveClick(
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (onClick == null) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "smooth_press_scale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Creates a frosted liquid glass border with specular highlights.
 */
@Composable
fun liquidGlassBorder(
    strokeWidth: Dp = 1.dp,
    glowColor: Color = MaterialTheme.colorScheme.primary
): BorderStroke {
    return BorderStroke(
        width = strokeWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                glowColor.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.15f),
                glowColor.copy(alpha = 0.05f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    )
}
