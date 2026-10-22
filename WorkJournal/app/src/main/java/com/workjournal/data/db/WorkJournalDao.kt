package com.workjournal.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkJournalDao {

    // ========== КАТЕГОРИИ ==========

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // ========== ЗАПИСИ ==========

    @Query("SELECT * FROM work_records ORDER BY workDate DESC")
    fun getAllRecords(): Flow<List<WorkRecordEntity>>

    @Query("SELECT * FROM work_records WHERE id = :id")
    suspend fun getRecordById(id: Long): WorkRecordEntity?

    @Query("""
        SELECT DISTINCT wr.* FROM work_records wr
        LEFT JOIN custom_fields cf ON cf.recordId = wr.id
        WHERE wr.caption     LIKE '%' || :q || '%'
           OR wr.workType    LIKE '%' || :q || '%'
           OR wr.address     LIKE '%' || :q || '%'
           OR wr.description LIKE '%' || :q || '%'
           OR wr.categoryName LIKE '%' || :q || '%'
           OR cf.fieldValue  LIKE '%' || :q || '%'
           OR cf.fieldName   LIKE '%' || :q || '%'
        ORDER BY wr.workDate DESC
    """)
    suspend fun searchAll(q: String): List<WorkRecordEntity>

    @Query("""
        SELECT DISTINCT wr.* FROM work_records wr
        LEFT JOIN custom_fields cf ON cf.recordId = wr.id
        WHERE (:q = '' OR wr.caption LIKE '%' || :q || '%'
                       OR wr.workType LIKE '%' || :q || '%'
                       OR wr.address LIKE '%' || :q || '%'
                       OR wr.description LIKE '%' || :q || '%'
                       OR wr.categoryName LIKE '%' || :q || '%'
                       OR cf.fieldValue LIKE '%' || :q || '%')
          AND (:from IS NULL OR wr.workDate >= :from)
          AND (:to   IS NULL OR wr.workDate <= :to)
          AND (:cat  IS NULL OR wr.categoryName LIKE '%' || :cat || '%')
        ORDER BY wr.workDate DESC
    """)
    suspend fun searchWithFilters(q: String, from: Long?, to: Long?, cat: String?): List<WorkRecordEntity>

    /** Поиск по конкретному полю */
    @Query("""
        SELECT DISTINCT wr.* FROM work_records wr
        JOIN custom_fields cf ON cf.recordId = wr.id
        WHERE cf.fieldName = :fieldName
          AND cf.fieldValue LIKE '%' || :value || '%'
        ORDER BY wr.workDate DESC
    """)
    suspend fun searchByCustomField(fieldName: String, value: String): List<WorkRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WorkRecordEntity): Long

    @Update
    suspend fun updateRecord(record: WorkRecordEntity)

    @Delete
    suspend fun deleteRecord(record: WorkRecordEntity)

    @Query("SELECT COUNT(*) FROM work_records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM work_records WHERE workDate >= :monthStart")
    fun getMonthCount(monthStart: Long): Flow<Int>

    // ========== ФОТО ==========

    @Query("SELECT * FROM photos WHERE recordId = :recordId ORDER BY id ASC")
    suspend fun getPhotosForRecord(recordId: Long): List<PhotoEntity>

    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    // ========== КАСТОМНЫЕ ПОЛЯ ==========

    @Query("SELECT * FROM custom_fields WHERE recordId = :recordId ORDER BY id ASC")
    suspend fun getCustomFields(recordId: Long): List<CustomFieldEntity>

    @Insert
    suspend fun insertCustomField(field: CustomFieldEntity): Long

    @Query("DELETE FROM custom_fields WHERE recordId = :recordId")
    suspend fun deleteCustomFields(recordId: Long)

    // ========== ШАБЛОНЫ ПОЛЕЙ ==========

    @Query("SELECT * FROM field_templates ORDER BY usageCount DESC")
    fun getAllFieldTemplates(): Flow<List<FieldTemplateEntity>>

    @Query("SELECT * FROM field_templates WHERE fieldName = :name LIMIT 1")
    suspend fun getFieldTemplate(name: String): FieldTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldTemplate(template: FieldTemplateEntity)

    @Query("UPDATE field_templates SET usageCount = usageCount + 1 WHERE fieldName = :name")
    suspend fun incrementFieldUsage(name: String)
}