package com.rwbot.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        // --- Блок: токены для API ---
        Text(
            text = "Токены доступа",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // Поле 1: токен Wildberries (WB)
        Text("Токен WB (Wildberries)")
        OutlinedTextField(
            value = state.wbToken,
            onValueChange = viewModel::updateWbToken,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Вставьте токен из личного кабинета WB") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        // Поле 2: токен Yandex (API-ключ)
        Text("Токен Yandex (API-ключ)")
        OutlinedTextField(
            value = state.yandexApiKey,
            onValueChange = viewModel::updateYandexApiKey,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Вставьте API-ключ из консоли Yandex Cloud") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        Text("Yandex Folder ID")
        OutlinedTextField(
            value = state.yandexFolderId,
            onValueChange = viewModel::updateYandexFolderId,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ID каталога в Yandex Cloud") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text("Порог сложности (1-5)")
        OutlinedTextField(
            value = state.complexityThreshold.toString(),
            onValueChange = { it.toIntOrNull()?.let { n -> viewModel.updateComplexityThreshold(n) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Мин. рейтинг для автоответа")
        OutlinedTextField(
            value = state.minRating.toString(),
            onValueChange = { it.toIntOrNull()?.let { n -> viewModel.updateMinRating(n) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Blacklist (по одному слову на строку)")
        OutlinedTextField(
            value = state.blacklistText,
            onValueChange = viewModel::updateBlacklistText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.save() }) {
            Text(if (state.saved) "Сохранено" else "Сохранить")
        }
    }
}
