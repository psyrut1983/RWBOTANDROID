package com.rwbot.android.data.repository

import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import kotlinx.coroutines.flow.Flow

/** Репозиторий отзывов: локальная Room + синхронизация с Wildberries API. */
interface ReviewRepository {

    fun getAllReviewsFlow(): Flow<List<ReviewEntity>>
    fun getReviewsByStatusFlow(status: ReviewStatus): Flow<List<ReviewEntity>>
    suspend fun getReviewById(id: String): ReviewEntity?
    suspend fun syncFromWildberries(): Result<Int>
    suspend fun sendAnswerToWildberries(feedbackId: String, text: String): Result<Unit>
    suspend fun updateReview(review: ReviewEntity)
    suspend fun getModerationCount(): Int
    /** Количество неотвеченных отзывов (NEW + ON_MODERATION). */
    fun getUnansweredCountFlow(): Flow<Int>
}
