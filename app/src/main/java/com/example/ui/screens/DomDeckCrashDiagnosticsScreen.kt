package com.example.ui.screens

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/**
 * Reads the Android OS-owned exit history for this package.
 *
 * No SceneView/Filament instance is created here. Crash records do not
 * guarantee the presence of a stack trace and may be overwritten by Android.
 * Nothing is transmitted automatically; copying/exporting requires a tap.
 */
@Composable
fun DomDeckCrashDiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<ApplicationExitInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf("") }

    LaunchedEffect(revision) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    manager.getHistoricalProcessExitReasons(context.packageName, 0, 20)
                } else {
                    emptyList()
                }
            }
        }
        result.onSuccess {
            records = it
            errorMessage = null
        }.onFailure {
            records = emptyList()
            errorMessage = it.message ?: it.javaClass.simpleName
        }
        isLoading = false
    }

    // Native tombstones are protobuf files, not plain logcat text.
    // They can be useful even when the visible crash summary lacks a stack.
    val latestNative = records.firstOrNull {
        it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE
    }
    val exportTombstone = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination ->
        val selected = latestNative
        if (destination != null && selected != null) {
            scope.launch {
                feedback = "Salvando rastro nativo..."
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val input = selected.traceInputStream
                            ?: error("O Android não preservou o rastro nativo.")
                        input.use { trace ->
                            val output = context.contentResolver.openOutputStream(destination)
                                ?: error("Não foi possível criar o arquivo.")
                            output.use { file -> trace.copyTo(file) }
                        }
                    }
                }
                feedback = saved.fold(
                    onSuccess = { "Rastro nativo salvo (${it} bytes)." },
                    onFailure = { "Rastro indisponível: ${it.message ?: it.javaClass.simpleName}" }
                )
            }
        }
    }

    val report = remember(records, errorMessage, isLoading) {
        buildExitReport(records, errorMessage, isLoading)
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF18212A)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.testTag("crash_report_back")) {
                Text("Voltar")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Diagnóstico de encerramentos", color = Color.White)
                Text("Somente registros locais do Android · nenhum envio automático",
                    color = Color(0xFFC1CEDB))
            }
            OutlinedButton(
                onClick = { revision++ },
                modifier = Modifier.testTag("crash_report_refresh")
            ) { Text("Atualizar") }
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("DomDeck crash report", report))
                    feedback = "Relatório copiado. Cole o texto na conversa."
                },
                modifier = Modifier.testTag("crash_report_copy")
            ) { Text("Copiar relatório") }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && latestNative != null) {
                OutlinedButton(
                    onClick = { exportTombstone.launch("domdeck-native-crash.pb") },
                    modifier = Modifier.testTag("crash_report_export_native")
                ) { Text("Salvar rastro nativo") }
            }
        }

        if (feedback.isNotBlank()) {
            Text(feedback, color = Color(0xFFBFA5FF))
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                text = report,
                color = Color(0xFFE3EAF1),
                modifier = Modifier.fillMaxWidth().testTag("crash_report_text")
            )
            Text(
                text = "Os rastros nativos podem conter detalhes técnicos e dados do processo. " +
                    "Revise o arquivo antes de compartilhá-lo. O Android pode apagar registros " +
                    "antigos; a ausência de registros não descarta um crash.",
                color = Color(0xFFAAB9C6),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

internal fun exitReasonLabel(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_CRASH -> "Exceção Java/Kotlin"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Falha nativa (C/C++/GPU)"
    ApplicationExitInfo.REASON_ANR -> "Sem resposta (ANR)"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "Memória insuficiente"
    ApplicationExitInfo.REASON_SIGNALED -> "Processo encerrado por sinal"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "Encerramento solicitado pelo usuário/sistema"
    ApplicationExitInfo.REASON_EXIT_SELF -> "Encerramento voluntário do processo"
    ApplicationExitInfo.REASON_OTHER -> "Outro motivo"
    else -> "Motivo ${reason}"
}

private fun buildExitReport(
    records: List<ApplicationExitInfo>,
    errorMessage: String?,
    isLoading: Boolean
): String = buildString {
    appendLine("DomDeck — Histórico de encerramentos")
    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine()
    if (isLoading) {
        appendLine("Consultando registros do Android...")
        return@buildString
    }
    if (errorMessage != null) {
        appendLine("Falha ao consultar os registros: ${errorMessage}")
        return@buildString
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        appendLine("Histórico indisponível neste Android (requer API 30).")
        return@buildString
    }
    if (records.isEmpty()) {
        appendLine("Nenhum encerramento preservado pelo sistema.")
        return@buildString
    }

    appendLine("Registros disponíveis: ${records.size}")
    appendLine("Os primeiros são os mais recentes.")
    records.forEachIndexed { index, info ->
        appendLine()
        appendLine("#${index + 1} — ${exitReasonLabel(info.reason)}")
        appendLine("Data: ${DateFormat.getDateTimeInstance().format(Date(info.timestamp))}")
        appendLine("Motivo: ${info.reason} | Sinal/status: ${info.status} | PID: ${info.pid}")
        appendLine("Processo: ${info.processName}")
        val description = info.description
        if (!description.isNullOrBlank()) {
            appendLine("Descrição: ${description.take(1500)}")
        } else {
            appendLine("Descrição: não fornecida pelo Android")
        }
        if (info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
            appendLine("Possível rastro binário: use 'Salvar rastro nativo' se disponível.")
        }
    }
}
