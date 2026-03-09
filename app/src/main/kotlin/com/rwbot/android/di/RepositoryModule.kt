package com.rwbot.android.di

import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.data.repository.ReviewRepositoryImpl
import com.rwbot.android.data.repository.YandexRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
}
