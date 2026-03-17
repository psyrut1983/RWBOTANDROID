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
import com.rwbot.android.domain.rag.RagRetriever
import javax.inject.Inject
import javax.inject.Singleton

/** Результат обработки одного отзыва пайплайном. */
sealed class PipelineResult {
    data class OnModeration(val review: ReviewEntity) : PipelineResult()
    data class Error(val message: String) : PipelineResult()
}

/** Сколько похожих отзывов подставлять в контекст RAG. */
private const val RAG_LIMIT = 5

/**
 * Пайплайн обработки одного отзыва: классификация → RAG (поиск похожих) → генерация ответа (Yandex GPT) → решение → автоотправка или очередь модерации.
 * После успешной обработки отзыв добавляется в архив RAG.
 */
@Singleton
class ReviewPipeline @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val yandexRepository: YandexRepository,
    private val secureSettings: SecureSettings,
    private val classifier: ReviewClassifier,
    private val decisionEngine: DecisionEngine,
    private val ragRetriever: RagRetriever
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
        // RAG: поиск похожих отзывов для контекста (при пустом архиве вернётся emptyList)
        val ragContext = ragRetriever.findSimilar(review.text, RAG_LIMIT)
        val generateResult = yandexRepository.generateResponse(review.text, ragContext = ragContext)
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
            // ВАЖНО: по требованиям UX мы всегда просим подтверждение пользователя.
            // Поэтому даже если DecisionEngine решил AUTO_SEND — мы не отправляем автоматически,
            // а переводим в ON_MODERATION и показываем текст на одобрение/редактирование.
            status = ReviewStatus.ON_MODERATION,
            updatedAt = System.currentTimeMillis()
        )
        // decision сейчас используется только для аналитики/отладки. Отправка — только вручную из UI.
        @Suppress("UNUSED_VARIABLE")
        val _decisionForDebug = decision

        reviewRepository.updateReview(updated)
        // В архив RAG добавляем только отправленные ответы (при одобрении в ReviewDetailViewModel)
        return PipelineResult.OnModeration(updated)
    }
}
