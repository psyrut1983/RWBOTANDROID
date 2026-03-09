package com.rwbot.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    ) {
        Text("Токен Wildberries")
        OutlinedTextField(
            value = state.wbToken,
            onValueChange = viewModel::updateWbToken,
            modifier = Modifier.fillMaxSize(),
            singleLine = false
        )
        Spacer(Modifier.height(8.dp))
        Text("Yandex API ключ")
        OutlinedTextField(
            value = state.yandexApiKey,
            onValueChange = viewModel::updateYandexApiKey,
            modifier = Modifier.fillMaxSize()
        )
        Spacer(Modifier.height(8.dp))
        Text("Yandex Folder ID")
        OutlinedTextField(
            value = state.yandexFolderId,
            onValueChange = viewModel::updateYandexFolderId,
            modifier = Modifier.fillMaxSize()
        )
        Spacer(Modifier.height(8.dp))
        Text("Порог сложности (1-5)")
        OutlinedTextField(
            value = state.complexityThreshold.toString(),
            onValueChange = { it.toIntOrNull()?.let { n -> viewModel.updateComplexityThreshold(n) } }
        )
        Spacer(Modifier.height(8.dp))
        Text("Мин. рейтинг для автоответа")
        OutlinedTextField(
            value = state.minRating.toString(),
            onValueChange = { it.toIntOrNull()?.let { n -> viewModel.updateMinRating(n) } }
        )
        Spacer(Modifier.height(8.dp))
        Text("Blacklist (по одному слову на строку)")
        OutlinedTextField(
            value = state.blacklistText,
            onValueChange = viewModel::updateBlacklistText,
            modifier = Modifier.fillMaxSize(),
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.save() }) {
            Text(if (state.saved) "Сохранено" else "Сохранить")
        }
    }
}
