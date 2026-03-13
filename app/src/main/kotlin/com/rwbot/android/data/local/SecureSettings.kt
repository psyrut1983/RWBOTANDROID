package com.rwbot.android.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Безопасное хранение секретов и настроек (токены, ключи, пороги, blacklist).
 * Пытается использовать EncryptedSharedPreferences; при ошибке (эмулятор без lock screen, Keystore) — обычные SharedPreferences.
 */
@Singleton
class SecureSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "rwbot_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("rwbot_secure_prefs", Context.MODE_PRIVATE)
    }

    // Секреты (не логировать)
    var wbApiToken: String?
        get() = prefs.getString(KEY_WB_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_WB_TOKEN, value).apply()

    var yandexApiKey: String?
        get() = prefs.getString(KEY_YANDEX_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_YANDEX_API_KEY, value).apply()

    var yandexFolderId: String?
        get() = prefs.getString(KEY_YANDEX_FOLDER_ID, null)
        set(value) = prefs.edit().putString(KEY_YANDEX_FOLDER_ID, value).apply()

    // Пороги
    var complexityThreshold: Int
        get() = prefs.getInt(KEY_COMPLEXITY_THRESHOLD, 4)
        set(value) = prefs.edit().putInt(KEY_COMPLEXITY_THRESHOLD, value).apply()

    var confidenceThreshold: Double
        get() = prefs.getFloat(KEY_CONFIDENCE_THRESHOLD, 0.8f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CONFIDENCE_THRESHOLD, value.toFloat()).apply()

    var minRatingForAutoResponse: Int
        get() = prefs.getInt(KEY_MIN_RATING, 3)
        set(value) = prefs.edit().putInt(KEY_MIN_RATING, value).apply()

    // Blacklist: сохраняем как множество строк (через запятую или JSON)
    var blacklistWords: Set<String>
        get() = prefs.getString(KEY_BLACKLIST, null)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        set(value) = prefs.edit().putString(KEY_BLACKLIST, value.joinToString(",")).apply()

    fun hasWbToken(): Boolean = !wbApiToken.isNullOrBlank()
    fun hasYandexCredentials(): Boolean = !yandexApiKey.isNullOrBlank() && !yandexFolderId.isNullOrBlank()

    companion object {
        private const val KEY_WB_TOKEN = "wb_api_token"
        private const val KEY_YANDEX_API_KEY = "yandex_api_key"
        private const val KEY_YANDEX_FOLDER_ID = "yandex_folder_id"
        private const val KEY_COMPLEXITY_THRESHOLD = "complexity_threshold"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_MIN_RATING = "min_rating_auto"
        private const val KEY_BLACKLIST = "blacklist"
    }
}
