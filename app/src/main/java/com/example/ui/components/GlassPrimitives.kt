package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentBlue
import com.example.ui.theme.DomDeckBackground
import com.example.ui.theme.DomDeckGlassBottom
import com.example.ui.theme.DomDeckGlassElevatedBottom
import com.example.ui.theme.DomDeckGlassElevatedTop
import com.example.ui.theme.DomDeckGlassFloatingBottom
import com.example.ui.theme.DomDeckGlassFloatingTop
import com.example.ui.theme.DomDeckGlassRimAccent
import com.example.ui.theme.DomDeckGlassRimBlue
import com.example.ui.theme.DomDeckGlassRimWhite
import com.example.ui.theme.DomDeckGlassTop
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextTertiary

/**
 * Atmospheric dark backdrop with subtle purple and sapphire-blue light fields.
 * Applies RenderEffect-backed Modifier.blur only to the background glow layer on Android 12+ (API 31+),
 * keeping all foreground UI and text crisp on all supported Android versions.
 */
@Composable
fun DomDeckAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DomDeckBackground)
    ) {
        val glowModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                .fillMaxSize()
                .blur(radius = 52.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier.fillMaxSize()
        }

        Canvas(modifier = glowModifier) {
            val w = size.width
            val h = size.height

            // Soft upper-left lavender atmospheric orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DomDeckAccent.copy(alpha = 0.22f),
                        DomDeckAccent.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.16f, h * 0.12f),
                    radius = maxOf(w, h) * 0.56f
                ),
                radius = maxOf(w, h) * 0.56f,
                center = Offset(w * 0.16f, h * 0.12f)
            )

            // Deep sapphire-blue ambient field on the right / mid
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DomDeckAccentBlue.copy(alpha = 0.19f),
                        Color(0xFF1D4ED8).copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.36f),
                    radius = maxOf(w, h) * 0.62f
                ),
                radius = maxOf(w, h) * 0.62f,
                center = Offset(w * 0.88f, h * 0.36f)
            )

            // Subtle lower indigo-violet anchor glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.38f, h * 0.92f),
                    radius = maxOf(w, h) * 0.52f
                ),
                radius = maxOf(w, h) * 0.52f,
                center = Offset(w * 0.38f, h * 0.92f)
            )
        }

        content()
    }
}

/**
 * Translucent Liquid Glass panel with frosted layered gradients, specular edge reflection,
 * and diffuse ambient elevation shadow. Never blurs its own foreground text content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    accentGlow: Boolean = false,
    elevation: Dp = if (elevated) 14.dp else 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val fillBrush = if (elevated) {
        Brush.linearGradient(
            colors = listOf(DomDeckGlassElevatedTop, DomDeckGlassElevatedBottom)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(DomDeckGlassTop, DomDeckGlassBottom)
        )
    }

    val borderBrush = if (accentGlow) {
        Brush.linearGradient(
            colors = listOf(
                DomDeckGlassRimWhite,
                DomDeckGlassRimAccent,
                DomDeckGlassRimBlue,
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                DomDeckGlassRimBlue.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val shadowSpot = if (accentGlow) DomDeckAccent.copy(alpha = 0.28f) else Color(0xFF02040A)
    val interactionSource = remember { MutableInteractionSource() }

    val baseModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.72f),
            spotColor = shadowSpot
        )
        .clip(shape)
        .background(fillBrush)
        .drawBehind {
            // Subtle top inner specular highlight to simulate Liquid Glass edge refraction
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.32f
                ),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
            )
        }
        .border(BorderStroke(1.dp, borderBrush), shape)
        .let { m ->
            if (onClick != null) {
                m.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = DomDeckAccent.copy(alpha = 0.25f)),
                    onClick = onClick
                )
            } else {
                m
            }
        }

    Box(modifier = baseModifier) {
        content()
    }
}

/**
 * Floating translucent glass container for NavigationRail / NavigationBar and modal bars.
 * Uses a dedicated blurred ambient backing layer on API 31+ behind sharp navigation icons/labels.
 */
@Composable
fun GlassFloatingContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.26f),
            DomDeckGlassRimAccent,
            DomDeckGlassRimBlue,
            Color.White.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.8f),
                spotColor = DomDeckAccent.copy(alpha = 0.24f)
            )
            .clip(shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        // Dedicated backing frosted layer (blurred only on API 31+, never affecting children)
        val backingModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(DomDeckGlassFloatingTop, DomDeckGlassFloatingBottom)
                    )
                )
                .blur(18.dp, BlurredEdgeTreatment.Rectangle)
        } else {
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(DomDeckGlassFloatingTop, DomDeckGlassFloatingBottom)
                    )
                )
        }

        Box(modifier = backingModifier)

        // Specular top sheen overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.11f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.28f
                        )
                    )
                }
        )

        content()
    }
}

/**
 * Discreet translucent glass button avoiding solid filled purple blocks.
 * Uses subtle lavender rim & glow only when accentHighlight = true.
 */
@Composable
fun GlassActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentHighlight: Boolean = false,
    dangerHighlight: Boolean = false,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val fillBrush = when {
        !enabled -> Brush.verticalGradient(
            colors = listOf(Color(0x55191D2B), Color(0x44121520))
        )
        dangerHighlight -> Brush.verticalGradient(
            colors = listOf(Color(0x553B1820), Color(0x44241016))
        )
        accentHighlight -> Brush.linearGradient(
            colors = listOf(
                Color(0x40A78BFA),
                Color(0x2860A5FA),
                Color(0x99191E2D)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xBA242A3E), Color(0x99171B29))
        )
    }

    val borderBrush = when {
        !enabled -> Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f))
        )
        dangerHighlight -> Brush.linearGradient(
            colors = listOf(
                Color(0x88F87171),
                Color(0x33F87171)
            )
        )
        accentHighlight -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f),
                DomDeckAccent.copy(alpha = 0.65f),
                DomDeckAccentBlue.copy(alpha = 0.35f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.07f)
            )
        )
    }

    val contentColor = if (enabled) DomDeckTextPrimary else DomDeckTextTertiary
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .shadow(
                elevation = if (enabled && accentHighlight) 10.dp else 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = if (accentHighlight) DomDeckAccent.copy(alpha = 0.3f) else Color.Black
            )
            .clip(shape)
            .background(fillBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = DomDeckAccent.copy(alpha = 0.28f)),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}
