package com.f7developer.tapeback.capsule

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
import androidx.work.workDataOf
import com.f7developer.tapeback.MainActivity
import com.f7developer.tapeback.R
import com.f7developer.tapeback.TapeBackApp
import com.f7developer.tapeback.core.Notifications
import com.f7developer.tapeback.data.RecordingRepository
import java.util.concurrent.TimeUnit

/**
 * Schedules a local notification for each sealed capsule with WorkManager, which persists across
 * reboots and needs no exact-alarm permission. Unlocking itself is time-based ([CapsulePolicy]),
 * so a late notification never keeps a capsule locked.
 */
class CapsuleScheduler(
    private val context: Context,
    private val recordings: RecordingRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun schedule(recordingId: Long, unlockAt: Long) {
        val delay = (unlockAt - clock()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<CapsuleUnlockWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_RECORDING_ID to recordingId))
            .addTag(TAG_CAPSULE)
            .build()
        workManager.enqueueUniqueWork(workName(recordingId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(recordingId: Long) {
        workManager.cancelUniqueWork(workName(recordingId))
    }

    /** Defensive re-sync on app start: every pending capsule has exactly one scheduled job. */
    suspend fun resync() {
        recordings.pendingCapsules().forEach { r ->
            val unlockAt = r.capsuleUnlockAt ?: return@forEach
            val policy = if (unlockAt <= clock()) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            val request = OneTimeWorkRequestBuilder<CapsuleUnlockWorker>()
                .setInitialDelay((unlockAt - clock()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_RECORDING_ID to r.id))
                .addTag(TAG_CAPSULE)
                .build()
            workManager.enqueueUniqueWork(workName(r.id), policy, request)
        }
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
        const val TAG_CAPSULE = "capsule"
        fun workName(id: Long) = "capsule-$id"
    }
}

class CapsuleUnlockWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(CapsuleScheduler.KEY_RECORDING_ID, -1L)
        if (id < 0) return Result.failure()
        val container = (applicationContext as TapeBackApp).container
        val repo: RecordingRepository = container.recordingRepository
        val item = repo.get(id) ?: return Result.success() // deleted meanwhile
        val unlockAt = item.recording.capsuleUnlockAt ?: return Result.success() // seal broken
        if (System.currentTimeMillis() < unlockAt) {
            // Woken early (e.g. clock change): reschedule for the remaining time.
            container.capsuleScheduler.schedule(id, unlockAt)
            return Result.success()
        }
        if (!item.recording.capsuleNotified) {
            postNotification(id, item.recording.title, item.cassetteTitle)
            repo.markCapsuleNotified(id)
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(id: Long, title: String, cassette: String) {
        val ctx = applicationContext
        if (!Notifications.canPost(ctx)) return
        val open = PendingIntent.getActivity(
            ctx,
            id.toInt(),
            Intent(ctx, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_CAPSULES)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, Notifications.CHANNEL_CAPSULES)
            .setSmallIcon(R.drawable.ic_stat_tape)
            .setContentTitle(ctx.getString(R.string.notif_capsule_title))
            .setContentText(ctx.getString(R.string.notif_capsule_text, title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(ctx.getString(R.string.notif_capsule_big, title, cassette)))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFFF8A1F.toInt())
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(Notifications.CAPSULE_ID_BASE + (id % 10_000).toInt(), notification)
        }
    }
}
