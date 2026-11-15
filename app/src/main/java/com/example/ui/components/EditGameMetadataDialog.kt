package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.GameMetadata
import com.example.model.GameEntry
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckBorder
import com.example.ui.theme.DomDeckSurface
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary
import com.example.ui.theme.DomDeckTextTertiary

@Composable
fun EditGameMetadataDialog(
    game: GameEntry,
    metadata: GameMetadata,
    onDismiss: () -> Unit,
    onSave: (customTitle: String?, customSynopsis: String?, pickedCoverUri: Uri?, clearCustomCover: Boolean) -> Unit,
    onRestoreAutomatic: () -> Unit
) {
    var titleText by remember(game.uriString, metadata.effectiveTitle) {
        mutableStateOf(metadata.effectiveTitle)
    }
    var synopsisText by remember(game.uriString, metadata.effectiveDescription) {
        mutableStateOf(metadata.effectiveDescription.orEmpty())
    }
    var pickedCoverUri by remember(game.uriString) {
        mutableStateOf<Uri?>(null)
    }
    var clearCustomCover by remember(game.uriString) {
        mutableStateOf(false)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedCoverUri = uri
            clearCustomCover = false
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0x99121622),
        unfocusedContainerColor = Color(0x7710141F),
        focusedBorderColor = DomDeckAccent.copy(alpha = 0.78f),
        unfocusedBorderColor = DomDeckBorder.copy(alpha = 0.7f),
        focusedTextColor = DomDeckTextPrimary,
        unfocusedTextColor = DomDeckTextPrimary
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DomDeckSurface.copy(alpha = 0.96f),
        title = {
            Text(
                text = "Editar informações",
                style = MaterialTheme.typography.titleLarge,
                color = DomDeckTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "As alterações manuais são salvas localmente e têm prioridade sobre a busca automática. O arquivo original não é modificado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DomDeckTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(0.76f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF10131D))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(10.dp))
                    ) {
                        when {
                            pickedCoverUri != null -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(pickedCoverUri).build(),
                                    contentDescription = "Pré-visualização da capa selecionada",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            !clearCustomCover -> {
                                GameCoverArtwork(
                                    title = titleText.ifBlank { game.displayTitle },
                                    consoleShortName = "PS1",
                                    fileType = game.fileType,
                                    coverPath = metadata.effectiveCoverPath,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                GameCoverPlaceholder(
                                    title = titleText.ifBlank { game.displayTitle },
                                    consoleShortName = "PS1",
                                    fileType = game.fileType,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassActionButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_pick_cover_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = DomDeckAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Escolher capa",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if ((metadata.isManualCover && !clearCustomCover) || pickedCoverUri != null) {
                            GlassActionButton(
                                onClick = {
                                    pickedCoverUri = null
                                    clearCustomCover = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_clear_cover_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = DomDeckTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Remover capa manual",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Título exibido", color = DomDeckTextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_title_input")
                )

                OutlinedTextField(
                    value = synopsisText,
                    onValueChange = { synopsisText = it },
                    label = { Text("Sinopse / Descrição", color = DomDeckTextTertiary) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_synopsis_input")
                )

                TextButton(
                    onClick = onRestoreAutomatic,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .testTag("edit_restore_auto_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = DomDeckAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar automático", color = DomDeckAccent)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedTitle = titleText.trim().takeIf { it.isNotBlank() }
                    val trimmedSynopsis = synopsisText.trim().takeIf { it.isNotBlank() }
                    val saveTitle = if (
                        !metadata.isManualTitle &&
                        trimmedTitle == metadata.effectiveTitle
                    ) {
                        null
                    } else {
                        trimmedTitle
                    }
                    val saveSynopsis = if (
                        !metadata.isManualSynopsis &&
                        trimmedSynopsis == metadata.effectiveDescription
                    ) {
                        null
                    } else {
                        trimmedSynopsis
                    }
                    onSave(saveTitle, saveSynopsis, pickedCoverUri, clearCustomCover)
                },
                modifier = Modifier.testTag("edit_save_button")
            ) {
                Text("Salvar", color = DomDeckAccent)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_cancel_button")
            ) {
                Text("Cancelar", color = DomDeckTextSecondary)
            }
        }
    )
}
