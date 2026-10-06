package com.workjournal.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ТАБЛИЦА categories — пользовательские категории работ.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val isDefault: Boolean = false
)

/**
 * ТАБЛИЦА work_records — основная таблица всех работ.
 * Категория теперь хранится как строка (name + emoji),
 * а не ссылка на ID — проще и надёжнее для MVP.
 */
@Entity(tableName = "work_records")
data class WorkRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val caption: String,
    val workDate: Long,
    val createdAt: Long,
    val workType: String? = null,
    val address: String? = null,
    val description: String? = null,
    val categoryName: String = "Прочее",
    val categoryEmoji: String = "🔧",
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * ТАБЛИЦА photos — фотографии.
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = WorkRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val takenAt: Long? = null
)
