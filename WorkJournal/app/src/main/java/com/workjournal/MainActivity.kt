package com.workjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workjournal.ui.WorkJournalNavGraph
import com.workjournal.ui.theme.WorkJournalTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity в приложении.
 *
 * @AndroidEntryPoint — необходимо для работы Hilt в Activity.
 * Всё остальное управляется через Jetpack Compose и Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Контент рисуется под статус-бар

        setContent {
            WorkJournalTheme {
                WorkJournalNavGraph()
            }
        }
    }
}
