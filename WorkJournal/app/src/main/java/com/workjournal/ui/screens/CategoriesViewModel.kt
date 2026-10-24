package com.workjournal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkCategory
import com.workjournal.domain.model.WorkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    // Что делать с работами при удалении категории
    sealed class DeleteAction {
        object None : DeleteAction()
        // Показываем диалог — в категории есть работы
        data class WithRecords(
            val category: WorkCategory,
            val records: List<WorkRecord>,
            val otherCategories: List<WorkCategory>
        ) : DeleteAction()
    }

    data class UiState(
        val categories: List<WorkCategory> = emptyList(),
        val newName: String = "",
        val showAddDialog: Boolean = false,
        val deleteAction: DeleteAction = DeleteAction.None,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, newName = "", error = null) }
    fun hideAddDialog() = _uiState.update { it.copy(showAddDialog = false) }
    fun onNameChanged(v: String) = _uiState.update { it.copy(newName = v) }

    fun addCategory() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = "Введите название") }; return }
        viewModelScope.launch {
            repository.addCategory(name)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    /**
     * Нажали удалить категорию.
     * Если в ней нет работ — удаляем сразу.
     * Если есть — показываем диалог с выбором.
     */
    fun requestDelete(cat: WorkCategory) {
        if (cat.isDefault) return
        viewModelScope.launch {
            // Ищем работы в этой категории
            val records = repository.searchWithFilters(category = cat.name)
            if (records.isEmpty()) {
                // Нет работ — удаляем сразу
                repository.deleteCategory(cat)
            } else {
                // Есть работы — показываем диалог
                val others = _uiState.value.categories.filter { it.id != cat.id }
                _uiState.update {
                    it.copy(deleteAction = DeleteAction.WithRecords(cat, records, others))
                }
            }
        }
    }

    /** Пользователь выбрал: удалить категорию ВМЕСТЕ с работами */
    fun deleteCategoryWithRecords() {
        val action = _uiState.value.deleteAction as? DeleteAction.WithRecords ?: return
        viewModelScope.launch {
            // Удаляем все работы этой категории
            action.records.forEach { repository.deleteRecord(it) }
            // Удаляем категорию
            repository.deleteCategory(action.category)
            _uiState.update { it.copy(deleteAction = DeleteAction.None) }
        }
    }

    /** Пользователь выбрал: перенести работы в другую категорию */
    fun moveRecordsToCategory(targetCategory: WorkCategory) {
        val action = _uiState.value.deleteAction as? DeleteAction.WithRecords ?: return
        viewModelScope.launch {
            // Переносим все работы
            action.records.forEach { record ->
                repository.updateRecord(
                    record.copy(
                        categoryName  = targetCategory.name,
                        categoryEmoji = targetCategory.emoji
                    )
                )
            }
            // Удаляем старую категорию
            repository.deleteCategory(action.category)
            _uiState.update { it.copy(deleteAction = DeleteAction.None) }
        }
    }

    fun dismissDeleteDialog() = _uiState.update { it.copy(deleteAction = DeleteAction.None) }
}