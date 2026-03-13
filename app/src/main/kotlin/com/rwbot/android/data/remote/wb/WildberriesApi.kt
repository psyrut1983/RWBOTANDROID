package com.rwbot.android.data.remote.wb

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit-интерфейс для Wildberries API (отзывы и ответы).
 * Базовый URL: https://feedbacks-api.wildberries.ru/
 * Заголовок Authorization с токеном добавляется через Interceptor.
 */
interface WildberriesApi {

    @GET("api/v1/feedbacks")
    suspend fun getFeedbacks(
        @Query("take") take: Int = 50,
        @Query("skip") skip: Int = 0,
        // WB не принимает null для isAnswered (\"Плохой формат isAnswered\"), поэтому всегда явно передаём false.
        @Query("isAnswered") isAnswered: Boolean = false
    ): Response<WbFeedbacksResponseDto>

    @POST("api/v1/feedbacks/answer")
    suspend fun sendAnswer(@Body body: WbAnswerRequest): Response<Unit>
}
