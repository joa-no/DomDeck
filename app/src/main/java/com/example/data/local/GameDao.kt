package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class ConsoleGameCount(
    val consoleId: String,
    val gameCount: Int
)

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY displayTitle COLLATE NOCASE ASC")
    fun observeAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE consoleId = :consoleId ORDER BY displayTitle COLLATE NOCASE ASC")
    fun observeGamesForConsole(consoleId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun observeGameById(gameId: Long): Flow<GameEntity?>

    @Query("SELECT COUNT(*) FROM games")
    fun observeTotalGameCount(): Flow<Int>

    @Query("SELECT consoleId, COUNT(*) AS gameCount FROM games GROUP BY consoleId")
    fun observeConsoleCounts(): Flow<List<ConsoleGameCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Query("DELETE FROM games WHERE consoleId = :consoleId")
    suspend fun deleteGamesForConsole(consoleId: String)

    @Transaction
    suspend fun replaceGamesForConsole(consoleId: String, games: List<GameEntity>) {
        deleteGamesForConsole(consoleId)
        if (games.isNotEmpty()) {
            insertAll(games.distinctBy { it.uriString })
        }
    }
}
