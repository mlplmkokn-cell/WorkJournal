package com.workjournal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workjournal.domain.model.WorkRecord
import com.workjournal.ui.components.WorkCardCompact
import com.workjournal.ui.theme.*

/**
 * Экран AI-ассистента.
 * Чат с Gemini — задаёшь вопрос, получаешь ответ + карточки записей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Автоскролл вниз при новых сообщениях
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Ассистент", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            Text(
                                if (state.apiKeyConfigured) "Gemini · активен"
                                else "Требуется настройка",
                                fontSize = 11.sp,
                                color = if (state.apiKeyConfigured)
                                    Color.White.copy(alpha = 0.7f)
                                else
                                    Color(0xFFFFCC80)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green900,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ChatInput(
                text = state.inputText,
                onTextChange = { viewModel.onInputChanged(it) },
                onSend = { viewModel.sendMessage() },
                isLoading = state.isThinking,
                enabled = !state.isThinking
            )
        }
    ) { padding ->

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages) { message ->
                ChatMessage(
                    message = message,
                    onRecordClick = onRecordClick
                )
            }

            // Анимация "думает..."
            if (state.isThinking) {
                item {
                    ThinkingIndicator()
                }
            }
        }
    }
}

/**
 * Одно сообщение в чате.
 */
@Composable
private fun ChatMessage(
    message: AssistantViewModel.Message,
    onRecordClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // Пузырь сообщения
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        message.isUser  -> Green700
                        message.isError -> MaterialTheme.colorScheme.errorContainer
                        else            -> MaterialTheme.colorScheme.surface
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = when {
                    message.isUser  -> Color.White
                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                    else            -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 14.sp
            )
        }

        // Карточки найденных записей (только для ответов бота)
        if (!message.isUser && message.matchedRecords.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Найденные записи:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                message.matchedRecords.forEach { record ->
                    WorkCardCompact(
                        record = record,
                        onClick = { onRecordClick(record.id) }
                    )
                }
            }
        }
    }
}

/**
 * Строка ввода снизу.
 */
@Composable
private fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Спросите о записях журнала...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                enabled = enabled
            )

            FilledIconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                containerColor = Green700,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color.White)
            }
        }
    }
}

/**
 * Анимация "Ассистент думает..."
 */
@Composable
private fun ThinkingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("🤖", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            "Анализирую журнал...",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
