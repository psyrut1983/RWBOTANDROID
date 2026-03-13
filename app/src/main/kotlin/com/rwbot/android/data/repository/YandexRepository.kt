package com.rwbot.android.data.repository

import com.rwbot.android.data.local.SecureSettings
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
    private val yandexApi: YandexApi,
    private val secureSettings: SecureSettings
) {

    /** Формат modelUri по документации Yandex: gpt://<folder-id>/yandexgpt/latest */
    private fun modelUri(): String {
        val folderId = secureSettings.yandexFolderId?.trim()?.takeIf { it.isNotEmpty() }
            ?: return "gpt://yandexgpt/latest" // fallback для старых настроек
        return "gpt://$folderId/yandexgpt/latest"
    }

    suspend fun generateResponse(reviewText: String, systemPrompt: String = DEFAULT_SYSTEM_PROMPT): Result<String> {
        // Yandex GPT не принимает пустое сообщение пользователя — возвращает 400 "empty message text"
        val text = reviewText.trim()
        if (text.isEmpty()) {
            return Result.Error("Нельзя сгенерировать ответ для пустого текста отзыва", null)
        }
        return try {
            val request = YandexCompletionRequest(
                modelUri = modelUri(),
                completionOptions = CompletionOptionsDto(temperature = 0.6, maxTokens = "500"),
                messages = listOf(
                    MessageDto(role = "system", text = systemPrompt),
                    MessageDto(role = "user", text = text)
                )
            )
            val response = yandexApi.complete(request)
            if (!response.isSuccessful) {
                val code = response.code()
                val body = response.errorBody()?.string().orEmpty()
                val message = when (code) {
                    400 -> when {
                        body.contains("does not match with service account folder ID") || body.contains("folder") ->
                            "Каталог (Folder ID) в настройках не совпадает с каталогом API-ключа. Укажите тот же каталог, в котором создан ключ в Yandex Cloud."
                        body.contains("empty message text") ->
                            "Текст отзыва пустой. Добавьте текст отзыва для генерации ответа."
                        else -> "Неверный запрос к Yandex GPT (400). Проверьте настройки."
                    }
                    401, 403 -> "Проверьте API-ключ и folder_id в настройках"
                    429 -> "Превышена квота Yandex. Подождите."
                    else -> "Ошибка Yandex GPT: $code"
                }
                return Result.Error(message, HttpException(response))
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
