package com.rwbot.android.domain.rag

import com.rwbot.android.data.local.dao.ReviewArchiveDao
import com.rwbot.android.data.local.entity.ReviewArchiveEntity
import com.rwbot.android.data.repository.Result
import com.rwbot.android.data.repository.YandexRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRetrieverImplTest {

    @Test
    fun `findSimilar - blank text returns empty`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>(relaxed = true)
        val rag = RagRetrieverImpl(dao, yandex)

        val result = rag.findSimilar("   ", limit = 5)

        assertTrue(result.isEmpty())
        // Частая ошибка новичков: даже при пустом вводе случайно дергать сеть/БД.
        coVerify(exactly = 0) { yandex.embed(any()) }
        coVerify(exactly = 0) { dao.getAllWithEmbeddings() }
    }

    @Test
    fun `findSimilar - non positive limit returns empty`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>(relaxed = true)
        val rag = RagRetrieverImpl(dao, yandex)

        val result = rag.findSimilar("text", limit = 0)

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { yandex.embed(any()) }
        coVerify(exactly = 0) { dao.getAllWithEmbeddings() }
    }

    @Test
    fun `findSimilar - embed error returns empty and dao not queried`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>()
        coEvery { yandex.embed(any()) } returns Result.Error("fail")
        val rag = RagRetrieverImpl(dao, yandex)

        val result = rag.findSimilar("text", limit = 5)

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { dao.getAllWithEmbeddings() }
    }

    @Test
    fun `findSimilar - returns top by cosine similarity, skips invalid archive rows`() = runTest {
        val dao = mockk<ReviewArchiveDao>()
        val yandex = mockk<YandexRepository>()
        // Вектор запроса
        coEvery { yandex.embed("query") } returns Result.Success(floatArrayOf(1f, 0f))

        // Архив: одна запись без эмбеддинга (должна быть пропущена),
        // одна с неверной размерностью (должна быть пропущена),
        // и две валидные (должны быть отсортированы).
        val archive = listOf(
            ReviewArchiveEntity(
                id = "no_emb",
                reviewText = "A",
                responseText = "RA",
                embedding = null,
                createdAt = 1L
            ),
            ReviewArchiveEntity(
                id = "bad_dim",
                reviewText = "B",
                responseText = "RB",
                embedding = floatArrayOf(1f, 2f, 3f),
                createdAt = 2L
            ),
            // Косинус с (1,0):
            // (1,0) -> 1
            ReviewArchiveEntity(
                id = "best",
                reviewText = "BEST_REVIEW",
                responseText = "BEST_RESPONSE",
                embedding = floatArrayOf(1f, 0f),
                createdAt = 3L
            ),
            // Косинус с (1,0):
            // (0,1) -> 0
            ReviewArchiveEntity(
                id = "worst",
                reviewText = "WORST_REVIEW",
                responseText = "WORST_RESPONSE",
                embedding = floatArrayOf(0f, 1f),
                createdAt = 4L
            )
        )
        coEvery { dao.getAllWithEmbeddings() } returns archive

        val rag = RagRetrieverImpl(dao, yandex)
        val result = rag.findSimilar("query", limit = 1)

        assertEquals(1, result.size)
        assertEquals("BEST_REVIEW", result[0].reviewText)
        assertEquals("BEST_RESPONSE", result[0].responseText)
    }

    @Test
    fun `addToArchive - blank reviewText does nothing`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>(relaxed = true)
        val rag = RagRetrieverImpl(dao, yandex)

        rag.addToArchive("id", "   ", "resp")

        coVerify(exactly = 0) { yandex.embed(any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `addToArchive - embed error does not insert`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>()
        coEvery { yandex.embed(any()) } returns Result.Error("fail")
        val rag = RagRetrieverImpl(dao, yandex)

        rag.addToArchive("id1", "text", "resp")

        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `addToArchive - embed success inserts entity with embedding`() = runTest {
        val dao = mockk<ReviewArchiveDao>(relaxed = true)
        val yandex = mockk<YandexRepository>()
        val embedding = floatArrayOf(0.1f, 0.2f)
        coEvery { yandex.embed("hello") } returns Result.Success(embedding)

        val rag = RagRetrieverImpl(dao, yandex)
        rag.addToArchive("rid", "hello", "world")

        val captured = slot<ReviewArchiveEntity>()
        coVerify(exactly = 1) { dao.insert(capture(captured)) }

        assertEquals("rid", captured.captured.id)
        assertEquals("hello", captured.captured.reviewText)
        assertEquals("world", captured.captured.responseText)
        assertTrue(captured.captured.embedding!!.contentEquals(embedding))
        assertTrue(
            "createdAt должен быть установлен текущим временем",
            captured.captured.createdAt > 0
        )
    }
}

