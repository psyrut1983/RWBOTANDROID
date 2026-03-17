package com.rwbot.android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rwbot.android.ui.MainActivity

/**
 * Показывает на иконке приложения число неотвеченных отзывов (бейдж).
 * На большинстве устройств бейдж берётся из уведомления: показываем одно
 * тихое уведомление с числом; при 0 — уведомление снимаем, бейдж исчезает.
 */
object BadgeHelper {

    private const val CHANNEL_ID = "reviews_badge"
    private const val NOTIFICATION_ID = 1001

    /**
     * Обновить бейдж: при count > 0 показать/обновить уведомление с числом,
     * при count == 0 — убрать уведомление.
     */
    @JvmStatic
    fun updateBadge(context: Context, count: Int) {
        ensureChannel(context)
        if (count <= 0) {
            cancelNotification(context)
            return
        }
        val title = if (count == 1) "1 неотвеченный отзыв" else "Неотвеченных отзывов: $count"
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Нажмите, чтобы открыть приложение")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setNumber(count)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) { /* нет разрешения POST_NOTIFICATIONS */ }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Отзывы",
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(true) }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun cancelNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: SecurityException) { }
    }
}
