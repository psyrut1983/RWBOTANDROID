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
    @SerializedName("productName") val productName: String?
)

data class AnswerDto(
    @SerializedName("text") val text: String?,
    @SerializedName("state") val state: String?
)
