package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import com.example.ui.components.ConsoleShowcase
import com.example.ui.components.ConsoleShowcaseMode
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.viewmodel.DomDeckUiState

/**
 * Library-first home. Emulator status and setup stay available without occupying
 * the primary visual hierarchy. The existing scanner and launch flow are unchanged.
 */
@Composable
fun HomeScreen(
    uiState: DomDeckUiState,
    onOpenPs1Showcase: () -> Unit,
    onOpenConsoles: () -> Unit,
    onOpenPs1Setup: () -> Unit,
    onOpenGameDetails: (GameEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val ps1GameCount = uiState.consoleGameCounts[ConsoleCatalog.PS1_ID] ?: 0
    val hasFolder = uiState.ps1Settings.folderUri != null
    val duckInstalled = uiState.duckStationStatus.isInstalled

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 680.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .testTag("home_screen_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "DOMDECK",
                                style = MaterialTheme.typography.labelLarge,
                                color = DomDeckAccent
                            )
                            Text(
                                text = "Sua biblioteca",
                                style = MaterialTheme.typography.headlineMedium,
                                color = DomDeckTextPrimary
                            )
                        }
                        Text(
                            text = if (uiState.totalGameCount == 1) {
                                "1 jogo"
                            } else {
                                "${uiState.totalGameCount} jogos"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = DomDeckTextSecondary,
                            modifier = Modifier.testTag("home_total_games_text")
                        )
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_ps1_quick_card"),
                        shape = RoundedCornerShape(28.dp),
                        elevated = true
                    ) {
                        if (wideLayout) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Ps1HeroCopy(
                                    gameCount = ps1GameCount,
                                    hasFolder = hasFolder,
                                    duckInstalled = duckInstalled,
                                    onOpenLibrary = onOpenPs1Showcase,
                                    onOpenSetup = onOpenPs1Setup,
                                    modifier = Modifier.weight(1.2f)
                                )
                                ConsoleShowcase(
                                    consoleName = "PlayStation 1",
                                    consoleShortName = "PS1",
                                    mode = ConsoleShowcaseMode.IDLE_PREVIEW,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(238.dp)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ConsoleShowcase(
                                    consoleName = "PlayStation 1",
                                    consoleShortName = "PS1",
                                    mode = ConsoleShowcaseMode.IDLE_PREVIEW,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(185.dp)
                                )
                                Ps1HeroCopy(
                                    gameCount = ps1GameCount,
                                    hasFolder = hasFolder,
                                    duckInstalled = duckInstalled,
                                    onOpenLibrary = onOpenPs1Showcase,
                                    onOpenSetup = onOpenPs1Setup
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Jogos da biblioteca",
                                style = MaterialTheme.typography.titleLarge,
                                color = DomDeckTextPrimary
                            )
                            if (uiState.allGames.isNotEmpty()) {
                                TextButton(onClick = onOpenPs1Showcase) {
                                    Text("Ver todos", color = DomDeckAccent)
                                }
                            }
                        }

                        if (uiState.allGames.isEmpty()) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = DomDeckAccent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Sua coleção começa aqui",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DomDeckTextPrimary
                                        )
                                        Text(
                                            text = if (hasFolder) {
                                                "Abra o console para conferir ou atualizar os jogos encontrados."
                                            } else {
                                                "Selecione sua pasta de jogos de PS1 para montar a biblioteca."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = DomDeckTextSecondary
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(uiState.allGames.take(10), key = { it.id }) { game ->
                                    val metadata = uiState.gameMetadataByUri[game.uriString]
                                    val title = metadata?.effectiveTitle?.takeIf { it.isNotBlank() }
                                        ?: game.displayTitle
                                    GlassCard(
                                        modifier = Modifier
                                            .width(146.dp)
                                            .testTag("home_game_card_${game.id}"),
                                        shape = RoundedCornerShape(18.dp),
                                        onClick = { onOpenGameDetails(game) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(9.dp),
                                            verticalArrangement = Arrangement.spacedBy(9.dp)
                                        ) {
                                            GameCoverArtwork(
                                                title = title,
                                                consoleShortName = "PS1",
                                                fileType = game.fileType,
                                                coverPath = metadata?.effectiveCoverPath,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.76f)
                                            )
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = DomDeckTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SportsEsports,
                                contentDescription = null,
                                tint = DomDeckAccent,
                                modifier = Modifier.size(23.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Explorar consoles",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DomDeckTextPrimary
                                )
                                Text(
                                    text = "Outros sistemas estão em desenvolvimento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DomDeckTextSecondary,
                                    maxLines = 2
                                )
                            }
                            GlassActionButton(
                                onClick = onOpenConsoles,
                                modifier = Modifier.testTag("home_explore_consoles_button")
                            ) {
                                Text("Ver", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Ps1HeroCopy(
    gameCount: Int,
    hasFolder: Boolean,
    duckInstalled: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatusPill(
            text = if (duckInstalled) "DuckStation detectado" else "Emulador não detectado",
            tone = if (duckInstalled) StatusTone.SUCCESS else StatusTone.NEUTRAL
        )
        Text(
            text = "PlayStation 1",
            style = MaterialTheme.typography.headlineLarge,
            color = DomDeckTextPrimary
        )
        Text(
            text = when {
                gameCount > 0 -> if (gameCount == 1) {
                    "1 jogo na sua coleção. Escolha uma capa para começar."
                } else {
                    "$gameCount jogos na sua coleção. Escolha uma capa para começar."
                }
                hasFolder -> "Sua pasta está conectada. Abra a biblioteca para explorar seus jogos."
                else -> "Seus jogos, suas capas e suas histórias em um só lugar."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = DomDeckTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassActionButton(
                onClick = onOpenLibrary,
                accentHighlight = true,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Abrir biblioteca",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            GlassActionButton(
                onClick = onOpenSetup,
                modifier = Modifier.testTag("home_setup_ps1_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Configurar PS1",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
