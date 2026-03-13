package com.rwbot.android.domain.pipeline

import com.rwbot.android.data.local.SecureSettings
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.data.repository.YandexRepository
import com.rwbot.android.domain.classification.ClassificationResult
import com.rwbot.android.domain.classification.ReviewCategory
import com.rwbot.android.domain.classification.ReviewClassifier
import com.rwbot.android.domain.decision.DecisionEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Тесты пайплайна обработки отзыва: классификация → Yandex GPT → решение → авто/модерация.
 */
class ReviewPipelineTest {

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var yandexRepository: YandexRepository
    private lateinit var secureSettings: SecureSettings
    private lateinit var classifier: ReviewClassifier
    private lateinit var decisionEngine: DecisionEngine
    private lateinit var pipeline: ReviewPipeline

    private val newReview = ReviewEntity(
        id = "r1",
        text = "Спасибо, товар отличный!",
        rating = 5,
        productArticle = "123",
        supplierArticle = null,
        authorName = "User",
        createdDate = null,
        status = ReviewStatus.NEW,
        generatedResponse = null,
        updatedAt = 0L
    )

    @Before
    fun setUp() {
        reviewRepository = mockk(relaxed = true)
        yandexRepository = mockk(relaxed = true)
        secureSettings = mockk(relaxed = true)
        classifier = ReviewClassifier()
        decisionEngine = DecisionEngine()
        every { secureSettings.complexityThreshold } returns 4
        every { secureSettings.confidenceThreshold } returns 0.8
        every { secureSettings.minRatingForAutoResponse } returns 3
        every { secureSettings.blacklistWords } returns emptySet()
        pipeline = ReviewPipeline(
            reviewRepository = reviewRepository,
            yandexRepository = yandexRepository,
            secureSettings = secureSettings,
            classifier = classifier,
            decisionEngine = decisionEngine
        )
    }

    @Test
    fun alreadyProcessed_returnsError() = runTest {
        val onModeration = newReview.copy(status = ReviewStatus.ON_MODERATION)
        val result = pipeline.processReview(onModeration)
        assertEquals(PipelineResult.Error("Отзыв уже обработан"), result)
    }

    @Test
    fun yandexError_returnsError() = runTest {
        coEvery { yandexRepository.generateResponse(any()) } returns Result.Error("Нет сети")
        val result = pipeline.processReview(newReview)
        assert(result is PipelineResult.Error)
        assertEquals("Нет сети", (result as PipelineResult.Error).message)
    }

    @Test
    fun decisionModerate_savesAndReturnsOnModeration() = runTest {
        // Высокая сложность → движок вернёт MODERATE (не AUTO_SEND)
        val highComplexity = ClassificationResult(ReviewCategory.COMPLAINT, 5, false)
        classifier = mockk(relaxed = true)
        every { classifier.classify(any(), any(), any()) } returns highComplexity
        pipeline = ReviewPipeline(reviewRepository, yandexRepository, secureSettings, classifier, decisionEngine)
        coEvery { yandexRepository.generateResponse(any()) } returns Result.Success("Спасибо!")
        val result = pipeline.processReview(newReview)
        assert(result is PipelineResult.OnModeration)
        coVerify(exactly = 1) { reviewRepository.updateReview(any()) }
    }

    @Test
    fun decisionAutoSend_sendsToWbAndUpdatesReview() = runTest {
        coEvery { yandexRepository.generateResponse(any()) } returns Result.Success("Спасибо!")
        coEvery { reviewRepository.sendAnswerToWildberries(any(), any()) } returns Result.Success(Unit)
        val result = pipeline.processReview(newReview)
        assert(result is PipelineResult.AutoSent)
        coVerify { reviewRepository.sendAnswerToWildberries("r1", "Спасибо!") }
        coVerify { reviewRepository.updateReview(any()) }
    }

    @Test
    fun decisionAutoSend_butWbFails_returnsError() = runTest {
        coEvery { yandexRepository.generateResponse(any()) } returns Result.Success("Спасибо!")
        coEvery { reviewRepository.sendAnswerToWildberries(any(), any()) } returns Result.Error("401")
        val result = pipeline.processReview(newReview)
        assert(result is PipelineResult.Error)
        assertEquals("401", (result as PipelineResult.Error).message)
    }
}
