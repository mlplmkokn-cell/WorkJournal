package com.workjournal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workjournal.data.ai.GeminiAssistant
import com.workjournal.data.repository.SettingsRepository
import com.workjournal.data.repository.WorkRepository
import com.workjournal.domain.model.WorkRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: WorkRepository,
    private val assistant: GeminiAssistant,
    private val settings: SettingsRepository
) : ViewModel() {

    data class Message(
        val text: String,
        val isUser: Boolean,
        val matchedRecords: List<WorkRecord> = emptyList(),
        val isError: Boolean = false
    )

    data class UiState(
        val messages: List<Message> = emptyList(),
        val inputText: String = "",
        val isThinking: Boolean = false,
        val apiKeyConfigured: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    // Кешируем все записи — они нужны при каждом запросе
    private var cachedRecords: List<WorkRecord> = emptyList()

    init {
        _uiState.update { it.copy(apiKeyConfigured = settings.geminiApiKey.isNotBlank()) }

        // Подписываемся на изменения записей
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                cachedRecords = records
            }
        }

        // Приветственное сообщение
        addBotMessage(
            "Привет! Я ассистент журнала работ. Могу ответить на вопросы о записях:\n\n" +
            "• «Сколько качелей починили в 2026 году?»\n" +
            "• «Все работы на Тевосяна 22а»\n" +
            "• «Что делали в октябре?»\n" +
            "• «Сколько всего записей за этот месяц?»"
        )
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val question = _uiState.value.inputText.trim()
        if (question.isBlank() || _uiState.value.isThinking) return

        // Добавляем сообщение пользователя
        val userMsg = Message(text = question, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            val apiKey = settings.geminiApiKey

            val result = assistant.ask(
                question = question,
                allRecords = cachedRecords,
                apiKey = apiKey
            )

            // Находим записи по ID которые вернул Gemini
            val matchedRecords = result.matchedIds.mapNotNull { id ->
                cachedRecords.find { it.id == id }
            }

            val botMsg = Message(
                text = result.answer,
                isUser = false,
                matchedRecords = matchedRecords,
                isError = result.isError
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + botMsg,
                    isThinking = false
                )
            }
        }
    }

    fun updateApiKey(key: String) {
        settings.geminiApiKey = key.trim()
        _uiState.update { it.copy(apiKeyConfigured = key.isNotBlank()) }
    }

    private fun addBotMessage(text: String) {
        _uiState.update {
            it.copy(messages = it.messages + Message(text = text, isUser = false))
        }
    }
}
