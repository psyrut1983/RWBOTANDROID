package com.rwbot.android.data.repository

import com.rwbot.android.data.remote.yandex.YandexApi
import com.rwbot.android.data.remote.yandex.YandexCompletionRequest
import com.rwbot.android.data.remote.yandex.CompletionOptionsDto
import com.rwbot.android.data.remote.yandex.MessageDto
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Репозиторий для вызова Yandex GPT (генерация ответа на отзыв). */
@Singleton
class YandexRepository @Inject constructor(
    private val yandexApi: YandexApi
) {

    /** Модель по умолчанию (уточнить по документации Yandex). */
    private val defaultModelUri = "gpt://yandexgpt/latest"

    suspend fun generateResponse(reviewText: String, systemPrompt: String = DEFAULT_SYSTEM_PROMPT): Result<String> {
        return try {
            val request = YandexCompletionRequest(
                modelUri = defaultModelUri,
                completionOptions = CompletionOptionsDto(temperature = 0.6, maxTokens = "500"),
                messages = listOf(
                    MessageDto(role = "system", text = systemPrompt),
                    MessageDto(role = "user", text = reviewText)
                )
            )
            val response = yandexApi.complete(request)
            if (!response.isSuccessful) {
                val code = response.code()
                return Result.Error(
                    when (code) {
                        401, 403 -> "Проверьте API-ключ и folder_id в настройках"
                        429 -> "Превышена квота Yandex. Подождите."
                        else -> "Ошибка Yandex GPT: $code"
                    },
                    HttpException(response)
                )
            }
            val text = response.body()?.result?.alternatives?.firstOrNull()?.message?.text
                ?: return Result.Error("Пустой ответ от Yandex GPT", null)
            Result.Success(text)
        } catch (e: IOException) {
            Result.Error("Нет сети или таймаут", e)
        } catch (e: HttpException) {
            Result.Error("Ошибка: ${e.code()}", e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка генерации", e)
        }
    }

    companion object {
        private const val DEFAULT_SYSTEM_PROMPT =
            "Ты помощник продавца на маркетплейсе. Отвечай на отзыв покупателя кратко и вежливо. " +
            "Благодари за отзыв, при изложении проблемы — предложи решение или контакты поддержки. Не придумывай факты."
    }
}
