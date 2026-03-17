package com.rwbot.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность Room: архив отзывов для RAG.
 * Хранит текст отзыва, ответ и вектор эмбеддинга для поиска похожих по косинусному сходству.
 */
@Entity(tableName = "review_archive")
data class ReviewArchiveEntity(
    @PrimaryKey
    /** ID отзыва (совпадает с id в таблице reviews). */
    val id: String,
    /** Текст отзыва. */
    val reviewText: String,
    /** Оценка (1..5). 0 — если неизвестно (например, старые записи до миграции). */
    val rating: Int,
    /** Сгенерированный ответ (для контекста в промпте GPT). */
    val responseText: String?,
    /** Вектор эмбеддинга (хранится как BLOB через TypeConverter). Размерность задаётся моделью Yandex. */
    val embedding: FloatArray?,
    /** Время добавления в архив. */
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ReviewArchiveEntity
        if (id != other.id) return false
        if (reviewText != other.reviewText) return false
        if (rating != other.rating) return false
        if (responseText != other.responseText) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (createdAt != other.createdAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + reviewText.hashCode()
        result = 31 * result + rating
        result = 31 * result + (responseText?.hashCode() ?: 0)
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
