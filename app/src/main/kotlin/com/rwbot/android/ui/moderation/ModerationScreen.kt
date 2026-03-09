package com.rwbot.android.ui.moderation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.ui.reviews.ReviewsScreen
import com.rwbot.android.ui.reviews.ReviewsViewModel

@Composable
fun ModerationScreen(
    viewModel: ReviewsViewModel,
    onReviewClick: (String) -> Unit
) {
    LaunchedEffect(Unit) { viewModel.setFilter(ReviewStatus.ON_MODERATION) }
    Column(Modifier.fillMaxSize()) {
        Text("Очередь модерации")
        ReviewsScreen(viewModel = viewModel, onReviewClick = onReviewClick)
    }
}
