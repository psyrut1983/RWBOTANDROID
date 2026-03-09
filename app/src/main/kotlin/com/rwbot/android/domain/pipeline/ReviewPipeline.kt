package com.rwbot.android.domain.pipeline

import com.rwbot.android.data.local.SecureSettings
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.data.repository.YandexRepository
import com.rwbot.android.domain.classification.ReviewClassifier
import com.rwbot.android.domain.decision.Decision
import com.rwbot.android.domain.decision.DecisionEngine
import com.rwbot.android.domain.decision.DecisionSettings
import javax.inject.Inject
import javax.inject.Singleton

/** Результат обработки одного отзыва пайплайном. */
sealed class PipelineResult {
    data class AutoSent(val review: ReviewEntity) : PipelineResult()
    data class OnModeration(val review: ReviewEntity) : PipelineResult()
    data class Error(val message: String) : PipelineResult()
}

/**
 * Пайплайн обработки одного отзыва: классификация → генерация ответа (Yandex GPT) → решение → автоотправка или очередь модерации.
 * Все вызовы к API и Room — через репозитории.
 */
@Singleton
class ReviewPipeline @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val yandexRepository: YandexRepository,
    private val secureSettings: SecureSettings,
    private val classifier: ReviewClassifier,
    private val decisionEngine: DecisionEngine
) {

    suspend fun processReview(review: ReviewEntity): PipelineResult {
        if (review.status != ReviewStatus.NEW) {
            return PipelineResult.Error("Отзыв уже обработан")
        }
        val settings = DecisionSettings(
            complexityThreshold = secureSettings.complexityThreshold,
            confidenceThreshold = secureSettings.confidenceThreshold,
            minRatingForAutoResponse = secureSettings.minRatingForAutoResponse
        )
        val classification = classifier.classify(
            reviewText = review.text,
            rating = review.rating,
            blacklistWords = secureSettings.blacklistWords
        )
        val generateResult = yandexRepository.generateResponse(review.text)
        val responseText = when (generateResult) {
            is Result.Success -> generateResult.data
            is Result.Error -> return PipelineResult.Error(generateResult.message)
        }
        val confidenceScore = 1.0 // В v1 не получаем от API, считаем 1.0
        val decision = decisionEngine.decide(
            classification = classification,
            rating = review.rating,
            confidenceScore = confidenceScore,
            settings = settings
        )
        val updated = review.copy(
            generatedResponse = responseText,
            status = if (decision == Decision.AUTO_SEND) ReviewStatus.ANSWERED else ReviewStatus.ON_MODERATION,
            updatedAt = System.currentTimeMillis()
        )
        return when (decision) {
            Decision.AUTO_SEND -> {
                val sendResult = reviewRepository.sendAnswerToWildberries(review.id, responseText)
                when (sendResult) {
                    is Result.Success -> {
                        reviewRepository.updateReview(updated.copy(status = ReviewStatus.ANSWERED))
                        PipelineResult.AutoSent(updated)
                    }
                    is Result.Error -> PipelineResult.Error(sendResult.message)
                }
            }
            Decision.MODERATE -> {
                reviewRepository.updateReview(updated)
                PipelineResult.OnModeration(updated)
            }
        }
    }
}
