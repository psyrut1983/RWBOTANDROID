package com.rwbot.android.ui.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.ui.util.ProductImageByArticle

@Composable
fun ReviewsScreen(viewModel: ReviewsViewModel) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        state.syncMessage?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 24.sp
            )
        }

        Button(
            onClick = { viewModel.sync() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            enabled = !state.isLoading
        ) {
            Text(
                text = if (state.isLoading) "Загрузка..." else "Обновить отзывы WB",
                fontSize = 24.sp
            )
        }

        FilterRow(
            current = state.filter,
            onSelect = viewModel::setFilter
        )

        if (state.reviews.isEmpty()) {
            Text(
                text = "Сейчас нет отзывов, которые требуют обработки.",
                modifier = Modifier.padding(16.dp),
                fontSize = 24.sp
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.reviews, key = { it.id }) { review ->
                    ReviewItem(
                        review = review,
                        onClick = { viewModel.selectReview(review.id) }
                    )
                }
            }
        }
    }

    state.selectedReview?.let { review ->
        ReviewDetailsDialog(
            review = review,
            message = state.reviewMessage,
            processing = state.processing,
            draftResponseText = state.draftResponseText,
            isApprovalDialogVisible = state.isApprovalDialogVisible,
            onDismiss = viewModel::closeReviewDetails,
            onProcess = viewModel::processSelectedReview,
            onApprove = viewModel::openApprovalDialog,
            onReject = viewModel::rejectSelectedReview,
            onDraftChanged = viewModel::updateDraftResponse,
            onDismissApproval = viewModel::dismissApprovalDialog,
            onSendApproved = viewModel::sendApprovedAnswer
        )
    }
}

@Composable
private fun ReviewItem(review: ReviewEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Показываем фото товара прямо в списке, чтобы отзыв было проще опознать.
            ProductImageByArticle(
                supplierArticle = review.supplierArticle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = review.text.take(120).plus(if (review.text.length > 120) "..." else ""),
                    fontSize = 28.sp
                )
                Text(
                    text = "★ ${review.rating} · ${review.status}",
                    fontSize = 24.sp
                )
                Text(
                    text = "Артикул WB: ${review.productArticle}" +
                        (review.supplierArticle?.let { " · Артикул продавца: $it" } ?: ""),
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ReviewDetailsDialog(
    review: ReviewEntity,
    message: String?,
    processing: Boolean,
    draftResponseText: String,
    isApprovalDialogVisible: Boolean,
    onDismiss: () -> Unit,
    onProcess: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onDismissApproval: () -> Unit,
    onSendApproved: () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Работа с отзывом") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message?.let {
                    Text(text = it, fontSize = 18.sp)
                }
                ProductImageByArticle(
                    supplierArticle = review.supplierArticle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Text(text = "Отзыв: ${review.text}", fontSize = 22.sp)
                Text(text = "Рейтинг: ${review.rating}", fontSize = 20.sp)
                Text(text = "Статус: ${review.status}", fontSize = 20.sp)
                Text(text = "Артикул WB: ${review.productArticle}", fontSize = 18.sp)
                review.supplierArticle?.let {
                    Text(text = "Артикул продавца: $it", fontSize = 18.sp)
                }
                review.generatedResponse?.let {
                    Text(text = "Сгенерированный ответ: $it", fontSize = 18.sp)
                }

                if (isApprovalDialogVisible) {
                    // Поле редактирования находится на том же экране, чтобы не уводить пользователя в другой экран.
                    OutlinedTextField(
                        value = draftResponseText,
                        onValueChange = onDraftChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Текст ответа") },
                        minLines = 4
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isApprovalDialogVisible -> {
                        Button(
                            onClick = onSendApproved,
                            enabled = !processing
                        ) {
                            Text(if (processing) "Отправка..." else "Отправить")
                        }
                    }
                    review.status == ReviewStatus.NEW -> {
                        Button(
                            onClick = onProcess,
                            enabled = !processing
                        ) {
                            Text(if (processing) "Обработка..." else "Обработать")
                        }
                    }
                    review.status == ReviewStatus.ON_MODERATION -> {
                        Button(
                            onClick = onApprove,
                            enabled = !processing
                        ) {
                            Text("Одобрить")
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (review.status == ReviewStatus.ON_MODERATION && !isApprovalDialogVisible) {
                    Button(
                        onClick = onReject,
                        enabled = !processing
                    ) {
                        Text("Отклонить")
                    }
                }
                Button(
                    onClick = if (isApprovalDialogVisible) onDismissApproval else onDismiss
                ) {
                    Text(if (isApprovalDialogVisible) "Закрыть редактор" else "Закрыть")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(current: ReviewStatus?, onSelect: (ReviewStatus?) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = current == null,
            onClick = { onSelect(null) },
            label = { Text("Все входящие") }
        )
        FilterChip(
            selected = current == ReviewStatus.NEW,
            onClick = { onSelect(ReviewStatus.NEW) },
            label = { Text("Новые") }
        )
        FilterChip(
            selected = current == ReviewStatus.ON_MODERATION,
            onClick = { onSelect(ReviewStatus.ON_MODERATION) },
            label = { Text("На модерации") }
        )
    }
}
