package com.workjournal.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
    val state   = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) tempPhotoUri?.let { viewModel.addPhotoFromUri(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { viewModel.addPhotoFromUri(it) }
    }

    fun openCamera() {
        if (cameraPermission.status.isGranted) {
            viewModel.createTempPhotoFile()?.let { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            }
        } else cameraPermission.launchPermissionRequest()
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
                OutlinedButton(onClick = { openCamera() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Камера")
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Галерея")
                }
            }
            if (state.photoPaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photoPaths) { path ->
                        Box {
                            AsyncImage(
                                model = path,
                                contentDescription = null,
                                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removePhoto(path) },
                                modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // ---- КАТЕГОРИЯ ----
            SectionLabel("Категория")
            if (state.availableCategories.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Green700)
            } else {
                CategoryChips(
                    categories = state.availableCategories,
                    selected   = state.selectedCategory,
                    onSelect   = { viewModel.onCategorySelected(it) }
                )
            }

            // ---- ПОДПИСЬ ----
            SectionLabel("Подпись")
            OutlinedTextField(
                value = state.caption,
                onValueChange = { viewModel.onCaptionChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Замена качели, Тевосяна 22а — поменяли сиденье", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                minLines = 2, maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )
            // Результат разбора
            if (!state.parsedWorkType.isNullOrBlank() || !state.parsedAddress.isNullOrBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = Green100), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("✨ Распознано:", style = MaterialTheme.typography.labelSmall, color = Green800, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        if (!state.parsedWorkType.isNullOrBlank()) ParsedRow("Тип", state.parsedWorkType)
                        if (!state.parsedAddress.isNullOrBlank())  ParsedRow("Адрес", state.parsedAddress)
                        if (!state.parsedDescription.isNullOrBlank()) ParsedRow("Описание", state.parsedDescription)
                    }
                }
            }

            // ---- ДАТА / ВРЕМЯ ----
            DateTimeSection(selectedDate = state.selectedDate, onDateChanged = { viewModel.onDateChanged(it) }, context = context)

            // ---- КНОПКА ----
            Button(
                onClick = { viewModel.saveRecord() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.caption.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
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

@Composable
private fun SectionLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun CategoryChips(
    categories: List<WorkCategory>,
    selected: WorkCategory?,
    onSelect: (WorkCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { cat ->
                    FilterChip(
                        selected = cat.id == selected?.id,
                        onClick = { onSelect(cat) },
                        label = { Text("${cat.emoji} ${cat.name}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green100,
                            selectedLabelColor = Green800
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Green800, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DateTimeSection(selectedDate: Long, onDateChanged: (Long) -> Unit, context: android.content.Context) {
    val cal    = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm",      Locale.getDefault())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Дата и время", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(4.dp), color = Green700) {
                Text("Авто", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d); onDateChanged(cal.timeInMillis)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(dateFmt.format(Date(selectedDate)))
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(context, { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); onDateChanged(cal.timeInMillis)
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(timeFmt.format(Date(selectedDate)))
            }
        }
    }
}
