package com.example.data.metadata

import com.example.model.GameEntry

sealed interface DescriptionFetchResult {
    data class Found(
        val resolvedTitle: String?,
        val description: String,
        val sourceLabel: String,
        val sourceUrl: String,
        val licenseText: String
    ) : DescriptionFetchResult

    data object NotFound : DescriptionFetchResult
    data class NetworkError(val message: String) : DescriptionFetchResult
}

interface GameDescriptionSource {
    suspend fun fetchPs1Description(game: GameEntry, searchTitle: String): DescriptionFetchResult
}
