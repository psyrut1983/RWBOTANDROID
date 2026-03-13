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
            val receivedIds = mutableListOf<String>()
            do {
                val response = wbApi.getFeedbacks(take = take, skip = skip)
                if (!response.isSuccessful) {
                    val code = response.code()
                    val bodyMsg = response.errorBody()?.string()?.take(200)?.let { " — $it" } ?: ""
                    return Result.Error(
                        when (code) {
                            400 -> "Неверный запрос WB (400)$bodyMsg"
                            401 -> "Проверьте токен WB в настройках"
                            429 -> "Слишком много запросов. Подождите."
                            else -> "Ошибка WB API: $code$bodyMsg"
                        },
                        HttpException(response)
                    )
                }
                val list = response.body()?.data?.feedbacks ?: emptyList()
                val entities = mutableListOf<ReviewEntity>()
                for (dto in list) {
                    dto.id?.let { receivedIds.add(it) }
                    dto.toEntity { id -> reviewDao.getById(id) }?.let { entities.add(it) }
                }
                if (entities.isNotEmpty()) {
                    reviewDao.insertAll(entities)
                    total += entities.size
                }
                skip += take
            } while (list.size == take)
            // Отзывы, которых нет в списке неотвеченных с WB (например обработаны с другого устройства),
            // помечаем как ANSWERED, чтобы они исчезали из списка необработанных.
            val receivedSet = receivedIds.toSet()
            val toMarkAnswered = reviewDao.getIdsNewOrOnModeration().filter { it !in receivedSet }
            if (toMarkAnswered.isNotEmpty()) {
                val now = System.currentTimeMillis()
                toMarkAnswered.chunked(500).forEach { chunk ->
                    reviewDao.markAsAnsweredByIds(chunk, now)
                }
            }
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
                val bodyMsg = response.errorBody()?.string()?.take(200)?.let { " — $it" } ?: ""
                return Result.Error(
                    when (code) {
                        400 -> "Неверный запрос WB (400)$bodyMsg"
                        401 -> "Проверьте токен WB в настройках"
                        429 -> "Слишком много запросов"
                        else -> "Ошибка отправки: $code$bodyMsg"
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

private suspend fun WbFeedbackDto.toEntity(getExisting: suspend (String) -> ReviewEntity?): ReviewEntity? {
    val id = id ?: return null
    val existing = getExisting(id)
    val rawSupplier = productDetails?.vendorCode
        ?: productDetails?.supplierArticle
        ?: existing?.supplierArticle
    val supplierArticle = rawSupplier?.takeIf { it.isNotBlank() }
    return ReviewEntity(
        id = id,
        text = text ?: "",
        rating = productValuation ?: 0,
        productArticle = nmId?.toString() ?: productDetails?.nmId?.toString() ?: "",
        supplierArticle = supplierArticle,
        authorName = userName,
        createdDate = createdDate,
        status = existing?.status ?: ReviewStatus.NEW,
        generatedResponse = existing?.generatedResponse ?: answer?.text,
        updatedAt = System.currentTimeMillis()
    )
}
