package com.workjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workjournal.domain.model.WorkCategory
import com.workjournal.ui.theme.Green700
import com.workjournal.ui.theme.Green900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Диалог добавления
    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddDialog() },
            title = { Text("Новая категория") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.newName,
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = { Text("Название") },
                        placeholder = { Text("Например: 🛝 Качели") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.error != null
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addCategory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddDialog() }) { Text("Отмена") }
            }
        )
    }

    // Диалог умного удаления — категория содержит работы
    val deleteAction = state.deleteAction
    if (deleteAction is CategoriesViewModel.DeleteAction.WithRecords) {
        var selectedTarget by remember { mutableStateOf<WorkCategory?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Категория «${deleteAction.category.name}» содержит работы") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "В этой категории ${deleteAction.records.size} " +
                        if (deleteAction.records.size == 1) "запись" else "записей" + ".",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Что сделать с этими записями?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Выбор: перенести в другую категорию
                    if (deleteAction.otherCategories.isNotEmpty()) {
                        Text(
                            "Перенести в категорию:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        deleteAction.otherCategories.forEach { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedTarget?.id == cat.id,
                                    onClick = { selectedTarget = cat }
                                )
                                Text(cat.name, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Кнопка перенести (активна только если выбрана целевая категория)
                    if (deleteAction.otherCategories.isNotEmpty()) {
                        Button(
                            onClick = {
                                selectedTarget?.let { viewModel.moveRecordsToCategory(it) }
                            },
                            enabled = selectedTarget != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700)
                        ) { Text("Перенести и удалить категорию") }
                    }
                    // Кнопка удалить всё
                    OutlinedButton(
                        onClick = { viewModel.deleteCategoryWithRecords() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Удалить категорию и все записи") }
                    // Отмена
                    TextButton(
                        onClick = { viewModel.dismissDeleteDialog() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена") }
                }
            },
            dismissButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Категории", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = Green700
            ) {
                Icon(Icons.Default.Add, "Добавить", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.categories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🗂️", fontSize = 48.sp)
                        Text(
                            "Категорий пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Нажмите + чтобы добавить первую категорию",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.categories, key = { it.id }) { cat ->
                    CategoryRow(
                        cat = cat,
                        onDelete = { viewModel.requestDelete(cat) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(cat: WorkCategory, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cat.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}