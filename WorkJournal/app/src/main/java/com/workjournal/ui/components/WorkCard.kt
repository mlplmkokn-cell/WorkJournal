package com.workjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.workjournal.domain.model.WorkRecord
import com.workjournal.ui.theme.Green100
import com.workjournal.ui.theme.Green700
import com.workjournal.ui.theme.Green800
import java.text.SimpleDateFormat
import java.util.*

/**
 * Карточка записи для главного экрана и поиска.
 */
@Composable
fun WorkCard(
    record: WorkRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Фото (если есть)
            if (record.photos.isNotEmpty()) {
                AsyncImage(
                    model = record.photos.first().filePath,
                    contentDescription = "Фото работы",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Заглушка если фото нет
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = record.category.emoji,
                        fontSize = 36.sp
                    )
                }
            }

            // Информация о записи
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Заголовок
                Text(
                    text = record.workType ?: record.caption,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Адрес
                if (!record.address.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Green700
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = record.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Дата и количество фото
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(record.workDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    // Бейдж с количеством фото
                    if (record.photos.isNotEmpty()) {
                        PhotoCountBadge(count = record.photos.size)
                    }
                }
            }
        }
    }
}

/**
 * Компактная карточка для экрана поиска (горизонтальная).
 */
@Composable
fun WorkCardCompact(
    record: WorkRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Миниатюра фото слева
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (record.photos.isNotEmpty()) {
                    AsyncImage(
                        model = record.photos.first().filePath,
                        contentDescription = null,
                        modifier = Modifier
                            .width(80.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = record.category.emoji, fontSize = 28.sp)
                }
            }

            // Информация справа
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Text(
                    text = record.workType ?: record.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!record.address.isNullOrBlank()) {
                    Text(
                        text = record.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = formatDate(record.workDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/**
 * Бейдж с количеством фотографий.
 */
@Composable
fun PhotoCountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Green100
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                Icons.Default.Photo,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = Green800
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = Green800
            )
        }
    }
}

/**
 * Форматирование даты для отображения.
 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
