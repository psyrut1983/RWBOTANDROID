package com.rwbot.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rwbot.android.data.repository.ReviewRepository
import com.rwbot.android.util.BadgeHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Воркер: синхронизация отзывов с WB по расписанию (8:00 и 21:00).
 * После выполнения перепланирует себя на следующий день в то же время.
 */
@HiltWorker
class SyncReviewsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val reviewRepository: ReviewRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Выполняем синхронизацию с Wildberries (то же, что по кнопке «Обновить отзывы WB»)
        reviewRepository.syncFromWildberries()

        // Обновляем бейдж на иконке по актуальному количеству неотвеченных
        val count = reviewRepository.getUnansweredCountFlow().first()
        BadgeHelper.updateBadge(context, count)

        // Перепланируем на следующий день в тот же час (8 или 21)
        val hour = params.inputData.getInt(KEY_SCHEDULE_HOUR, 8)
        scheduleNext(context, hour)

        Result.success()
    }

    companion object {
        const val KEY_SCHEDULE_HOUR = "schedule_hour"
        const val WORK_NAME_8 = "sync_reviews_08"
        const val WORK_NAME_21 = "sync_reviews_21"

        /** Запланировать следующий запуск через delayMillis до часа hour (0–23). */
        fun scheduleNext(context: Context, hour: Int) {
            val delayMs = delayUntilNextHour(hour)
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<SyncReviewsWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(androidx.work.workDataOf(KEY_SCHEDULE_HOUR to hour))
                .build()
            val workName = if (hour == 8) WORK_NAME_8 else WORK_NAME_21
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        /** Миллисекунды до следующего наступления указанного часа (в локальной тайм-зоне). */
        private fun delayUntilNextHour(hour: Int): Long {
            val now = ZonedDateTime.now()
            var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return java.time.Duration.between(now, next).toMillis()
        }
    }
}
