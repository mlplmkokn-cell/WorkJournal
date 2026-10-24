package com.workjournal.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.workjournal.domain.model.WorkCategory
import com.workjournal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddWorkScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddWorkViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newFieldName by remember { mutableStateOf("") }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) tempPhotoUri?.let { viewModel.addPhotoFromUri(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { viewModel.addPhotoFromUri(it) } }

    fun openCamera() {
        if (cameraPermission.status.isGranted) {
            viewModel.createTempPhotoFile()?.let { file ->
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            }
        } else cameraPermission.launchPermissionRequest()
    }

    // Диалог: Добавить категорию
    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Новая категория", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Название") },
                        placeholder = { Text("Например: 🛝 Качели") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Отмена") }
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.addNewCategory(newCategoryName)
                                    newCategoryName = ""
                                    showAddCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            modifier = Modifier.weight(1f)
                        ) { Text("Добавить") }
                    }
                }
            }
        }
    }

    // Диалог: Добавить поле
    if (showAddFieldDialog) {
        Dialog(onDismissRequest = { showAddFieldDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Новое поле", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    if (state.fieldTemplates.isNotEmpty()) {
                        Text(
                            "Часто используемые:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.fieldTemplates.take(5).forEach { template ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.addCustomField(template.fieldName)
                                    showAddFieldDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text(template.fieldName, modifier = Modifier.weight(1f)) }
                        }
                        HorizontalDivider()
                    }
                    OutlinedTextField(
                        value = newFieldName,
                        onValueChange = { newFieldName = it },
                        label = { Text("Название поля") },
                        placeholder = { Text("ID площадки, Адрес, Улица...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAddFieldDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Отмена") }
                        Button(
                            onClick = {
                                if (newFieldName.isNotBlank()) {
                                    viewModel.addCustomField(newFieldName)
                                    newFieldName = ""
                                    showAddFieldDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            modifier = Modifier.weight(1f)
                        ) { Text("Добавить") }
                    }
                }
            }
        }
    }

    // Диалог: Подтверждение при неполных данных
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelSave() },
            title = { Text("Не все поля заполнены") },
            text = {
                Column {
                    Text("Следующие поля пустые:")
                    Spacer(Modifier.height(8.dp))
                    state.missingFields.forEach { field ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = MaterialTheme.colorScheme.error)
                            Text(field)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Сохранить всё равно?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) { Text("Да, сохранить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelSave() }) { Text("Вернуться") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая работа", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---- ФОТО ----
            SectionLabel("Фотографии")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openCamera() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
            }
            if (state.photoPaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photoPaths) { path ->
                        Box {
                            AsyncImage(
                                model = path, contentDescription = null,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removePhoto(path) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    Icons.Default.Close, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ---- КАТЕГОРИЯ ----
            SectionLabel("Категория")
            CategoryChipsWithPlus(
                categories = state.availableCategories,
                selectedCategory = state.selectedCategory,
                onSelect = { viewModel.onCategorySelected(it) },
                onAddClick = { showAddCategoryDialog = true }
            )

            // ---- ПОДПИСЬ ----
            SectionLabel("Подпись")
            OutlinedTextField(
                value = state.caption,
                onValueChange = { viewModel.onCaptionChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Замена качели, Тевосяна 22а — поменяли сиденье",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = 2, maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )
            if (!state.parsedWorkType.isNullOrBlank() || !state.parsedAddress.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Green100),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "✨ Распознано:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Green800, fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!state.parsedWorkType.isNullOrBlank()) ParsedRow("Тип", state.parsedWorkType)
                        if (!state.parsedAddress.isNullOrBlank()) ParsedRow("Адрес", state.parsedAddress)
                    }
                }
            }

            // ---- КАСТОМНЫЕ ПОЛЯ ----
            if (state.customFields.isNotEmpty()) {
                SectionLabel("Дополнительные поля")
                state.customFields.forEachIndexed { index, field ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = field.fieldValue,
                            onValueChange = { viewModel.updateCustomField(index, it) },
                            label = { Text(field.fieldName) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        IconButton(
                            onClick = { viewModel.removeCustomField(index) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, "Удалить поле",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { showAddFieldDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Добавить поле (ID, Адрес, Улица...)")
            }

            // ---- ДАТА ----
            SectionLabel("Дата")
            val cal = Calendar.getInstance().apply { timeInMillis = state.selectedDate }
            val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> cal.set(y, m, d); viewModel.onDateChanged(cal.timeInMillis) },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateFmt.format(Date(state.selectedDate)), fontSize = 15.sp)
            }

            // ---- СОХРАНИТЬ ----
            Button(
                onClick = { viewModel.trySave() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.caption.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Чипы категорий — плюсик ВСЕГДА виден, идёт после всех категорий.
 * Используем wrap через несколько Row — надёжно без FlowRow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipsWithPlus(
    categories: List<WorkCategory>,
    selectedCategory: WorkCategory?,
    onSelect: (WorkCategory) -> Unit,
    onAddClick: () -> Unit
) {
    // Разбиваем категории по строкам по 3 штуки
    // Плюсик добавляем в конец последней строки отдельно
    val rows = categories.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { cat ->
                    FilterChip(
                        selected = cat.id == selectedCategory?.id,
                        onClick = { onSelect(cat) },
                        label = { Text(cat.name, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green100,
                            selectedLabelColor = Green800
                        )
                    )
                }
            }
        }
        // Плюсик — всегда отдельной строкой в конце
        // Так он никогда не потеряется
        OutlinedButton(
            onClick = onAddClick,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (categories.isEmpty()) "Добавить категорию" else "+ ещё категорию", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun ParsedRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Text(
            value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, color = Green800,
            modifier = Modifier.weight(1f)
        )
    }
}