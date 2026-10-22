package com.workjournal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Режимы поиска
sealed class SearchMode {
    object ALL : SearchMode()
    object CAPTION : SearchMode()
    object CATEGORY : SearchMode()
    object DATE : SearchMode()
    data class CUSTOM_FIELD(val fieldName: String) : SearchMode()

    override fun equals(other: Any?): Boolean {
        if (this is CUSTOM_FIELD && other is CUSTOM_FIELD) return fieldName == other.fieldName
        return this::class == other?.let { it::class }
    }
    override fun hashCode(): Int = when (this) {
        is CUSTOM_FIELD -> fieldName.hashCode()
        else -> this::class.hashCode()
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val searchMode: SearchMode = SearchMode.ALL,
        val customFieldNames: List<String> = emptyList(),
        val results: List<WorkRecord> = emptyList(),
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
        val resultMessage: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Загружаем названия кастомных полей для показа в чипах
        viewModelScope.launch {
            repository.getAllFieldTemplates().collect { templates ->
                _uiState.update { it.copy(customFieldNames = templates.map { t -> t.fieldName }) }
            }
        }
    }

    fun onQueryChanged(query: String) = _uiState.update { it.copy(query = query) }
    fun onModeChanged(mode: SearchMode) = _uiState.update { it.copy(searchMode = mode, hasSearched = false) }

    fun search() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isSearching = true) }

        viewModelScope.launch {
            val results = when (state.searchMode) {
                SearchMode.ALL -> repository.searchRecords(query)
                SearchMode.CAPTION -> repository.searchWithFilters(query = query)
                SearchMode.CATEGORY -> repository.searchWithFilters(category = query)
                SearchMode.DATE -> repository.searchRecords(query)
                is SearchMode.CUSTOM_FIELD -> repository.searchByCustomField(state.searchMode.fieldName, query)
            }

            val msg = when (results.size) {
                0 -> "Ничего не найдено по запросу «$query»"
                1 -> "Найдена 1 запись"
                in 2..4 -> "Найдено ${results.size} записи"
                else -> "Найдено ${results.size} записей"
            }

            _uiState.update {
                it.copy(results = results, isSearching = false, hasSearched = true, resultMessage = msg)
            }
        }
    }
}