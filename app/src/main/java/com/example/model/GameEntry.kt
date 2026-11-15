package com.example.model

data class GameEntry(
    val id: Long = 0L,
    val consoleId: String,
    val displayTitle: String,
    val fileName: String,
    val uriString: String,
    val sourceFolderUri: String,
    val fileType: String,
    val needsVerification: Boolean = false,
    val statusNote: String? = null,
    val fileSizeBytes: Long = 0L,
    val lastScannedAt: Long = 0L
)
