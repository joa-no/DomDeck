package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.ConsoleIntegrationStatus
import com.example.model.GameEntry
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentMuted
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary
import com.example.viewmodel.DomDeckUiState

@Composable
fun HomeScreen(
    uiState: DomDeckUiState,
    onOpenPs1Showcase: () -> Unit,
    onOpenConsoles: () -> Unit,
    onOpenPs1Setup: () -> Unit,
    onOpenGameDetails: (GameEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuredCount = if (uiState.ps1Settings.folderUri != null) 1 else 0
    val ps1GameCount = uiState.consoleGameCounts[ConsoleCatalog.PS1_ID] ?: 0
    val isDuckAssociated = uiState.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DomDeck",
                        style = MaterialTheme.typography.displaySmall,
                        color = DomDeckTextPrimary
                    )
                    Text(
                        text = "Frontend local para organizar sua biblioteca sem copiar arquivos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DomDeckTextSecondary
                    )
                }
            }

            // Library summary Liquid Glass deck card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevated = true,
                    accentGlow = true
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "RESUMO DA BIBLIOTECA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DomDeckAccent
                                )
                                val countLabel = when (uiState.totalGameCount) {
                                    0 -> "0 jogos indexados"
                                    1 -> "1 jogo indexado"
                                    else -> "${uiState.totalGameCount} jogos indexados"
                                }
                                Text(
                                    text = countLabel,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DomDeckTextPrimary,
                                    modifier = Modifier.testTag("home_total_games_text")
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DomDeckAccentMuted,
                                border = BorderStroke(1.dp, DomDeckAccent.copy(alpha = 0.38f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SportsEsports,
                                    contentDescription = "Biblioteca DomDeck",
                                    tint = DomDeckAccent,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(26.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                text = "$configuredCount de ${ConsoleCatalog.allConsoles.size} consoles",
                                tone = if (configuredCount > 0) StatusTone.ACCENT else StatusTone.NEUTRAL
                            )
                            StatusPill(
                                text = if (uiState.duckStationStatus.isInstalled) {
                                    "DuckStation detectado"
                                } else {
                                    "DuckStation não encontrado"
                                },
                                tone = if (uiState.duckStationStatus.isInstalled) {
                                    StatusTone.SUCCESS
                                } else {
                                    StatusTone.WARNING
                                },
                                icon = if (uiState.duckStationStatus.isInstalled) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.WarningAmber
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassActionButton(
                                onClick = onOpenPs1Setup,
                                accentHighlight = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_setup_ps1_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = DomDeckAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Configurar PS1",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            GlassActionButton(
                                onClick = onOpenConsoles,
                                accentHighlight = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_explore_consoles_button")
                            ) {
                                Text(
                                    text = "Ver consoles",
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
                        }
                    }
                }
            }

            // Quick console access
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acesso rápido aos consoles",
                            style = MaterialTheme.typography.titleLarge,
                            color = DomDeckTextPrimary
                        )
                        TextButton(onClick = onOpenConsoles) {
                            Text("Todos", color = DomDeckAccent)
                        }
                    }

                    // Active PS1 glass card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_ps1_quick_card"),
                        shape = RoundedCornerShape(20.dp),
                        accentGlow = true,
                        onClick = onOpenPs1Showcase
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = DomDeckAccentMuted,
                                    border = BorderStroke(1.dp, DomDeckAccent.copy(alpha = 0.35f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Album,
                                            contentDescription = "PlayStation 1",
                                            tint = DomDeckAccent
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PlayStation 1",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DomDeckTextPrimary
                                    )
                                    val subtitle = when {
                                        ps1GameCount > 0 -> "$ps1GameCount jogo(s) • ${uiState.ps1Settings.folderLabel ?: "Pasta SAF"}"
                                        uiState.ps1Settings.folderUri != null -> "Pasta selecionada • Toque para abrir"
                                        else -> "Integração disponível • Configurar pasta e emulador"
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DomDeckTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!isDuckAssociated && uiState.duckStationStatus.isInstalled) {
                                        Text(
                                            text = "DuckStation detectado (pendente associação)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DomDeckAccent
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Abrir PlayStation 1",
                                tint = DomDeckTextSecondary
                            )
                        }
                    }

                    // Compact horizontal glass chips for remaining catalog systems
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = ConsoleCatalog.allConsoles.filter {
                                it.integrationStatus == ConsoleIntegrationStatus.IN_DEVELOPMENT
                            },
                            key = { it.id }
                        ) { console ->
                            GlassCard(
                                shape = RoundedCornerShape(16.dp),
                                elevation = 4.dp,
                                onClick = onOpenConsoles
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = console.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DomDeckTextPrimary
                                    )
                                    Text(
                                        text = "Integração em desenvolvimento",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DomDeckTextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent / Indexed games or honest empty state
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Jogos recentes na biblioteca",
                        style = MaterialTheme.typography.titleLarge,
                        color = DomDeckTextPrimary
                    )

                    if (uiState.allGames.isEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = DomDeckAccent
                                    )
                                    Text(
                                        text = "Nenhum jogo indexado ainda",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DomDeckTextPrimary
                                    )
                                }
                                Text(
                                    text = "Selecione a sua pasta de ROMs de PlayStation 1 e faça uma varredura para listar seus arquivos originais sem copiá-los.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DomDeckTextSecondary
                                )
                                GlassActionButton(
                                    onClick = onOpenPs1Setup,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Configurar PlayStation 1",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.allGames.take(10),
                                key = { it.id }
                            ) { game ->
                                val meta = uiState.gameMetadataByUri[game.uriString]
                                val resolvedTitle = meta?.effectiveTitle?.takeIf { it.isNotBlank() }
                                    ?: game.displayTitle
                                GlassCard(
                                    modifier = Modifier
                                        .width(148.dp)
                                        .testTag("home_game_card_${game.id}"),
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = 10.dp,
                                    onClick = { onOpenGameDetails(game) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GameCoverArtwork(
                                            title = resolvedTitle,
                                            consoleShortName = "PS1",
                                            fileType = game.fileType,
                                            coverPath = meta?.effectiveCoverPath,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.76f)
                                        )
                                        Text(
                                            text = resolvedTitle,
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
        }
    }
}
