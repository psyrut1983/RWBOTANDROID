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

    @Query("SELECT * FROM review_archive WHERE id = :id")
    suspend fun getById(id: String): ReviewArchiveEntity?

    @Query("SELECT COUNT(*) FROM review_archive")
    suspend fun count(): Int
}
