package com.workjournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Цвета приложения — зелёная тема (ЖКХ / природа)
val Green900 = Color(0xFF1a3a2a)  // Тёмно-зелёный — шапка
val Green700 = Color(0xFF2d7a4f)  // Основной зелёный — кнопки
val Green500 = Color(0xFF4CAF50)  // Светло-зелёный
val Green100 = Color(0xFFe8f5ee)  // Очень светлый зелёный — фон бейджей
val Green800 = Color(0xFF1a6b40)  // Текст на зелёном фоне

val White = Color(0xFFFFFFFF)
val Gray50 = Color(0xFFF5F5F5)
val Gray100 = Color(0xFFEEEEEE)
val Gray400 = Color(0xFF9E9E9E)
val Gray700 = Color(0xFF616161)
val Gray900 = Color(0xFF212121)

/**
 * Цветовая схема приложения.
 * Material3 использует semantic цвета:
 * - primary — основной цвет (кнопки, акценты)
 * - background — фон
 * - surface — фон карточек
 * - onPrimary — цвет текста на primary-элементах
 */
private val AppColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = Green500,
    onSecondary = White,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray400
)

/**
 * Основная тема приложения.
 * Оборачивает всё приложение в MaterialTheme.
 */
@Composable
fun WorkJournalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
