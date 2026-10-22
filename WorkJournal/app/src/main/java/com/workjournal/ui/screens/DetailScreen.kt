package com.workjournal.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.workjournal.domain.model.WorkRecord
import com.workjournal.ui.components.formatDate
import com.workjournal.ui.theme.*
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Это действие нельзя отменить. Все фотографии тоже будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteRecord { onBack() }; showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.record != null) "Запись #${state.record!!.id}" else "Загрузка...",
                        fontWeight = FontWeight.Medium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Удалить", tint = Color.White.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green900, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
            state.record == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("Запись не найдена")
            }
            else -> RecordContent(
                record = state.record!!,
                modifier = Modifier.padding(padding),
                context = context
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordContent(record: WorkRecord, modifier: Modifier, context: Context) {
    // Полноэкранный просмотр фото
    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val photos = record.photos.map { it.filePath }

    // Полноэкранный диалог с зумом
    fullscreenPhotoIndex?.let { startIndex ->
        FullscreenPhotoViewer(
            photos = photos,
            startIndex = startIndex,
            onDismiss = { fullscreenPhotoIndex = null },
            context = context
        )
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ===== ГАЛЕРЕЯ =====
        if (photos.isNotEmpty()) {
            val pagerState = rememberPagerState { photos.size }

            Box {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) { page ->
                    // Нажатие на фото → полный экран
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = photos[page],
                            contentDescription = "Фото ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, _, _ -> }
                                },
                            contentScale = ContentScale.Crop
                        )
                        // Невидимая кнопка на весь экран для открытия полного просмотра
                        Surface(
                            onClick = { fullscreenPhotoIndex = page },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {}
                    }
                }

                // Кнопки сохранить
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            savePhotoToGallery(context, photos[pagerState.currentPage])
                            Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT).show()
                        },
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Download, "Сохранить фото", Modifier.size(20.dp))
                    }
                    if (photos.size > 1) {
                        SmallFloatingActionButton(
                            onClick = {
                                photos.forEach { savePhotoToGallery(context, it) }
                                Toast.makeText(context, "Все фото сохранены", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.DoneAll, "Все фото", Modifier.size(20.dp))
                        }
                    }
                }

                // Точки-индикаторы
                if (photos.size > 1) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        photos.indices.forEach { i ->
                            Box(modifier = Modifier.padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == i) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == i) Color.White else Color.White.copy(0.5f)))
                        }
                    }
                    Surface(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        shape = RoundedCornerShape(20.dp), color = Color.Black.copy(0.5f)) {
                        Text("${pagerState.currentPage + 1}/${photos.size}",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White, fontSize = 12.sp)
                    }
                }

                // Подсказка "нажми для увеличения"
                Surface(modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = RoundedCornerShape(20.dp), color = Color.Black.copy(0.4f)) {
                    Text("🔍 увеличить",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White, fontSize = 11.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(record.categoryEmoji.ifBlank { "📋" }, fontSize = 64.sp)
            }
        }

        // ===== ИНФОРМАЦИЯ =====
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(record.workType ?: record.caption,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    if (!record.address.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = Green700)
                            Spacer(Modifier.width(2.dp))
                            Text(record.address, style = MaterialTheme.typography.bodyLarge, color = Green700)
                        }
                    }
                }
                if (record.categoryEmoji.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Green100) {
                        Text(record.categoryEmoji, Modifier.padding(8.dp), fontSize = 24.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(formatDate(record.workDate), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Метаданные
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.categoryName.isNotBlank()) MetaCard("Категория", record.categoryName, Modifier.weight(1f))
                MetaCard("Фотографий", "${record.photos.size} шт.", Modifier.weight(1f))
            }

            // Кастомные поля
            if (record.customFields.isNotEmpty()) {
                HorizontalDivider()
                Text("Дополнительно", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                record.customFields.forEach { field ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(field.fieldName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 80.dp, max = 120.dp))
                        Text(field.fieldValue, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Исходная подпись", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("«${record.caption}»", style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Полноэкранный просмотр фото с зумом пальцами.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullscreenPhotoViewer(
    photos: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
    context: Context
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Состояние зума для каждой страницы
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                AsyncImage(
                    model = photos[page],
                    contentDescription = "Фото ${page + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                // Смещение только при зуме
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Кнопка закрыть
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .background(Color.Black.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Закрыть", tint = Color.White)
            }

            // Счётчик фото
            if (photos.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    shape = RoundedCornerShape(20.dp), color = Color.Black.copy(0.5f)
                ) {
                    Text("${pagerState.currentPage + 1} / ${photos.size}",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White, fontSize = 14.sp)
                }
            }

            // Кнопка сохранить внизу
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        savePhotoToGallery(context, photos[pagerState.currentPage])
                        Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color.White.copy(0.15f),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Download, "Сохранить")
                }
                if (photos.size > 1) {
                    FloatingActionButton(
                        onClick = {
                            photos.forEach { savePhotoToGallery(context, it) }
                            Toast.makeText(context, "Все фото сохранены", Toast.LENGTH_SHORT).show()
                        },
                        containerColor = Color.White.copy(0.15f),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.DoneAll, "Все фото")
                    }
                }
            }
        }
    }
}

fun savePhotoToGallery(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return
        val filename = "WorkJournal_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WorkJournal")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    FileInputStream(file).use { input -> input.copyTo(out) }
                }
            }
        } else {
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dir = File(pictures, "WorkJournal").also { it.mkdirs() }
            file.copyTo(File(dir, filename), overwrite = true)
        }
    } catch (e: Exception) { e.printStackTrace() }
}

@Composable
private fun MetaCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}