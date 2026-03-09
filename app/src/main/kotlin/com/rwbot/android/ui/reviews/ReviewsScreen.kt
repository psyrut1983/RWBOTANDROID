package com.rwbot.android.ui.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus

@Composable
fun ReviewsScreen(
    viewModel: ReviewsViewModel,
    onReviewClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.setFilter(null) }
    Column(Modifier.fillMaxSize()) {
        state.syncMessage?.let { Text(it, Modifier.padding(8.dp)) }
        Button(
            onClick = { viewModel.sync() },
            modifier = Modifier.padding(8.dp),
            enabled = !state.isLoading
        ) { Text(if (state.isLoading) "Загрузка…" else "Обновить из WB") }
        LazyColumn(Modifier.weight(1f)) {
            items(state.reviews, key = { it.id }) { review ->
                ReviewItem(review = review, onClick = { onReviewClick(review.id) })
            }
        }
        FilterRow(
            current = state.filter,
            onSelect = viewModel::setFilter
        )
    }
}

@Composable
private fun ReviewItem(review: ReviewEntity, onClick: () -> Unit) {
    Card(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(review.text.take(100).plus(if (review.text.length > 100) "…" else ""))
            Text("★ ${review.rating} · ${review.status}")
        }
    }
}

@Composable
private fun FilterRow(current: ReviewStatus?, onSelect: (ReviewStatus?) -> Unit) {
    val scroll = rememberScrollState()
    Row(Modifier.padding(8.dp).horizontalScroll(scroll)) {
        FilterChip(selected = current == null, onClick = { onSelect(null) }, label = { Text("Все") })
        ReviewStatus.entries.forEach { status ->
            FilterChip(
                selected = current == status,
                onClick = { onSelect(status) },
                label = { Text(status.name) }
            )
        }
    }
}
