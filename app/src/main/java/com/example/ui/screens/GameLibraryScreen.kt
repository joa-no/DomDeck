package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.data.GameMetadata
import com.example.model.GameEntry
import com.example.model.ScanState
import com.example.ui.components.ConsoleShowcase
import com.example.ui.components.ConsoleShowcaseMode
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckBorder
import com.example.ui.theme.DomDeckSurfaceElevated
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary
import com.example.viewmodel.DomDeckUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    uiState: DomDeckUiState,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onFolderPicked: (Uri?) -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenGameDetails: (GameEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose {
            onCancelScan()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = onFolderPicked
    )

    val hasFolder = !uiState.ps1Settings.folderUri.isNullOrBlank()
    val isScanning = uiState.scanState is ScanState.Scanning

    val filteredGames = remember(uiState.ps1Games, uiState.gameMetadataByUri, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            uiState.ps1Games
        } else {
            uiState.ps1Games.filter { game ->
                val resolvedTitle = uiState.gameMetadataByUri[game.uriString]?.effectiveTitle
                (resolvedTitle != null && resolvedTitle.contains(q, ignoreCase = true)) ||
                    game.displayTitle.contains(q, ignoreCase = true) ||
                    game.fileName.contains(q, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PlayStation 1",
                            style = MaterialTheme.typography.titleLarge,
                            color = DomDeckTextPrimary
                        )
                        StatusPill(
                            text = "${uiState.ps1Games.size}",
                            tone = StatusTone.ACCENT
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("library_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DomDeckTextPrimary
                        )
                    }
                },
                actions = {
                    if (hasFolder) {
                        IconButton(
                            onClick = { if (!isScanning) onStartScan() },
                            modifier = Modifier.testTag("library_rescan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Reexaminar biblioteca",
                                tint = DomDeckTextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSetup,
                        modifier = Modifier.testTag("library_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Configurar PlayStation",
                            tint = DomDeckTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x7A0E111A)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.ps1Games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .testTag("library_empty_state"),
                        shape = RoundedCornerShape(24.dp),
                        elevated = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ConsoleShowcase(
                                consoleName = "PlayStation 1",
                                consoleShortName = "PS1",
                                mode = ConsoleShowcaseMode.IDLE_PREVIEW,
                                modifier = Modifier.size(width = 230.dp, height = 152.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (hasFolder) {
                                    "Nenhum jogo indexado na pasta atual"
                                } else {
                                    "Biblioteca PlayStation 1 não configurada"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = DomDeckTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (hasFolder) {
                                    "Pasta selecionada: ${uiState.ps1Settings.folderLabel ?: "SAF"}. Execute uma varredura para listar ROMs compatíveis (.chd, .cue, .pbp, .iso, .m3u)."
                                } else {
                                    "Selecione a pasta onde estão seus arquivos de PlayStation 1. Os arquivos permanecem no local original sem cópia."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = DomDeckTextSecondary
                            )

                            if (uiState.scanState is ScanState.Scanning) {
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DomDeckAccent,
                                    trackColor = DomDeckSurfaceElevated
                                )
                            }

                            if (uiState.scanState is ScanState.Error) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = uiState.scanState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlassActionButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    enabled = !isScanning,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("library_empty_pick_folder_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = DomDeckAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasFolder) "Alterar pasta" else "Selecionar pasta",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (hasFolder) {
                                    GlassActionButton(
                                        onClick = onStartScan,
                                        enabled = !isScanning,
                                        accentHighlight = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("library_empty_scan_button")
                                    ) {
                                        Text(
                                            text = "Examinar biblioteca",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier
                        .widthIn(max = 1120.dp)
                        .fillMaxSize()
                        .testTag("library_games_grid"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isScanning) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = DomDeckAccent,
                                trackColor = DomDeckSurfaceElevated
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input"),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "Filtrar por nome do arquivo…",
                                    color = DomDeckTextTertiary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = DomDeckTextSecondary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Limpar filtro",
                                            tint = DomDeckTextSecondary
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xA6151A28),
                                unfocusedContainerColor = Color(0x88121622),
                                focusedBorderColor = DomDeckAccent.copy(alpha = 0.75f),
                                unfocusedBorderColor = DomDeckBorder.copy(alpha = 0.7f),
                                focusedTextColor = DomDeckTextPrimary,
                                unfocusedTextColor = DomDeckTextPrimary
                            )
                        )
                    }

                    items(
                        items = filteredGames,
                        key = { it.id }
                    ) { game ->
                        GameLibraryCard(
                            game = game,
                            metadata = uiState.gameMetadataByUri[game.uriString],
                            onClick = { onOpenGameDetails(game) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameLibraryCard(
    game: GameEntry,
    metadata: GameMetadata?,
    onClick: () -> Unit
) {
    val resolvedTitle = metadata?.effectiveTitle?.takeIf { it.isNotBlank() } ?: game.displayTitle

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_card_${game.id}"),
        shape = RoundedCornerShape(20.dp),
        elevated = true,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameCoverArtwork(
                title = resolvedTitle,
                consoleShortName = "PS1",
                fileType = game.fileType,
                coverPath = metadata?.effectiveCoverPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.76f)
            )

            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.titleSmall,
                color = DomDeckTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("library_card_title_${game.id}")
            )

            if (game.needsVerification) {
                StatusPill(
                    text = "Requer verificação",
                    tone = StatusTone.WARNING,
                    icon = Icons.Outlined.WarningAmber
                )
            } else if (metadata?.hasManualEdits == true) {
                StatusPill(
                    text = "Editado",
                    tone = StatusTone.ACCENT
                )
            }
        }
    }
}
