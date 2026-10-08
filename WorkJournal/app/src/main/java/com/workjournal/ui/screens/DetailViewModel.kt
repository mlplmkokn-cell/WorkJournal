package com.workjournal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: WorkRepository,
    savedStateHandle: SavedStateHandle  // Автоматически получает аргументы навигации
) : ViewModel() {

    data class UiState(
        val record: WorkRecord? = null,
        val isLoading: Boolean = true,
        val currentPhotoIndex: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Получаем ID из аргументов навигации
        val recordId = savedStateHandle.get<Long>("id") ?: 0L
        loadRecord(recordId)
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            val record = repository.getRecordById(id)
            _uiState.update {
                it.copy(record = record, isLoading = false)
            }
        }
    }

    fun onPhotoSelected(index: Int) {
        _uiState.update { it.copy(currentPhotoIndex = index) }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            repository.deleteRecord(record)
            onDeleted()
        }
    }
}
