package com.example.data.metadata

import com.example.model.GameEntry

sealed interface CoverFetchResult {
    data class Found(
        val bytes: ByteArray,
        val sourceLabel: String,
        val resolvedUrl: String
    ) : CoverFetchResult

    data object NotFound : CoverFetchResult
    data class Blocked(val reason: String) : CoverFetchResult
    data class NetworkError(val message: String) : CoverFetchResult
}

interface CoverArtSource {
    suspend fun fetchPs1Cover(game: GameEntry, searchTitle: String): CoverFetchResult
}
