package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.DomDeckAccent
import java.io.File

/**
 * Displays locally cached or user-picked boxart with Liquid Glass framing,
 * falling back seamlessly to GameCoverPlaceholder when no cover file is available.
 */
@Composable
fun GameCoverArtwork(
    title: String,
    consoleShortName: String,
    fileType: String,
    coverPath: String?,
    modifier: Modifier = Modifier
) {
    val coverFile = remember(coverPath) {
        coverPath?.let { File(it) }?.takeIf { it.exists() && it.isFile && it.length() > 0L }
    }
    var loadFailed by remember(coverPath) { mutableStateOf(false) }

    if (coverFile == null || loadFailed) {
        GameCoverPlaceholder(
            title = title,
            consoleShortName = consoleShortName,
            fileType = fileType,
            modifier = modifier
        )
    } else {
        val context = LocalContext.current
        val coverShape = RoundedCornerShape(14.dp)
        val imageRequest = remember(coverFile.absolutePath, coverFile.lastModified()) {
            ImageRequest.Builder(context)
                .data(coverFile)
                .crossfade(true)
                .build()
        }

        Box(
            modifier = modifier
                .shadow(
                    elevation = 12.dp,
                    shape = coverShape,
                    ambientColor = Color.Black.copy(alpha = 0.78f),
                    spotColor = DomDeckAccent.copy(alpha = 0.24f)
                )
                .clip(coverShape)
                .background(Color(0xFF10131D))
                .border(
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.26f),
                                DomDeckAccent.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ),
                    shape = coverShape
                )
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Capa de $title",
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
