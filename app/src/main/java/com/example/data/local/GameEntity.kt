package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.GameEntry

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["consoleId", "uriString"], unique = true)
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val consoleId: String,
    val displayTitle: String,
    val fileName: String,
    val uriString: String,
    val sourceFolderUri: String,
    val fileType: String,
    val needsVerification: Boolean,
    val statusNote: String?,
    val fileSizeBytes: Long,
    val lastScannedAt: Long
) {
    fun toDomain(): GameEntry = GameEntry(
        id = id,
        consoleId = consoleId,
        displayTitle = displayTitle,
        fileName = fileName,
        uriString = uriString,
        sourceFolderUri = sourceFolderUri,
        fileType = fileType,
        needsVerification = needsVerification,
        statusNote = statusNote,
        fileSizeBytes = fileSizeBytes,
        lastScannedAt = lastScannedAt
    )

    companion object {
        fun fromDomain(entry: GameEntry): GameEntity = GameEntity(
            id = entry.id,
            consoleId = entry.consoleId,
            displayTitle = entry.displayTitle,
            fileName = entry.fileName,
            uriString = entry.uriString,
            sourceFolderUri = entry.sourceFolderUri,
            fileType = entry.fileType,
            needsVerification = entry.needsVerification,
            statusNote = entry.statusNote,
            fileSizeBytes = entry.fileSizeBytes,
            lastScannedAt = entry.lastScannedAt
        )
    }
}
