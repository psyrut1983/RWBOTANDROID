package com.rwbot.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Точка входа приложения. Hilt генерирует компонент внедрения зависимостей. */
@HiltAndroidApp
class App : Application()
