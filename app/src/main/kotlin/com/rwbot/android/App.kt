package com.rwbot.android

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import com.rwbot.android.work.SyncReviewsWorker
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Точка входа приложения. Hilt генерирует компонент внедрения зависимостей. */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleSyncReviews()
    }

    /** Планирует автообновление отзывов в 8:00 и 21:00 (по одному разу в день каждый). */
    private fun scheduleSyncReviews() {
        val wm = WorkManager.getInstance(this)
        listOf(8, 21).forEach { hour ->
            val delayMs = delayUntilNextHour(hour)
            val request = OneTimeWorkRequestBuilder<SyncReviewsWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(androidx.work.workDataOf(SyncReviewsWorker.KEY_SCHEDULE_HOUR to hour))
                .build()
            val name = if (hour == 8) SyncReviewsWorker.WORK_NAME_8 else SyncReviewsWorker.WORK_NAME_21
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
        }
    }

    private fun delayUntilNextHour(hour: Int): Long {
        val now = java.time.ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return java.time.Duration.between(now, next).toMillis()
    }
}
