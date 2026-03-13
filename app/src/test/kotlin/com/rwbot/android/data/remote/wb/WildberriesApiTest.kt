package com.rwbot.android.data.remote.wb

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
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
 * Тест эндпоинтов Wildberries API через MockWebServer.
 * Проверяет: правильный путь запроса, парсинг ответа GET /api/v1/feedbacks
 * и отправку POST /api/v1/feedbacks/answer с корректным телом.
 */
class WildberriesApiTest {

    private lateinit var mockServer: okhttp3.mockwebserver.MockWebServer
    private lateinit var api: WildberriesApi

    @Before
    fun setUp() {
        mockServer = okhttp3.mockwebserver.MockWebServer()
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
            .create(WildberriesApi::class.java)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    /**
     * Ответ WB для GET /api/v1/feedbacks (минимальный валидный JSON по нашим DTO).
     */
    private val singleFeedbackJson = """
        [{
            "id": "test-feedback-123",
            "text": "Товар хороший, спасибо",
            "productValuation": 5,
            "nmId": 12345678,
            "productDetails": { "nmId": 12345678, "productName": "Товар" },
            "createdDate": "2025-03-01T12:00:00",
            "state": "none",
            "userName": "Покупатель",
            "pros": null,
            "cons": null,
            "answer": null
        }]
    """.trimIndent()

    @Test
    fun getFeedbacks_correctPathAndQuery() = runTest {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody(singleFeedbackJson)
                .setResponseCode(200)
        )
        val response = api.getFeedbacks(take = 50, skip = 0)
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(1, body!!.size)
        assertEquals("test-feedback-123", body[0].id)
        assertEquals("Товар хороший, спасибо", body[0].text)
        assertEquals(5, body[0].productValuation)
        assertEquals(12345678L, body[0].nmId)
        assertEquals("Покупатель", body[0].userName)

        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/api/v1/feedbacks"))
        assertTrue(request.path!!.contains("take=50"))
        assertTrue(request.path!!.contains("skip=0"))
    }

    @Test
    fun getFeedbacks_emptyList_parsedCorrectly() = runTest {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse()
                .setBody("[]")
                .setResponseCode(200)
        )
        val response = api.getFeedbacks(take = 10, skip = 100)
        assertTrue(response.isSuccessful)
        assertNotNull(response.body())
        assertTrue(response.body()!!.isEmpty())
        val request = mockServer.takeRequest()
        assertTrue(request.path!!.contains("take=10"))
        assertTrue(request.path!!.contains("skip=100"))
    }

    @Test
    fun sendAnswer_correctPathAndBody() = runTest {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse().setResponseCode(200)
        )
        val requestBody = WbAnswerRequest(feedbackId = "fb-456", text = "Спасибо за отзыв!")
        val response = api.sendAnswer(requestBody)
        assertTrue(response.isSuccessful)

        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/feedbacks/answer", request.path)
        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains("fb-456"))
        assertTrue(bodyStr.contains("Спасибо за отзыв!"))
    }

    @Test
    fun getFeedbacks_401_returnsUnsuccessful() = runTest {
        mockServer.enqueue(
            okhttp3.mockwebserver.MockResponse().setResponseCode(401)
        )
        val response = api.getFeedbacks()
        assertTrue(!response.isSuccessful)
        assertEquals(401, response.code())
    }
}
