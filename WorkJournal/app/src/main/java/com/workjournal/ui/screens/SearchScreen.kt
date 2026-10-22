package com.workjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workjournal.ui.components.WorkCardCompact
import com.workjournal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onRecordClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green900, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Выбор режима поиска
            item {
                Text("Искать по:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                // Чипы режимов
                val modes = listOf(
                    "Везде" to SearchMode.ALL,
                    "Подпись" to SearchMode.CAPTION,
                    "Категория" to SearchMode.CATEGORY,
                    "Дата" to SearchMode.DATE
                ) + state.customFieldNames.map { it to SearchMode.CUSTOM_FIELD(it) }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    modes.forEach { (label, mode) ->
                        FilterChip(
                            selected = state.searchMode == mode,
                            onClick = { viewModel.onModeChanged(mode) },
                            label = { Text(label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Green100,
                                selectedLabelColor = Green800
                            )
                        )
                    }
                }
            }

            // Поле поиска
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(when (state.searchMode) {
                                SearchMode.ALL -> "Ищет по всем полям..."
                                SearchMode.CAPTION -> "Текст подписи..."
                                SearchMode.CATEGORY -> "Название категории..."
                                SearchMode.DATE -> "Например: октябрь 2026"
                                is SearchMode.CUSTOM_FIELD -> "Значение поля «${state.searchMode.fieldName}»..."
                            }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
                    )
                    Button(onClick = { viewModel.search() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                    ) { Text("Найти") }
                }
            }

            // Результаты
            if (state.isSearching) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = Green700)
                    }
                }
            } else if (state.hasSearched) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Green100),
                        shape = RoundedCornerShape(10.dp)) {
                        Text(state.resultMessage, Modifier.padding(12.dp),
                            color = Green800, fontWeight = FontWeight.Medium)
                    }
                }
                items(state.results, key = { it.id }) { record ->
                    WorkCardCompact(record = record, onClick = { onRecordClick(record.id) })
                }
            } else {
                // Подсказки
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Примеры:", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf("Тевосяна", "Замена качели", "октябрь").forEach { ex ->
                            OutlinedButton(
                                onClick = { viewModel.onQueryChanged(ex); viewModel.search() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(ex, modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}