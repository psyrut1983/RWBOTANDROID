package com.rwbot.android.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rwbot.android.data.local.entity.ReviewStatus

@Composable
fun ReviewDetailScreen(viewModel: ReviewDetailViewModel) {
    val state by viewModel.state.collectAsState()
    val review = state.review ?: return
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
    ) {
        state.message?.let { Text(it) }
        Text("Отзыв: ${review.text}")
        Text("Рейтинг: ${review.rating}")
        Text("Артикул: ${review.productArticle}")
        review.generatedResponse?.let { Text("Ответ: $it") }
        Spacer(Modifier.height(8.dp))
        when (review.status) {
            ReviewStatus.NEW -> Button(onClick = { viewModel.process() }, enabled = !state.processing) {
                Text(if (state.processing) "Обработка…" else "Обработать")
            }
            ReviewStatus.ON_MODERATION -> {
                Button(onClick = { viewModel.approve() }, enabled = !state.processing) { Text("Одобрить") }
                Button(onClick = { viewModel.reject() }) { Text("Отклонить") }
            }
            else -> { }
        }
    }
}
