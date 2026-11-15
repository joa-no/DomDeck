package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameMetadata
import com.example.data.GameRepository
import com.example.data.MetadataRepository
import com.example.data.OfflineFirstMetadataRepository
import com.example.data.Ps1Settings
import com.example.data.SettingsRepository
import com.example.data.local.DomDeckDatabase
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import com.example.model.LaunchResult
import com.example.model.ScanState
import com.example.platform.EmulatorDetector
import com.example.platform.EmulatorLauncher
import com.example.platform.EmulatorStatus
import com.example.platform.RomScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DomDeckUiState(
    val ps1Settings: Ps1Settings = Ps1Settings(),
    val hasFolderReadPermission: Boolean = false,
    val duckStationStatus: EmulatorStatus = EmulatorStatus(
        packageName = ConsoleCatalog.DUCKSTATION_PACKAGE,
        displayName = "DuckStation",
        isInstalled = false
    ),
    val totalGameCount: Int = 0,
    val consoleGameCounts: Map<String, Int> = emptyMap(),
    val ps1Games: List<GameEntry> = emptyList(),
    val allGames: List<GameEntry> = emptyList(),
    val scanState: ScanState = ScanState.Idle,
    val launchResult: LaunchResult = LaunchResult.Idle,
    val infoMessage: String? = null,
    val gameMetadataByUri: Map<String, GameMetadata> = emptyMap()
)

