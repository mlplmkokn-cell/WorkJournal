package com.workjournal.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Таблица записей о работах */
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
    val categoryName: String = "",
    val categoryEmoji: String = "🔧",
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** Таблица фотографий */
@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = WorkRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
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

/** Таблица категорий — пользователь добавляет свои */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "",
    val isDefault: Boolean = false
)

/**
 * Таблица кастомных полей.
 * Пользователь сам придумывает названия: "ID площадки", "Адрес", "Район" и т.д.
 * Каждая запись может иметь любые поля.
 */
@Entity(
    tableName = "custom_fields",
    foreignKeys = [ForeignKey(
        entity = WorkRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recordId")]
)
data class CustomFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val fieldName: String,   // Например: "ID площадки"
    val fieldValue: String   // Например: "42А"
)

/**
 * Таблица шаблонов полей — запоминает какие поля пользователь использовал раньше.
 * Чтобы при следующем добавлении предлагать те же поля.
 */
@Entity(tableName = "field_templates")
data class FieldTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fieldName: String,   // "ID площадки", "Адрес", "Улица" и т.д.
    val usageCount: Int = 1  // Как часто использовалось — для сортировки
)