package com.example.data

import com.example.data.local.GameDao
import com.example.data.local.GameEntity
import com.example.model.GameEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<GameEntry>> =
        gameDao.observeAllGames().map { entities -> entities.map { it.toDomain() } }

    val totalGameCount: Flow<Int> = gameDao.observeTotalGameCount()

    val consoleCounts: Flow<Map<String, Int>> =
        gameDao.observeConsoleCounts().map { list ->
            list.associate { it.consoleId to it.gameCount }
        }

    fun observeGamesForConsole(consoleId: String): Flow<List<GameEntry>> =
        gameDao.observeGamesForConsole(consoleId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeGameById(gameId: Long): Flow<GameEntry?> =
        gameDao.observeGameById(gameId).map { it?.toDomain() }

    suspend fun replaceGamesForConsole(consoleId: String, entries: List<GameEntry>) {
        val entities = entries.distinctBy { it.uriString }.map { GameEntity.fromDomain(it) }
        gameDao.replaceGamesForConsole(consoleId, entities)
    }

    suspend fun clearGamesForConsole(consoleId: String) {
        gameDao.deleteGamesForConsole(consoleId)
    }
}
