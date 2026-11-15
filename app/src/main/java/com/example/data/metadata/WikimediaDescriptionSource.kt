package com.example.data.metadata

import android.net.Uri
import com.example.model.GameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Fetches encyclopedic descriptions for PlayStation 1 titles from Wikimedia public APIs,
 * querying pt.wikipedia.org first and falling back to en.wikipedia.org.
 *
 * Strictly validates that the returned article refers to the original PS1 game and preserves
 * visible Wikipedia + CC BY-SA 4.0 attribution.
 */
class WikimediaDescriptionSource(
    private val httpClient: OkHttpClient,
    private val ptBaseUrl: String = DEFAULT_PT_BASE_URL,
    private val enBaseUrl: String = DEFAULT_EN_BASE_URL
) : GameDescriptionSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchPs1Description(
        game: GameEntry,
        searchTitle: String
    ): DescriptionFetchResult = withContext(Dispatchers.IO) {
        val cleanQuery = searchTitle.ifBlank { GameTitleNormalizer.normalizeForSearch(game.fileName) }
        if (cleanQuery.isBlank()) return@withContext DescriptionFetchResult.NotFound

        var networkFailure: String? = null

        val endpoints = listOf(
            WikiEdition("PT", ptBaseUrl.trimEnd('/'), "Wikipédia (PT)"),
            WikiEdition("EN", enBaseUrl.trimEnd('/'), "Wikipedia (EN)")
        )

        for (edition in endpoints) {
            when (val result = queryEdition(edition, cleanQuery)) {
                is DescriptionFetchResult.Found -> return@withContext result
                is DescriptionFetchResult.NetworkError -> networkFailure = result.message
                DescriptionFetchResult.NotFound -> Unit
            }
        }

        if (networkFailure != null) {
            DescriptionFetchResult.NetworkError(networkFailure)
        } else {
            DescriptionFetchResult.NotFound
        }
    }

    private fun queryEdition(edition: WikiEdition, cleanQuery: String): DescriptionFetchResult {
        val candidateTitles = linkedSetOf<String>()

        // 1. Search Wikipedia with explicit PlayStation context
        val searchTerm = "\"$cleanQuery\" PlayStation"
        val searchUrl =
            "${edition.baseUrl}/w/api.php?action=query&list=search&srsearch=${Uri.encode(searchTerm)}&srlimit=5&format=json&utf8=1"

        try {
            executeGet(searchUrl)?.use { response ->
                if (response.code == 429 || response.code >= 500) {
                    return DescriptionFetchResult.NetworkError(
                        "Serviço da Wikipédia temporariamente indisponível (HTTP ${response.code})."
                    )
                }
                if (response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val parsed = runCatching { json.decodeFromString<WikiSearchResponse>(raw) }.getOrNull()
                    parsed?.query?.search.orEmpty().forEach { hit ->
                        if (hit.title.isNotBlank()) {
                            candidateTitles.add(hit.title)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            return DescriptionFetchResult.NetworkError(e.message ?: "Erro de conexão ao consultar Wikipédia.")
        }

        // 2. Also include direct exact-title / disambiguated game lookups in case search returned noisy ordering
        if (edition.code == "PT") {
            candidateTitles.add("$cleanQuery (jogo eletrônico)")
        } else {
            candidateTitles.add("$cleanQuery (video game)")
        }
        candidateTitles.add(cleanQuery)

        // 3. Validate each candidate summary strictly before returning
        var checked = 0
        for (candidate in candidateTitles) {
            if (checked >= 5) break
            checked++
            val normalizedPageTitle = candidate.trim().replace(' ', '_')
            val summaryUrl = "${edition.baseUrl}/api/rest_v1/page/summary/${Uri.encode(normalizedPageTitle)}"

            try {
                executeGet(summaryUrl)?.use { response ->
                    if (response.code == 429 || response.code >= 500) {
                        return DescriptionFetchResult.NetworkError(
                            "Serviço da Wikipédia indisponível (HTTP ${response.code})."
                        )
                    }
                    if (!response.isSuccessful) return@use

                    val body = response.body?.string().orEmpty()
                    val summary = runCatching { json.decodeFromString<WikiSummaryResponse>(body) }.getOrNull()
                        ?: return@use

                    val extract = summary.extract?.trim().orEmpty()
                    if (extract.isBlank()) return@use

                    val isValid = GameTitleNormalizer.isValidPs1WikipediaMatch(
                        queryTitle = cleanQuery,
                        articleTitle = summary.title,
                        articleType = summary.type,
                        description = summary.description,
                        extractOrSnippet = extract
                    )
                    if (!isValid) return@use

                    val pageUrl = summary.contentUrls?.desktop?.page
                        ?.takeIf { it.isNotBlank() }
                        ?: "${edition.baseUrl}/wiki/${Uri.encode(normalizedPageTitle)}"

                    return DescriptionFetchResult.Found(
                        resolvedTitle = cleanQuery,
                        description = extract,
                        sourceLabel = edition.label,
                        sourceUrl = pageUrl,
                        licenseText = WIKIPEDIA_LICENSE
                    )
                }
            } catch (e: IOException) {
                return DescriptionFetchResult.NetworkError(e.message ?: "Erro de rede ao ler resumo da Wikipédia.")
            }
        }

        return DescriptionFetchResult.NotFound
    }

    private fun executeGet(url: String) = run {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        httpClient.newCall(request).execute()
    }

    private data class WikiEdition(
        val code: String,
        val baseUrl: String,
        val label: String
    )

    @Serializable
    private data class WikiSearchResponse(
        val query: WikiQueryBlock? = null
    )

    @Serializable
    private data class WikiQueryBlock(
        val search: List<WikiSearchHit> = emptyList()
    )

    @Serializable
    private data class WikiSearchHit(
        val title: String = "",
        val snippet: String = ""
    )

    @Serializable
    private data class WikiSummaryResponse(
        val type: String? = null,
        val title: String = "",
        val description: String? = null,
        val extract: String? = null,
        @SerialName("content_urls")
        val contentUrls: WikiContentUrls? = null
    )

    @Serializable
    private data class WikiContentUrls(
        val desktop: WikiDesktopUrl? = null
    )

    @Serializable
    private data class WikiDesktopUrl(
        val page: String? = null
    )

    companion object {
        const val DEFAULT_PT_BASE_URL = "https://pt.wikipedia.org"
        const val DEFAULT_EN_BASE_URL = "https://en.wikipedia.org"
        const val WIKIPEDIA_LICENSE = "CC BY-SA 4.0"
        private const val USER_AGENT = "DomDeck/0.2 (Android Retro Launcher; contact: local-app)"
    }
}
