package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentBlue
import com.example.ui.theme.DomDeckBackground
import com.example.ui.theme.DomDeckSurface
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary

enum class ConsoleShowcaseMode {
    IDLE_PREVIEW,
    DISC_INSERT_TRANSITION
}

/**
 * Abstraction for console visual presentation.
 * Currently backed by a lightweight geometric 2.5D Compose Canvas representation;
 * designed so a future low-poly .glb renderer can replace the internal visual body.
 */
@Composable
fun ConsoleShowcase(
    consoleName: String,
    consoleShortName: String,
    mode: ConsoleShowcaseMode,
    modifier: Modifier = Modifier,
    onTransitionFinished: () -> Unit = {}
) {
    val latestOnFinished by rememberUpdatedState(onTransitionFinished)
    val insertProgress = remember(mode) {
        Animatable(if (mode == ConsoleShowcaseMode.DISC_INSERT_TRANSITION) 0f else 0.18f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "console_idle_glow")
    val idleFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_float"
    )

    LaunchedEffect(mode) {
        if (mode == ConsoleShowcaseMode.DISC_INSERT_TRANSITION) {
            insertProgress.snapTo(0f)
            insertProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
            latestOnFinished()
        }
    }

    val progress = insertProgress.value
    val labelDesc = if (mode == ConsoleShowcaseMode.DISC_INSERT_TRANSITION) {
        "Animação de disco entrando no console $consoleName"
    } else {
        "Representação estilizada do console $consoleName"
    }

    Box(
        modifier = modifier
            .semantics { contentDescription = labelDesc }
            .testTag("console_showcase_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val baseCy = h * 0.62f

            val deckWidth = (w * 0.72f).coerceAtMost(320.dp.toPx())
            val deckHeight = deckWidth * 0.34f
            val deckDepth = deckWidth * 0.11f

            // Subtle ambient lavender halo underneath deck
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DomDeckAccent.copy(alpha = 0.16f + 0.10f * progress),
                        Color.Transparent
                    ),
                    center = Offset(cx, baseCy),
                    radius = deckWidth * 0.72f
                ),
                radius = deckWidth * 0.72f,
                center = Offset(cx, baseCy)
            )

            // 2.5D Console base shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.48f),
                topLeft = Offset(cx - deckWidth * 0.52f, baseCy + deckHeight * 0.28f),
                size = Size(deckWidth * 1.04f, deckHeight * 0.62f)
            )

            // Front bevel / extruded side (2.5D thickness)
            val frontFacePath = Path().apply {
                moveTo(cx - deckWidth * 0.48f, baseCy)
                lineTo(cx + deckWidth * 0.48f, baseCy)
                lineTo(cx + deckWidth * 0.48f, baseCy + deckDepth)
                lineTo(cx - deckWidth * 0.48f, baseCy + deckDepth)
                close()
            }
            drawPath(
                path = frontFacePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF23283A), Color(0xFF131620))
                )
            )

            // Top isometric-ish console deck surface
            val topDeckPath = Path().apply {
                moveTo(cx - deckWidth * 0.40f, baseCy - deckHeight * 0.56f)
                lineTo(cx + deckWidth * 0.40f, baseCy - deckHeight * 0.56f)
                lineTo(cx + deckWidth * 0.48f, baseCy)
                lineTo(cx - deckWidth * 0.48f, baseCy)
                close()
            }
            drawPath(
                path = topDeckPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2A3044), Color(0xFF1D2231)),
                    start = Offset(cx - deckWidth * 0.4f, baseCy - deckHeight * 0.6f),
                    end = Offset(cx + deckWidth * 0.4f, baseCy)
                )
            )
            drawPath(
                path = topDeckPath,
                color = Color(0xFF3A415A),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Recessed circular disc bay on top deck
            val bayWidth = deckWidth * 0.46f
            val bayHeight = deckHeight * 0.44f
            val bayTopLeft = Offset(cx - bayWidth * 0.5f, baseCy - deckHeight * 0.47f)

            drawOval(
                color = Color(0xFF10131C),
                topLeft = bayTopLeft,
                size = Size(bayWidth, bayHeight)
            )
            drawOval(
                color = DomDeckAccent.copy(alpha = 0.25f + 0.35f * progress),
                topLeft = bayTopLeft,
                size = Size(bayWidth, bayHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center spindle inside bay
            drawOval(
                color = Color(0xFF3A415A),
                topLeft = Offset(cx - bayWidth * 0.11f, baseCy - deckHeight * 0.31f),
                size = Size(bayWidth * 0.22f, bayHeight * 0.30f)
            )

            // Optical disc animation (enters bay cleanly over 650ms)
            val discInsertT = (progress / 0.72f).coerceIn(0f, 1f)
            val lidCloseT = ((progress - 0.62f) / 0.38f).coerceIn(0f, 1f)

            val hoverOffset = if (mode == ConsoleShowcaseMode.IDLE_PREVIEW) {
                (idleFloat - 0.5f) * 6.dp.toPx()
            } else {
                0f
            }

            val startY = baseCy - deckHeight * 1.15f + hoverOffset
            val endY = baseCy - deckHeight * 0.25f
            val discCenterY = startY + (endY - startY) * discInsertT
            val discScale = 1f - 0.14f * discInsertT
            val discAlpha = (1f - lidCloseT * 0.85f).coerceIn(0.15f, 1f)
            val discRadiusX = bayWidth * 0.47f * discScale
            val discRadiusY = bayHeight * 0.46f * discScale
            val discRotation = discInsertT * 160f + (idleFloat * 25f)

            withTransform({
                translate(left = cx, top = discCenterY)
            }) {
                // Disc body
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFDDE3F5).copy(alpha = 0.85f * discAlpha),
                            DomDeckAccent.copy(alpha = 0.75f * discAlpha),
                            Color(0xFF9CA8C7).copy(alpha = 0.80f * discAlpha),
                            DomDeckAccent.copy(alpha = 0.65f * discAlpha),
                            Color(0xFFDDE3F5).copy(alpha = 0.85f * discAlpha)
                        )
                    ),
                    topLeft = Offset(-discRadiusX, -discRadiusY),
                    size = Size(discRadiusX * 2f, discRadiusY * 2f)
                )
                // Disc highlight arc rotating
                drawArc(
                    color = Color.White.copy(alpha = 0.65f * discAlpha),
                    startAngle = discRotation,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(-discRadiusX * 0.78f, -discRadiusY * 0.78f),
                    size = Size(discRadiusX * 1.56f, discRadiusY * 1.56f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Inner disc ring
                drawOval(
                    color = Color(0xFF131620).copy(alpha = 0.9f * discAlpha),
                    topLeft = Offset(-discRadiusX * 0.28f, -discRadiusY * 0.28f),
                    size = Size(discRadiusX * 0.56f, discRadiusY * 0.56f)
                )
            }

            // Stylized hinged bay cover closing smoothly at the end of the 650ms transition
            if (lidCloseT > 0.02f) {
                val lidAlpha = lidCloseT * 0.92f
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B3146).copy(alpha = lidAlpha),
                            Color(0xFF1F2434).copy(alpha = lidAlpha)
                        )
                    ),
                    topLeft = Offset(
                        bayTopLeft.x - 2.dp.toPx(),
                        bayTopLeft.y - (1f - lidCloseT) * 14.dp.toPx()
                    ),
                    size = Size(bayWidth + 4.dp.toPx(), bayHeight + 2.dp.toPx())
                )
                drawOval(
                    color = DomDeckAccent.copy(alpha = 0.45f * lidAlpha),
                    topLeft = Offset(
                        bayTopLeft.x - 2.dp.toPx(),
                        bayTopLeft.y - (1f - lidCloseT) * 14.dp.toPx()
                    ),
                    size = Size(bayWidth + 4.dp.toPx(), bayHeight + 2.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Front accent LED bar & dual controller port hints
            val ledGlowAlpha = if (mode == ConsoleShowcaseMode.DISC_INSERT_TRANSITION) {
                0.45f + 0.55f * lidCloseT
            } else {
                0.55f + 0.25f * idleFloat
            }
            drawRoundRect(
                color = DomDeckAccent.copy(alpha = ledGlowAlpha),
                topLeft = Offset(cx - deckWidth * 0.16f, baseCy + deckDepth * 0.36f),
                size = Size(deckWidth * 0.32f, 3.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Left and right stylized deck ports
            drawRoundRect(
                color = Color(0xFF0E1016),
                topLeft = Offset(cx - deckWidth * 0.37f, baseCy + deckDepth * 0.26f),
                size = Size(deckWidth * 0.11f, deckDepth * 0.46f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF0E1016),
                topLeft = Offset(cx + deckWidth * 0.26f, baseCy + deckDepth * 0.26f),
                size = Size(deckWidth * 0.11f, deckDepth * 0.46f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        // Subtle short console tag badge in bottom corner of showcase
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(10.dp),
            color = DomDeckSurface.copy(alpha = 0.85f)
        ) {
            Text(
                text = consoleShortName,
                style = MaterialTheme.typography.labelSmall,
                color = DomDeckAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ConsoleTransitionOverlay(
    consoleName: String,
    consoleShortName: String,
    onAnimationComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("console_transition_overlay"),
        contentAlignment = Alignment.Center
    ) {
        val backdropModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                .fillMaxSize()
                .background(DomDeckBackground.copy(alpha = 0.86f))
                .blur(20.dp, BlurredEdgeTreatment.Rectangle)
        } else {
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DomDeckAccent.copy(alpha = 0.12f),
                            DomDeckAccentBlue.copy(alpha = 0.08f),
                            DomDeckBackground.copy(alpha = 0.94f)
                        )
                    )
                )
        }
        Box(modifier = backdropModifier)

        GlassCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(26.dp),
            elevated = true,
            accentGlow = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ConsoleShowcase(
                    consoleName = consoleName,
                    consoleShortName = consoleShortName,
                    mode = ConsoleShowcaseMode.DISC_INSERT_TRANSITION,
                    modifier = Modifier.size(width = 240.dp, height = 168.dp),
                    onTransitionFinished = onAnimationComplete
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = consoleName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = DomDeckTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Abrindo biblioteca do console…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DomDeckTextSecondary
                )
            }
        }
    }
}
