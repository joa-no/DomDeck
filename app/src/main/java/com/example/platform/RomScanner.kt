package com.example.platform

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class RomScanOutcome(
    val games: List<GameEntry>,
    val inspectedEntries: Int,
    val verificationCount: Int
)

object RomScannerRules {
    val PRIMARY_PS1_EXTENSIONS: Set<String> = setOf("chd", "cue", "pbp", "iso", "m3u")
    val AUXILIARY_IGNORED_EXTENSIONS: Set<String> = setOf("sbi", "sub")

    private val CUE_FILE_REGEX = Regex(
        pattern = """(?i)^\s*FILE\s+(?:"([^"]+)"|(\S+))""",
        options = setOf(RegexOption.MULTILINE)
    )
    private val TRACK_SUFFIX_REGEX = Regex(
        pattern = """(?i)\s*[\[(]?\s*track\s*\d+\s*[\])]?$"""
    )

    fun getExtensionLower(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        if (dotIndex < 0 || dotIndex == fileName.lastIndex) return ""
        return fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
    }

    fun getBaseStem(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }

    fun deriveDisplayTitle(fileName: String): String {
        val stem = getBaseStem(fileName)
            .replace('_', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
        return stem.ifBlank { fileName }
    }

    fun extractCueReferencedPaths(cueContent: String): List<String> {
        val normalized = cueContent.removePrefix("\uFEFF")
        val seenLower = LinkedHashSet<String>()
        val result = mutableListOf<String>()
        for (match in CUE_FILE_REGEX.findAll(normalized)) {
            val rawPath = match.groupValues[1].ifBlank { match.groupValues[2] }.trim()
            if (rawPath.isNotEmpty() && seenLower.add(rawPath.lowercase(Locale.ROOT))) {
                result.add(rawPath)
            }
        }
        return result
    }

    fun extractCueReferencedFileNames(cueContent: String): Set<String> {
        val referenced = mutableSetOf<String>()
        for (rawPath in extractCueReferencedPaths(cueContent)) {
            val baseName = rawPath.substringAfterLast('/').substringAfterLast('\\')
            if (baseName.isNotEmpty()) {
                referenced.add(baseName.lowercase(Locale.ROOT))
            }
        }
        return referenced
    }

    fun isBinAssociatedWithCue(
        binFileName: String,
        cueReferencedFilesLower: Set<String>,
        cueStemsLower: Set<String>
    ): Boolean {
        val lowerName = binFileName.lowercase(Locale.ROOT)
        if (lowerName in cueReferencedFilesLower) return true

        val binStemLower = getBaseStem(binFileName).lowercase(Locale.ROOT)
        if (binStemLower in cueStemsLower) return true

        val strippedTrackStem = binStemLower.replace(TRACK_SUFFIX_REGEX, "").trim()
        if (strippedTrackStem.isNotEmpty() && strippedTrackStem in cueStemsLower) return true

        return false
    }
}

class RomScanner(private val context: Context) {

    suspend fun scanPs1Folder(
        treeUri: Uri,
        onProgress: (inspectedEntries: Int, currentFolder: String) -> Unit
    ): Result<RomScanOutcome> = withContext(Dispatchers.IO) {
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri)
            if (root == null || !root.exists() || !root.isDirectory) {
                return@withContext Result.failure(
                    IllegalStateException("A pasta selecionada não existe ou não está acessível.")
                )
            }
            if (!root.canRead()) {
                return@withContext Result.failure(
                    SecurityException("Permissão de leitura indisponível para esta pasta. Selecione-a novamente.")
                )
            }

            val scannedAt = System.currentTimeMillis()
            val discoveredByUri = LinkedHashMap<String, GameEntry>()
            val visitedDirs = HashSet<String>()
            val queue = ArrayDeque<DocumentFile>()
            queue.add(root)

            var inspectedCount = 0
            val sourceFolderUriString = treeUri.toString()

