package com.rwbot.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Модуль Hilt для приложения. Сетевые и БД модули добавятся в фазе 2. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
