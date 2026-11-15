package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameEntity::class, GameMetadataEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DomDeckDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameMetadataDao(): GameMetadataDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `game_metadata` (
                        `gameUri` TEXT NOT NULL,
                        `normalizedTitle` TEXT NOT NULL,
                        `autoTitle` TEXT,
                        `autoDescription` TEXT,
                        `autoCoverPath` TEXT,
                        `autoCoverSource` TEXT,
                        `descriptionSourceName` TEXT,
                        `descriptionSourceUrl` TEXT,
                        `descriptionLicense` TEXT,
                        `manualTitle` TEXT,
                        `manualSynopsis` TEXT,
                        `manualCoverPath` TEXT,
                        `fetchStatus` TEXT NOT NULL,
                        `lastAttemptTimestamp` INTEGER NOT NULL,
                        `lastSuccessTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`gameUri`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: DomDeckDatabase? = null

        fun getInstance(context: Context): DomDeckDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DomDeckDatabase::class.java,
                    "domdeck_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
