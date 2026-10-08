# Базовые правила ProGuard для WorkJournal
# Room — сохраняем Entity классы
-keep class com.workjournal.data.db.** { *; }
-keep class com.workjournal.domain.model.** { *; }
