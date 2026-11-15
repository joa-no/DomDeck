package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
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
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.ConsoleIntegrationStatus
import com.example.model.ConsoleSystem
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
fun SettingsScreen(
    uiState: DomDeckUiState,
    onOpenPs1Setup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedInDevConsole by remember { mutableStateOf<ConsoleSystem?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize()
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Configurações",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DomDeckTextPrimary
                    )
                    Text(
                        text = "Configure o emulador e a pasta de leitura para cada console suportado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DomDeckTextSecondary
                    )
                }
            }

            item {
                Text(
                    text = "Consoles cadastrados",
                    style = MaterialTheme.typography.titleMedium,
                    color = DomDeckTextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(
                items = ConsoleCatalog.allConsoles,
                key = { it.id }
            ) { console ->
                val isPs1 = console.integrationStatus == ConsoleIntegrationStatus.AVAILABLE
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_console_row_${console.id}"),
                    shape = RoundedCornerShape(18.dp),
                    accentGlow = isPs1,
                    onClick = {
                        if (isPs1) onOpenPs1Setup() else selectedInDevConsole = console
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPs1) DomDeckAccentMuted else Color(0x66202638),
                            border = BorderStroke(
                                1.dp,
                                if (isPs1) DomDeckAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPs1) Icons.Outlined.Album else Icons.Outlined.Memory,
                                    contentDescription = console.name,
                                    tint = if (isPs1) DomDeckAccent else DomDeckTextTertiary
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isPs1) "Configurar PlayStation 1" else console.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = DomDeckTextPrimary
                            )
                            if (isPs1) {
                                val folderText = uiState.ps1Settings.folderLabel
                                    ?: "Nenhuma pasta selecionada"
                                val emuText = when {
                                    uiState.duckStationStatus.isInstalled &&
                                        uiState.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE ->
                                        "DuckStation associado"
                                    uiState.duckStationStatus.isInstalled ->
                                        "DuckStation detectado"
                                    else -> "DuckStation não encontrado"
                                }
                                Text(
                                    text = "$emuText • $folderText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DomDeckTextSecondary
                                )
                            } else {
                                StatusPill(
                                    text = "Integração em desenvolvimento",
                                    tone = StatusTone.NEUTRAL,
                                    icon = Icons.Outlined.Build
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DomDeckTextSecondary
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Sobre o DomDeck",
                    style = MaterialTheme.typography.titleMedium,
                    color = DomDeckTextSecondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = DomDeckAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "DomDeck v0.1 • Frontend e Launcher",
                                style = MaterialTheme.typography.titleSmall,
                                color = DomDeckTextPrimary
                            )
                        }
                        Text(
                            text = "O DomDeck organiza suas ROMs diretamente na pasta autorizada via SAF sem copiar arquivos, não inclui jogos, BIOS ou emuladores e solicita a abertura dos títulos em emuladores instalados por você.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DomDeckTextSecondary
                        )
                    }
                }
            }
        }
    }

    selectedInDevConsole?.let { console ->
        AlertDialog(
            onDismissRequest = { selectedInDevConsole = null },
            containerColor = DomDeckSurface.copy(alpha = 0.94f),
            title = {
                Text(
                    text = console.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = DomDeckTextPrimary
                )
            },
            text = {
                Text(
                    text = "Integração em desenvolvimento. A configuração completa da v0.1 está disponível para PlayStation 1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DomDeckTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedInDevConsole = null }) {
                    Text("Fechar", color = DomDeckAccent)
                }
            }
        )
    }
}
