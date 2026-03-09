package com.rwbot.android.data.remote.yandex

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit-интерфейс для Yandex GPT (completion).
 * Заголовки Authorization (Api-Key) и x-folder-id добавляются через Interceptor.
 */
interface YandexApi {

    @POST("foundationModels/v1/completion")
    suspend fun complete(@Body body: YandexCompletionRequest): Response<YandexCompletionResponse>
}
