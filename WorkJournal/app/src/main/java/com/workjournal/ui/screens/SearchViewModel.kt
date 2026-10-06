package com.workjournal.ui.screens

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
class SearchViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val results: List<WorkRecord> = emptyList(),
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
        val resultMessage: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isSearching = true) }

        viewModelScope.launch {
            val results = repository.searchRecords(query)
            val message = when (results.size) {
                0 -> "Ничего не найдено по запросу «$query»"
                1 -> "Найдена 1 запись"
                in 2..4 -> "Найдено ${results.size} записи"
                else -> "Найдено ${results.size} записей"
            }
            _uiState.update {
                it.copy(
                    results = results,
                    isSearching = false,
                    hasSearched = true,
                    resultMessage = message
                )
            }
        }
    }
}
