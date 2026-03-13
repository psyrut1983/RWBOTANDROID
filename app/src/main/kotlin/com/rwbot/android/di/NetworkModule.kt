package com.rwbot.android.di

import com.rwbot.android.BuildConfig
import com.rwbot.android.data.local.SecureSettings
import com.rwbot.android.data.remote.wb.WildberriesApi
import com.rwbot.android.data.remote.yandex.YandexApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Таймаут берём из BuildConfig, он задаётся в Gradle через NETWORK_TIMEOUT_SECONDS
    private val TIMEOUT_SECONDS: Long = BuildConfig.NETWORK_TIMEOUT_SECONDS.toLong()

    @Provides
    @Singleton
    @Named("wb")
    fun provideWbOkHttp(secureSettings: SecureSettings): OkHttpClient {
        // В релизе не логируем тело/заголовки, чтобы не выводить токены
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.BUILD_TYPE == "debug") HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = secureSettings.wbApiToken
                val request = chain.request().newBuilder()
                // WB API отзывов требует заголовок Authorization (ответ 401: "empty Authorization header").
                // JWT-токен передаём как "Bearer <token>".
                if (!token.isNullOrBlank()) {
                    val authValue = if (token.trim().startsWith("Bearer ")) token.trim() else "Bearer ${token.trim()}"
                    request.addHeader("Authorization", authValue)
                }
                chain.proceed(request.build())
            }
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWildberriesApi(@Named("wb") client: OkHttpClient): WildberriesApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.WB_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WildberriesApi::class.java)
    }

    @Provides
    @Singleton
    @Named("yandex")
    fun provideYandexOkHttp(secureSettings: SecureSettings): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.BUILD_TYPE == "debug") HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                secureSettings.yandexApiKey?.let { request.addHeader("Authorization", "Api-Key $it") }
                secureSettings.yandexFolderId?.let { request.addHeader("x-folder-id", it) }
                chain.proceed(request.build())
            }
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideYandexApi(@Named("yandex") client: OkHttpClient): YandexApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.YANDEX_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YandexApi::class.java)
    }
}
