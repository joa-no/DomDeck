package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameMetadataDao {
    @Query("SELECT * FROM game_metadata")
    fun observeAll(): Flow<List<GameMetadataEntity>>

    @Query("SELECT * FROM game_metadata WHERE gameUri = :gameUri LIMIT 1")
    fun observeByUri(gameUri: String): Flow<GameMetadataEntity?>

    @Query("SELECT * FROM game_metadata WHERE gameUri = :gameUri LIMIT 1")
    suspend fun getByUri(gameUri: String): GameMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GameMetadataEntity)

    @Query("DELETE FROM game_metadata WHERE gameUri = :gameUri")
    suspend fun deleteByUri(gameUri: String)

    @Query("DELETE FROM game_metadata")
    suspend fun deleteAll()
}
