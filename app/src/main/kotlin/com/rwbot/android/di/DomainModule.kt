package com.rwbot.android.di

import com.rwbot.android.domain.classification.ReviewClassifier
import com.rwbot.android.domain.decision.DecisionEngine
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
}