class DomDeckViewModel @JvmOverloads constructor(
    application: Application,
    customMetadataRepository: MetadataRepository? = null
) : AndroidViewModel(application) {

    private val database = DomDeckDatabase.getInstance(application)
    private val gameRepository = GameRepository(database.gameDao())
    private val settingsRepository = SettingsRepository(application)
    private val metadataRepository: MetadataRepository = customMetadataRepository
        ?: OfflineFirstMetadataRepository.createDefault(application, database.gameMetadataDao())
    private val emulatorDetector = EmulatorDetector(application)
    private val emulatorLauncher = EmulatorLauncher(application, emulatorDetector)
    private val romScanner = RomScanner(application)

    private val _duckStationStatus = MutableStateFlow(emulatorDetector.detectDuckStation())
    private val _hasFolderReadPermission = MutableStateFlow(false)
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    private val _launchResult = MutableStateFlow<LaunchResult>(LaunchResult.Idle)
    private val _infoMessage = MutableStateFlow<String?>(null)

    private var activeScanJob: Job? = null
    private var metadataPrefetchJob: Job? = null

    val uiState: StateFlow<DomDeckUiState> = combine(
        settingsRepository.ps1Settings,
        _hasFolderReadPermission,
        _duckStationStatus,
        gameRepository.totalGameCount,
        gameRepository.consoleCounts,
        gameRepository.observeGamesForConsole(ConsoleCatalog.PS1_ID),
        gameRepository.allGames,
        _scanState,
        _launchResult,
        _infoMessage,
        metadataRepository.observeAllMetadata()
    ) { values ->
        val settings = values[0] as Ps1Settings
        val hasPerm = values[1] as Boolean
        val duckStatus = values[2] as EmulatorStatus
        val totalCount = values[3] as Int
        @Suppress("UNCHECKED_CAST")
        val consoleCounts = values[4] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val ps1List = values[5] as List<GameEntry>
        @Suppress("UNCHECKED_CAST")
        val allList = values[6] as List<GameEntry>
        val scan = values[7] as ScanState
        val launch = values[8] as LaunchResult
        val info = values[9] as String?
        @Suppress("UNCHECKED_CAST")
        val metadataMap = values[10] as Map<String, GameMetadata>

        DomDeckUiState(
            ps1Settings = settings,
            hasFolderReadPermission = hasPerm,
            duckStationStatus = duckStatus,
            totalGameCount = totalCount,
            consoleGameCounts = consoleCounts,
            ps1Games = ps1List,
            allGames = allList,
            scanState = scan,
            launchResult = launch,
            infoMessage = info,
            gameMetadataByUri = metadataMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DomDeckUiState(duckStationStatus = _duckStationStatus.value)
    )

    init {
        viewModelScope.launch {
            settingsRepository.ps1Settings.collect { settings ->
                _hasFolderReadPermission.value =
                    settingsRepository.hasPersistedReadPermission(settings.folderUri)
            }
        }
        viewModelScope.launch {
            val initialGames = gameRepository.observeGamesForConsole(ConsoleCatalog.PS1_ID).first()
            if (initialGames.isNotEmpty()) {
                scheduleMetadataPrefetch(initialGames)
            }
        }
    }

    fun refreshEnvironmentStatus() {
        _duckStationStatus.value = emulatorDetector.detectDuckStation()
        viewModelScope.launch {
            val currentFolder = settingsRepository.ps1Settings.first().folderUri
            _hasFolderReadPermission.value = settingsRepository.hasPersistedReadPermission(currentFolder)
        }
    }

    fun onPs1FolderPicked(uri: Uri?) {
        if (uri == null) {
            _infoMessage.value = "Seleção de pasta cancelada. Nenhuma alteração aplicada."
            return
        }

        val context = getApplication<Application>()
        var permissionPersisted = false
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            permissionPersisted = true
        } catch (_: SecurityException) {
            permissionPersisted = settingsRepository.hasPersistedReadPermission(uri.toString())
        } catch (_: Exception) {
            permissionPersisted = false
        }

        val docName = runCatching {
            DocumentFile.fromTreeUri(context, uri)?.name
        }.getOrNull()

        val label = docName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            ?: "Pasta selecionada"

        val folderUriString = uri.toString()
        activeScanJob?.cancel()
        activeScanJob = viewModelScope.launch {
            settingsRepository.savePs1Folder(folderUriString, label)
            val hasPerm = permissionPersisted || settingsRepository.hasPersistedReadPermission(folderUriString)
            _hasFolderReadPermission.value = hasPerm
            _infoMessage.value = if (hasPerm) {
                "Pasta \"$label\" vinculada com permissão de leitura. Atualizando biblioteca…"
            } else {
                "Pasta \"$label\" selecionada, mas o provedor não confirmou permissão persistente."
            }
            executePs1Scan(
                folderUri = uri,
                folderUriString = folderUriString,
                initialFolderLabel = label
            )
        }
    }

    fun setDuckStationAssociated(associated: Boolean) {
        viewModelScope.launch {
            val pkg = if (associated) ConsoleCatalog.DUCKSTATION_PACKAGE else null
            settingsRepository.savePs1EmulatorPackage(pkg)
            _infoMessage.value = if (associated) {
                "DuckStation associado ao PlayStation 1."
            } else {
                "Associação do emulador removida para PlayStation 1."
            }
        }
    }

    fun startPs1Scan() {
        activeScanJob?.cancel()
        activeScanJob = viewModelScope.launch {
            val settings = settingsRepository.ps1Settings.first()
            val folderUriString = settings.folderUri
            if (folderUriString.isNullOrBlank()) {
                _scanState.value = ScanState.Error(
                    "Selecione uma pasta de ROMs antes de examinar a biblioteca."
                )
                return@launch
            }

            val folderUri = runCatching { Uri.parse(folderUriString) }.getOrNull()
            if (folderUri == null) {
                _scanState.value = ScanState.Error("A URI da pasta configurada é inválida.")
                return@launch
            }

            executePs1Scan(
                folderUri = folderUri,
                folderUriString = folderUriString,
                initialFolderLabel = settings.folderLabel ?: "Pasta raiz"
            )
        }
    }

    private suspend fun executePs1Scan(
        folderUri: Uri,
        folderUriString: String,
        initialFolderLabel: String
    ) {
        _scanState.value = ScanState.Scanning(
            inspectedEntries = 0,
            currentFolder = initialFolderLabel
        )

        val result = try {
            romScanner.scanPs1Folder(folderUri) { inspected, currentFolder ->
                _scanState.value = ScanState.Scanning(
                    inspectedEntries = inspected,
                    currentFolder = currentFolder
                )
            }
        } catch (ce: CancellationException) {
            _scanState.value = ScanState.Idle
            throw ce
        }

        result.fold(
            onSuccess = { outcome ->
                gameRepository.replaceGamesForConsole(ConsoleCatalog.PS1_ID, outcome.games)
                settingsRepository.updatePs1LastScanTimestamp(System.currentTimeMillis())
                _hasFolderReadPermission.value =
                    _hasFolderReadPermission.value || settingsRepository.hasPersistedReadPermission(folderUriString)

                val msg = when {
                    outcome.games.isEmpty() ->
                        "Varredura concluída (${outcome.inspectedEntries} itens inspecionados). " +
                            "Nenhum arquivo .chd, .cue, .pbp, .iso ou .m3u foi encontrado."
                    outcome.verificationCount > 0 ->
                        "${outcome.games.size} entrada(s) indexada(s) " +
                            "(${outcome.verificationCount} .bin sem .cue marcado(s) para verificação)."
                    else ->
                        "${outcome.games.size} jogo(s) candidato(s) indexado(s) sem duplicação nem cópia de arquivos."
                }
                _scanState.value = ScanState.Success(
                    totalGames = outcome.games.size,
                    verificationCount = outcome.verificationCount,
                    message = msg
                )
                scheduleMetadataPrefetch(outcome.games)
            },
            onFailure = { error ->
                _hasFolderReadPermission.value =
                    settingsRepository.hasPersistedReadPermission(folderUriString)
                _scanState.value = ScanState.Error(
                    error.message ?: "Não foi possível concluir a leitura da pasta."
                )
            }
        )
    }

    fun cancelActiveScan() {
        if (activeScanJob?.isActive == true) {
            activeScanJob?.cancel()
            activeScanJob = null
            if (_scanState.value is ScanState.Scanning) {
                _scanState.value = ScanState.Idle
            }
        }
    }

    fun resetScanFeedback() {
        if (_scanState.value !is ScanState.Scanning) {
            _scanState.value = ScanState.Idle
        }
    }

    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    private fun scheduleMetadataPrefetch(games: List<GameEntry>) {
        if (games.isEmpty()) return
        metadataPrefetchJob?.cancel()
        metadataPrefetchJob = viewModelScope.launch {
            metadataRepository.prefetchMissingMetadata(games)
        }
    }

    fun ensureGameMetadata(game: GameEntry, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            metadataRepository.ensureMetadataCached(game, forceRefresh = forceRefresh)
        }
    }

    fun saveManualGameMetadata(
        game: GameEntry,
        customTitle: String?,
        customSynopsis: String?,
        pickedCoverUri: Uri?,
        clearCustomCover: Boolean = false
    ) {
        viewModelScope.launch {
            metadataRepository.saveManualEdits(
                game = game,
                customTitle = customTitle,
                customSynopsis = customSynopsis,
                pickedCoverUri = pickedCoverUri,
                clearCustomCover = clearCustomCover
            )
        }
    }

    fun restoreAutomaticGameMetadata(game: GameEntry) {
        viewModelScope.launch {
            metadataRepository.restoreAutomaticMetadata(game, refetchNow = true)
        }
    }

    fun observeGame(gameId: Long) = gameRepository.observeGameById(gameId)

    fun launchPs1Game(game: GameEntry) {
        viewModelScope.launch {
            _duckStationStatus.value = emulatorDetector.detectDuckStation()
            val settings = settingsRepository.ps1Settings.first()
            _hasFolderReadPermission.value = settingsRepository.hasPersistedReadPermission(settings.folderUri)

            val configuredPkg = settings.emulatorPackage
            if (configuredPkg.isNullOrBlank()) {
                _launchResult.value = LaunchResult.Failed(
                    reason = "Nenhum emulador está associado ao PlayStation 1 nas configurações.",
                    emulatorPackage = null,
                    canOpenMainApp = _duckStationStatus.value.canLaunchMainApp
                )
                return@launch
            }

            val activeFolderUri = settings.folderUri
            if (activeFolderUri.isNullOrBlank() || game.sourceFolderUri != activeFolderUri) {
                _launchResult.value = LaunchResult.Failed(
                    reason = "Este registro pertence a uma pasta anterior e a biblioteca ainda não foi atualizada para a pasta atual. Execute \"Examinar biblioteca\" antes de jogar.",
                    emulatorPackage = configuredPkg,
                    canOpenMainApp = _duckStationStatus.value.canLaunchMainApp
                )
                return@launch
            }

            if (_scanState.value is ScanState.Error) {
                _launchResult.value = LaunchResult.Failed(
                    reason = "A última varredura da pasta falhou. Use \"Examinar biblioteca\" para validar os arquivos antes de iniciar.",
                    emulatorPackage = configuredPkg,
                    canOpenMainApp = _duckStationStatus.value.canLaunchMainApp
                )
                return@launch
            }

            _launchResult.value = emulatorLauncher.launchPs1Game(game, configuredPkg)
        }
    }

    fun openDuckStationApp() {
        val opened = emulatorLauncher.openDuckStationMain()
        if (!opened) {
            _infoMessage.value = "Não foi possível abrir o DuckStation. Confirme se o app está instalado."
        }
    }

    fun dismissLaunchResult() {
        _launchResult.value = LaunchResult.Idle
    }

    override fun onCleared() {
        super.onCleared()
        activeScanJob?.cancel()
        metadataPrefetchJob?.cancel()
    }
}
