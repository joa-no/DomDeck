package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.GameEntry
import com.example.model.LaunchResult
import com.example.ui.components.ConsoleShowcase
import com.example.ui.components.ConsoleShowcaseMode
import com.example.ui.components.GameCoverArtwork
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.viewmodel.DomDeckUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Fullscreen, landscape-oriented game frontend.
 *
 * No scrolling sections: the selected title remains centered and neighboring
 * covers act as a horizontal selector. Secondary setup pages are separate.
 */
@Composable
fun HomeScreen(
    uiState: DomDeckUiState,
    onOpenPs1Showcase: () -> Unit,
    onOpenConsoles: () -> Unit,
    onOpenPs1Setup: () -> Unit,
    onOpenGameDetails: (GameEntry) -> Unit,
    onOpenSettings: () -> Unit = {},
    onPlayGame: (GameEntry) -> Unit = {},
    onDismissLaunchResult: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val games = uiState.ps1Games
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(games.size) {
        selectedIndex = selectedIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    }
    val selected = games.getOrNull(selectedIndex)

    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            dateText = SimpleDateFormat("EEE, d MMM", Locale("pt", "BR")).format(now)
            delay(30_000)
        }
    }

    val launchError = uiState.launchResult as? LaunchResult.Failed
    if (launchError != null) {
        AlertDialog(
            onDismissRequest = onDismissLaunchResult,
            title = { Text("Não foi possível iniciar o jogo") },
            text = { Text(launchError.reason) },
            confirmButton = {
                TextButton(onClick = onDismissLaunchResult) {
                    Text("Fechar")
                }
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 410.dp
        val edge = if (compact) 12.dp else 22.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = edge, end = edge, top = if (compact) 8.dp else 16.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clock / system headline / options. The center headline uses an overlay
            // so it stays centered regardless of button widths.
            Box(modifier = Modifier.fillMaxWidth().height(if (compact) 62.dp else 78.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleLarge,
                        color = DomDeckTextPrimary
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = DomDeckTextSecondary,
                        maxLines = 1
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYSTATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp),
                        color = DomDeckTextSecondary
                    )
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.displayMedium,
                        color = DomDeckTextPrimary
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag("nav_settings")
                        .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = DomDeckTextPrimary)
                }
            }

            // Current system and indexed titles
            Text(
                text = if (games.size == 1) "1 JOGO NA BIBLIOTECA" else "${games.size} JOGOS NA BIBLIOTECA",
                style = MaterialTheme.typography.labelSmall,
                color = DomDeckTextSecondary,
                modifier = Modifier.testTag("home_total_games_text")
            )

            // Full-width coverflow, no vertical or horizontal scrolling containers.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(games.size, selectedIndex) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragTotal += amount
                            },
                            onDragEnd = {
                                if (dragTotal < -55f && selectedIndex < games.lastIndex) selectedIndex++
                                else if (dragTotal > 55f && selectedIndex > 0) selectedIndex--
                                dragTotal = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (games.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ConsoleShowcase(
                            consoleName = "PlayStation 1",
                            consoleShortName = "PS1",
                            mode = ConsoleShowcaseMode.IDLE_PREVIEW,
                            modifier = Modifier
                                .width((maxWidth * 0.55f).coerceAtMost(360.dp))
                                .height((maxHeight * 0.70f).coerceAtLeast(130.dp))
                        )
                        Text(
                            text = "Sua biblioteca está vazia",
                            style = MaterialTheme.typography.titleMedium,
                            color = DomDeckTextPrimary
                        )
                        Text(
                            text = "Configure a pasta de jogos para começar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DomDeckTextSecondary
                        )
                    }
                } else {
                    val heroHeight = (maxHeight * if (compact) 0.73f else 0.78f)
                        .coerceAtMost(330.dp)
                    val sideHeight = heroHeight * 0.72f
                    val cardGap = if (maxWidth < 680.dp) 13.dp else 34.dp

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        Box(
                            modifier = Modifier.size(width = sideHeight * 0.76f, height = sideHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            games.getOrNull(selectedIndex - 1)?.let { game ->
                                FrontendCover(
                                    game = game,
                                    uiState = uiState,
                                    height = sideHeight,
                                    featured = false,
                                    rotationY = 12f,
                                    onClick = { selectedIndex-- }
                                )
                            }
                        }
                        FrontendCover(
                            game = games[selectedIndex],
                            uiState = uiState,
                            height = heroHeight,
                            featured = true,
                            rotationY = 0f,
                            onClick = { onOpenGameDetails(games[selectedIndex]) }
                        )
                        Box(
                            modifier = Modifier.size(width = sideHeight * 0.76f, height = sideHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            games.getOrNull(selectedIndex + 1)?.let { game ->
                                FrontendCover(
                                    game = game,
                                    uiState = uiState,
                                    height = sideHeight,
                                    featured = false,
                                    rotationY = -12f,
                                    onClick = { selectedIndex++ }
                                )
                            }
                        }
                    }
                }
            }

            // Game title and position are fixed below the coverflow.
            Column(
                modifier = Modifier.height(if (compact) 58.dp else 78.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selected?.let {
                        uiState.gameMetadataByUri[it.uriString]?.effectiveTitle?.takeIf { title -> title.isNotBlank() }
                            ?: it.displayTitle
                    } ?: "PLAYSTATION 1",
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    color = DomDeckTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f).testTag("frontend_selected_game_title")
                )
                Text(
                    text = if (selected == null) "SEM JOGOS" else "${selectedIndex + 1} / ${games.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DomDeckAccent
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FrontendFooterAction(
                    label = "SISTEMAS",
                    icon = { Icon(Icons.Outlined.SportsEsports, contentDescription = null) },
                    onClick = onOpenConsoles,
                    modifier = Modifier.testTag("nav_consoles")
                )
                FrontendFooterAction(
                    label = "ANTERIOR",
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                    enabled = selectedIndex > 0,
                    onClick = { selectedIndex-- }
                )
                FrontendFooterAction(
                    label = if (selected == null) "CONFIGURAR" else "JOGAR",
                    icon = {
                        Icon(
                            if (selected == null) Icons.Outlined.Settings else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                    },
                    emphasized = true,
                    onClick = {
                        selected?.let(onPlayGame) ?: onOpenPs1Setup()
                    },
                    modifier = Modifier.testTag("frontend_primary_action")
                )
                FrontendFooterAction(
                    label = "PRÓXIMO",
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    enabled = selectedIndex < games.lastIndex,
                    onClick = { selectedIndex++ }
                )
                FrontendFooterAction(
                    label = "DETALHES",
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    enabled = selected != null,
                    onClick = { selected?.let(onOpenGameDetails) }
                )
            }
        }
    }
}

