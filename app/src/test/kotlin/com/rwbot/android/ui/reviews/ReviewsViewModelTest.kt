package com.rwbot.android.ui.reviews

import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import com.rwbot.android.util.MainCoroutineRule

/**
 * Тесты ReviewsViewModel: фильтр, синхронизация, сообщения.
 * Один StandardTestDispatcher для Main и для runTest — advanceUntilIdle() продвигает корутины ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainRule = MainCoroutineRule(testDispatcher)

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var viewModel: ReviewsViewModel

    private val sampleReviews = listOf(
        ReviewEntity("1", "Текст", 5, "art", null, null, null, ReviewStatus.NEW, null, 0L)
    )

    @Before
    fun setUp() {
        reviewRepository = mockk(relaxed = true)
        every { reviewRepository.getAllReviewsFlow() } returns flowOf(sampleReviews)
    }

    @Test
    fun initialState_hasReviewsFromFlow() = runTest(testDispatcher) {
        viewModel = ReviewsViewModel(reviewRepository)
        // Важно: stateIn(WhileSubscribed) начинает собирать Flow только при наличии подписчика.
        // В unit-тестах подписчик отсутствует, если мы просто читаем state.value, поэтому держим подписку фоном.
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.reviews.size)
        assertEquals("1", viewModel.state.value.reviews[0].id)
    }

    @Test
    fun setFilter_filtersByStatus() = runTest(testDispatcher) {
        val twoReviews = sampleReviews + ReviewEntity("2", "x", 4, "a", null, null, null, ReviewStatus.ON_MODERATION, null, 0L)
        every { reviewRepository.getAllReviewsFlow() } returns flowOf(twoReviews)
        viewModel = ReviewsViewModel(reviewRepository)
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        viewModel.setFilter(ReviewStatus.ON_MODERATION)
        advanceUntilIdle()
        assertEquals(ReviewStatus.ON_MODERATION, viewModel.state.value.filter)
        assertEquals(1, viewModel.state.value.reviews.size)
    }

    @Test
    fun sync_success_setsMessage() = runTest(testDispatcher) {
        viewModel = ReviewsViewModel(reviewRepository)
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Success(5)
        viewModel.sync()
        advanceUntilIdle()
        assertEquals("Загружено: 5", viewModel.state.value.syncMessage)
    }

    @Test
    fun sync_error_setsMessage() = runTest(testDispatcher) {
        viewModel = ReviewsViewModel(reviewRepository)
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Error("Нет сети")
        viewModel.sync()
        advanceUntilIdle()
        assertEquals("Нет сети", viewModel.state.value.syncMessage)
    }

    @Test
    fun clearMessage_clearsSyncMessage() = runTest(testDispatcher) {
        viewModel = ReviewsViewModel(reviewRepository)
        backgroundScope.launch { viewModel.state.collect { } }
        coEvery { reviewRepository.syncFromWildberries() } returns Result.Success(0)
        viewModel.sync()
        advanceUntilIdle()
        viewModel.clearMessage()
        // После изменения MutableStateFlow нужно “прокрутить” планировщик,
        // чтобы combine/stateIn успели пересчитать state.
        advanceUntilIdle()
        assertNull(viewModel.state.value.syncMessage)
    }
}
