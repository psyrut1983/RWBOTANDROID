package com.rwbot.android.ui.stats

import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.rwbot.android.util.MainCoroutineRule

/**
 * Тесты StatsViewModel: загрузка счётчиков по статусам.
 */
class StatsViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var reviewDao: ReviewDao
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        reviewDao = mockk(relaxed = true)
        coEvery { reviewDao.countByStatus(ReviewStatus.NEW) } returns 10
        coEvery { reviewDao.countByStatus(ReviewStatus.ON_MODERATION) } returns 2
        coEvery { reviewDao.countByStatus(ReviewStatus.ANSWERED) } returns 50
        coEvery { reviewDao.countByStatus(ReviewStatus.REJECTED) } returns 1
        viewModel = StatsViewModel(reviewDao)
    }

    @Test
    fun load_populatesCounts() = runTest {
        advanceUntilIdle()
        assertEquals(63, viewModel.state.value.total)
        assertEquals(50, viewModel.state.value.answered)
        assertEquals(2, viewModel.state.value.onModeration)
        assertEquals(1, viewModel.state.value.rejected)
    }

    @Test
    fun load_afterReload_updatesState() = runTest {
        advanceUntilIdle()
        coEvery { reviewDao.countByStatus(ReviewStatus.ON_MODERATION) } returns 5
        viewModel.load()
        advanceUntilIdle()
        assertEquals(5, viewModel.state.value.onModeration)
    }
}
