package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentBlue
import com.example.ui.theme.DomDeckAccentMuted
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import kotlin.math.abs

/**
 * Original geometric placeholder cover with recessed glass frame depth,
 * preparing card artwork slots for future real covers without invented art.
 */
@Composable
fun GameCoverPlaceholder(
    title: String,
    consoleShortName: String,
    fileType: String,
    modifier: Modifier = Modifier
) {
    val seed = abs(title.hashCode())
    val accentShift = when (seed % 4) {
        0 -> DomDeckAccent
        1 -> DomDeckAccentBlue
        2 -> Color(0xFFC4B5FD)
        else -> Color(0xFF7DD3FC)
    }
    val initial = title.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "P"
    val coverShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = coverShape,
                ambientColor = Color.Black.copy(alpha = 0.75f),
                spotColor = accentShift.copy(alpha = 0.25f)
            )
            .clip(coverShape)
            .background(Color(0xAA10131D))
            .border(
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            accentShift.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                ),
                shape = coverShape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Layered recessed cover backdrop
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF21273B),
                        Color(0xFF141825),
                        Color(0xFF0C0E16)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Ambient radial tint
            val center = Offset(w * 0.76f, h * 0.34f)
            val outerR = w * 0.44f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentShift.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = outerR * 1.4f
                ),
                radius = outerR * 1.4f,
                center = center
            )

            // Optical disc geometry
            drawCircle(
                color = accentShift.copy(alpha = 0.28f),
                radius = outerR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = accentShift.copy(alpha = 0.18f),
                radius = outerR * 0.36f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Subtle diagonal Liquid Glass specular sheen
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.75f, h * 0.55f)
                ),
                start = Offset(-w * 0.1f, h * 0.18f),
                end = Offset(w * 0.65f, -h * 0.08f),
                strokeWidth = 28.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DomDeckAccentMuted,
                    border = BorderStroke(1.dp, DomDeckAccent.copy(alpha = 0.38f))
                ) {
                    Text(
                        text = consoleShortName,
                        style = MaterialTheme.typography.labelSmall,
                        color = DomDeckAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x8A1A1F2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = fileType,
                        style = MaterialTheme.typography.labelSmall,
                        color = DomDeckTextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accentShift.copy(alpha = 0.80f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = DomDeckTextPrimary.copy(alpha = 0.94f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
