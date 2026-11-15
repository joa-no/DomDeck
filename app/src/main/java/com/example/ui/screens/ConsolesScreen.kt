package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.ConsoleIntegrationStatus
import com.example.model.ConsoleSystem
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckAccentMuted
import com.example.ui.theme.DomDeckSurface
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary
import com.example.viewmodel.DomDeckUiState

@Composable
fun ConsolesScreen(
    uiState: DomDeckUiState,
    onOpenPs1WithShowcase: () -> Unit,
    onOpenPs1Setup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inDevDialogConsole by remember { mutableStateOf<ConsoleSystem?>(null) }

    val myLibraryConsoles = remember(uiState.consoleGameCounts) {
        ConsoleCatalog.allConsoles.filter { (uiState.consoleGameCounts[it.id] ?: 0) > 0 }
    }
    val exploreConsoles = remember(uiState.consoleGameCounts) {
        ConsoleCatalog.allConsoles.filter { (uiState.consoleGameCounts[it.id] ?: 0) == 0 }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize()
                .testTag("consoles_screen_grid"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Emuladores e Consoles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DomDeckTextPrimary
                    )
                    Text(
                        text = "Consoles organizados de acordo com a quantidade real de jogos indexados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DomDeckTextSecondary
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Minha biblioteca",
                    style = MaterialTheme.typography.titleLarge,
                    color = DomDeckTextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (myLibraryConsoles.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GlassCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("my_library_empty_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhum console com jogos encontrados ainda",
                                style = MaterialTheme.typography.titleMedium,
                                color = DomDeckTextPrimary
                            )
                            Text(
                                text = "Configure a pasta do PlayStation 1 e examine a biblioteca para que ele apareça nesta seção.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DomDeckTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = myLibraryConsoles,
                    key = { "lib_${it.id}" }
                ) { console ->
                    ConsoleCard(
                        console = console,
                        gameCount = uiState.consoleGameCounts[console.id] ?: 0,
                        uiState = uiState,
                        onClick = {
                            if (console.id == ConsoleCatalog.PS1_ID) {
                                onOpenPs1WithShowcase()
                            } else {
                                inDevDialogConsole = console
                            }
                        },
                        onConfigurePs1 = onOpenPs1Setup
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Explorar consoles",
                    style = MaterialTheme.typography.titleLarge,
                    color = DomDeckTextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(
                items = exploreConsoles,
                key = { "explore_${it.id}" }
            ) { console ->
                ConsoleCard(
                    console = console,
                    gameCount = 0,
                    uiState = uiState,
                    onClick = {
                        if (console.id == ConsoleCatalog.PS1_ID) {
                            onOpenPs1WithShowcase()
                        } else {
                            inDevDialogConsole = console
                        }
                    },
                    onConfigurePs1 = onOpenPs1Setup
                )
            }
        }
    }

    inDevDialogConsole?.let { system ->
        AlertDialog(
            onDismissRequest = { inDevDialogConsole = null },
            containerColor = DomDeckSurface.copy(alpha = 0.94f),
            title = {
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = DomDeckTextPrimary
                )
            },
            text = {
                Text(
                    text = "Integração em desenvolvimento.\n\nNa versão 0.1 do DomDeck, o fluxo completo de configuração, varredura SAF e integração exploratória com emulador externo está disponível para PlayStation 1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DomDeckTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { inDevDialogConsole = null }) {
                    Text("Entendi", color = DomDeckAccent)
                }
            }
        )
    }
}

@Composable
private fun ConsoleCard(
    console: ConsoleSystem,
    gameCount: Int,
    uiState: DomDeckUiState,
    onClick: () -> Unit,
    onConfigurePs1: () -> Unit
) {
    val isPs1 = console.id == ConsoleCatalog.PS1_ID
    val isAvailable = console.integrationStatus == ConsoleIntegrationStatus.AVAILABLE
    val isFolderConfigured = isPs1 && !uiState.ps1Settings.folderUri.isNullOrBlank()
    val isDuckAssociated = isPs1 &&
        uiState.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("console_card_${console.id}"),
        shape = RoundedCornerShape(22.dp),
        elevated = isPs1,
        accentGlow = isPs1,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAvailable) DomDeckAccentMuted else Color(0x66202638),
                        border = BorderStroke(
                            1.dp,
                            if (isAvailable) DomDeckAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPs1) Icons.Outlined.Album else Icons.Outlined.Memory,
                                contentDescription = console.name,
                                tint = if (isAvailable) DomDeckAccent else DomDeckTextSecondary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = console.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = DomDeckTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = console.mediaFormatLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = DomDeckTextSecondary
                        )
                    }
                }

                if (isPs1 && gameCount > 0) {
                    StatusPill(
                        text = "$gameCount jogo(s)",
                        tone = StatusTone.ACCENT
                    )
                }
            }

            if (isPs1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = when {
                            isFolderConfigured && uiState.hasFolderReadPermission -> "Pasta configurada"
                            isFolderConfigured -> "Permissão expirada"
                            else -> "Pasta pendente"
                        },
                        tone = when {
                            isFolderConfigured && uiState.hasFolderReadPermission -> StatusTone.SUCCESS
                            isFolderConfigured -> StatusTone.WARNING
                            else -> StatusTone.NEUTRAL
                        }
                    )

                    StatusPill(
                        text = when {
                            uiState.duckStationStatus.isInstalled && isDuckAssociated -> "DuckStation pronto"
                            uiState.duckStationStatus.isInstalled -> "DuckStation detectado"
                            else -> "Emulador ausente"
                        },
                        tone = when {
                            uiState.duckStationStatus.isInstalled && isDuckAssociated -> StatusTone.SUCCESS
                            uiState.duckStationStatus.isInstalled -> StatusTone.ACCENT
                            else -> StatusTone.WARNING
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toque para abrir biblioteca",
                        style = MaterialTheme.typography.labelMedium,
                        color = DomDeckAccent
                    )
                    GlassActionButton(
                        onClick = onConfigurePs1,
                        modifier = Modifier.testTag("console_ps1_setup_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configurar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                StatusPill(
                    text = "Integração em desenvolvimento",
                    tone = StatusTone.NEUTRAL,
                    icon = Icons.Outlined.Build
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Nenhuma ROM indexada para este sistema nesta versão.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DomDeckTextTertiary
                )
            }
        }
    }
}
