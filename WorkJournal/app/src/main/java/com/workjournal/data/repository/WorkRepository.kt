package com.workjournal.data.repository

import com.workjournal.data.db.*
import com.workjournal.domain.model.DEFAULT_CATEGORIES
import com.workjournal.domain.model.PhotoRecord
import com.workjournal.domain.model.WorkCategory
import com.workjournal.domain.model.WorkRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepository @Inject constructor(
    private val dao: WorkJournalDao
) {
    // ========== КАТЕГОРИИ ==========

    fun getAllCategories(): Flow<List<WorkCategory>> =
        dao.getAllCategories().map { list -> list.map { it.toDomain() } }

    /**
     * При первом запуске заполняем встроенные категории.
     */
    suspend fun initDefaultCategories() {
        if (dao.getCategoryCount() == 0) {
            DEFAULT_CATEGORIES.forEach { cat ->
                dao.insertCategory(
                    CategoryEntity(name = cat.name, emoji = cat.emoji, isDefault = cat.isDefault)
                )
            }
        }
    }

    suspend fun addCategory(name: String, emoji: String) {
        dao.insertCategory(CategoryEntity(name = name, emoji = emoji, isDefault = false))
    }

    suspend fun deleteCategory(category: WorkCategory) {
        if (!category.isDefault) {
            dao.deleteCategory(CategoryEntity(id = category.id, name = category.name, emoji = category.emoji))
        }
    }

    // ========== ЗАПИСИ ==========

    fun getAllRecords(): Flow<List<WorkRecord>> =
        dao.getAllRecords().map { entities ->
            entities.map { e -> e.toDomain(dao.getPhotosForRecord(e.id)) }
        }

    suspend fun getRecordById(id: Long): WorkRecord? {
        val e = dao.getRecordById(id) ?: return null
        return e.toDomain(dao.getPhotosForRecord(e.id))
    }

    suspend fun searchRecords(query: String): List<WorkRecord> =
        dao.searchRecords(query).map { e -> e.toDomain(dao.getPhotosForRecord(e.id)) }

    /**
     * AI передаёт сюда структурированные параметры после разбора вопроса.
     * Мы делаем обычный SQL-запрос — никакого «угадывания».
     */
    suspend fun searchWithFilters(
        query: String = "",
        from: Long? = null,
        to: Long? = null,
        category: String? = null
    ): List<WorkRecord> =
        dao.searchWithFilters(query, from, to, category)
            .map { e -> e.toDomain(dao.getPhotosForRecord(e.id)) }

    suspend fun insertRecord(record: WorkRecord): Long {
        val id = dao.insertRecord(record.toEntity())
        record.photos.forEach { photo ->
            dao.insertPhoto(photo.copy(recordId = id).toEntity())
        }
        return id
    }

    suspend fun updateRecord(record: WorkRecord) = dao.updateRecord(record.toEntity())

    suspend fun deleteRecord(record: WorkRecord) = dao.deleteRecord(record.toEntity())

    suspend fun addPhoto(recordId: Long, filePath: String, thumbnailPath: String? = null, takenAt: Long? = null) {
        dao.insertPhoto(PhotoEntity(recordId = recordId, filePath = filePath, thumbnailPath = thumbnailPath, takenAt = takenAt))
    }

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    fun getMonthCount(monthStart: Long): Flow<Int> = dao.getMonthCount(monthStart)
}
