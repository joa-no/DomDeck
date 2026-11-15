package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.ScanState
import com.example.ui.components.GlassActionButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckBackground
import com.example.ui.theme.DomDeckError
import com.example.ui.theme.DomDeckErrorContainer
import com.example.ui.theme.DomDeckSuccess
import com.example.ui.theme.DomDeckSuccessContainer
import com.example.ui.theme.DomDeckSurfaceElevated
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary
import com.example.viewmodel.DomDeckUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleSetupScreen(
    uiState: DomDeckUiState,
    onBack: () -> Unit,
    onRefreshDetection: () -> Unit,
    onFolderPicked: (Uri?) -> Unit,
    onAssociateDuckStation: (Boolean) -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onOpenPs1Library: () -> Unit,
    onDismissInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose {
            onCancelScan()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = onFolderPicked
    )

    val hasFolder = !uiState.ps1Settings.folderUri.isNullOrBlank()
    val isDuckInstalled = uiState.duckStationStatus.isInstalled
    val isDuckAssociated =
        uiState.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE
    val isScanning = uiState.scanState is ScanState.Scanning

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurar PlayStation",
                        style = MaterialTheme.typography.titleLarge,
                        color = DomDeckTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DomDeckTextPrimary
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxSize()
                    .testTag("console_setup_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.infoMessage?.let { msg ->
                    item {
                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            accentGlow = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DomDeckTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissInfo) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Fechar aviso",
                                        tint = DomDeckTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Emulator detection & association
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SportsEsports,
                                        contentDescription = null,
                                        tint = DomDeckAccent
                                    )
                                    Text(
                                        text = "1. Emulador externo",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DomDeckTextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onRefreshDetection,
                                    modifier = Modifier.testTag("setup_refresh_emulator_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Verificar DuckStation novamente",
                                        tint = DomDeckTextSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill(
                                    text = if (isDuckInstalled) {
                                        "DuckStation detectado"
                                    } else {
                                        "DuckStation não encontrado"
                                    },
                                    tone = if (isDuckInstalled) {
                                        StatusTone.SUCCESS
                                    } else {
                                        StatusTone.WARNING
                                    },
                                    icon = if (isDuckInstalled) {
                                        Icons.Outlined.CheckCircle
                                    } else {
                                        Icons.Outlined.WarningAmber
                                    },
                                    modifier = Modifier.testTag("duckstation_status_pill")
                                )
                                uiState.duckStationStatus.versionName?.let { ver ->
                                    Text(
                                        text = "v$ver",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DomDeckTextTertiary
                                    )
                                }
                            }

                            Text(
                                text = "Pacote verificado: ${ConsoleCatalog.DUCKSTATION_PACKAGE}. O DomDeck não instala nem altera configurações internas do DuckStation (BIOS, controles e pastas do emulador continuam configurados por você no próprio app).",
                                style = MaterialTheme.typography.bodySmall,
                                color = DomDeckTextSecondary
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x66101420),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Associar DuckStation ao PS1",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DomDeckTextPrimary
                                        )
                                        Text(
                                            text = if (isDuckInstalled) {
                                                "Habilita tentativa de abertura pelo botão Jogar"
                                            } else {
                                                "Instale o DuckStation para habilitar o lançamento"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isDuckAssociated,
                                        onCheckedChange = onAssociateDuckStation,
                                        enabled = isDuckInstalled || isDuckAssociated,
                                        modifier = Modifier.testTag("associate_duckstation_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DomDeckBackground,
                                            checkedTrackColor = DomDeckAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Folder picker (SAF)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = DomDeckAccent
                                )
                                Text(
                                    text = "2. Pasta de ROMs (SAF)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DomDeckTextPrimary
                                )
                            }

                            if (hasFolder) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x66101420),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = uiState.ps1Settings.folderLabel ?: "Pasta selecionada",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DomDeckTextPrimary,
                                            modifier = Modifier.testTag("selected_folder_label")
                                        )
                                        Text(
                                            text = uiState.ps1Settings.folderUri.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextTertiary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        StatusPill(
                                            text = if (uiState.hasFolderReadPermission) {
                                                "Permissão de leitura persistida"
                                            } else {
                                                "Permissão pendente — selecione novamente"
                                            },
                                            tone = if (uiState.hasFolderReadPermission) {
                                                StatusTone.SUCCESS
                                            } else {
                                                StatusTone.WARNING
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhuma pasta selecionada. O DomDeck utiliza somente acesso de leitura do seletor nativo do Android e nunca copia seus arquivos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DomDeckTextSecondary
                                )
                            }

                            GlassActionButton(
                                onClick = {
                                    val initialUri = uiState.ps1Settings.folderUri?.let {
                                        runCatching { Uri.parse(it) }.getOrNull()
                                    }
                                    folderPickerLauncher.launch(initialUri)
                                },
                                enabled = !isScanning,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_ps1_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    tint = DomDeckAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasFolder) "Alterar pasta" else "Selecionar pasta",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // 3. Scanner
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = DomDeckAccent
                                )
                                Text(
                                    text = "3. Varredura da biblioteca",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DomDeckTextPrimary
                                )
                            }

                            Text(
                                text = "Na v0.1, o scanner identifica candidatos PS1 pelas extensões .chd, .cue, .pbp, .iso e .m3u. Arquivos auxiliares (.sbi, .sub e .bin vinculados a .cue) são ignorados; arquivos .bin sem .cue são marcados para verificação. A identificação por extensão é aproximada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DomDeckTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassActionButton(
                                    onClick = onStartScan,
                                    enabled = hasFolder && !isScanning,
                                    accentHighlight = hasFolder && !isScanning,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_ps1_library_button")
                                ) {
                                    Text(
                                        text = "Examinar biblioteca",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                if (isScanning) {
                                    GlassActionButton(
                                        onClick = onCancelScan,
                                        dangerHighlight = true,
                                        modifier = Modifier.testTag("cancel_scan_button")
                                    ) {
                                        Text(
                                            text = "Cancelar",
                                            color = DomDeckError,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            when (val state = uiState.scanState) {
                                ScanState.Idle -> {
                                    if (uiState.ps1Games.isNotEmpty()) {
                                        Text(
                                            text = "Biblioteca atual: ${uiState.ps1Games.size} entrada(s) indexada(s).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextSecondary
                                        )
                                    }
                                }

                                is ScanState.Scanning -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .testTag("scan_progress_indicator"),
                                            color = DomDeckAccent,
                                            trackColor = DomDeckSurfaceElevated
                                        )
                                        Text(
                                            text = "Examinando \"${state.currentFolder}\" (${state.inspectedEntries} itens verificados)…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DomDeckTextSecondary
                                        )
                                    }
                                }

                                is ScanState.Success -> {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DomDeckSuccessContainer,
                                        border = BorderStroke(1.dp, DomDeckSuccess.copy(alpha = 0.35f)),
                                        modifier = Modifier.testTag("scan_success_card")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = DomDeckSuccess,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DomDeckTextPrimary
                                            )
                                        }
                                    }
                                }

                                is ScanState.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = DomDeckErrorContainer,
                                        border = BorderStroke(1.dp, DomDeckError.copy(alpha = 0.35f)),
                                        modifier = Modifier.testTag("scan_error_card")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ErrorOutline,
                                                contentDescription = null,
                                                tint = DomDeckError,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DomDeckTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.ps1Games.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = onOpenPs1Library,
                                        modifier = Modifier.testTag("setup_open_library_button")
                                    ) {
                                        Text(
                                            text = "Abrir biblioteca PS1 (${uiState.ps1Games.size})",
                                            color = DomDeckAccent
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
