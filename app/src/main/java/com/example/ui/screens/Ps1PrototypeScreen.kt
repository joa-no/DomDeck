package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.math.Rotation
import com.example.ui.theme.DomDeckAccent
import com.example.ui.theme.DomDeckTextPrimary
import com.example.ui.theme.DomDeckTextSecondary

/**
 * Isolated real-time 3D geometry / animation experiment.
 * No game scanner, metadata, or emulator state is mutated here.
 * The model uses placeholder surfaces, not copyrighted game box art.
 */
@Composable
fun Ps1PrototypeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var requestedClip by remember { mutableStateOf<String?>(null) }
    var yaw by remember { mutableFloatStateOf(0f) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, "models/domdeck_ps1_test.glb")

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF18212A)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.testTag("prototype_back")) {
                Text("Voltar")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DomDeck · Caixa PS1", color = DomDeckTextPrimary)
                Text("Teste isolado de geometria e animação", color = DomDeckTextSecondary)
            }
            Text(if (model == null) "Carregando..." else "Modelo 3D", color = DomDeckAccent)
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            SceneView(
                modifier = Modifier.fillMaxSize().testTag("ps1_prototype_viewport"),
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = rememberCameraManipulator()
            ) {
                model?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 1.3f,
                        autoAnimate = false,
                        animationName = requestedClip,
                        animationLoop = false,
                        rotation = Rotation(y = yaw)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { requestedClip = "Open" },
                enabled = model != null,
                colors = ButtonDefaults.buttonColors(containerColor = DomDeckAccent),
                modifier = Modifier.testTag("prototype_open")
            ) { Text("Abrir") }
            OutlinedButton(
                onClick = { requestedClip = "Close" },
                enabled = model != null,
                modifier = Modifier.testTag("prototype_close")
            ) { Text("Fechar") }
            OutlinedButton(
                onClick = { yaw = (yaw + 30f) % 360f },
                enabled = model != null,
                modifier = Modifier.testTag("prototype_rotate")
            ) { Text("Girar 30°") }
            Text("Arraste para orbitar", color = DomDeckTextSecondary)
        }
    }
}
