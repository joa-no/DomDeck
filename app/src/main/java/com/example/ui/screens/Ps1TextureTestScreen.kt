package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.viewmodel.DomDeckUiState
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.rememberCameraManipulator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Deliberately separate from Ps1PrototypeScreen: verifies the local cover
 * bitmap and SceneView ImageNode without replacing or modifying the working GLB.
 * The same Bitmap is shown as Compose 2D and as a textured 3D plane.
 */
@Composable
fun Ps1TextureTestScreen(
    uiState: DomDeckUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val games = uiState.ps1Games
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(games.size) {
        selectedIndex = selectedIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    }
    val game = games.getOrNull(selectedIndex)
    val coverPath = game?.let { uiState.gameMetadataByUri[it.uriString]?.effectiveCoverPath }
    val title = game?.let {
        uiState.gameMetadataByUri[it.uriString]?.effectiveTitle
            ?.takeIf { value -> value.isNotBlank() } ?: it.displayTitle
    } ?: "Nenhum jogo"

    val diagnostic = remember { createDiagnosticBitmap() }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Verificando imagem...") }
    var yaw by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(game?.uriString, coverPath) {
        loadedBitmap = null
        status = when {
            game == null -> "Sem jogos · usando padrão de diagnóstico"
            coverPath.isNullOrBlank() -> "Capa não cadastrada · usando padrão de diagnóstico"
            else -> "Lendo capa local..."
        }
        if (!coverPath.isNullOrBlank()) {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(coverPath)
                    check(file.isFile && file.length() > 0L)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    check(bounds.outWidth > 0 && bounds.outHeight > 0)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) {
                        sample *= 2
                    }
                    BitmapFactory.decodeFile(
                        file.absolutePath,
                        BitmapFactory.Options().apply { inSampleSize = sample }
                    ) ?: error("O Android não decodificou a imagem")
                }.getOrNull()
            }
            if (bitmap != null) {
                loadedBitmap = bitmap
                status = "Imagem local decodificada: ${bitmap.width} × ${bitmap.height}"
            } else {
                status = "Falha ao ler a capa local · usando padrão de diagnóstico"
            }
        }
    }

    // One Bitmap is sent to both renderers, so the left and right sides must
    // show the same artwork if reading and 3D texture upload are both healthy.
    val image = loadedBitmap ?: diagnostic

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF18212A)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.testTag("texture_test_back")) {
                Text("Voltar")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Teste isolado de textura 3D", color = Color.White)
                Text(
                    text = status,
                    color = Color(0xFFB9CBD8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("texture_test_status")
                )
            }
            Text(if (loadedBitmap != null) "CAPA LOCAL" else "PADRÃO DE TESTE", color = Color(0xFFAC8CF5))
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxSize().testTag("texture_test_3d_viewport"),
                contentAlignment = Alignment.Center
            ) {
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    cameraManipulator = rememberCameraManipulator()
                ) {
                    ImageNode(
                        bitmap = image,
                        size = Size(x = 1.05f, y = 1.35f),
                        rotation = Rotation(y = yaw)
                    )
                }
            }
            Column(
                modifier = Modifier.width(145.dp).padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Referência 2D", color = Color.White)
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Referência 2D da mesma imagem enviada ao 3D",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(125.dp)
                        .height(155.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("texture_test_2d_reference")
                )
                Text("Compare com a imagem 3D", color = Color(0xFFB9CBD8))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { if (selectedIndex > 0) selectedIndex-- },
                enabled = selectedIndex > 0
            ) { Text("Anterior") }
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { if (selectedIndex < games.lastIndex) selectedIndex++ },
                enabled = selectedIndex < games.lastIndex
            ) { Text("Próximo") }
            OutlinedButton(
                onClick = { yaw = (yaw + 30f) % 360f },
                modifier = Modifier.testTag("texture_test_rotate")
            ) { Text("Girar 30°") }
        }
    }
}

private fun createDiagnosticBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(256, 320, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.rgb(31, 68, 133)
    canvas.drawColor(paint.color)
    paint.color = AndroidColor.rgb(251, 208, 64)
    canvas.drawRect(28f, 28f, 228f, 292f, paint)
    paint.color = AndroidColor.rgb(31, 68, 133)
    canvas.drawRect(44f, 44f, 212f, 276f, paint)
    paint.color = AndroidColor.WHITE
    paint.textSize = 33f
    paint.isFakeBoldText = true
    canvas.drawText("TESTE", 65f, 148f, paint)
    canvas.drawText("3D", 107f, 202f, paint)
    return bitmap
}
