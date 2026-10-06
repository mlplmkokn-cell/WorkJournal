package com.workjournal.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkJournalDao {

    // ========== КАТЕГОРИИ ==========

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
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
        SELECT * FROM work_records
        WHERE caption     LIKE '%' || :q || '%'
           OR workType    LIKE '%' || :q || '%'
           OR address     LIKE '%' || :q || '%'
           OR description LIKE '%' || :q || '%'
           OR categoryName LIKE '%' || :q || '%'
        ORDER BY workDate DESC
    """)
    suspend fun searchRecords(q: String): List<WorkRecordEntity>

    /**
     * Основной метод для AI-поиска:
     * AI переводит вопрос в параметры → мы делаем SQL-запрос.
     * Все параметры опциональны (null = не фильтровать).
     */
    @Query("""
        SELECT * FROM work_records
        WHERE (:q   = '' OR caption LIKE '%' || :q || '%'
                         OR workType LIKE '%' || :q || '%'
                         OR address  LIKE '%' || :q || '%'
                         OR description LIKE '%' || :q || '%'
                         OR categoryName LIKE '%' || :q || '%')
          AND (:from IS NULL OR workDate >= :from)
          AND (:to   IS NULL OR workDate <= :to)
          AND (:cat  IS NULL OR categoryName LIKE '%' || :cat || '%')
        ORDER BY workDate DESC
    """)
    suspend fun searchWithFilters(
        q: String,
        from: Long?,
        to: Long?,
        cat: String?
    ): List<WorkRecordEntity>

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

    @Query("DELETE FROM photos WHERE recordId = :recordId")
    suspend fun deletePhotosForRecord(recordId: Long)
}
