package com.rwbot.android.ui.reviews

import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.domain.pipeline.PipelineResult
import com.rwbot.android.domain.pipeline.ReviewPipeline
import com.rwbot.android.domain.rag.RagRetriever
import com.rwbot.android.util.MainCoroutineRule
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Тесты ReviewsViewModel для единого экрана отзывов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainCoroutineRule(testDispatcher)

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var reviewPipeline: ReviewPipeline
    private lateinit var ragRetriever: RagRetriever
    private lateinit var viewModel: ReviewsViewModel

    private val sampleReviews = mutableListOf(
        ReviewEntity("1", "Текст", 5, "art", null, null, null, ReviewStatus.NEW, null, 0L),
        ReviewEntity("2", "Нужна проверка", 4, "art2", null, null, null, ReviewStatus.ON_MODERATION, "Ответ", 0L),
        ReviewEntity("3", "Готово", 5, "art3", null, null, null, ReviewStatus.ANSWERED, null, 0L)
    )

    private val newReview = sampleReviews[0]
    private val moderationReview = sampleReviews[1]

    @Before
    fun setUp() {
        reviewRepository = mockk(relaxed = true)
        reviewPipeline = mockk(relaxed = true)
        ragRetriever = mockk(relaxed = true)
        every { reviewRepository.getAllReviewsFlow() } returns flowOf(sampleReviews)
        every { reviewRepository.getUnansweredCountFlow() } returns flowOf(2)
        viewModel = ReviewsViewModel(reviewRepository, reviewPipeline, ragRetriever)
    }

    @Test
    fun initialState_hasReviewsFromFlow() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        // На главном экране показываются только входящие отзывы, уже обработанные скрываются.
        assertEquals(2, viewModel.state.value.reviews.size)
        assertEquals("1", viewModel.state.value.reviews[0].id)
    }

    @Test
    fun setFilter_filtersByStatus() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        viewModel.setFilter(ReviewStatus.ON_MODERATION)
        advanceUntilIdle()
        assertEquals(ReviewStatus.ON_MODERATION, viewModel.state.value.filter)
        assertEquals(1, viewModel.state.value.reviews.size)
        assertEquals("2", viewModel.state.value.reviews.first().id)
    }

    @Test
    fun sync_success_setsMessage() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Success(5)
        viewModel.sync()
        advanceUntilIdle()
        assertEquals("Загружено: 5", viewModel.state.value.syncMessage)
    }

    @Test
    fun sync_error_setsMessage() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Error("Нет сети")
        viewModel.sync()
        advanceUntilIdle()
        assertEquals("Нет сети", viewModel.state.value.syncMessage)
    }

    @Test
    fun clearMessage_clearsSyncMessage() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Success(0)
        viewModel.sync()
        advanceUntilIdle()
        viewModel.clearMessage()
        advanceUntilIdle()
        assertNull(viewModel.state.value.syncMessage)
    }

    @Test
    fun processSelectedReview_opensApprovalDialog() = runTest(testDispatcher) {
        val processedReview = newReview.copy(
            status = ReviewStatus.ON_MODERATION,
            generatedResponse = "Сгенерированный ответ"
        )
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewPipeline.processReview(newReview) } returns PipelineResult.OnModeration(processedReview)

        viewModel.selectReview(newReview.id)
        advanceUntilIdle()
        viewModel.processSelectedReview()
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isApprovalDialogVisible)
        assertEquals("Сгенерированный ответ", viewModel.state.value.draftResponseText)
    }

    @Test
    fun sendApprovedAnswer_marksReviewAnsweredAndClosesDetails() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        viewModel.selectReview(moderationReview.id)
        advanceUntilIdle()
        viewModel.openApprovalDialog()
        viewModel.updateDraftResponse("Финальный ответ")
        coEvery { reviewRepository.sendAnswerToWildberries(moderationReview.id, "Финальный ответ") } returns Result.Success(Unit)
        coEvery { reviewRepository.updateReview(any()) } just Runs

        viewModel.sendApprovedAnswer()
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedReview)
        assertEquals(false, viewModel.state.value.isApprovalDialogVisible)
        coVerify { reviewRepository.updateReview(match { it.status == ReviewStatus.ANSWERED }) }
    }

    @Test
    fun rejectSelectedReview_marksReviewRejectedAndClosesDetails() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect { } }
        viewModel.selectReview(moderationReview.id)
        coEvery { reviewRepository.updateReview(any()) } just Runs

        viewModel.rejectSelectedReview()
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedReview)
        coVerify { reviewRepository.updateReview(match { it.status == ReviewStatus.REJECTED }) }
    }
}
