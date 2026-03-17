package com.rwbot.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rwbot.android.data.local.entity.ReviewEntity
import com.rwbot.android.data.local.entity.ReviewStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<ReviewEntity>)

    @Update
    suspend fun update(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE id = :id")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatusFlow(status: ReviewStatus): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<ReviewEntity>

    @Query("SELECT COUNT(*) FROM reviews WHERE status = :status")
    suspend fun countByStatus(status: ReviewStatus): Int

    @Query("SELECT COUNT(*) FROM reviews WHERE status = :status AND updatedAt >= :since")
    suspend fun countByStatusSince(status: ReviewStatus, since: Long): Int

    /** Количество неотвеченных отзывов (NEW + ON_MODERATION) — для бейджа на иконке. */
    @Query("SELECT COUNT(*) FROM reviews WHERE status IN ('NEW','ON_MODERATION')")
    fun getUnansweredCountFlow(): Flow<Int>

    /** Список id отзывов со статусом NEW или ON_MODERATION (для синхронизации с WB). */
    @Query("SELECT id FROM reviews WHERE status IN ('NEW','ON_MODERATION')")
    suspend fun getIdsNewOrOnModeration(): List<String>

    /**
     * Пометить указанные отзывы как ANSWERED (обработаны на WB, в т.ч. с другого устройства).
     */
    @Query("UPDATE reviews SET status = 'ANSWERED', updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun markAsAnsweredByIds(ids: List<String>, updatedAt: Long)
}
