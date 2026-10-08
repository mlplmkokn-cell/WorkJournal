package com.workjournal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    data class UiState(
        val categories: List<WorkCategory> = emptyList(),
        val newName: String = "",
        val newEmoji: String = "",
        val showAddDialog: Boolean = false,
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

    fun showAddDialog()  { _uiState.update { it.copy(showAddDialog = true, newName = "", newEmoji = "", error = null) } }
    fun hideAddDialog()  { _uiState.update { it.copy(showAddDialog = false) } }
    fun onNameChanged(v: String)  { _uiState.update { it.copy(newName = v) } }
    fun onEmojiChanged(v: String) { _uiState.update { it.copy(newEmoji = v.take(2)) } }

    fun addCategory() {
        val name  = _uiState.value.newName.trim()
        val emoji = _uiState.value.newEmoji.trim().ifBlank { "🔧" }
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Введите название") }
            return
        }
        viewModelScope.launch {
            repository.addCategory(name, emoji)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteCategory(cat: WorkCategory) {
        if (cat.isDefault) return
        viewModelScope.launch { repository.deleteCategory(cat) }
    }
}
