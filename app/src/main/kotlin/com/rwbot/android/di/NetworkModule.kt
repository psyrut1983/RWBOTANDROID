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

    private const val TIMEOUT = 30L

    @Provides
    @Singleton
    @Named("wb")
    fun provideWbOkHttp(secureSettings: SecureSettings): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = secureSettings.wbApiToken
                val request = chain.request().newBuilder()
                if (!token.isNullOrBlank()) {
                    request.addHeader("Authorization", token)
                }
                chain.proceed(request.build())
            }
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
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
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                secureSettings.yandexApiKey?.let { request.addHeader("Authorization", "Api-Key $it") }
                secureSettings.yandexFolderId?.let { request.addHeader("x-folder-id", it) }
                chain.proceed(request.build())
            }
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
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