@Composable
private fun FrontendCover(
    game: GameEntry,
    uiState: DomDeckUiState,
    height: Dp,
    featured: Boolean,
    rotationY: Float,
    onClick: () -> Unit
) {
    val metadata = uiState.gameMetadataByUri[game.uriString]
    val title = metadata?.effectiveTitle?.takeIf { it.isNotBlank() } ?: game.displayTitle

    Box(
        modifier = Modifier
            .size(width = height * 0.76f, height = height)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 14f * density
                alpha = if (featured) 1f else 0.68f
                scaleX = if (featured) 1f else 0.94f
                scaleY = if (featured) 1f else 0.94f
            }
            .clickable(onClick = onClick)
            .testTag("game_card_${game.id}")
    ) {
        GameCoverArtwork(
            title = title,
            consoleShortName = "PS1",
            fileType = game.fileType,
            coverPath = metadata?.effectiveCoverPath,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FrontendFooterAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .background(if (emphasized) DomDeckAccent.copy(alpha = 0.22f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.material3.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                (if (!enabled) DomDeckTextSecondary.copy(alpha = 0.4f)
                else if (emphasized) DomDeckAccent
                else DomDeckTextSecondary)
        ) {
            Box(modifier = Modifier.size(17.dp)) {
                icon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
                color = androidx.compose.material3.LocalContentColor.current,
                maxLines = 1
            )
        }
    }
}
