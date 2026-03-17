package com.rwbot.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rwbot.android.data.local.entity.ReviewArchiveEntity

/** DAO для архива отзывов RAG. */
@Dao
interface ReviewArchiveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReviewArchiveEntity)

    /** Все записи архива с эмбеддингами (для поиска похожих). */
    @Query("SELECT * FROM review_archive WHERE embedding IS NOT NULL")
    suspend fun getAllWithEmbeddings(): List<ReviewArchiveEntity>

    /** Последние записи архива по рейтингу (для отзывов без текста). */
    @Query(
        """
        SELECT * FROM review_archive
        WHERE rating = :rating AND responseText IS NOT NULL AND responseText != ''
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentByRating(rating: Int, limit: Int): List<ReviewArchiveEntity>

    @Query("SELECT * FROM review_archive WHERE id = :id")
    suspend fun getById(id: String): ReviewArchiveEntity?

    @Query("SELECT COUNT(*) FROM review_archive")
    suspend fun count(): Int
}
