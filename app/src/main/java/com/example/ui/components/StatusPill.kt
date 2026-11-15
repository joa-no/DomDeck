package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentMuted
import com.example.ui.theme.DomDeckError
import com.example.ui.theme.DomDeckErrorContainer
import com.example.ui.theme.DomDeckSuccess
import com.example.ui.theme.DomDeckSuccessContainer
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckWarning
import com.example.ui.theme.DomDeckWarningContainer

enum class StatusTone {
    NEUTRAL,
    ACCENT,
    SUCCESS,
    WARNING,
    ERROR
}

@Composable
fun StatusPill(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (bg, fg, border) = when (tone) {
        StatusTone.NEUTRAL -> Triple(
            Color(0x73202638),
            DomDeckTextSecondary,
            Color.White.copy(alpha = 0.14f)
        )
        StatusTone.ACCENT -> Triple(
            DomDeckAccentMuted,
            DomDeckAccent,
            DomDeckAccent.copy(alpha = 0.42f)
        )
        StatusTone.SUCCESS -> Triple(
            DomDeckSuccessContainer,
            DomDeckSuccess,
            DomDeckSuccess.copy(alpha = 0.36f)
        )
        StatusTone.WARNING -> Triple(
            DomDeckWarningContainer,
            DomDeckWarning,
            DomDeckWarning.copy(alpha = 0.36f)
        )
        StatusTone.ERROR -> Triple(
            DomDeckErrorContainer,
            DomDeckError,
            DomDeckError.copy(alpha = 0.36f)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}
