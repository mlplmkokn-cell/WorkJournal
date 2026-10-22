package com.workjournal.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkRecordEntity::class,
        PhotoEntity::class,
        CategoryEntity::class,
        CustomFieldEntity::class,
        FieldTemplateEntity::class
    ],
    version = 3,   // Увеличиваем версию — добавили custom_fields и field_templates
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workJournalDao(): WorkJournalDao
}
}
