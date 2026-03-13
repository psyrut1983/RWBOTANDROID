package com.rwbot.android.di

import com.rwbot.android.data.local.dao.ReviewArchiveDao
import com.rwbot.android.data.repository.YandexRepository
import com.rwbot.android.domain.classification.ReviewClassifier
import com.rwbot.android.domain.decision.DecisionEngine
import com.rwbot.android.domain.rag.RagRetriever
import com.rwbot.android.domain.rag.RagRetrieverImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideReviewClassifier(): ReviewClassifier = ReviewClassifier()

    @Provides
    @Singleton
    fun provideDecisionEngine(): DecisionEngine = DecisionEngine()

    @Provides
    @Singleton
    fun provideRagRetriever(
        reviewArchiveDao: ReviewArchiveDao,
        yandexRepository: YandexRepository
    ): RagRetriever = RagRetrieverImpl(reviewArchiveDao, yandexRepository)
}
