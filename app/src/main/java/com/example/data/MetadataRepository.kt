package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.data.local.GameMetadataDao
import com.example.data.local.GameMetadataEntity
import com.example.data.metadata.CoverArtSource
import com.example.data.metadata.CoverFetchResult
import com.example.data.metadata.CoverFileCache
import com.example.data.metadata.DescriptionFetchResult
import com.example.data.metadata.GameDescriptionSource
import com.example.data.metadata.GameTitleNormalizer
import com.example.data.metadata.LibretroCoverArtSource
import com.example.data.metadata.WikimediaDescriptionSource
import com.example.model.GameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class MetadataFetchStatus {
    IDLE,
    LOADING,
    COMPLETE,
    PARTIAL,
    NOT_FOUND,
    OFFLINE_NO_CACHE,
    ERROR
}

data class GameMetadata(
    val gameUri: String = "",
    val normalizedTitle: String = "",
    val effectiveTitle: String = "",
    val effectiveCoverPath: String? = null,
    val effectiveDescription: String? = null,
    val synopsis: String = DESCRIPTION_UNAVAILABLE,
    val coverArtUri: String? = null,
    val isManualTitle: Boolean = false,
    val isManualCover: Boolean = false,
    val isManualSynopsis: Boolean = false,
    val hasManualEdits: Boolean = false,
    val isDescriptionManual: Boolean = false,
    val coverSourceLabel: String? = null,
    val descriptionSourceLabel: String? = null,
    val descriptionSourceUrl: String? = null,
    val descriptionLicense: String? = null,
    val fetchStatus: MetadataFetchStatus = MetadataFetchStatus.IDLE,
    val isLoading: Boolean = false
) {
    companion object {
        const val DESCRIPTION_UNAVAILABLE = "Descrição não disponível"

        fun fallbackFor(game: GameEntry): GameMetadata {
            val clean = GameTitleNormalizer.normalizeForSearch(game.fileName)
                .ifBlank { game.displayTitle }
            return GameMetadata(
                gameUri = game.uriString,
                normalizedTitle = clean,
                effectiveTitle = game.displayTitle,
                effectiveCoverPath = null,
                effectiveDescription = null,
                synopsis = DESCRIPTION_UNAVAILABLE,
                coverArtUri = null,
                fetchStatus = MetadataFetchStatus.IDLE
            )
        }
    }
}

interface MetadataRepository {
    fun observeAllMetadata(): Flow<Map<String, GameMetadata>>
    fun observeMetadata(game: GameEntry): Flow<GameMetadata>
    suspend fun getMetadata(game: GameEntry): GameMetadata
    suspend fun ensureMetadataCached(game: GameEntry, forceRefresh: Boolean = false): GameMetadata
    suspend fun prefetchMissingMetadata(games: List<GameEntry>, maxToFetch: Int = DEFAULT_PREFETCH_LIMIT)
    suspend fun saveManualEdits(
        game: GameEntry,
        customTitle: String?,
        customSynopsis: String?,
        pickedCoverUri: Uri?,
        clearCustomCover: Boolean = false
    ): GameMetadata
    suspend fun restoreAutomaticMetadata(game: GameEntry, refetchNow: Boolean = true): GameMetadata

    companion object {
        const val DEFAULT_PREFETCH_LIMIT = 12
    }
}

