package com.rwbot.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Статус отзыва в локальной БД. */
enum class ReviewStatus {
    /** Новый, не обработан */
    NEW,
    /** На модерации (сгенерирован ответ, ждёт одобрения) */
    ON_MODERATION,
    /** Ответ отправлен в WB */
    ANSWERED,
    /** Ответ отклонён (не отправлять) */
    REJECTED
}

/**
 * Сущность Room: отзыв с Wildberries + локальные поля (статус, сгенерированный ответ).
 */
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val rating: Int,
    /** Артикул Wildberries (nmId) */
    val productArticle: String,
    /** Артикул продавца (для подбора фото по имени файла в product_images) */
    val supplierArticle: String?,
    val authorName: String?,
    val createdDate: String?,
    /** Локальный статус обработки */
    val status: ReviewStatus,
    /** Сгенерированный ответ (если есть) */
    val generatedResponse: String?,
    /** Дата последнего обновления в приложении */
    val updatedAt: Long
)
