package com.rwbot.android.data.repository

import com.rwbot.android.data.local.SecureSettings
import com.rwbot.android.data.remote.yandex.YandexApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Тесты YandexRepository: генерация ответа, обработка успеха и ошибок.
 */
class YandexRepositoryTest {

    private lateinit var yandexApi: YandexApi
    private lateinit var secureSettings: SecureSettings
    private lateinit var repo: YandexRepository

    @Before
    fun setUp() {
        yandexApi = mockk(relaxed = true)
        secureSettings = mockk(relaxed = true)
        // Важно: в тестах не нужно реальные ключи/Folder ID — достаточно заглушек.
        every { secureSettings.yandexFolderId } returns "test-folder"
        repo = YandexRepository(yandexApi, secureSettings)
    }

    @Test
    fun generateResponse_success_returnsText() = runTest {
        val responseBody = com.rwbot.android.data.remote.yandex.YandexCompletionResponse(
            result = com.rwbot.android.data.remote.yandex.ResultDto(
                alternatives = listOf(
                    com.rwbot.android.data.remote.yandex.AlternativeDto(
                        message = com.rwbot.android.data.remote.yandex.MessageDto("assistant", "Спасибо!"),
                        status = "FINAL"
                    )
                )
            )
        )
        coEvery { yandexApi.complete(any()) } returns Response.success(responseBody)

        val result = repo.generateResponse("Текст отзыва")

        assertTrue(result is Result.Success)
        assertEquals("Спасибо!", (result as Result.Success).data)
    }

    @Test
    fun generateResponse_emptyBody_returnsError() = runTest {
        coEvery { yandexApi.complete(any()) } returns Response.success(null)

        val result = repo.generateResponse("x")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Пустой"))
    }

    @Test
    fun generateResponse_401_returnsError() = runTest {
        coEvery { yandexApi.complete(any()) } returns Response.error(401, "".toResponseBody(null))

        val result = repo.generateResponse("x")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("API-ключ") || result.message.contains("настройках"))
    }
}
