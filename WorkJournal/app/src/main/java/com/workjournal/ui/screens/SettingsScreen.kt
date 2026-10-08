package com.workjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workjournal.ui.theme.Green700
import com.workjournal.ui.theme.Green900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
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

            // --- Блок AI ассистент ---
            Text("AI Ассистент", fontWeight = FontWeight.Medium, fontSize = 16.sp)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Для работы AI-ассистента нужен бесплатный ключ Google Gemini API.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Инструкция
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Как получить бесплатный ключ:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("1. Открой aistudio.google.com", fontSize = 12.sp)
                            Text("2. Войди через Google аккаунт", fontSize = 12.sp)
                            Text("3. Нажми «Get API key» → «Create API key»", fontSize = 12.sp)
                            Text("4. Скопируй ключ и вставь ниже", fontSize = 12.sp)
                            Text("✅ Бесплатно: 1500 запросов в день", fontSize = 12.sp, color = Green700)
                        }
                    }

                    TextButton(
                        onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }
                    ) {
                        Text("Открыть aistudio.google.com →", color = Green700)
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            saved = false
                        },
                        label = { Text("API ключ Gemini") },
                        placeholder = { Text("AIza...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.updateApiKey(apiKeyInput)
                                saved = true
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (saved) "✓ Сохранено" else "Сохранить")
                        }
                        if (state.apiKeyConfigured) {
                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = ""
                                    viewModel.updateApiKey("")
                                    saved = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Удалить") }
                        }
                    }

                    if (state.apiKeyConfigured && !saved) {
                        Text("✅ Ключ настроен", color = Green700, fontSize = 13.sp)
                    }
                }
            }

            // --- Блок Категории ---
            Text("Справочники", fontWeight = FontWeight.Medium, fontSize = 16.sp)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                ListItem(
                    headlineContent = { Text("Категории работ") },
                    supportingContent = { Text("Добавить или удалить категории") },
                    leadingContent = { Text("🗂️", fontSize = 24.sp) },
                    trailingContent = {
                        TextButton(onClick = onCategoriesClick) { Text("Открыть →", color = Green700) }
                    }
                )
            }
        }
    }
}
