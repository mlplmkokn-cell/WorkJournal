package com.workjournal

import android.app.Application
import com.workjournal.data.repository.WorkRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkJournalApp : Application() {

    // Hilt внедряет репозиторий автоматически
    @Inject lateinit var workRepository: WorkRepository

    override fun onCreate() {
        super.onCreate()
        // Заполняем категории по умолчанию при первом запуске
        CoroutineScope(Dispatchers.IO).launch {
            workRepository.initDefaultCategories()
        }
    }
}
