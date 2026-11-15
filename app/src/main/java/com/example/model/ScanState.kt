package com.example.model

sealed interface ScanState {
    data object Idle : ScanState

    data class Scanning(
        val inspectedEntries: Int,
        val currentFolder: String
    ) : ScanState

    data class Success(
        val totalGames: Int,
        val verificationCount: Int,
        val message: String
    ) : ScanState

    data class Error(
        val message: String
    ) : ScanState
}