            while (queue.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val dir = queue.removeFirst()
                val dirUriKey = dir.uri.toString()
                if (!visitedDirs.add(dirUriKey)) continue

                val dirName = dir.name ?: "Pasta"
                val children = try {
                    dir.listFiles()
                } catch (sec: SecurityException) {
                    return@withContext Result.failure(
                        SecurityException("Acesso negado ao ler \"$dirName\". Reautorize a pasta nas configurações.")
                    )
                }

                val filesInDir = mutableListOf<DocumentFile>()
                for (child in children) {
                    currentCoroutineContext().ensureActive()
                    inspectedCount++
                    if (child.isDirectory) {
                        if (child.canRead()) {
                            queue.addLast(child)
                        }
                    } else if (child.isFile) {
                        filesInDir.add(child)
                    }
                }

                onProgress(inspectedCount, dirName)

                // Collect CUE metadata within this directory to filter out associated BIN tracks
                val cueFiles = filesInDir.filter {
                    val name = it.name ?: return@filter false
                    RomScannerRules.getExtensionLower(name) == "cue"
                }
                val cueStemsLower = cueFiles.mapNotNull { file ->
                    file.name?.let { RomScannerRules.getBaseStem(it).lowercase(Locale.ROOT) }
                }.toSet()

                val cueReferencedLower = mutableSetOf<String>()
                for (cueFile in cueFiles) {
                    currentCoroutineContext().ensureActive()
                    val text = readSmallTextSafely(cueFile.uri)
                    if (text != null) {
                        cueReferencedLower.addAll(RomScannerRules.extractCueReferencedFileNames(text))
                    }
                }

                for (file in filesInDir) {
                    currentCoroutineContext().ensureActive()
                    val fileName = file.name ?: continue
                    val ext = RomScannerRules.getExtensionLower(fileName)
                    if (ext.isEmpty() || ext in RomScannerRules.AUXILIARY_IGNORED_EXTENSIONS) {
                        continue
                    }

                    val uriKey = file.uri.toString()
                    if (uriKey in discoveredByUri) continue

                    when {
                        ext in RomScannerRules.PRIMARY_PS1_EXTENSIONS -> {
                            discoveredByUri[uriKey] = GameEntry(
                                consoleId = ConsoleCatalog.PS1_ID,
                                displayTitle = RomScannerRules.deriveDisplayTitle(fileName),
                                fileName = fileName,
                                uriString = uriKey,
                                sourceFolderUri = sourceFolderUriString,
                                fileType = ext.uppercase(Locale.ROOT),
                                needsVerification = false,
                                statusNote = null,
                                fileSizeBytes = file.length(),
                                lastScannedAt = scannedAt
                            )
                        }
                        ext == "bin" -> {
                            val associatedWithCue = RomScannerRules.isBinAssociatedWithCue(
                                binFileName = fileName,
                                cueReferencedFilesLower = cueReferencedLower,
                                cueStemsLower = cueStemsLower
                            )
                            if (!associatedWithCue) {
                                discoveredByUri[uriKey] = GameEntry(
                                    consoleId = ConsoleCatalog.PS1_ID,
                                    displayTitle = RomScannerRules.deriveDisplayTitle(fileName),
                                    fileName = fileName,
                                    uriString = uriKey,
                                    sourceFolderUri = sourceFolderUriString,
                                    fileType = "BIN",
                                    needsVerification = true,
                                    statusNote = "Arquivo .bin sem .cue associado — requer verificação",
                                    fileSizeBytes = file.length(),
                                    lastScannedAt = scannedAt
                                )
                            }
                        }
                    }
                }
            }

            val games = discoveredByUri.values.sortedBy { it.displayTitle.lowercase(Locale.ROOT) }
            val verificationCount = games.count { it.needsVerification }
            Result.success(
                RomScanOutcome(
                    games = games,
                    inspectedEntries = inspectedCount,
                    verificationCount = verificationCount
                )
            )
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (se: SecurityException) {
            Result.failure(
                SecurityException("Permissão da pasta expirada ou negada. Selecione a pasta novamente.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Falha inesperada ao examinar a pasta.")
            )
        }
    }

    private fun readSmallTextSafely(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val buffer = CharArray(4096)
                val sb = StringBuilder()
                var totalRead = 0
                while (totalRead < 65_536) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    sb.appendRange(buffer, 0, read)
                    totalRead += read
                }
                sb.toString()
            }
        } catch (_: Exception) {
            null
        }
    }
}