class OfflineFirstMetadataRepository(
    private val context: Context,
    private val metadataDao: GameMetadataDao,
    private val coverCache: CoverFileCache = CoverFileCache(context),
    private val coverSources: List<CoverArtSource>,
    private val descriptionSources: List<GameDescriptionSource>,
    private val isNetworkAvailable: () -> Boolean = { checkConnectivity(context) },
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : MetadataRepository {

    private val inFlightUris = MutableStateFlow<Set<String>>(emptySet())
    private val perUriLocks = ConcurrentHashMap<String, Mutex>()

    override fun observeAllMetadata(): Flow<Map<String, GameMetadata>> {
        return combine(metadataDao.observeAll(), inFlightUris) { entities, loadingUris ->
            entities.associate { entity ->
                entity.gameUri to entity.toDomainModel(
                    fallbackTitle = entity.normalizedTitle.ifBlank { "PlayStation 1" },
                    isLoading = entity.gameUri in loadingUris
                )
            }
        }
    }

    override fun observeMetadata(game: GameEntry): Flow<GameMetadata> {
        return combine(metadataDao.observeByUri(game.uriString), inFlightUris) { entity, loadingUris ->
            val isLoading = game.uriString in loadingUris
            entity?.toDomainModel(
                fallbackTitle = game.displayTitle,
                isLoading = isLoading
            ) ?: GameMetadata.fallbackFor(game).copy(isLoading = isLoading)
        }
    }

    override suspend fun getMetadata(game: GameEntry): GameMetadata {
        val existing = metadataDao.getByUri(game.uriString)
        val isLoading = game.uriString in inFlightUris.value
        return existing?.toDomainModel(game.displayTitle, isLoading)
            ?: GameMetadata.fallbackFor(game).copy(isLoading = isLoading)
    }

    override suspend fun ensureMetadataCached(game: GameEntry, forceRefresh: Boolean): GameMetadata {
        val lock = perUriLocks.computeIfAbsent(game.uriString) { Mutex() }
        return lock.withLock {
            val existing = metadataDao.getByUri(game.uriString)
            val now = clockMs()
            val normalizedTitle = GameTitleNormalizer.normalizeForSearch(game.fileName)
                .ifBlank { game.displayTitle }

            val hasValidAutoCover = coverCache.isReadableFile(existing?.autoCoverPath)
            val hasManualCover = coverCache.isReadableFile(existing?.manualCoverPath)
            val hasCover = hasValidAutoCover || hasManualCover
            val hasDescription = !existing?.autoDescription.isNullOrBlank() ||
                !existing?.manualSynopsis.isNullOrBlank()

            val recentlyAttempted = existing != null &&
                existing.lastAttemptTimestamp > 0L &&
                (now - existing.lastAttemptTimestamp) < RETRY_COOLDOWN_MS

            // Avoid redundant downloads when cached assets exist or a recent attempt already ran
            if (!forceRefresh && ((hasCover && hasDescription) || recentlyAttempted)) {
                return@withLock (existing ?: defaultEntity(game, normalizedTitle))
                    .toDomainModel(game.displayTitle, isLoading = false)
            }

            if (!isNetworkAvailable()) {
                val base = existing ?: defaultEntity(game, normalizedTitle)
                val nextStatus = if (hasCover || hasDescription) {
                    base.fetchStatus
                } else {
                    MetadataFetchStatus.OFFLINE_NO_CACHE.name
                }
                val offlineEntity = base.copy(
                    normalizedTitle = normalizedTitle,
                    fetchStatus = nextStatus
                )
                if (existing == null) {
                    metadataDao.upsert(offlineEntity)
                }
                return@withLock offlineEntity.toDomainModel(game.displayTitle, isLoading = false)
            }

            setInFlight(game.uriString, loading = true)
            try {
                var autoCoverPath = if (hasValidAutoCover) existing?.autoCoverPath else null
                var autoCoverSource = if (hasValidAutoCover) existing?.autoCoverSource else null
                var autoDescription = existing?.autoDescription
                var autoTitle = existing?.autoTitle
                var descSourceName = existing?.descriptionSourceName
                var descSourceUrl = existing?.descriptionSourceUrl
                var descLicense = existing?.descriptionLicense

                var anyBlockedOrNetworkError = false

                // 1. Fetch cover if missing or forced
                if (forceRefresh || autoCoverPath == null) {
                    for (source in coverSources) {
                        when (val coverRes = source.fetchPs1Cover(game, normalizedTitle)) {
                            is CoverFetchResult.Found -> {
                                val savedPath = coverCache.saveAutomaticCover(game.uriString, coverRes.bytes)
                                if (savedPath != null) {
                                    autoCoverPath = savedPath
                                    autoCoverSource = coverRes.sourceLabel
                                }
                                break
                            }
                            is CoverFetchResult.Blocked -> {
                                anyBlockedOrNetworkError = true
                            }
                            is CoverFetchResult.NetworkError -> {
                                anyBlockedOrNetworkError = true
                            }
                            CoverFetchResult.NotFound -> Unit
                        }
                    }
                }

                // 2. Fetch encyclopedic description if missing or forced
                if (forceRefresh || autoDescription.isNullOrBlank()) {
                    for (source in descriptionSources) {
                        when (val descRes = source.fetchPs1Description(game, normalizedTitle)) {
                            is DescriptionFetchResult.Found -> {
                                autoTitle = descRes.resolvedTitle?.takeIf { it.isNotBlank() } ?: normalizedTitle
                                autoDescription = descRes.description
                                descSourceName = descRes.sourceLabel
                                descSourceUrl = descRes.sourceUrl
                                descLicense = descRes.licenseText
                                break
                            }
                            is DescriptionFetchResult.NetworkError -> {
                                anyBlockedOrNetworkError = true
                            }
                            DescriptionFetchResult.NotFound -> Unit
                        }
                    }
                }

                val hasFinalCover = coverCache.isReadableFile(autoCoverPath)
                val hasFinalDesc = !autoDescription.isNullOrBlank()
                val status = when {
                    hasFinalCover && hasFinalDesc -> MetadataFetchStatus.COMPLETE
                    hasFinalCover || hasFinalDesc -> MetadataFetchStatus.PARTIAL
                    anyBlockedOrNetworkError -> MetadataFetchStatus.ERROR
                    else -> MetadataFetchStatus.NOT_FOUND
                }

                val updated = (existing ?: defaultEntity(game, normalizedTitle)).copy(
                    normalizedTitle = normalizedTitle,
                    autoTitle = autoTitle ?: normalizedTitle,
                    autoDescription = autoDescription,
                    autoCoverPath = autoCoverPath,
                    autoCoverSource = autoCoverSource,
                    descriptionSourceName = descSourceName,
                    descriptionSourceUrl = descSourceUrl,
                    descriptionLicense = descLicense,
                    fetchStatus = status.name,
                    lastAttemptTimestamp = now,
                    lastSuccessTimestamp = if (hasFinalCover || hasFinalDesc) now else (existing?.lastSuccessTimestamp ?: 0L)
                )
                metadataDao.upsert(updated)
                updated.toDomainModel(game.displayTitle, isLoading = false)
            } finally {
                setInFlight(game.uriString, loading = false)
            }
        }
    }

    override suspend fun prefetchMissingMetadata(games: List<GameEntry>, maxToFetch: Int) {
        if (games.isEmpty() || maxToFetch <= 0 || !isNetworkAvailable()) return
        withContext(Dispatchers.IO) {
            var attemptedCount = 0
            for (game in games) {
                if (attemptedCount >= maxToFetch) break
                val existing = metadataDao.getByUri(game.uriString)
                val hasCover = coverCache.isReadableFile(existing?.autoCoverPath) ||
                    coverCache.isReadableFile(existing?.manualCoverPath)
                val hasDesc = !existing?.autoDescription.isNullOrBlank() ||
                    !existing?.manualSynopsis.isNullOrBlank()
                val alreadyAttemptedRecently = existing != null &&
                    existing.lastAttemptTimestamp > 0L &&
                    (clockMs() - existing.lastAttemptTimestamp) < RETRY_COOLDOWN_MS

                if ((hasCover && hasDesc) || alreadyAttemptedRecently) {
                    continue
                }
                attemptedCount++
                runCatching { ensureMetadataCached(game, forceRefresh = false) }
            }
        }
    }

    override suspend fun saveManualEdits(
        game: GameEntry,
        customTitle: String?,
        customSynopsis: String?,
        pickedCoverUri: Uri?,
        clearCustomCover: Boolean
    ): GameMetadata {
        val lock = perUriLocks.computeIfAbsent(game.uriString) { Mutex() }
        return lock.withLock {
            val normalizedTitle = GameTitleNormalizer.normalizeForSearch(game.fileName)
                .ifBlank { game.displayTitle }
            val existing = metadataDao.getByUri(game.uriString) ?: defaultEntity(game, normalizedTitle)

            var nextManualCoverPath = existing.manualCoverPath
            if (clearCustomCover) {
                coverCache.deleteFileIfOwned(existing.manualCoverPath)
                nextManualCoverPath = null
            } else if (pickedCoverUri != null) {
                val savedCustom = coverCache.saveCustomCoverFromUri(game.uriString, pickedCoverUri)
                if (savedCustom != null) {
                    if (existing.manualCoverPath != savedCustom) {
                        coverCache.deleteFileIfOwned(existing.manualCoverPath)
                    }
                    nextManualCoverPath = savedCustom
                }
            }

            val nextManualTitle = customTitle?.trim()?.takeIf { it.isNotBlank() }
            val nextManualSynopsis = customSynopsis?.trim()?.takeIf { it.isNotBlank() }

            val updated = existing.copy(
                normalizedTitle = normalizedTitle,
                manualTitle = nextManualTitle,
                manualSynopsis = nextManualSynopsis,
                manualCoverPath = nextManualCoverPath
            )
            metadataDao.upsert(updated)
            updated.toDomainModel(game.displayTitle, isLoading = false)
        }
    }

    override suspend fun restoreAutomaticMetadata(
        game: GameEntry,
        refetchNow: Boolean
    ): GameMetadata {
        val lock = perUriLocks.computeIfAbsent(game.uriString) { Mutex() }
        val cleared = lock.withLock {
            val existing = metadataDao.getByUri(game.uriString)
            if (existing != null) {
                coverCache.deleteFileIfOwned(existing.manualCoverPath)
                val updated = existing.copy(
                    manualTitle = null,
                    manualSynopsis = null,
                    manualCoverPath = null,
                    lastAttemptTimestamp = 0L
                )
                metadataDao.upsert(updated)
                updated.toDomainModel(game.displayTitle, isLoading = false)
            } else {
                GameMetadata.fallbackFor(game)
            }
        }
        return if (refetchNow && isNetworkAvailable()) {
            ensureMetadataCached(game, forceRefresh = true)
        } else {
            cleared
        }
    }

    private fun setInFlight(uri: String, loading: Boolean) {
        inFlightUris.value = if (loading) {
            inFlightUris.value + uri
        } else {
            inFlightUris.value - uri
        }
    }

    private fun defaultEntity(game: GameEntry, normalizedTitle: String): GameMetadataEntity {
        return GameMetadataEntity(
            gameUri = game.uriString,
            normalizedTitle = normalizedTitle,
            autoTitle = normalizedTitle
        )
    }

    private fun GameMetadataEntity.toDomainModel(
        fallbackTitle: String,
        isLoading: Boolean
    ): GameMetadata {
        val manualCoverValid = coverCache.isReadableFile(manualCoverPath)
        val autoCoverValid = coverCache.isReadableFile(autoCoverPath)

        val resolvedTitle = manualTitle?.takeIf { it.isNotBlank() }
            ?: autoTitle?.takeIf { it.isNotBlank() }
            ?: normalizedTitle.takeIf { it.isNotBlank() }
            ?: fallbackTitle

        val resolvedCover = when {
            manualCoverValid -> manualCoverPath
            autoCoverValid -> autoCoverPath
            else -> null
        }

        val hasManualDesc = !manualSynopsis.isNullOrBlank()
        val hasAutoDesc = !autoDescription.isNullOrBlank()
        val resolvedDescription = when {
            hasManualDesc -> manualSynopsis
            hasAutoDesc -> autoDescription
            else -> null
        }

        val parsedStatus = runCatching { MetadataFetchStatus.valueOf(fetchStatus) }
            .getOrDefault(MetadataFetchStatus.IDLE)

        return GameMetadata(
            gameUri = gameUri,
            normalizedTitle = normalizedTitle,
            effectiveTitle = resolvedTitle,
            effectiveCoverPath = resolvedCover,
            effectiveDescription = resolvedDescription,
            synopsis = resolvedDescription ?: GameMetadata.DESCRIPTION_UNAVAILABLE,
            coverArtUri = resolvedCover,
            isManualTitle = !manualTitle.isNullOrBlank(),
            isManualCover = manualCoverValid,
            isManualSynopsis = hasManualDesc,
            hasManualEdits = !manualTitle.isNullOrBlank() || manualCoverValid || hasManualDesc,
            isDescriptionManual = hasManualDesc,
            coverSourceLabel = when {
                manualCoverValid -> "Capa personalizada"
                autoCoverValid -> autoCoverSource ?: LibretroCoverArtSource.SOURCE_LABEL
                else -> null
            },
            descriptionSourceLabel = if (hasManualDesc) {
                "Editado manualmente"
            } else if (hasAutoDesc) {
                descriptionSourceName
            } else {
                null
            },
            descriptionSourceUrl = if (hasManualDesc) null else descriptionSourceUrl,
            descriptionLicense = if (hasManualDesc) null else descriptionLicense,
            fetchStatus = if (isLoading) MetadataFetchStatus.LOADING else parsedStatus,
            isLoading = isLoading
        )
    }

    companion object {
        const val RETRY_COOLDOWN_MS = 24L * 60L * 60L * 1000L

        fun createDefault(context: Context, metadataDao: GameMetadataDao): OfflineFirstMetadataRepository {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            return OfflineFirstMetadataRepository(
                context = context.applicationContext,
                metadataDao = metadataDao,
                coverCache = CoverFileCache(context.applicationContext),
                coverSources = listOf(LibretroCoverArtSource(httpClient)),
                descriptionSources = listOf(WikimediaDescriptionSource(httpClient))
            )
        }

        private fun checkConnectivity(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
