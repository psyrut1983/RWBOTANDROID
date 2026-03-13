package com.rwbot.android.data.remote.yandex

import com.google.gson.annotations.SerializedName

/** Запрос к Yandex GPT completion API. */
data class YandexCompletionRequest(
    @SerializedName("modelUri") val modelUri: String,
    @SerializedName("completionOptions") val completionOptions: CompletionOptionsDto? = null,
    @SerializedName("messages") val messages: List<MessageDto>
)

data class CompletionOptionsDto(
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("temperature") val temperature: Double = 0.6,
    @SerializedName("maxTokens") val maxTokens: String = "500"
)

data class MessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("text") val text: String
)

/** Ответ Yandex GPT (упрощённая структура по документации). */
data class YandexCompletionResponse(
    @SerializedName("result") val result: ResultDto?
)

data class ResultDto(
    @SerializedName("alternatives") val alternatives: List<AlternativeDto>?
)

data class AlternativeDto(
    @SerializedName("message") val message: MessageDto?,
    @SerializedName("status") val status: String?
)

// --- Embeddings API (RAG) ---

/** Запрос к Yandex Embeddings API (textEmbedding). */
data class YandexEmbeddingRequest(
    @SerializedName("modelUri") val modelUri: String,
    @SerializedName("text") val text: String
)

/** Ответ Yandex Embeddings API. Вектор может быть в result.embedding или в embedding. */
data class YandexEmbeddingResponse(
    @SerializedName("result") val result: EmbeddingResultDto? = null,
    @SerializedName("embedding") val embedding: List<Double>? = null
)

data class EmbeddingResultDto(
    @SerializedName("embedding") val embedding: List<Double>? = null,
    @SerializedName("numTokens") val numTokens: Int? = null
)
