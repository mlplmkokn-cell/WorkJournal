package com.workjournal.domain.model

data class WorkRecord(
    val id: Long = 0,
    val caption: String,
    val workDate: Long,
    val createdAt: Long,
    val workType: String? = null,
    val address: String? = null,
    val description: String? = null,
    val categoryName: String = "",
    val categoryEmoji: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photos: List<PhotoRecord> = emptyList(),
    // Кастомные поля — пользователь добавляет сам
    val customFields: List<CustomField> = emptyList()
)

/** Одно кастомное поле записи */
data class CustomField(
    val id: Long = 0,
    val recordId: Long = 0,
    val fieldName: String,   // "ID площадки"
    val fieldValue: String   // "42А"
)

/** Категория */
data class WorkCategory(
    val id: Long = 0,
    val name: String,
    val emoji: String = "",
    val isDefault: Boolean = false
)

/** Шаблон поля — подсказка что раньше использовалось */
data class FieldTemplate(
    val id: Long = 0,
    val fieldName: String,
    val usageCount: Int = 1
)

val DEFAULT_CATEGORIES = emptyList<WorkCategory>()

data class PhotoRecord(
    val id: Long = 0,
    val recordId: Long,
    val filePath: String,
    val thumbnailPath: String?,
    val takenAt: Long?
)