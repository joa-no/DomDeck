package com.example.ui.screens

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GameMetadata
import com.example.data.MetadataFetchStatus
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import com.example.model.LaunchResult
import com.example.ui.components.EditGameMetadataDialog
import com.example.ui.components.GameCoverArtwork
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckSurface
import com.example.ui.theme.DomDeckSurfaceElevated
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary
import com.example.viewmodel.DomDeckUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    game: GameEntry?,
    uiState: DomDeckUiState,
    onEnsureMetadata: (game: GameEntry, forceRefresh: Boolean) -> Unit,
    onSaveManualMetadata: (
        game: GameEntry,
        customTitle: String?,
        customSynopsis: String?,
        pickedCoverUri: Uri?,
        clearCustomCover: Boolean
    ) -> Unit,
    onRestoreAutomaticMetadata: (GameEntry) -> Unit,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onPlayClick: (GameEntry) -> Unit,
    onOpenDuckStationApp: () -> Unit,
    onDismissLaunchResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isDuckInstalled = uiState.duckStationStatus.isInstalled
    val isDuckAssociated =
        uiState.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE
    val canAttemptPlay = game != null && isDuckInstalled && isDuckAssociated

    val metadata = if (game != null) {
        uiState.gameMetadataByUri[game.uriString] ?: GameMetadata.fallbackFor(game)
    } else {
        GameMetadata()
    }

    LaunchedEffect(game?.uriString) {
        if (game != null) {
            onEnsureMetadata(game, false)
        }
    }

    var isEditDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalhes do título",
                        style = MaterialTheme.typography.titleLarge,
                        color = DomDeckTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DomDeckTextPrimary
                        )
                    }
                },
                actions = {
                    if (game != null) {
                        IconButton(
                            onClick = { onEnsureMetadata(game, true) },
                            enabled = !metadata.isLoading,
                            modifier = Modifier.testTag("details_refresh_metadata_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Atualizar metadados",
                                tint = DomDeckTextSecondary
                            )
                        }
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
            if (game == null) {
                GlassCard(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 480.dp),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Título não encontrado na biblioteca",
                            style = MaterialTheme.typography.titleLarge,
                            color = DomDeckTextPrimary
                        )
                        Text(
                            text = "Este item pode ter sido removido após uma nova varredura.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DomDeckTextSecondary
                        )
                        GlassActionButton(onClick = onBack) {
                            Text("Voltar à biblioteca")
                        }
                    }
                }
            } else {
                val resolvedTitle = metadata.effectiveTitle.ifBlank { game.displayTitle }

                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    val isWide = maxWidth >= 640.dp

                    val coverBlock: @Composable (Modifier) -> Unit = { m ->
                        Column(
                            modifier = m,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GameCoverArtwork(
                                title = resolvedTitle,
                                consoleShortName = "PS1",
                                fileType = game.fileType,
                                coverPath = metadata.effectiveCoverPath,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.76f)
                                    .testTag("details_cover_artwork")
                            )
                            metadata.coverSourceLabel?.let { coverSource ->
                                Text(
                                    text = "Capa: $coverSource",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DomDeckTextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    val detailsBlock: @Composable (Modifier) -> Unit = { m ->
                        Column(
                            modifier = m,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(text = "PlayStation 1", tone = StatusTone.ACCENT)
                                StatusPill(text = game.fileType, tone = StatusTone.NEUTRAL)
                                if (game.needsVerification) {
                                    StatusPill(
                                        text = "Requer verificação",
                                        tone = StatusTone.WARNING,
                                        icon = Icons.Outlined.WarningAmber
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = resolvedTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DomDeckTextPrimary,
                                    modifier = Modifier.testTag("details_game_title")
                                )
                                if (!resolvedTitle.equals(game.displayTitle, ignoreCase = true)) {
                                    Text(
                                        text = game.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DomDeckTextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Play CTA + explanation when disabled (DuckStation Method A preserved)
                            GlassActionButton(
                                onClick = { onPlayClick(game) },
                                enabled = canAttemptPlay,
                                accentHighlight = canAttemptPlay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("details_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = if (canAttemptPlay) DomDeckAccent else DomDeckTextTertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Jogar",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (!canAttemptPlay) {
                                val disabledReason = when {
                                    !isDuckInstalled ->
                                        "DuckStation não detectado no aparelho. Instale o emulador por conta própria e associe-o em Configurar PlayStation."
                                    else ->
                                        "Nenhum emulador associado ao PS1. Ative a associação com o DuckStation em Configurar PlayStation."
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x66101420),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.testTag("details_play_disabled_reason")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = null,
                                            tint = DomDeckAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = disabledReason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = onOpenSetup) {
                                            Text("Configurar", color = DomDeckAccent)
                                        }
                                    }
                                }
                            }

                            // Description / Synopsis card (honest attribution & manual edit action)
                            GlassCard(
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (metadata.isDescriptionManual) "Sinopse" else "Descrição",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DomDeckTextPrimary,
                                            modifier = Modifier.testTag("details_description_heading")
                                        )
                                        TextButton(
                                            onClick = { isEditDialogVisible = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("details_edit_metadata_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = null,
                                                tint = DomDeckAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Editar informações",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = DomDeckAccent
                                            )
                                        }
                                    }

                                    if (metadata.isLoading) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = DomDeckAccent,
                                            trackColor = DomDeckSurfaceElevated
                                        )
                                    }

                                    Text(
                                        text = metadata.effectiveDescription
                                            ?: GameMetadata.DESCRIPTION_UNAVAILABLE,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (metadata.effectiveDescription != null) {
                                            DomDeckTextSecondary
                                        } else {
                                            DomDeckTextTertiary
                                        },
                                        modifier = Modifier.testTag("details_description_text")
                                    )

                                    if (metadata.fetchStatus == MetadataFetchStatus.OFFLINE_NO_CACHE &&
                                        metadata.effectiveDescription == null
                                    ) {
                                        Text(
                                            text = "Sem conexão no momento. As informações serão buscadas quando houver acesso à rede.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DomDeckTextTertiary
                                        )
                                    }

                                    if (!metadata.isDescriptionManual &&
                                        metadata.effectiveDescription != null &&
                                        !metadata.descriptionSourceLabel.isNullOrBlank()
                                    ) {
                                        val licensePart = metadata.descriptionLicense
                                            ?.let { " • $it" }
                                            .orEmpty()
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.testTag("details_metadata_attribution")
                                        ) {
                                            Text(
                                                text = "Fonte: ${metadata.descriptionSourceLabel}$licensePart",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DomDeckTextTertiary
                                            )
                                            metadata.descriptionSourceUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                                Text(
                                                    text = url,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        textDecoration = TextDecoration.Underline
                                                    ),
                                                    color = DomDeckAccent.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.clickable {
                                                        runCatching { uriHandler.openUri(url) }
                                                    }
                                                )
                                            }
                                        }
                                    } else if (metadata.isDescriptionManual) {
                                        Text(
                                            text = "Conteúdo editado manualmente",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DomDeckTextTertiary
                                        )
                                    }
                                }
                            }

                            // File & Integration honest status
                            GlassCard(
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Arquivo e integração",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DomDeckTextPrimary
                                    )

                                    DetailLine("Arquivo original", game.fileName)
                                    DetailLine(
                                        "Tamanho",
                                        if (game.fileSizeBytes > 0L) {
                                            Formatter.formatShortFileSize(context, game.fileSizeBytes)
                                        } else {
                                            "Indisponível"
                                        }
                                    )
                                    DetailLine(
                                        "Emulador",
                                        when {
                                            isDuckInstalled && isDuckAssociated -> "DuckStation associado"
                                            isDuckInstalled -> "DuckStation detectado (não associado)"
                                            else -> "DuckStation não encontrado"
                                        }
                                    )
                                    DetailLine(
                                        "Permissão SAF",
                                        if (uiState.hasFolderReadPermission) {
                                            "Leitura ativa na pasta"
                                        } else {
                                            "Requer verificação da pasta"
                                        }
                                    )
                                    DetailLine("URI SAF original", game.uriString)

                                    if (!game.statusNote.isNullOrBlank()) {
                                        Text(
                                            text = game.statusNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        GlassActionButton(
                                            onClick = onOpenSetup,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Configurações PS1",
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (uiState.duckStationStatus.canLaunchMainApp) {
                                            GlassActionButton(
                                                onClick = onOpenDuckStationApp,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("details_open_duckstation_button"),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Abrir DuckStation",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            coverBlock(Modifier.width(220.dp))
                            detailsBlock(Modifier.weight(1f))
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                coverBlock(Modifier.widthIn(max = 200.dp).fillMaxWidth(0.56f))
                            }
                            detailsBlock(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (isEditDialogVisible && game != null) {
        EditGameMetadataDialog(
            game = game,
            metadata = metadata,
            onDismiss = { isEditDialogVisible = false },
            onSave = { customTitle, customSynopsis, pickedCoverUri, clearCustomCover ->
                isEditDialogVisible = false
                onSaveManualMetadata(
                    game,
                    customTitle,
                    customSynopsis,
                    pickedCoverUri,
                    clearCustomCover
                )
            },
            onRestoreAutomatic = {
                isEditDialogVisible = false
                onRestoreAutomaticMetadata(game)
            }
        )
    }

    when (val launch = uiState.launchResult) {
        LaunchResult.Idle -> Unit
        is LaunchResult.IntentDispatched -> {
            AlertDialog(
                onDismissRequest = onDismissLaunchResult,
                containerColor = DomDeckSurface.copy(alpha = 0.94f),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = DomDeckAccent
                    )
                },
                title = {
                    Text(
                        text = "Solicitação enviada",
                        color = DomDeckTextPrimary
                    )
                },
                text = {
                    Text(
                        text = launch.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DomDeckTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismissLaunchResult()
                            onOpenDuckStationApp()
                        }
                    ) {
                        Text("Abrir DuckStation", color = DomDeckAccent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissLaunchResult) {
                        Text("Fechar", color = DomDeckTextSecondary)
                    }
                }
            )
        }
        is LaunchResult.Failed -> {
            AlertDialog(
                onDismissRequest = onDismissLaunchResult,
                containerColor = DomDeckSurface.copy(alpha = 0.94f),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Não foi possível iniciar diretamente",
                        color = DomDeckTextPrimary
                    )
                },
                text = {
                    Text(
                        text = launch.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DomDeckTextSecondary
                    )
                },
                confirmButton = {
                    if (launch.canOpenMainApp) {
                        TextButton(
                            onClick = {
                                onDismissLaunchResult()
                                onOpenDuckStationApp()
                            }
                        ) {
                            Text("Abrir DuckStation", color = DomDeckAccent)
                        }
                    } else {
                        TextButton(onClick = onDismissLaunchResult) {
                            Text("Entendi", color = DomDeckAccent)
                        }
                    }
                },
                dismissButton = {
                    if (launch.canOpenMainApp) {
                        TextButton(onClick = onDismissLaunchResult) {
                            Text("Fechar", color = DomDeckTextSecondary)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DomDeckTextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = DomDeckTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
