package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_metadata")
data class GameMetadataEntity(
    @PrimaryKey
    val gameUri: String,
    val normalizedTitle: String,
    val autoTitle: String? = null,
    val autoDescription: String? = null,
    val autoCoverPath: String? = null,
    val autoCoverSource: String? = null,
    val descriptionSourceName: String? = null,
    val descriptionSourceUrl: String? = null,
    val descriptionLicense: String? = null,
    val manualTitle: String? = null,
    val manualSynopsis: String? = null,
    val manualCoverPath: String? = null,
    val fetchStatus: String = "IDLE",
    val lastAttemptTimestamp: Long = 0L,
    val lastSuccessTimestamp: Long = 0L
)
