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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.example.model.ConsoleCatalog
import com.example.model.ConsoleIntegrationStatus
import com.example.model.ConsoleSystem
import com.example.ui.components.ConsoleShowcase
import com.example.ui.components.ConsoleShowcaseMode
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.viewmodel.DomDeckUiState

/**
 * Frontend-style system selector: one focused console and previous/next systems.
 * No grid and no scrolling panel.
 */
@Composable
fun ConsolesScreen(
    uiState: DomDeckUiState,
    onOpenPs1WithShowcase: () -> Unit,
    onOpenPs1Setup: () -> Unit,
    onReturnHome: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val systems = ConsoleCatalog.allConsoles
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = systems[selectedIndex]
    var showUnavailable by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxHeight < 410.dp
        val edge = if (compact) 12.dp else 22.dp

        Column(
            modifier = Modifier.fillMaxSize()
                .padding(start = edge, end = edge, top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(if (compact) 58.dp else 76.dp)) {
                Text(
                    text = "DOMDECK",
                    style = MaterialTheme.typography.labelLarge,
                    color = DomDeckAccent,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SISTEMAS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DomDeckTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedIndex + 1} / ${systems.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DomDeckTextSecondary
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopEnd).testTag("nav_settings")
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = DomDeckTextPrimary)
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .pointerInput(selectedIndex) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                total += amount
                            },
                            onDragEnd = {
                                if (total < -55f && selectedIndex < systems.lastIndex) selectedIndex++
                                else if (total > 55f && selectedIndex > 0) selectedIndex--
                                total = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val stageHeight = maxHeight
                val stageWidth = maxWidth
                val centerHeight = (stageHeight * 0.72f).coerceAtMost(290.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                        systems.getOrNull(selectedIndex - 1)?.let { system ->
                            ConsoleNeighbor(system = system, onClick = { selectedIndex-- })
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width((stageWidth * 0.35f).coerceAtMost(320.dp))
                            .height(centerHeight)
                            .clickable {
                                if (selected.id == ConsoleCatalog.PS1_ID) onOpenPs1WithShowcase()
                                else showUnavailable = true
                            }
                            .testTag("console_card_${selected.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected.id == ConsoleCatalog.PS1_ID) {
                            ConsoleShowcase(
                                consoleName = selected.name,
                                consoleShortName = selected.shortName,
                                mode = ConsoleShowcaseMode.IDLE_PREVIEW,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = selected.shortName,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = DomDeckTextPrimary,
                                modifier = Modifier.graphicsLayer { alpha = 0.74f }
                            )
                        }
                    }
                    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                        systems.getOrNull(selectedIndex + 1)?.let { system ->
                            ConsoleNeighbor(system = system, onClick = { selectedIndex++ })
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.height(if (compact) 60.dp else 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DomDeckTextPrimary
                )
                Text(
                    text = if (selected.integrationStatus == ConsoleIntegrationStatus.AVAILABLE) {
                        val count = uiState.consoleGameCounts[selected.id] ?: 0
                        "${count} jogos • ${selected.targetEmulatorName ?: "Emulador externo"}"
                    } else {
                        "Integração em desenvolvimento"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DomDeckTextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VOLTAR",
                    color = DomDeckTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable(onClick = onReturnHome)
                        .padding(10.dp).testTag("nav_home")
                )
                IconButton(
                    onClick = { if (selectedIndex > 0) selectedIndex-- },
                    enabled = selectedIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Console anterior")
                }
                Text(
                    text = if (selected.id == ConsoleCatalog.PS1_ID) "ABRIR BIBLIOTECA" else "EM DESENVOLVIMENTO",
                    color = DomDeckAccent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            if (selected.id == ConsoleCatalog.PS1_ID) onOpenPs1WithShowcase()
                            else showUnavailable = true
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                )
                IconButton(
                    onClick = { if (selectedIndex < systems.lastIndex) selectedIndex++ },
                    enabled = selectedIndex < systems.lastIndex
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo console")
                }
                Text(
                    text = "CONFIGURAR PS1",
                    color = DomDeckTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable(onClick = onOpenPs1Setup).padding(10.dp)
                )
            }
        }
    }

    if (showUnavailable) {
        AlertDialog(
            onDismissRequest = { showUnavailable = false },
            title = { Text(selected.name) },
            text = { Text("Este console ainda não possui integração no DomDeck. O PlayStation 1 é o sistema disponível nesta versão.") },
            confirmButton = {
                TextButton(onClick = { showUnavailable = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
private fun ConsoleNeighbor(system: ConsoleSystem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(142.dp)
            .height(150.dp)
            .graphicsLayer { alpha = 0.5f }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = system.name,
            style = MaterialTheme.typography.titleLarge,
            color = DomDeckTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
