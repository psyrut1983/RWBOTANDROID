package com.rwbot.android.data.remote.wb

import com.google.gson.annotations.SerializedName

/** DTO отзыва из Wildberries API (поля по документации WB). */
data class WbFeedbackDto(
    @SerializedName("id") val id: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("productValuation") val productValuation: Int?,
    @SerializedName("nmId") val nmId: Long?,
    @SerializedName("productDetails") val productDetails: ProductDetailsDto?,
    @SerializedName("createdDate") val createdDate: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("userName") val userName: String?,
    @SerializedName("pros") val pros: String?,
    @SerializedName("cons") val cons: String?,
    @SerializedName("answer") val answer: AnswerDto?
)

data class ProductDetailsDto(
    @SerializedName("nmId") val nmId: Long?,
    @SerializedName("productName") val productName: String?,
    /** Артикул продавца (название файла фото в product_images) */
    @SerializedName("vendorCode") val vendorCode: String? = null,
    @SerializedName("supplierArticle") val supplierArticle: String? = null
)

data class AnswerDto(
    @SerializedName("text") val text: String?,
    @SerializedName("state") val state: String?
)

/**
 * Обёртка ответа GET /api/v1/feedbacks.
 * WB возвращает { "data": { "feedbacks": [ ... ] } }, а не массив напрямую.
 */
data class WbFeedbacksResponseDto(
    @SerializedName("data") val data: WbFeedbacksDataDto?
)

data class WbFeedbacksDataDto(
    @SerializedName("feedbacks") val feedbacks: List<WbFeedbackDto>?
)
