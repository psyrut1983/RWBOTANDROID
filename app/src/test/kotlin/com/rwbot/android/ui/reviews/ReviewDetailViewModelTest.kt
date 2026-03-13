package com.rwbot.android.ui.reviews

import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.domain.pipeline.PipelineResult
import com.rwbot.android.domain.pipeline.ReviewPipeline
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.rwbot.android.util.MainCoroutineRule

/**
 * Тесты ReviewDetailViewModel: загрузка отзыва, обработка, одобрение, отклонение.
 */
class ReviewDetailViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var reviewPipeline: ReviewPipeline
    private lateinit var viewModel: ReviewDetailViewModel

    private val reviewNew = ReviewEntity(
        "r1", "Текст", 5, "art", null, null, null,
        ReviewStatus.NEW, null, 0L
    )
    private val reviewOnMod = reviewNew.copy(status = ReviewStatus.ON_MODERATION, generatedResponse = "Ответ")

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("reviewId" to "r1"))
        reviewRepository = mockk(relaxed = true)
        reviewPipeline = mockk(relaxed = true)
    }

    @Test
    fun load_setsReviewInState() = runTest {
        coEvery { reviewRepository.getReviewById("r1") } returns reviewNew
        viewModel = ReviewDetailViewModel(savedStateHandle, reviewRepository, reviewPipeline)
        advanceUntilIdle()
        assertEquals("r1", viewModel.state.value.review?.id)
        assertEquals(ReviewStatus.NEW, viewModel.state.value.review?.status)
    }

    @Test
    fun process_successOnModeration_updatesState() = runTest {
        coEvery { reviewRepository.getReviewById("r1") } returns reviewNew
        coEvery { reviewPipeline.processReview(reviewNew) } returns PipelineResult.OnModeration(reviewOnMod)
        viewModel = ReviewDetailViewModel(savedStateHandle, reviewRepository, reviewPipeline)
        advanceUntilIdle()
        viewModel.process()
        advanceUntilIdle()
        assertEquals("Отзыв на модерации", viewModel.state.value.message)
        assertEquals(ReviewStatus.ON_MODERATION, viewModel.state.value.review?.status)
    }

    @Test
    fun approve_success_updatesState() = runTest {
        coEvery { reviewRepository.getReviewById("r1") } returns reviewOnMod
        coEvery { reviewRepository.sendAnswerToWildberries("r1", "Ответ") } returns Result.Success(Unit)
        coEvery { reviewRepository.updateReview(any()) } just Runs
        viewModel = ReviewDetailViewModel(savedStateHandle, reviewRepository, reviewPipeline)
        advanceUntilIdle()
        viewModel.approve()
        advanceUntilIdle()
        assertEquals("Отправлено", viewModel.state.value.message)
        assertEquals(ReviewStatus.ANSWERED, viewModel.state.value.review?.status)
    }

    @Test
    fun reject_updatesReviewToRejected() = runTest {
        coEvery { reviewRepository.getReviewById("r1") } returns reviewOnMod
        coEvery { reviewRepository.updateReview(any()) } just Runs
        viewModel = ReviewDetailViewModel(savedStateHandle, reviewRepository, reviewPipeline)
        advanceUntilIdle()
        viewModel.reject()
        advanceUntilIdle()
        assertEquals("Отклонено", viewModel.state.value.message)
        assertEquals(ReviewStatus.REJECTED, viewModel.state.value.review?.status)
        coVerify { reviewRepository.updateReview(match { it.status == ReviewStatus.REJECTED }) }
    }
}
