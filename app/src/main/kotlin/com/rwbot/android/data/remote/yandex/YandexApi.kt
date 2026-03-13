package com.rwbot.android.data.remote.yandex

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit-интерфейс для Yandex GPT (completion) и Embeddings (RAG).
 * Заголовки Authorization (Api-Key) и x-folder-id добавляются через Interceptor.
 */
interface YandexApi {

    @POST("foundationModels/v1/completion")
    suspend fun complete(@Body body: YandexCompletionRequest): Response<YandexCompletionResponse>

    /**
     * Эмбеддинг одного текста для RAG.
     * Путь уточнять по актуальной документации: https://cloud.yandex.ru/docs/yandexgpt/embeddings/api-ref/Embeddings/textEmbedding
     */
    @POST("foundationModels/v1/textEmbedding")
    suspend fun textEmbedding(@Body body: YandexEmbeddingRequest): Response<YandexEmbeddingResponse>
}
