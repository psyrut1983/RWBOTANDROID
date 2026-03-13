package com.rwbot.android.ui.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.ui.util.ProductImageByArticle

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
        // Сообщение о состоянии (ошибка, успех) — тоже крупным шрифтом
        state.message?.let { Text(it) }

        // Большое фото над текстом для слабовидящего пользователя
        ProductImageByArticle(
            supplierArticle = review.supplierArticle,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentScale = ContentScale.Crop
        )

        // Крупные подписи к артикулу WB и артикулу продавца
        Text(
            text = "Артикул WB: ${review.productArticle}",
            modifier = Modifier.padding(horizontal = 8.dp),
            fontSize = 32.sp
        )
        review.supplierArticle?.let {
            Text(
                text = "Артикул продавца: $it",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 32.sp
            )
        }

        // Основной текст отзыва — ещё крупнее (примерно в 3 раза больше стандартного)
        Text(
            text = "Отзыв: ${review.text}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            fontSize = 48.sp
        )

        Text(
            text = "Рейтинг: ${review.rating}",
            modifier = Modifier.padding(horizontal = 8.dp),
            fontSize = 32.sp
        )

        review.generatedResponse?.let {
            Text(
                text = "Ответ: $it",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                fontSize = 36.sp
            )
        }
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
