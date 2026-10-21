package com.workjournal.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.CaptionParser
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.PhotoRecord
import com.workjournal.domain.model.WorkCategory
import com.workjournal.domain.model.WorkRecord
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
        val parsedDescription: String? = null,
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                _uiState.update { s ->
                    s.copy(
                        availableCategories = cats,
                        selectedCategory = s.selectedCategory ?: cats.firstOrNull()
                    )
                }
            }
        }
    }

    fun onCaptionChanged(caption: String) {
        _uiState.update { it.copy(caption = caption) }
        if (caption.length > 5) {
            val parsed = CaptionParser.parse(caption)
            _uiState.update { s ->
                s.copy(
                    parsedWorkType = parsed.workType,
                    parsedAddress = parsed.address,
                    parsedDescription = parsed.description
                )
            }
        }
    }

    fun onCategorySelected(category: WorkCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onDateChanged(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    // Добавить новую категорию прямо с экрана добавления работы
    fun addNewCategory(name: String, emoji: String) {
        viewModelScope.launch {
            repository.addCategory(name, emoji)
            // Автоматически выбираем только что добавленную
            val cats = repository.getAllCategories()
            // selectedCategory обновится через Flow в init
        }
    }

    fun addPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val savedPath = savePhotoFromUri(uri)
                if (savedPath != null) {
                    _uiState.update { it.copy(photoPaths = it.photoPaths + savedPath) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при добавлении фото: ${e.message}") }
            }
        }
    }

    fun removePhoto(path: String) {
        _uiState.update { it.copy(photoPaths = it.photoPaths.filter { p -> p != path }) }
        File(path).delete()
    }

    fun saveRecord() {
        val state = _uiState.value
        if (state.caption.isBlank()) {
            _uiState.update { it.copy(error = "Введите подпись") }
            return
        }
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
                    address       = state.parsedAddress,
                    description   = state.parsedDescription,
                    categoryName  = cat?.name  ?: "",
                    categoryEmoji = cat?.emoji ?: "🔧",
                    photos = state.photoPaths.map { path ->
                        PhotoRecord(recordId = 0, filePath = path, thumbnailPath = null, takenAt = now)
                    }
                )
                repository.insertRecord(record)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Ошибка сохранения: ${e.message}") }
            }
        }
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
                // Сжимаем до 1920px по длинной стороне — хорошее качество, меньше места
                val scale = calcSampleSize(opts.outWidth, opts.outHeight, 1920, 1920)
                val bmp = BitmapFactory.decodeStream(input2, null,
                    BitmapFactory.Options().apply { inSampleSize = scale })
                bmp?.let {
                    FileOutputStream(dest).use { out ->
                        // 85% качества JPEG — визуально почти не отличить от оригинала
                        it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    it.recycle()
                }
            }
        }
        return if (dest.exists()) dest.absolutePath else null
    }

    private fun calcSampleSize(w: Int, h: Int, maxW: Int, maxH: Int): Int {
        var s = 1
        while ((h / 2 / s) >= maxH && (w / 2 / s) >= maxW) s *= 2
        return s
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}