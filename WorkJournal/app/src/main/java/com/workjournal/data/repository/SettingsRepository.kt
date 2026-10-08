package com.workjournal.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранит настройки приложения в SharedPreferences.
 * SharedPreferences — простое хранилище ключ-значение для настроек.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("work_journal_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit { putString("gemini_api_key", value) }

    var assistantEnabled: Boolean
        get() = prefs.getBoolean("assistant_enabled", false)
        set(value) = prefs.edit { putBoolean("assistant_enabled", value) }
}
