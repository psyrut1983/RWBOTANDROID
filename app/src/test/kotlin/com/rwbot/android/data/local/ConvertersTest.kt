package com.rwbot.android.data.local

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    @Test
    fun `fromFloatArray - null returns null`() {
        assertNull(Converters.fromFloatArray(null))
    }

    @Test
    fun `toFloatArray - null returns null`() {
        assertNull(Converters.toFloatArray(null))
    }

    @Test
    fun `toFloatArray - empty byte array returns null`() {
        assertNull(Converters.toFloatArray(byteArrayOf()))
    }

    @Test
    fun `floatArray roundtrip сохраняет значения`() {
        val original = floatArrayOf(0f, 1f, -1f, 0.5f, 123.456f)
        val blob = Converters.fromFloatArray(original)

        // Важно для новичков: Float — это число с плавающей точкой, поэтому сравнивать нужно с допуском.
        // Здесь используем assertArrayEquals с delta.
        val restored = Converters.toFloatArray(blob)

        // JUnit4 имеет overload assertArrayEquals(float[], float[], delta)
        assertArrayEquals(original, restored, 0.0001f)
    }

    @Test
    fun `fromFloatArray - использует little-endian формат`() {
        // 1.0f в IEEE754 это 0x3F800000.
        // В little-endian ожидаем байты: 00 00 80 3F
        val blob = Converters.fromFloatArray(floatArrayOf(1.0f))
        requireNotNull(blob)
        assertEquals(4, blob.size)
        assertArrayEquals(
            byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F),
            blob
        )
    }
}

