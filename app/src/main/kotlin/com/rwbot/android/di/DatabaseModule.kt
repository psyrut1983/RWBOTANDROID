package com.rwbot.android.di

import android.content.Context
import androidx.room.Room
import com.rwbot.android.data.local.AppDatabase
import com.rwbot.android.data.local.MIGRATION_1_2
import com.rwbot.android.data.local.MIGRATION_2_3
import com.rwbot.android.data.local.MIGRATION_3_4
import com.rwbot.android.data.local.dao.ReviewArchiveDao
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }

    @Provides
    @Singleton
    fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()

    @Provides
    @Singleton
    fun provideReviewArchiveDao(db: AppDatabase): ReviewArchiveDao = db.reviewArchiveDao()
}
