package com.workjournal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel главного экрана.
 *
 * ViewModel — хранит состояние UI и переживает повороты экрана.
 * @HiltViewModel — Hilt автоматически создаёт и внедряет зависимости.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WorkRepository
) : ViewModel() {

    /**
     * Состояние UI — всё что нужно главному экрану.
     */
    data class UiState(
        val records: List<WorkRecord> = emptyList(),
        val totalCount: Int = 0,
        val monthCount: Int = 0,
        val isLoading: Boolean = true
    )

    // Начало текущего месяца для статистики
    private val monthStart: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Объединяем все потоки данных в один UiState
    val uiState: StateFlow<UiState> = combine(
        repository.getAllRecords(),
        repository.getTotalCount(),
        repository.getMonthCount(monthStart)
    ) { records, total, month ->
        UiState(
            records = records,
            totalCount = total,
            monthCount = month,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState(isLoading = true)
    )
}
