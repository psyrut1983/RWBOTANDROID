package com.rwbot.android.data.remote.wb

import com.google.gson.annotations.SerializedName

/** Тело запроса на отправку ответа на отзыв (WB API). */
data class WbAnswerRequest(
    @SerializedName("id") val feedbackId: String,
    @SerializedName("text") val text: String
)
