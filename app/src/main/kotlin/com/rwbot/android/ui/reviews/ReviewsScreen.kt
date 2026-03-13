package com.rwbot.android.ui.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.ui.util.ProductImageByArticle

@Composable
fun ReviewsScreen(
    viewModel: ReviewsViewModel,
    onReviewClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        state.syncMessage?.let {
            Text(
                it,
                Modifier.padding(8.dp),
                fontSize = 28.sp
            )
        }
        Button(
            onClick = { viewModel.sync() },
            modifier = Modifier.padding(8.dp),
            enabled = !state.isLoading
        ) {
            Text(
                if (state.isLoading) "Загрузка…" else "Обновить отзывы WB",
                fontSize = 28.sp
            )
        }
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
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Фото на всю ширину экрана (карточка уже с отступами — фото на всю ширину карточки)
            ProductImageByArticle(
                supplierArticle = review.supplierArticle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp)) {
                // Крупный текст отзыва (краткая версия в списке)
                Text(
                    review.text.take(120).plus(if (review.text.length > 120) "…" else ""),
                    fontSize = 32.sp
                )
                // Рейтинг и статус — тоже крупно
                Text(
                    "★ ${review.rating} · ${review.status}",
                    fontSize = 28.sp
                )
                // Артикул WB (nmId) и артикул продавца
                Text(
                    "Артикул WB: ${review.productArticle}" +
                        (review.supplierArticle?.let { " · Артикул продавца: $it" } ?: ""),
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 24.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
