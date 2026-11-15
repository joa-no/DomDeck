package com.example.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import com.example.model.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class EmulatorLauncher(
    private val context: Context,
    private val emulatorDetector: EmulatorDetector
) {
    companion object {
        private const val TAG = "DomDeckLauncher"
        private const val DUCKSTATION_EMULATION_ACTIVITY =
            "com.github.stenzek.duckstation.EmulationActivity"
    }

    internal sealed interface CueResolutionResult {
        data class Resolved(val companionUris: List<Uri>) : CueResolutionResult
        data class Failure(val reason: String) : CueResolutionResult
    }

    suspend fun launchPs1Game(
        game: GameEntry,
        configuredPackage: String?
    ): LaunchResult {
        val targetPackage = configuredPackage ?: ConsoleCatalog.DUCKSTATION_PACKAGE
        val status = emulatorDetector.detectDuckStation()

        if (!status.isInstalled) {
            return LaunchResult.Failed(
                reason = "O DuckStation não está instalado ou não está visível neste dispositivo.",
                emulatorPackage = targetPackage,
                canOpenMainApp = false
            )
        }

        val gameUri = runCatching { Uri.parse(game.uriString) }.getOrNull()
        if (gameUri == null || game.uriString.isBlank()) {
            return LaunchResult.Failed(
                reason = "A URI SAF do arquivo não é válida. Reexamine a biblioteca antes de tentar novamente.",
                emulatorPackage = targetPackage,
                canOpenMainApp = status.canLaunchMainApp
            )
        }

        val isCueFile = game.fileType.equals("CUE", ignoreCase = true) ||
            game.fileName.endsWith(".cue", ignoreCase = true)

        val companionUris: List<Uri> = if (isCueFile) {
            when (val resolution = withContext(Dispatchers.IO) { resolveCueCompanionUris(game, gameUri) }) {
                is CueResolutionResult.Resolved -> resolution.companionUris
                is CueResolutionResult.Failure -> {
                    Log.w(
                        TAG,
                        "Falha ao resolver arquivos associados do CUE '${game.fileName}' " +
                            "(scheme=${gameUri.scheme}, authority=${gameUri.authority}): ${resolution.reason}"
                    )
                    return LaunchResult.Failed(
                        reason = resolution.reason,
                        emulatorPackage = ConsoleCatalog.DUCKSTATION_PACKAGE,
                        canOpenMainApp = status.canLaunchMainApp
                    )
                }
            }
        } else {
            emptyList()
        }

        val launchClipData = buildLaunchClipData(gameUri, companionUris)

        val intent = Intent().apply {
            component = ComponentName(
                ConsoleCatalog.DUCKSTATION_PACKAGE,
                DUCKSTATION_EMULATION_ACTIVITY
            )
            putExtra("bootPath", game.uriString)
            putExtra("resumeState", false)
            clipData = launchClipData
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        Log.d(
            TAG,
            "Preparando DuckStation intent (URI SAF): file=${game.fileName}, scheme=${gameUri.scheme}, " +
                "authority=${gameUri.authority}, clipItems=${launchClipData.itemCount}, " +
                "bootPath=${game.uriString}"
        )

        return try {
            context.startActivity(intent)
            val companionDetail = if (isCueFile && companionUris.isNotEmpty()) {
                " e ${companionUris.size} arquivo(s) associado(s) concedido(s) via ClipData"
            } else {
                ""
            }
            LaunchResult.IntentDispatched(
                message = "Comando enviado ao DuckStation com a URI SAF original (${game.fileName})$companionDetail.",
                emulatorPackage = ConsoleCatalog.DUCKSTATION_PACKAGE
            )
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "ActivityNotFoundException ao iniciar DuckStation (uri=${game.uriString})", e)
            LaunchResult.Failed(
                reason = "A tela interna de emulação do DuckStation (EmulationActivity) não pôde ser aberta diretamente nesta versão.",
                emulatorPackage = ConsoleCatalog.DUCKSTATION_PACKAGE,
                canOpenMainApp = status.canLaunchMainApp
            )
        } catch (e: SecurityException) {
            Log.w(
                TAG,
                "SecurityException ao conceder leitura ao DuckStation " +
                    "(scheme=${gameUri.scheme}, authority=${gameUri.authority}, clipItems=${launchClipData.itemCount})",
                e
            )
            LaunchResult.Failed(
                reason = "O sistema bloqueou o envio direto ou o acesso às URIs SAF (${e.localizedMessage ?: "SecurityException"}). " +
                    "Reautorize a pasta nas configurações e tente novamente.",
                emulatorPackage = ConsoleCatalog.DUCKSTATION_PACKAGE,
                canOpenMainApp = status.canLaunchMainApp
            )
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Erro ao iniciar DuckStation (uri=${game.uriString}, clipItems=${launchClipData.itemCount})",
                e
            )
            LaunchResult.Failed(
                reason = "Falha ao tentar abrir o jogo no DuckStation: ${e.localizedMessage ?: e.javaClass.simpleName}.",
                emulatorPackage = ConsoleCatalog.DUCKSTATION_PACKAGE,
                canOpenMainApp = status.canLaunchMainApp
            )
        }
    }

    internal fun buildLaunchClipData(primaryUri: Uri, companionUris: List<Uri>): ClipData {
        val clipData = ClipData.newRawUri("ROM", primaryUri)
        val seenUriStrings = HashSet<String>().apply { add(primaryUri.toString()) }
        for (companionUri in companionUris) {
            if (seenUriStrings.add(companionUri.toString())) {
                clipData.addItem(ClipData.Item(companionUri))
            }
        }
        return clipData
    }

    internal fun resolveCueCompanionUris(
        game: GameEntry,
        cueUri: Uri
    ): CueResolutionResult {
        val cueContent = readCueText(cueUri)
            ?: return CueResolutionResult.Failure(
                "Não foi possível ler o arquivo .cue (${game.fileName}). " +
                    "Verifique se o arquivo ainda existe e se a pasta mantém permissão de leitura."
            )

        val referencedPaths = RomScannerRules.extractCueReferencedPaths(cueContent)
        if (referencedPaths.isEmpty()) {
            return CueResolutionResult.Failure(
                "O arquivo .cue (${game.fileName}) não referencia nenhum arquivo de faixa (FILE)."
            )
        }

        val folderUri = runCatching { Uri.parse(game.sourceFolderUri) }.getOrNull()
        val treeRoot = if (folderUri != null && game.sourceFolderUri.isNotBlank()) {
            DocumentFile.fromTreeUri(context, folderUri)
        } else {
            null
        }

        if (treeRoot == null || !treeRoot.exists() || !treeRoot.canRead()) {
            return CueResolutionResult.Failure(
                "A pasta autorizada pelo usuário não está acessível para localizar os arquivos .bin associados a ${game.fileName}. " +
                    "Selecione a pasta novamente nas configurações."
            )
        }

        val cueParentDir = findParentDirectoryOfDocument(treeRoot, cueUri)
            ?: return CueResolutionResult.Failure(
                "Não foi possível localizar a pasta de ${game.fileName} dentro do diretório autorizado via SAF."
            )

        val resolvedUris = mutableListOf<Uri>()
        val missingOrUnreadable = mutableListOf<String>()
        val immediateChildren = safeListFiles(cueParentDir)

        for (rawRefPath in referencedPaths) {
            val targetDoc = resolveReferencedDocument(
                cueParentDir = cueParentDir,
                cachedParentChildren = immediateChildren,
                rawRelativePath = rawRefPath
            )
            if (targetDoc == null || !targetDoc.exists() || !targetDoc.isFile || !targetDoc.canRead()) {
                missingOrUnreadable.add(rawRefPath)
            } else {
                resolvedUris.add(targetDoc.uri)
            }
        }

        if (missingOrUnreadable.isNotEmpty()) {
            val missingList = missingOrUnreadable.joinToString(", ")
            return CueResolutionResult.Failure(
                "Arquivo(s) referenciado(s) pelo .cue ausente(s) ou sem permissão de leitura: $missingList. " +
                    "Confirme se o arquivo .bin está na mesma pasta autorizada."
            )
        }

        return CueResolutionResult.Resolved(resolvedUris)
    }

    private fun readCueText(cueUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(cueUri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val buffer = CharArray(4096)
                val sb = StringBuilder()
                var totalRead = 0
                while (totalRead < 131_072) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    sb.appendRange(buffer, 0, read)
                    totalRead += read
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao abrir InputStream do CUE: $cueUri", e)
            null
        }
    }

    private fun findParentDirectoryOfDocument(
        treeRoot: DocumentFile,
        targetUri: Uri
    ): DocumentFile? {
        val targetUriString = targetUri.toString()
        val visitedDirs = HashSet<String>()
        val queue = ArrayDeque<DocumentFile>()
        queue.add(treeRoot)

        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            val dirKey = dir.uri.toString()
            if (!visitedDirs.add(dirKey)) continue

            val children = safeListFiles(dir)
            for (child in children) {
                if (child.isDirectory) {
                    if (child.canRead()) {
                        queue.addLast(child)
                    }
                } else if (child.uri == targetUri || child.uri.toString() == targetUriString) {
                    return dir
                }
            }
        }
        return null
    }

    private fun resolveReferencedDocument(
        cueParentDir: DocumentFile,
        cachedParentChildren: List<DocumentFile>,
        rawRelativePath: String
    ): DocumentFile? {
        val normalizedSegments = rawRelativePath
            .replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." }

        if (normalizedSegments.isEmpty()) return null

        if (normalizedSegments.size == 1) {
            val targetName = normalizedSegments[0]
            return findMatchingChild(cachedParentChildren, targetName)
                ?: runCatching { cueParentDir.findFile(targetName) }.getOrNull()
        }

        var currentDir: DocumentFile = cueParentDir
        for (i in 0 until normalizedSegments.lastIndex) {
            val segment = normalizedSegments[i]
            if (segment == "..") {
                val parent = currentDir.parentFile ?: return null
                currentDir = parent
                continue
            }
            val children = if (currentDir.uri == cueParentDir.uri) {
                cachedParentChildren
            } else {
                safeListFiles(currentDir)
            }
            val nextDir = findMatchingChild(children, segment, expectDirectory = true)
                ?: runCatching { currentDir.findFile(segment) }.getOrNull()
                ?: return null
            if (!nextDir.isDirectory) return null
            currentDir = nextDir
        }

        val finalName = normalizedSegments.last()
        val finalChildren = if (currentDir.uri == cueParentDir.uri) {
            cachedParentChildren
        } else {
            safeListFiles(currentDir)
        }
        return findMatchingChild(finalChildren, finalName)
            ?: runCatching { currentDir.findFile(finalName) }.getOrNull()
    }

    private fun findMatchingChild(
        children: List<DocumentFile>,
        targetName: String,
        expectDirectory: Boolean = false
    ): DocumentFile? {
        val exactMatch = children.firstOrNull { child ->
            child.name == targetName && (!expectDirectory || child.isDirectory)
        }
        if (exactMatch != null) return exactMatch

        val targetLower = targetName.lowercase(Locale.ROOT)
        return children.firstOrNull { child ->
            child.name?.lowercase(Locale.ROOT) == targetLower &&
                (!expectDirectory || child.isDirectory)
        }
    }

    private fun safeListFiles(dir: DocumentFile): List<DocumentFile> {
        return try {
            dir.listFiles().toList()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao listar diretório SAF: ${dir.uri}", e)
            emptyList()
        }
    }

    fun openDuckStationMain(): Boolean {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(ConsoleCatalog.DUCKSTATION_PACKAGE)
            ?: return false

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
