package com.rwbot.android.data.remote.yandex

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Тест эндпоинта Yandex GPT: POST /foundationModels/v1/completion, парсинг ответа.
 */
class YandexApiTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var api: YandexApi

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        api = Retrofit.Builder()
            .baseUrl(mockServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandexApi::class.java)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    private val completionResponseJson = """
        {
            "result": {
                "alternatives": [{
                    "message": { "role": "assistant", "text": "Спасибо за отзыв!" },
                    "status": "ALTERNATIVE_STATUS_FINAL"
                }]
            }
        }
    """.trimIndent()

    @Test
    fun complete_correctPathAndParsing() = runTest {
        mockServer.enqueue(MockResponse().setBody(completionResponseJson).setResponseCode(200))
        val request = YandexCompletionRequest(
            modelUri = "gpt://yandexgpt/latest",
            messages = listOf(MessageDto("user", "Текст отзыва"))
        )
        val response = api.complete(request)
        assertTrue(response.isSuccessful)
        val text = response.body()?.result?.alternatives?.firstOrNull()?.message?.text
        assertEquals("Спасибо за отзыв!", text)

        val requestRecord = mockServer.takeRequest()
        assertEquals("POST", requestRecord.method)
        assertEquals("/foundationModels/v1/completion", requestRecord.path)
    }

    @Test
    fun complete_emptyAlternatives_parsed() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"result":{"alternatives":[]}}""").setResponseCode(200))
        val request = YandexCompletionRequest(
            modelUri = "gpt://yandexgpt/latest",
            messages = listOf(MessageDto("user", "x"))
        )
        val response = api.complete(request)
        assertTrue(response.isSuccessful)
        assertNotNull(response.body()?.result)
        assertTrue(response.body()?.result?.alternatives.isNullOrEmpty())
    }
}
