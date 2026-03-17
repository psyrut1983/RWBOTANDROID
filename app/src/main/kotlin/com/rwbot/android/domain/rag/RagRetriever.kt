package com.rwbot.android.domain.rag

/**
 * Элемент контекста RAG для промпта: похожий отзыв и ответ (пример для GPT).
 */
data class RagContextItem(
    val reviewText: String,
    val responseText: String?
)

/**
 * RAG: поиск похожих отзывов в архиве по эмбеддингам и добавление в архив.
 * Используется в пайплайне перед генерацией ответа и после успешной обработки.
 */
interface RagRetriever {

    /**
     * Найти до [limit] самых похожих отзывов в архиве для контекста GPT.
     * Возвращает пустой список при ошибке или пустом архиве.
     */
    suspend fun findSimilar(reviewText: String, limit: Int = 5): List<RagContextItem>

    /**
     * Найти примеры ответов из архива по той же оценке.
     * Используется, когда у отзыва нет текста (есть только рейтинг).
     */
    suspend fun findByRating(rating: Int, limit: Int = 5): List<RagContextItem>

    /**
     * Добавить отзыв и ответ в архив (с эмбеддингом) для будущего RAG.
     */
    suspend fun addToArchive(reviewId: String, reviewText: String, rating: Int, responseText: String?)
}
