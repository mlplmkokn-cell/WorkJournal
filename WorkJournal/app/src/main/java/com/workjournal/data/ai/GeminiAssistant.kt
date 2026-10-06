package com.workjournal.data.ai

import android.util.Log
import com.workjournal.domain.model.WorkRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ассистент на базе Google Gemini 1.5 Flash.
 *
 * Бесплатный лимит: 15 запросов/минуту, 1500/день — хватит с головой.
 * API ключ берём из BuildConfig (задаётся в local.properties).
 *
 * Как это работает:
 * 1. Берём все записи из локальной БД
 * 2. Упаковываем их в текст и отправляем вместе с вопросом в Gemini
 * 3. Gemini анализирует и отвечает на русском
 * 4. Дополнительно просим Gemini вернуть ID записей для показа карточек
 *
 * Gemini НЕ хранит данные — каждый запрос полностью самодостаточен.
 */
@Singleton
class GeminiAssistant @Inject constructor() {

    companion object {
        private const val TAG = "GeminiAssistant"
        // Бесплатная модель Gemini 1.5 Flash
        private const val MODEL = "gemini-1.5-flash-latest"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    }

    data class AssistantResult(
        val answer: String,           // Текстовый ответ для пользователя
        val matchedIds: List<Long>,   // ID записей для показа карточек
        val isError: Boolean = false
    )

    /**
     * Задать вопрос ассистенту.
     *
     * @param question — вопрос пользователя на русском
     * @param allRecords — все записи из БД (передаём контекст)
     * @param apiKey — ключ Gemini API
     */
    suspend fun ask(
        question: String,
        allRecords: List<WorkRecord>,
        apiKey: String
    ): AssistantResult = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext AssistantResult(
                answer = "⚠️ Ключ API не настроен. Перейди в Настройки → AI Ассистент и введи ключ Gemini.",
                matchedIds = emptyList(),
                isError = true
            )
        }

        if (allRecords.isEmpty()) {
            return@withContext AssistantResult(
                answer = "В журнале пока нет записей. Добавь первую работу!",
                matchedIds = emptyList()
            )
        }

        try {
            // Формируем краткий текст всех записей для передачи в Gemini
            val recordsText = buildRecordsContext(allRecords)

            val prompt = buildPrompt(question, recordsText)

            val response = callGeminiApi(prompt, apiKey)

            parseResponse(response, allRecords)

        } catch (e: Exception) {
            Log.e(TAG, "Gemini error", e)
            AssistantResult(
                answer = "Ошибка при обращении к ассистенту: ${e.message}\n\nПроверь подключение к интернету и API ключ.",
                matchedIds = emptyList(),
                isError = true
            )
        }
    }

    /**
     * Формируем краткое описание всех записей.
     * Не отправляем фото (это текстовый API) — только метаданные.
     */
    private fun buildRecordsContext(records: List<WorkRecord>): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        records.forEach { r ->
            sb.appendLine("ID:${r.id} | ${sdf.format(Date(r.workDate))} | ${r.categoryName} | ${r.address ?: ""} | ${r.workType ?: ""} | ${r.caption}")
        }
        return sb.toString().take(30_000) // Ограничение на размер контекста
    }

    /**
     * Системный промпт + вопрос пользователя.
     */
    private fun buildPrompt(question: String, recordsContext: String): String = """
Ты — ассистент журнала ремонтных работ. Отвечай ТОЛЬКО на основе данных ниже, не придумывай ничего.

ФОРМАТ КАЖДОЙ ЗАПИСИ:
ID | Дата и время | Категория | Адрес | Тип работы | Подпись

ДАННЫЕ ЖУРНАЛА:
$recordsContext

ВОПРОС ПОЛЬЗОВАТЕЛЯ: $question

ИНСТРУКЦИЯ:
1. Ответь на вопрос кратко и точно на русском языке.
2. Используй конкретные даты, адреса, числа из данных.
3. В конце ответа на ОТДЕЛЬНОЙ строке напиши: КАРТОЧКИ:[id1,id2,id3]
   — перечисли ID записей которые относятся к ответу (максимум 10).
   — Если нет подходящих записей, напиши КАРТОЧКИ:[]
4. Не добавляй лишних объяснений после строки КАРТОЧКИ.

Пример правильного ответа:
В 2026 году было проведено 5 ремонтов качелей: 21.08 на Тевосяна 22А, 14.09 там же, ...
КАРТОЧКИ:[42,67,89,112,134]
""".trimIndent()

    /**
     * HTTP-запрос к Gemini API.
     */
    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val url = URL("$BASE_URL?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000

        // Формируем JSON-тело запроса
        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)       // Низкая температура = меньше фантазии
                put("maxOutputTokens", 1024)
            })
        }.toString()

        conn.outputStream.bufferedWriter().use { it.write(body) }

        val responseCode = conn.responseCode
        return if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("HTTP $responseCode: $error")
        }
    }

    /**
     * Разбираем ответ Gemini — извлекаем текст и ID карточек.
     */
    private fun parseResponse(jsonResponse: String, allRecords: List<WorkRecord>): AssistantResult {
        val json = JSONObject(jsonResponse)
        val text = json
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        // Ищем строку "КАРТОЧКИ:[id1,id2,...]"
        val cardLineRegex = Regex("КАРТОЧКИ:\\[([0-9,\\s]*)\\]", RegexOption.IGNORE_CASE)
        val cardMatch = cardLineRegex.find(text)

        val ids: List<Long> = if (cardMatch != null) {
            cardMatch.groupValues[1]
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { id -> allRecords.any { it.id == id } } // Только реальные ID
        } else emptyList()

        // Убираем строку "КАРТОЧКИ:..." из текста ответа
        val cleanAnswer = text
            .replace(cardLineRegex, "")
            .trimEnd()

        return AssistantResult(answer = cleanAnswer, matchedIds = ids)
    }
}
