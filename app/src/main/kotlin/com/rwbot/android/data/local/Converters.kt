package com.rwbot.android.data.local

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Конвертеры Room для хранения вектора эмбеддинга (FloatArray) в BLOB.
 * Формат: 4 байта на каждый float (little-endian).
 */
object Converters {

    @TypeConverter
    @JvmStatic
    fun fromFloatArray(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer
            .allocate(value.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        value.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    @JvmStatic
    fun toFloatArray(blob: ByteArray?): FloatArray? {
        if (blob == null || blob.isEmpty()) return null
        val buffer = ByteBuffer
            .wrap(blob)
            .order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(blob.size / 4)
        for (i in result.indices) {
            result[i] = buffer.float
        }
        return result
    }
}
