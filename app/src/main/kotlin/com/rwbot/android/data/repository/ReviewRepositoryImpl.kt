package com.rwbot.android.data.repository

import com.rwbot.android.data.local.dao.ReviewDao
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import com.rwbot.android.data.remote.wb.WbFeedbackDto
import com.rwbot.android.data.remote.wb.WildberriesApi
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Результат операции с сетью/БД. */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val wbApi: WildberriesApi,
    private val reviewDao: ReviewDao
) : ReviewRepository {

    override fun getAllReviewsFlow(): Flow<List<ReviewEntity>> = reviewDao.getAllFlow()

    override fun getReviewsByStatusFlow(status: ReviewStatus): Flow<List<ReviewEntity>> =
        reviewDao.getByStatusFlow(status)

    override suspend fun getReviewById(id: String): ReviewEntity? = reviewDao.getById(id)

    override suspend fun syncFromWildberries(): Result<Int> {
        return try {
            var skip = 0
            val take = 50
            var total = 0
            do {
                val response = wbApi.getFeedbacks(take = take, skip = skip)
                if (!response.isSuccessful) {
                    val code = response.code()
                    return Result.Error(
                        when (code) {
                            401 -> "Проверьте токен WB в настройках"
                            429 -> "Слишком много запросов. Подождите."
                            else -> "Ошибка WB API: $code"
                        },
                        HttpException(response)
                    )
                }
                val list = response.body() ?: emptyList()
                val entities = list.mapNotNull { dto ->
                    dto.toEntity { id -> reviewDao.getById(id) }
                }
                if (entities.isNotEmpty()) {
                    reviewDao.insertAll(entities)
                    total += entities.size
                }
                skip += take
            } while (list.size == take)
            Result.Success(total)
        } catch (e: IOException) {
            Result.Error("Нет сети или таймаут", e)
        } catch (e: HttpException) {
            Result.Error("Ошибка WB: ${e.code()}", e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка загрузки", e)
        }
    }

    override suspend fun sendAnswerToWildberries(feedbackId: String, text: String): Result<Unit> {
        return try {
            val response = wbApi.sendAnswer(com.rwbot.android.data.remote.wb.WbAnswerRequest(feedbackId, text))
            if (!response.isSuccessful) {
                val code = response.code()
                return Result.Error(
                    when (code) {
                        401 -> "Проверьте токен WB в настройках"
                        429 -> "Слишком много запросов"
                        else -> "Ошибка отправки: $code"
                    },
                    HttpException(response)
                )
            }
            val existing = reviewDao.getById(feedbackId)
            if (existing != null) {
                reviewDao.update(existing.copy(status = ReviewStatus.ANSWERED, updatedAt = System.currentTimeMillis()))
            }
            Result.Success(Unit)
        } catch (e: IOException) {
            Result.Error("Нет сети", e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка отправки", e)
        }
    }

    override suspend fun updateReview(review: ReviewEntity) {
        reviewDao.update(review)
    }

    override suspend fun getModerationCount(): Int = reviewDao.countByStatus(ReviewStatus.ON_MODERATION)
}

private fun WbFeedbackDto.toEntity(getExisting: (String) -> ReviewEntity?): ReviewEntity? {
    val id = id ?: return null
    val existing = getExisting(id)
    return ReviewEntity(
        id = id,
        text = text ?: "",
        rating = productValuation ?: 0,
        productArticle = nmId?.toString() ?: productDetails?.nmId?.toString() ?: "",
        authorName = userName,
        createdDate = createdDate,
        status = existing?.status ?: ReviewStatus.NEW,
        generatedResponse = existing?.generatedResponse ?: answer?.text,
        updatedAt = System.currentTimeMillis()
    )
}
