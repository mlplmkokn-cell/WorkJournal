package com.workjournal.data.repository

import com.workjournal.data.db.*
import com.workjournal.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepository @Inject constructor(private val dao: WorkJournalDao) {

    // ========== КАТЕГОРИИ ==========

    fun getAllCategories(): Flow<List<WorkCategory>> =
        dao.getAllCategories().map { it.map { e -> e.toDomain() } }

    suspend fun initDefaultCategories() { /* Встроенных нет */ }

    suspend fun addCategory(name: String, emoji: String = "") {
        dao.insertCategory(CategoryEntity(name = name, emoji = emoji))
    }

    suspend fun deleteCategory(category: WorkCategory) {
        if (!category.isDefault)
            dao.deleteCategory(CategoryEntity(id = category.id, name = category.name, emoji = category.emoji))
    }

    // ========== ЗАПИСИ ==========

    fun getAllRecords(): Flow<List<WorkRecord>> =
        dao.getAllRecords().map { entities ->
            entities.map { e ->
                e.toDomain(dao.getPhotosForRecord(e.id), dao.getCustomFields(e.id))
            }
        }

    suspend fun getRecordById(id: Long): WorkRecord? {
        val e = dao.getRecordById(id) ?: return null
        return e.toDomain(dao.getPhotosForRecord(e.id), dao.getCustomFields(e.id))
    }

    suspend fun searchRecords(query: String): List<WorkRecord> =
        dao.searchAll(query).map { e ->
            e.toDomain(dao.getPhotosForRecord(e.id), dao.getCustomFields(e.id))
        }

    suspend fun searchWithFilters(
        query: String = "", from: Long? = null, to: Long? = null, category: String? = null
    ): List<WorkRecord> =
        dao.searchWithFilters(query, from, to, category).map { e ->
            e.toDomain(dao.getPhotosForRecord(e.id), dao.getCustomFields(e.id))
        }

    suspend fun searchByCustomField(fieldName: String, value: String): List<WorkRecord> =
        dao.searchByCustomField(fieldName, value).map { e ->
            e.toDomain(dao.getPhotosForRecord(e.id), dao.getCustomFields(e.id))
        }

    suspend fun insertRecord(record: WorkRecord): Long {
        val id = dao.insertRecord(record.toEntity())
        record.photos.forEach { dao.insertPhoto(it.copy(recordId = id).toEntity()) }
        record.customFields.forEach { dao.insertCustomField(it.copy(recordId = id).toEntity()) }
        // Запоминаем названия полей как шаблоны
        record.customFields.forEach { field ->
            val existing = dao.getFieldTemplate(field.fieldName)
            if (existing == null) {
                dao.insertFieldTemplate(FieldTemplateEntity(fieldName = field.fieldName))
            } else {
                dao.incrementFieldUsage(field.fieldName)
            }
        }
        return id
    }

    suspend fun updateRecord(record: WorkRecord) {
        dao.updateRecord(record.toEntity())
        dao.deleteCustomFields(record.id)
        record.customFields.forEach { dao.insertCustomField(it.copy(recordId = record.id).toEntity()) }
    }

    suspend fun deleteRecord(record: WorkRecord) = dao.deleteRecord(record.toEntity())

    suspend fun addPhoto(recordId: Long, filePath: String, thumbnailPath: String? = null, takenAt: Long? = null) {
        dao.insertPhoto(PhotoEntity(recordId = recordId, filePath = filePath, thumbnailPath = thumbnailPath, takenAt = takenAt))
    }

    // ========== ШАБЛОНЫ ПОЛЕЙ ==========

    fun getAllFieldTemplates(): Flow<List<FieldTemplate>> =
        dao.getAllFieldTemplates().map { it.map { e -> FieldTemplate(e.id, e.fieldName, e.usageCount) } }

    // ========== СТАТИСТИКА ==========

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    fun getMonthCount(monthStart: Long): Flow<Int> = dao.getMonthCount(monthStart)
}