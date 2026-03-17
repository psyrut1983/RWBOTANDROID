package com.rwbot.android.data.repository

import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.remote.wb.WildberriesApi
import com.rwbot.android.data.remote.wb.WbFeedbacksDataDto
import com.rwbot.android.data.remote.wb.WbFeedbacksResponseDto
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot

/**
 * Тесты репозитория отзывов: синхронизация с WB и отправка ответа.
 */
class ReviewRepositoryImplTest {

    private lateinit var wbApi: WildberriesApi
    private lateinit var reviewDao: ReviewDao
    private lateinit var repo: ReviewRepositoryImpl

    @Before
    fun setUp() {
        wbApi = mockk(relaxed = true)
        reviewDao = mockk(relaxed = true)
        every { reviewDao.getAllFlow() } returns flowOf(emptyList())
        every { reviewDao.getByStatusFlow(any()) } returns flowOf(emptyList())
        repo = ReviewRepositoryImpl(wbApi, reviewDao)
    }

    @Test
    fun sync_success_insertsEntities() = runTest {
        val dtoList = listOf(
            com.rwbot.android.data.remote.wb.WbFeedbackDto(
                id = "fb1",
                text = "Текст",
                productValuation = 5,
                nmId = 100L,
                productDetails = null,
                createdDate = null,
                state = null,
                userName = "U",
                pros = null,
                cons = null,
                answer = null
            )
        )
        val responseDto = WbFeedbacksResponseDto(data = WbFeedbacksDataDto(feedbacks = dtoList))
        coEvery { wbApi.getFeedbacks(50, 0) } returns Response.success(responseDto)
        coEvery { reviewDao.getById("fb1") } returns null
        val slot = slot<List<ReviewEntity>>()
        coEvery { reviewDao.insertAll(capture(slot)) } just Runs

        val result = repo.syncFromWildberries()

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data)
        assertEquals(1, slot.captured.size)
        assertEquals("fb1", slot.captured[0].id)
        assertEquals("Текст", slot.captured[0].text)
        assertEquals(5, slot.captured[0].rating)
    }

    @Test
    fun sync_401_returnsError() = runTest {
        coEvery { wbApi.getFeedbacks(50, 0) } returns Response.error(401, "".toResponseBody(null))
        val result = repo.syncFromWildberries()
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("токен"))
    }

    @Test
    fun sendAnswer_success_updatesReview() = runTest {
        coEvery { wbApi.sendAnswer(any()) } returns Response.success(Unit)
        val existing = ReviewEntity("fb1", "t", 5, "art", null, null, null, ReviewStatus.ON_MODERATION, "Ответ", 0L)
        coEvery { reviewDao.getById("fb1") } returns existing
        coEvery { reviewDao.update(any()) } just Runs

        val result = repo.sendAnswerToWildberries("fb1", "Ответ")

        assertTrue(result is Result.Success)
        coVerify { reviewDao.update(match { it.id == "fb1" && it.status == ReviewStatus.ANSWERED }) }
    }

    @Test
    fun getModerationCount_returnsDaoCount() = runTest {
        coEvery { reviewDao.countByStatus(ReviewStatus.ON_MODERATION) } returns 7
        assertEquals(7, repo.getModerationCount())
    }
}
