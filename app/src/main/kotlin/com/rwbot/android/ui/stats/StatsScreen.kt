package com.rwbot.android.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.padding(8.dp)) { Text("Всего отзывов: ${state.total}", Modifier.padding(16.dp)) }
        Card(Modifier.padding(8.dp)) { Text("Ответов отправлено: ${state.answered}", Modifier.padding(16.dp)) }
        Card(Modifier.padding(8.dp)) { Text("На модерации: ${state.onModeration}", Modifier.padding(16.dp)) }
        Card(Modifier.padding(8.dp)) { Text("Отклонено: ${state.rejected}", Modifier.padding(16.dp)) }
    }
}
