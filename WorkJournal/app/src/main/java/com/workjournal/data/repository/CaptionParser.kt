package com.workjournal.data.repository

object CaptionParser {

    private val WORK_VERBS = mapOf(
        "замен" to "Замена",    "поменял" to "Замена",
        "ремонт" to "Ремонт",  "починил" to "Починка",
        "покрас" to "Покраска", "установ" to "Установка",
        "демонтаж" to "Демонтаж", "монтаж" to "Монтаж",
        "чист" to "Чистка",    "уборк" to "Уборка",
        "спилил" to "Спил",    "покосил" to "Покос"
    )

    private val WORK_OBJECTS = mapOf(
        "качел" to "качели",   "лавочк" to "лавочка",
        "скамейк" to "скамейка", "урн" to "урна",
        "мусор" to "мусор",    "фонар" to "фонарь",
        "освещени" to "освещение", "дорожк" to "дорожка",
        "асфальт" to "асфальт", "ограждени" to "ограждение",
        "забор" to "забор",    "горк" to "горка",
        "песочниц" to "песочница", "дерев" to "дерево",
        "куст" to "кустарник", "газон" to "газон",
        "бак" to "бак"
    )

    // Ключевое слово для подбора категории из БД
    private val CATEGORY_KEYWORDS = mapOf(
        "качел" to "качели",
        "лавочк" to "лавочки", "скамейк" to "лавочки",
        "мусор" to "мусор",    "урн" to "мусор", "бак" to "мусор",
        "фонар" to "освещение", "освещени" to "освещение",
        "дорожк" to "дорожки", "асфальт" to "дорожки",
        "ограждени" to "ограждения", "забор" to "ограждения",
        "горк" to "детская",   "карусел" to "детская", "песочниц" to "детская",
        "дерев" to "озеленение", "куст" to "озеленение", "газон" to "озеленение"
    )

    data class ParseResult(
        val workType: String?,
        val address: String?,
        val description: String?,
        val suggestedCategoryKeyword: String   // Ищем по нему в списке категорий из БД
    )

    fun parse(caption: String): ParseResult {
        val lower = caption.lowercase()

        val verb   = WORK_VERBS.entries.firstOrNull   { (k, _) -> lower.contains(k) }?.value
        val obj    = WORK_OBJECTS.entries.firstOrNull { (k, _) -> lower.contains(k) }?.value
        val catKey = CATEGORY_KEYWORDS.entries.firstOrNull { (k, _) -> lower.contains(k) }?.value ?: ""

        val workType = when {
            verb != null && obj != null -> "$verb $obj"
            verb != null -> verb
            obj  != null -> "Работа с $obj"
            else -> null
        }

        val address = extractAddress(caption)
        val description = extractDescription(caption, address)

        return ParseResult(workType, address, description?.takeIf { it.length > 5 }, catKey)
    }

    private fun extractAddress(caption: String): String? {
        val pattern = Regex("""(?:ул\.?\s+|пр\.?\s+|пр-т\.?\s+|пер\.?\s+)?([А-ЯЁа-яё]+(?:\s+[А-ЯЁа-яё]+)?)\s+(\d+[а-яА-ЯёЁ]?(?:\s*к\.?\s*\d+)?)""")
        return pattern.find(caption)?.value?.trim()
    }

    private fun extractDescription(caption: String, address: String?): String? {
        var t = if (address != null) caption.replace(address, "") else caption
        t = t.trimStart(',', '-', '—', ' ', '\n')
        return t.takeIf { it.isNotBlank() && it != caption }
    }
}
