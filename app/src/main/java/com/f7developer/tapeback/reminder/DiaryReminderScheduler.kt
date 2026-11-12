package com.f7developer.tapeback.reminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.f7developer.tapeback.MainActivity
import com.f7developer.tapeback.R
import com.f7developer.tapeback.TapeBackApp
import com.f7developer.tapeback.core.Notifications
import com.f7developer.tapeback.domain.model.DiaryReminder
import com.f7developer.tapeback.domain.policy.ReminderPolicy
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Daily "how was your day?" nudge. One unique WorkManager job is always queued for the next
 * occurrence; the worker re-queues the following day after it runs, so the time stays exact
 * across DST changes and needs no exact-alarm permission.
 */
class DiaryReminderScheduler(private val context: Context) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun apply(reminder: DiaryReminder) {
        if (!reminder.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = System.currentTimeMillis()
        val next = ReminderPolicy.nextTrigger(now, reminder.minuteOfDay, ZoneId.systemDefault())
        val request = OneTimeWorkRequestBuilder<DiaryReminderWorker>()
            .setInitialDelay(next - now, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "diary-reminder"
    }
}

class DiaryReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TapeBackApp).container
        val reminder = container.settingsRepository.settings.first().reminder
        if (!reminder.enabled) return Result.success()
        val (start, end) = ReminderPolicy.dayBounds(System.currentTimeMillis(), ZoneId.systemDefault())
        // Already on tape today: stay quiet.
        if (container.recordingRepository.countCreatedBetween(start, end) == 0) post()
        container.reminderScheduler.apply(reminder)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun post() {
        val ctx = applicationContext
        if (!Notifications.canPost(ctx)) return
        val open = PendingIntent.getActivity(
            ctx,
            REQUEST_OPEN,
            Intent(ctx, MainActivity::class.java)
                .setAction(MainActivity.ACTION_DIARY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val record = PendingIntent.getActivity(
            ctx,
            REQUEST_RECORD,
            Intent(ctx, MainActivity::class.java)
                .setAction(MainActivity.ACTION_RECORD)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_tape)
            .setContentTitle(ctx.getString(R.string.notif_reminder_title))
            .setContentText(ctx.getString(R.string.notif_reminder_text))
            .setContentIntent(open)
            .addAction(R.drawable.ic_notif_record, ctx.getString(R.string.notif_reminder_record), record)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFFF8A1F.toInt())
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(Notifications.REMINDER_ID, notification) }
    }

    private companion object {
        const val REQUEST_OPEN = 31
        const val REQUEST_RECORD = 32
    }
}
