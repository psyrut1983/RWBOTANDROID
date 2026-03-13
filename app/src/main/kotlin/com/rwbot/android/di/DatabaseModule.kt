package com.rwbot.android.di

import android.content.Context
import androidx.room.Room
import com.rwbot.android.data.local.AppDatabase
import com.rwbot.android.data.local.MIGRATION_1_2
import com.rwbot.android.data.local.dao.ReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rwbot.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()
}
