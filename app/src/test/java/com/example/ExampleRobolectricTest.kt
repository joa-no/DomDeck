package com.example

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import com.example.model.LaunchResult
import com.example.model.ScanState
import com.example.navigation.DomDeckApp
import com.example.platform.EmulatorDetector
import com.example.platform.EmulatorLauncher
import com.example.ui.theme.DomDeckTheme
import com.example.viewmodel.DomDeckUiState
import com.example.viewmodel.DomDeckViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DomDeck", appName)
    }

    @Test
    fun `duckstation detector returns not installed cleanly when missing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = EmulatorDetector(context)
        val status = detector.detectDuckStation()
        assertFalse(status.isInstalled)
        assertEquals(ConsoleCatalog.DUCKSTATION_PACKAGE, status.packageName)
    }

    @Test
    fun `emulator launcher fails gracefully when duckstation is not installed`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = EmulatorDetector(context)
        val launcher = EmulatorLauncher(context, detector)
        val dummyEntry = GameEntry(
            id = 1L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Test Disc",
            fileName = "Test_Disc.chd",
            uriString = "content://com.android.externalstorage.documents/document/primary%3ATest_Disc.chd",
            sourceFolderUri = "content://com.android.externalstorage.documents/tree/primary%3APS1",
            fileType = "CHD"
        )
        val result = launcher.launchPs1Game(dummyEntry, ConsoleCatalog.DUCKSTATION_PACKAGE)
        assertTrue(result is LaunchResult.Failed)
    }

    @Test
    fun `buildLaunchClipData includes primary cue uri and unique companion bin uris`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = EmulatorLauncher(context, EmulatorDetector(context))
        val cueUri = Uri.parse("content://com.example.test.docs/tree/ps1/document/SilentHill.cue")
        val bin1Uri = Uri.parse("content://com.example.test.docs/tree/ps1/document/SilentHill1.bin")
        val bin2Uri = Uri.parse("content://com.example.test.docs/tree/ps1/document/SilentHill2.bin")

        val clipData = launcher.buildLaunchClipData(cueUri, listOf(bin1Uri, bin2Uri, bin1Uri))
        assertEquals(3, clipData.itemCount)
        assertEquals(cueUri, clipData.getItemAt(0).uri)
        assertEquals(bin1Uri, clipData.getItemAt(1).uri)
        assertEquals(bin2Uri, clipData.getItemAt(2).uri)
    }

    @Test
    fun `cue launch attaches associated bin uris in ClipData and preserves bootPath`(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authority = "com.example.test.saf"
        val tempDir = File(app.cacheDir, "saf_cue_test_${System.nanoTime()}").apply { mkdirs() }
        val cueFile = File(tempDir, "Silent Hill (USA).cue").apply {
            writeText("FILE \"Silent Hill (USA).bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\n")
        }
        val binFile = File(tempDir, "Silent Hill (USA).bin").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }

        FakeTreeDocumentsProvider.rootDir = tempDir
        val providerInfo = ProviderInfo().apply {
            this.authority = authority
            this.exported = true
            this.grantUriPermissions = true
            this.readPermission = Manifest.permission.MANAGE_DOCUMENTS
            this.writePermission = Manifest.permission.MANAGE_DOCUMENTS
        }
        Robolectric.buildContentProvider(FakeTreeDocumentsProvider::class.java).create(providerInfo)
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.MANAGE_DOCUMENTS)

        registerFakeDuckStation(app)

        val treeUri = DocumentsContract.buildTreeDocumentUri(authority, "root")
        val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "root")
        val cueUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "Silent Hill (USA).cue")
        val expectedBinUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "Silent Hill (USA).bin")
        app.grantUriPermission(app.packageName, treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        app.grantUriPermission(app.packageName, treeDocUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        app.grantUriPermission(app.packageName, cueUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        app.grantUriPermission(app.packageName, expectedBinUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val testSafContext = object : android.content.ContextWrapper(app) {
            override fun checkCallingOrSelfUriPermission(uri: Uri, modeFlags: Int): Int {
                return if (uri.authority == authority && (modeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    super.checkCallingOrSelfUriPermission(uri, modeFlags)
                }
            }

            override fun checkUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int): Int {
                return if (uri?.authority == authority && (modeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    super.checkUriPermission(uri, pid, uid, modeFlags)
                }
            }
        }

        val entry = GameEntry(
            id = 10L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Silent Hill (USA)",
            fileName = cueFile.name,
            uriString = cueUri.toString(),
            sourceFolderUri = treeUri.toString(),
            fileType = "CUE"
        )

        val launcher = EmulatorLauncher(testSafContext, EmulatorDetector(testSafContext))
        val result = launcher.launchPs1Game(entry, ConsoleCatalog.DUCKSTATION_PACKAGE)
        assertTrue((result as? LaunchResult.Failed)?.reason ?: "", result is LaunchResult.IntentDispatched)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(cueUri.toString(), startedIntent.getStringExtra("bootPath"))
        assertFalse(startedIntent.getBooleanExtra("resumeState", true))
        assertTrue((startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0)
        assertNotNull(startedIntent.clipData)
        assertEquals(2, startedIntent.clipData!!.itemCount)
        assertEquals(cueUri, startedIntent.clipData!!.getItemAt(0).uri)
        assertEquals(expectedBinUri, startedIntent.clipData!!.getItemAt(1).uri)

        // Verify missing BIN triggers clear failure
        binFile.delete()
        val missingResult = launcher.launchPs1Game(entry, ConsoleCatalog.DUCKSTATION_PACKAGE)
        assertTrue(missingResult is LaunchResult.Failed)
        assertTrue((missingResult as LaunchResult.Failed).reason.contains("Silent Hill (USA).bin"))

        tempDir.deleteRecursively()
        Unit
    }

    @Test
    fun `chd launch preserves single uri ClipData behavior`(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        registerFakeDuckStation(app)

        val chdUri = "content://com.android.externalstorage.documents/tree/primary%3APS1/document/primary%3APS1%2FSilentHill.chd"
        val entry = GameEntry(
            id = 11L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "SilentHill",
            fileName = "SilentHill.chd",
            uriString = chdUri,
            sourceFolderUri = "content://com.android.externalstorage.documents/tree/primary%3APS1",
            fileType = "CHD"
        )

        val launcher = EmulatorLauncher(app, EmulatorDetector(app))
        val result = launcher.launchPs1Game(entry, ConsoleCatalog.DUCKSTATION_PACKAGE)
        assertTrue(result is LaunchResult.IntentDispatched)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(chdUri, startedIntent.getStringExtra("bootPath"))
        assertEquals(1, startedIntent.clipData!!.itemCount)
        assertEquals(Uri.parse(chdUri), startedIntent.clipData!!.getItemAt(0).uri)
    }

    @Test
    fun `pbp launch preserves single uri ClipData behavior`(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        registerFakeDuckStation(app)

        val pbpUri = "content://com.android.externalstorage.documents/tree/primary%3APS1/document/primary%3APS1%2FMetalGear.pbp"
        val entry = GameEntry(
            id = 12L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "MetalGear",
            fileName = "MetalGear.pbp",
            uriString = pbpUri,
            sourceFolderUri = "content://com.android.externalstorage.documents/tree/primary%3APS1",
            fileType = "PBP"
        )

        val launcher = EmulatorLauncher(app, EmulatorDetector(app))
        val result = launcher.launchPs1Game(entry, ConsoleCatalog.DUCKSTATION_PACKAGE)
        assertTrue(result is LaunchResult.IntentDispatched)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(pbpUri, startedIntent.getStringExtra("bootPath"))
        assertEquals(1, startedIntent.clipData!!.itemCount)
        assertEquals(Uri.parse(pbpUri), startedIntent.clipData!!.getItemAt(0).uri)
    }

    @Test
    fun `folder change triggers auto scan, avoids duplicates, protects against failed scan, and persists SAF across restart`(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authority = "com.android.externalstorage.documents"
        val tempRoot = File(app.cacheDir, "vm_saf_test_${System.nanoTime()}").apply { mkdirs() }

        val folderADir = File(tempRoot, "ROM/PS1_A").apply { mkdirs() }
        File(folderADir, "Silent Hill (USA).cue").writeText(
            "FILE \"Silent Hill (USA).bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\n"
        )
        File(folderADir, "Silent Hill (USA).bin").writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        File(folderADir, "Metal Gear Solid.pbp").writeBytes(byteArrayOf(0x04, 0x05))

        val folderBDir = File(tempRoot, "ROM/PS1_B").apply { mkdirs() }
        File(folderBDir, "Castlevania.chd").writeBytes(byteArrayOf(0x06, 0x07))

        FakeTreeDocumentsProvider.rootDir = tempRoot
        val providerInfo = ProviderInfo().apply {
            this.authority = authority
            this.exported = true
            this.grantUriPermissions = true
            this.readPermission = Manifest.permission.MANAGE_DOCUMENTS
            this.writePermission = Manifest.permission.MANAGE_DOCUMENTS
        }
        Robolectric.buildContentProvider(FakeTreeDocumentsProvider::class.java).create(providerInfo)
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.MANAGE_DOCUMENTS)
        registerFakeDuckStation(app)

        val testApp = TestSafApplication(app, authority)
        val folderAUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:ROM/PS1_A")
        val folderBUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:ROM/PS1_B")
        val missingFolderUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:ROM/MISSING")

        val viewModel = DomDeckViewModel(testApp)
        val collectorJob = launch { viewModel.uiState.collect { } }

        viewModel.setDuckStationAssociated(true)

        // 1. Pick Folder A -> automatic scan indexes 2 games without duplicates
        viewModel.onPs1FolderPicked(folderAUri)
        val stateAfterA = awaitUiState(viewModel) {
            it.scanState is ScanState.Success && it.ps1Games.size == 2 && it.ps1Settings.folderUri == folderAUri.toString()
        }
        assertEquals(2, stateAfterA.totalGameCount)
        assertTrue(stateAfterA.hasFolderReadPermission)

        // 2. Manual rescan of Folder A -> still 2 games (no duplicates)
        viewModel.startPs1Scan()
        val stateAfterManual = awaitUiState(viewModel) {
            it.scanState is ScanState.Success && it.ps1Games.size == 2
        }
        assertEquals(2, stateAfterManual.ps1Games.size)

        // 3. Switch to Folder B -> automatic scan replaces old Folder A games with Folder B's 1 game
        viewModel.onPs1FolderPicked(folderBUri)
        val stateAfterB = awaitUiState(viewModel) {
            it.scanState is ScanState.Success &&
                it.ps1Settings.folderUri == folderBUri.toString() &&
                it.ps1Games.size == 1 &&
                it.ps1Games.first().fileName == "Castlevania.chd"
        }
        assertEquals(1, stateAfterB.totalGameCount)
        val gameFromFolderB = stateAfterB.ps1Games.first()

        // 4. Switch to an inaccessible/missing folder -> scan fails, DB is NOT wiped, stale Folder B URI is blocked from launch
        viewModel.onPs1FolderPicked(missingFolderUri)
        val stateAfterFail = awaitUiState(viewModel) {
            it.scanState is ScanState.Error && it.ps1Settings.folderUri == missingFolderUri.toString()
        }
        assertEquals(1, stateAfterFail.ps1Games.size)
        viewModel.launchPs1Game(gameFromFolderB)
        val blockedLaunch = awaitUiState(viewModel) { it.launchResult is LaunchResult.Failed }
        assertTrue(blockedLaunch.launchResult is LaunchResult.Failed)
        viewModel.dismissLaunchResult()

        // 5. Switch back to Folder A -> auto scan restores Folder A games cleanly
        viewModel.onPs1FolderPicked(folderAUri)
        awaitUiState(viewModel) {
            it.scanState is ScanState.Success &&
                it.ps1Settings.folderUri == folderAUri.toString() &&
                it.ps1Games.size == 2
        }
        collectorJob.cancel()

        // 6. Simulate app restart with a fresh ViewModel instance reading persisted DataStore + Room
        val restartedViewModel = DomDeckViewModel(testApp)
        val restartedCollector = launch { restartedViewModel.uiState.collect { } }
        val restartedState = awaitUiState(restartedViewModel) {
            it.ps1Settings.folderUri == folderAUri.toString() &&
                it.hasFolderReadPermission &&
                it.ps1Games.size == 2 &&
                it.ps1Settings.emulatorPackage == ConsoleCatalog.DUCKSTATION_PACKAGE
        }
        val silentHillCue = restartedState.ps1Games.first { it.fileName == "Silent Hill (USA).cue" }
        restartedViewModel.launchPs1Game(silentHillCue)
        val launchedAfterRestart = awaitUiState(restartedViewModel) {
            it.launchResult is LaunchResult.IntentDispatched
        }
        assertTrue(launchedAfterRestart.launchResult is LaunchResult.IntentDispatched)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(silentHillCue.uriString, startedIntent.getStringExtra("bootPath"))
        assertEquals(2, startedIntent.clipData!!.itemCount)

        restartedCollector.cancel()
        com.example.data.local.DomDeckDatabase.getInstance(testApp)
            .gameDao()
            .deleteGamesForConsole(ConsoleCatalog.PS1_ID)
        com.example.data.SettingsRepository(testApp).apply {
            clearPs1Folder()
            savePs1EmulatorPackage(null)
        }
        tempRoot.deleteRecursively()
        Unit
    }

    private suspend fun awaitUiState(
        viewModel: DomDeckViewModel,
        predicate: (DomDeckUiState) -> Boolean
    ): DomDeckUiState {
        return withTimeout(8_000) {
            while (true) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val current = viewModel.uiState.first()
                if (predicate(current)) return@withTimeout current
                delay(20)
            }
            @Suppress("UNREACHABLE_CODE")
            viewModel.uiState.value
        }
    }

    class TestSafApplication(
        private val base: Application,
        private val safAuthority: String
    ) : Application() {
        init {
            attachBaseContext(base)
        }

        override fun getApplicationContext(): Context = this

        override fun checkCallingOrSelfUriPermission(uri: Uri, modeFlags: Int): Int {
            return if (uri.authority == safAuthority && (modeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                super.checkCallingOrSelfUriPermission(uri, modeFlags)
            }
        }

        override fun checkUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int): Int {
            return if (uri?.authority == safAuthority && (modeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                super.checkUriPermission(uri, pid, uid, modeFlags)
            }
        }
    }

    @Test
    fun `app navigates across Home, Consoles, and Settings`() {
        composeTestRule.setContent {
            DomDeckTheme {
                DomDeckApp()
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_total_games_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 JOGOS NA BIBLIOTECA").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_consoles").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SISTEMAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("PlayStation 1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("console_card_ps1").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_console_row_ps1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Configurar PlayStation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("duckstation_status_pill").assertIsDisplayed()
    }

    @Test
    fun `metadata caches locally, serves offline without duplicate downloads, supports manual edits and restore across restart`(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = com.example.data.local.DomDeckDatabase.getInstance(app)
        db.gameMetadataDao().deleteAll()

        val testGame = GameEntry(
            id = 501L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Silent Hill (USA)",
            fileName = "Silent Hill (USA).cue",
            uriString = "content://com.example.test.docs/tree/ps1/document/SilentHillCacheTest.cue",
            sourceFolderUri = "content://com.example.test.docs/tree/ps1",
            fileType = "CUE"
        )

        var coverCalls = 0
        var descCalls = 0
        var online = true
        val fakePng = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        ) + ByteArray(64) { 0x02 }

        val fakeCoverSource = object : com.example.data.metadata.CoverArtSource {
            override suspend fun fetchPs1Cover(game: GameEntry, searchTitle: String): com.example.data.metadata.CoverFetchResult {
                coverCalls++
                return com.example.data.metadata.CoverFetchResult.Found(
                    bytes = fakePng,
                    sourceLabel = "Libretro Thumbnails",
                    resolvedUrl = "https://thumbnails.libretro.com/Sony%20-%20PlayStation/Named_Boxarts/Silent%20Hill%20(USA).png"
                )
            }
        }
        val fakeDescSource = object : com.example.data.metadata.GameDescriptionSource {
            override suspend fun fetchPs1Description(game: GameEntry, searchTitle: String): com.example.data.metadata.DescriptionFetchResult {
                descCalls++
                return com.example.data.metadata.DescriptionFetchResult.Found(
                    resolvedTitle = "Silent Hill",
                    description = "Silent Hill é um jogo eletrônico de survival horror lançado originalmente para PlayStation em 1999.",
                    sourceLabel = "Wikipédia (PT)",
                    sourceUrl = "https://pt.wikipedia.org/wiki/Silent_Hill_(jogo_eletr%C3%B4nico)",
                    licenseText = "CC BY-SA 4.0"
                )
            }
        }

        val repo1 = com.example.data.OfflineFirstMetadataRepository(
            context = app,
            metadataDao = db.gameMetadataDao(),
            coverCache = com.example.data.metadata.CoverFileCache(app),
            coverSources = listOf(fakeCoverSource),
            descriptionSources = listOf(fakeDescSource),
            isNetworkAvailable = { online }
        )

        try {
            // 1. First fetch caches cover file + description in Room
            val fetched = repo1.ensureMetadataCached(testGame, forceRefresh = false)
            assertEquals("Silent Hill", fetched.effectiveTitle)
            assertNotNull(fetched.effectiveCoverPath)
            assertTrue(File(fetched.effectiveCoverPath!!).exists())
            assertTrue(fetched.effectiveDescription!!.contains("1999"))
            assertEquals("Wikipédia (PT)", fetched.descriptionSourceLabel)
            assertEquals("CC BY-SA 4.0", fetched.descriptionLicense)
            assertEquals(1, coverCalls)
            assertEquals(1, descCalls)

            // 2. Subsequent call & offline mode serve cached data without redundant network requests
            online = false
            val cachedOffline = repo1.ensureMetadataCached(testGame, forceRefresh = false)
            assertEquals(fetched.effectiveCoverPath, cachedOffline.effectiveCoverPath)
            assertEquals(fetched.effectiveDescription, cachedOffline.effectiveDescription)
            assertEquals(1, coverCalls)
            assertEquals(1, descCalls)

            // 3. Manual edit overrides title, synopsis, and custom cover from picked URI
            val pickedImageFile = File(app.cacheDir, "custom_cover_pick.png").apply { writeBytes(fakePng) }
            val pickedUri = Uri.fromFile(pickedImageFile)
            val edited = repo1.saveManualEdits(
                game = testGame,
                customTitle = "Silent Hill - Edição Favorita",
                customSynopsis = "Minha sinopse personalizada salva localmente.",
                pickedCoverUri = pickedUri,
                clearCustomCover = false
            )
            assertEquals("Silent Hill - Edição Favorita", edited.effectiveTitle)
            assertEquals("Minha sinopse personalizada salva localmente.", edited.effectiveDescription)
            assertTrue(edited.isManualTitle)
            assertTrue(edited.isManualSynopsis)
            assertTrue(edited.isManualCover)

            // 4. Simulate app restart with a brand new OfflineFirstMetadataRepository reading Room + internal cache
            val repoAfterRestart = com.example.data.OfflineFirstMetadataRepository(
                context = app,
                metadataDao = db.gameMetadataDao(),
                coverCache = com.example.data.metadata.CoverFileCache(app),
                coverSources = listOf(fakeCoverSource),
                descriptionSources = listOf(fakeDescSource),
                isNetworkAvailable = { false }
            )
            val persistedAfterRestart = repoAfterRestart.getMetadata(testGame)
            assertEquals("Silent Hill - Edição Favorita", persistedAfterRestart.effectiveTitle)
            assertEquals("Minha sinopse personalizada salva localmente.", persistedAfterRestart.effectiveDescription)
            assertTrue(File(persistedAfterRestart.effectiveCoverPath!!).exists())

            // 5. Restore automatic metadata clears manual overrides and falls back cleanly to automatic metadata
            online = true
            val restored = repoAfterRestart.restoreAutomaticMetadata(testGame, refetchNow = false)
            assertEquals("Silent Hill", restored.effectiveTitle)
            assertFalse(restored.hasManualEdits)
            assertEquals("Wikipédia (PT)", restored.descriptionSourceLabel)
            assertTrue(File(restored.effectiveCoverPath!!).exists())

            pickedImageFile.delete()
        } finally {
            db.gameMetadataDao().deleteAll()
        }
    }

    @Test
    fun `game details screen shows honest fallback when unavailable and displays attribution and edit dialog when cached`() {
        val testGame = GameEntry(
            id = 77L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Silent Hill (USA)",
            fileName = "Silent Hill (USA).cue",
            uriString = "content://com.example.test/ps1/SilentHill.cue",
            sourceFolderUri = "content://com.example.test/ps1",
            fileType = "CUE"
        )

        val emptyUiState = DomDeckUiState(ps1Games = listOf(testGame), allGames = listOf(testGame))
        composeTestRule.setContent {
            DomDeckTheme {
                com.example.ui.screens.GameDetailsScreen(
                    game = testGame,
                    uiState = emptyUiState,
                    onEnsureMetadata = { _, _ -> },
                    onSaveManualMetadata = { _, _, _, _, _ -> },
                    onRestoreAutomaticMetadata = {},
                    onBack = {},
                    onOpenSetup = {},
                    onPlayClick = {},
                    onOpenDuckStationApp = {},
                    onDismissLaunchResult = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("details_play_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_description_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(com.example.data.GameMetadata.DESCRIPTION_UNAVAILABLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_edit_metadata_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `game details screen renders cached attribution and opens Editar informacoes dialog`() {
        val testGame = GameEntry(
            id = 78L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Silent Hill (USA)",
            fileName = "Silent Hill (USA).cue",
            uriString = "content://com.example.test/ps1/SilentHillCached.cue",
            sourceFolderUri = "content://com.example.test/ps1",
            fileType = "CUE"
        )
        val cachedMeta = com.example.data.GameMetadata(
            gameUri = testGame.uriString,
            normalizedTitle = "Silent Hill",
            effectiveTitle = "Silent Hill",
            effectiveDescription = "Silent Hill é um jogo eletrônico de survival horror para PlayStation.",
            descriptionSourceLabel = "Wikipédia (PT)",
            descriptionSourceUrl = "https://pt.wikipedia.org/wiki/Silent_Hill",
            descriptionLicense = "CC BY-SA 4.0",
            coverSourceLabel = "Libretro Thumbnails"
        )
        val cachedUiState = DomDeckUiState(
            ps1Games = listOf(testGame),
            allGames = listOf(testGame),
            gameMetadataByUri = mapOf(testGame.uriString to cachedMeta)
        )
        composeTestRule.setContent {
            DomDeckTheme {
                com.example.ui.screens.GameDetailsScreen(
                    game = testGame,
                    uiState = cachedUiState,
                    onEnsureMetadata = { _, _ -> },
                    onSaveManualMetadata = { _, _, _, _, _ -> },
                    onRestoreAutomaticMetadata = {},
                    onBack = {},
                    onOpenSetup = {},
                    onPlayClick = {},
                    onOpenDuckStationApp = {},
                    onDismissLaunchResult = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("details_metadata_attribution").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Fonte: Wikipédia (PT) • CC BY-SA 4.0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_edit_metadata_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_title_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_pick_cover_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_restore_auto_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_save_button").assertIsDisplayed()
    }

    @Test
    fun `room migration 1 to 2 creates game_metadata table preserving v1 schema expectations`() {
        val dbFile = ApplicationProvider.getApplicationContext<Application>()
            .getDatabasePath("migration_test_${System.nanoTime()}.db")
        dbFile.parentFile?.mkdirs()
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS games (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, consoleId TEXT NOT NULL, displayTitle TEXT NOT NULL, fileName TEXT NOT NULL, uriString TEXT NOT NULL, sourceFolderUri TEXT NOT NULL, fileType TEXT NOT NULL, fileSizeBytes INTEGER NOT NULL, needsVerification INTEGER NOT NULL, statusNote TEXT)"
            )
            sqlite.execSQL(
                "INSERT INTO games (consoleId, displayTitle, fileName, uriString, sourceFolderUri, fileType, fileSizeBytes, needsVerification, statusNote) VALUES ('ps1', 'Silent Hill (USA)', 'Silent Hill (USA).cue', 'content://ps1/sh.cue', 'content://ps1', 'CUE', 1234, 0, NULL)"
            )
            val supportDb = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
                .create(
                    androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
                        ApplicationProvider.getApplicationContext()
                    )
                        .name(dbFile.absolutePath)
                        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                        })
                        .build()
                ).writableDatabase
            com.example.data.local.DomDeckDatabase.MIGRATION_1_2.migrate(supportDb)
            supportDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='game_metadata'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            supportDb.query("SELECT COUNT(*) FROM games").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            supportDb.close()
        } finally {
            sqlite.close()
            dbFile.delete()
        }
    }

    @Test
    fun `game library screen renders vertical cover card with resolved title`() {
        val testGame = GameEntry(
            id = 88L,
            consoleId = ConsoleCatalog.PS1_ID,
            displayTitle = "Silent Hill (USA)",
            fileName = "Silent Hill (USA).cue",
            uriString = "content://com.example.test/ps1/SilentHillLib.cue",
            sourceFolderUri = "content://com.example.test/ps1",
            fileType = "CUE"
        )
        val metadata = com.example.data.GameMetadata(
            gameUri = testGame.uriString,
            normalizedTitle = "Silent Hill",
            effectiveTitle = "Silent Hill",
            effectiveDescription = "Survival horror no PlayStation 1.",
            descriptionSourceLabel = "Wikipédia (PT)",
            descriptionLicense = "CC BY-SA 4.0"
        )
        val libraryUiState = DomDeckUiState(
            ps1Games = listOf(testGame),
            allGames = listOf(testGame),
            gameMetadataByUri = mapOf(testGame.uriString to metadata)
        )

        composeTestRule.setContent {
            DomDeckTheme {
                com.example.ui.screens.GameLibraryScreen(
                    uiState = libraryUiState,
                    onBack = {},
                    onOpenSetup = {},
                    onFolderPicked = {},
                    onStartScan = {},
                    onCancelScan = {},
                    onOpenGameDetails = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("game_card_${testGame.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("library_card_title_${testGame.id}", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun registerFakeDuckStation(app: Application) {
        val shadowPm = Shadows.shadowOf(app.packageManager)
        val pkgInfo = PackageInfo().apply {
            packageName = ConsoleCatalog.DUCKSTATION_PACKAGE
            versionName = "0.1-test"
        }
        shadowPm.installPackage(pkgInfo)
        val component = ComponentName(
            ConsoleCatalog.DUCKSTATION_PACKAGE,
            "com.github.stenzek.duckstation.EmulationActivity"
        )
        shadowPm.addActivityIfNotPresent(component)
        shadowPm.addOrUpdateActivity(
            ActivityInfo().apply {
                packageName = component.packageName
                name = component.className
                exported = true
            }
        )
    }

    class FakeTreeDocumentsProvider : android.content.ContentProvider() {
        companion object {
            lateinit var rootDir: File
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val cols = projection ?: arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_FLAGS,
                DocumentsContract.Document.COLUMN_SIZE
            )
            val cursor = MatrixCursor(cols)
            val segments = uri.pathSegments
            val isChildren = segments.lastOrNull() == "children"
            val docId = if (isChildren) {
                segments.getOrNull(segments.size - 2) ?: "root"
            } else {
                DocumentsContract.getDocumentId(uri)
            }
            val targetFile = resolveFileForDocId(docId)
            if (!targetFile.exists()) return cursor

            if (isChildren) {
                targetFile.listFiles()?.forEach { child ->
                    val childId = when {
                        docId == "root" -> child.name
                        docId.endsWith(":") -> "$docId${child.name}"
                        else -> "$docId/${child.name}"
                    }
                    addFileRow(cursor, cols, childId, child)
                }
            } else {
                addFileRow(cursor, cols, docId, targetFile)
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: android.content.ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val docId = DocumentsContract.getDocumentId(uri)
            val file = resolveFileForDocId(docId)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        private fun resolveFileForDocId(docId: String): File {
            if (docId == "root") return rootDir
            val relative = if (':' in docId) docId.substringAfter(':') else docId
            return if (relative.isBlank()) rootDir else File(rootDir, relative)
        }

        private fun addFileRow(
            cursor: MatrixCursor,
            cols: Array<out String>,
            docId: String,
            file: File
        ) {
            val row = cursor.newRow()
            val mime = if (file.isDirectory) {
                DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                "application/octet-stream"
            }
            for (col in cols) {
                when (col) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add(docId)
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add(file.name)
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(mime)
                    DocumentsContract.Document.COLUMN_FLAGS -> row.add(0)
                    DocumentsContract.Document.COLUMN_SIZE -> row.add(file.length())
                    else -> row.add(null)
                }
            }
        }
    }
}
