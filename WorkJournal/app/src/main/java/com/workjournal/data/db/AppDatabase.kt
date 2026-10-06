package com.workjournal.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkRecordEntity::class,
        PhotoEntity::class,
        CategoryEntity::class
    ],
    version = 2,           // Увеличиваем версию — добавили таблицу categories
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workJournalDao(): WorkJournalDao
}
