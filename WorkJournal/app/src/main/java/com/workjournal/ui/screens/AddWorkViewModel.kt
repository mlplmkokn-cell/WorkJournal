package com.workjournal.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.CaptionParser
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddWorkViewModel @Inject constructor(
    private val repository: WorkRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val caption: String = "",
        val selectedDate: Long = System.currentTimeMillis(),
        val selectedCategory: WorkCategory? = null,
        val availableCategories: List<WorkCategory> = emptyList(),
        val photoPaths: List<String> = emptyList(),
        val parsedWorkType: String? = null,
        val parsedAddress: String? = null,
        val customFields: List<CustomField> = emptyList(),
        val fieldTemplates: List<FieldTemplate> = emptyList(),
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val error: String? = null,
        // Диалог подтверждения при неполных данных
        val showConfirmDialog: Boolean = false,
        val missingFields: List<String> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                _uiState.update { s ->
                    s.copy(availableCategories = cats,
                        selectedCategory = s.selectedCategory ?: cats.firstOrNull())
                }
            }
        }
        viewModelScope.launch {
            repository.getAllFieldTemplates().collect { templates ->
                _uiState.update { it.copy(fieldTemplates = templates) }
            }
        }
    }

    fun onCaptionChanged(caption: String) {
        _uiState.update { it.copy(caption = caption) }
        if (caption.length > 5) {
            val parsed = CaptionParser.parse(caption)
            _uiState.update { it.copy(parsedWorkType = parsed.workType, parsedAddress = parsed.address) }
        }
    }

    fun onCategorySelected(cat: WorkCategory) = _uiState.update { it.copy(selectedCategory = cat) }
    fun onDateChanged(date: Long) = _uiState.update { it.copy(selectedDate = date) }

    fun addNewCategory(name: String) {
        viewModelScope.launch { repository.addCategory(name.trim()) }
    }

    // ========== КАСТОМНЫЕ ПОЛЯ ==========

    fun addCustomField(name: String) {
        if (name.isBlank()) return
        val newField = CustomField(recordId = 0, fieldName = name.trim(), fieldValue = "")
        _uiState.update { it.copy(customFields = it.customFields + newField) }
    }

    fun updateCustomField(index: Int, value: String) {
        val fields = _uiState.value.customFields.toMutableList()
        if (index < fields.size) {
            fields[index] = fields[index].copy(fieldValue = value)
            _uiState.update { it.copy(customFields = fields) }
        }
    }

    fun removeCustomField(index: Int) {
        val fields = _uiState.value.customFields.toMutableList()
        if (index < fields.size) {
            fields.removeAt(index)
            _uiState.update { it.copy(customFields = fields) }
        }
    }

    // ========== СОХРАНЕНИЕ ==========

    fun trySave() {
        val state = _uiState.value
        if (state.caption.isBlank()) {
            _uiState.update { it.copy(error = "Введите подпись") }
            return
        }
        // Проверяем что не заполнено
        val missing = mutableListOf<String>()
        if (state.photoPaths.isEmpty()) missing.add("Фотографии")
        if (state.selectedCategory == null) missing.add("Категория")
        val emptyFields = state.customFields.filter { it.fieldValue.isBlank() }
        emptyFields.forEach { missing.add("${it.fieldName} (пусто)") }

        if (missing.isNotEmpty()) {
            _uiState.update { it.copy(showConfirmDialog = true, missingFields = missing) }
        } else {
            saveRecord()
        }
    }

    fun confirmSave() {
        _uiState.update { it.copy(showConfirmDialog = false) }
        saveRecord()
    }

    fun cancelSave() = _uiState.update { it.copy(showConfirmDialog = false) }

    private fun saveRecord() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val cat = state.selectedCategory
                val record = WorkRecord(
                    caption       = state.caption,
                    workDate      = state.selectedDate,
                    createdAt     = now,
                    workType      = state.parsedWorkType,
                    categoryName  = cat?.name ?: "",
                    categoryEmoji = cat?.emoji ?: "",
                    customFields  = state.customFields.filter { it.fieldValue.isNotBlank() },
                    photos        = state.photoPaths.map { path ->
                        PhotoRecord(recordId = 0, filePath = path, thumbnailPath = null, takenAt = now)
                    }
                )
                repository.insertRecord(record)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Ошибка: ${e.message}") }
            }
        }
    }

    // ========== ФОТО ==========

    fun addPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val path = savePhotoFromUri(uri)
                if (path != null) _uiState.update { it.copy(photoPaths = it.photoPaths + path) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка фото: ${e.message}") }
            }
        }
    }

    fun removePhoto(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths.filter { p -> p != path }) }
        File(path).delete()
    }

    fun createTempPhotoFile(): File? = try {
        val dir = File(context.filesDir, "Pictures").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        File(dir, "PHOTO_$ts.jpg")
    } catch (e: Exception) { null }

    private fun savePhotoFromUri(uri: Uri): String? {
        val dir = File(context.filesDir, "Pictures").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dest = File(dir, "PHOTO_$ts.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            context.contentResolver.openInputStream(uri)?.use { input2 ->
                val scale = calcSampleSize(opts.outWidth, opts.outHeight, 1920, 1920)
                val bmp = BitmapFactory.decodeStream(input2, null,
                    BitmapFactory.Options().apply { inSampleSize = scale })
                bmp?.let {
                    FileOutputStream(dest).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                    it.recycle()
                }
            }
        }
        return if (dest.exists()) dest.absolutePath else null
    }

    private fun calcSampleSize(w: Int, h: Int, maxW: Int, maxH: Int): Int {
        var s = 1
        if (h > maxH || w > maxW) while ((h / 2 / s) >= maxH && (w / 2 / s) >= maxW) s *= 2
        return s
    }
}