package com.workjournal.domain.model

/**
 * Главная модель данных — одна запись о работе.
 */
data class WorkRecord(
    val id: Long = 0,
    val caption: String,
    val workDate: Long,
    val createdAt: Long,
    val workType: String? = null,
    val address: String? = null,
    val description: String? = null,
    // Категория теперь просто строка — можно добавлять свои
    val categoryName: String = "Прочее",
    val categoryEmoji: String = "🔧",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photos: List<PhotoRecord> = emptyList()
)

/**
 * Категория работы — теперь хранится в базе, можно добавлять свои.
 */
data class WorkCategory(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val isDefault: Boolean = false  // Системные категории нельзя удалить
)

/**
 * Встроенные категории по умолчанию.
 */
val DEFAULT_CATEGORIES = listOf(
    WorkCategory(name = "Качели",              emoji = "🛝",  isDefault = true),
    WorkCategory(name = "Лавочки",             emoji = "🪑",  isDefault = true),
    WorkCategory(name = "Мусор / Урны",        emoji = "🗑️",  isDefault = true),
    WorkCategory(name = "Освещение",           emoji = "💡",  isDefault = true),
    WorkCategory(name = "Дорожки / Асфальт",  emoji = "🛣️",  isDefault = true),
    WorkCategory(name = "Ограждения",          emoji = "🚧",  isDefault = true),
    WorkCategory(name = "Детская площадка",    emoji = "🎠",  isDefault = true),
    WorkCategory(name = "Озеленение",          emoji = "🌳",  isDefault = true),
    WorkCategory(name = "Прочее",              emoji = "🔧",  isDefault = true)
)

/**
 * Одна фотография, прикреплённая к записи.
 */
data class PhotoRecord(
    val id: Long = 0,
    val recordId: Long,
    val filePath: String,
    val thumbnailPath: String?,
    val takenAt: Long?
)
