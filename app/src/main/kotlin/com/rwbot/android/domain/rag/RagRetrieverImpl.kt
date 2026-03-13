package com.rwbot.android.domain.rag

import com.rwbot.android.data.local.dao.ReviewArchiveDao
import com.rwbot.android.data.local.entity.ReviewArchiveEntity
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.YandexRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация RAG: эмбеддинги через Yandex API, хранение в Room.
 * Поиск похожих по косинусному сходству (для нормализованных векторов — скалярное произведение).
 */
@Singleton
class RagRetrieverImpl @Inject constructor(
    private val reviewArchiveDao: ReviewArchiveDao,
    private val yandexRepository: YandexRepository
) : RagRetriever {

    override suspend fun findSimilar(reviewText: String, limit: Int): List<RagContextItem> {
        if (reviewText.isBlank() || limit <= 0) return emptyList()
        val queryEmbedding = when (val r = yandexRepository.embed(reviewText)) {
            is Result.Success -> r.data
            is Result.Error -> return emptyList()
        }
        val archive = reviewArchiveDao.getAllWithEmbeddings()
        if (archive.isEmpty()) return emptyList()
        val withScore = archive.mapNotNull { entity ->
            val emb = entity.embedding ?: return@mapNotNull null
            if (emb.size != queryEmbedding.size) return@mapNotNull null
            val score = cosineSimilarity(queryEmbedding, emb)
            RagContextItem(entity.reviewText, entity.responseText) to score
        }
        return withScore
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    override suspend fun addToArchive(reviewId: String, reviewText: String, responseText: String?) {
        if (reviewText.isBlank()) return
        when (val r = yandexRepository.embed(reviewText)) {
            is Result.Success -> {
                val entity = ReviewArchiveEntity(
                    id = reviewId,
                    reviewText = reviewText,
                    responseText = responseText,
                    embedding = r.data,
                    createdAt = System.currentTimeMillis()
                )
                reviewArchiveDao.insert(entity)
            }
            is Result.Error -> { /* не добавляем без эмбеддинга; можно залогировать */ }
        }
    }

    /**
     * Косинусное сходство: dot(a,b) / (norm(a)*norm(b)).
     * Для уже нормализованных векторов Yandex достаточно dot(a,b).
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i].toDouble() * b[i].toDouble()
            normA += a[i].toDouble() * a[i].toDouble()
            normB += b[i].toDouble() * b[i].toDouble()
        }
        val denom = Math.sqrt(normA) * Math.sqrt(normB)
        return if (denom > 0) dot / denom else 0.0
    }
}
