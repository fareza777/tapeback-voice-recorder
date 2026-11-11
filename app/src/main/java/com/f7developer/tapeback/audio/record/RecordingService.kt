package com.f7developer.tapeback.audio.record

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.f7developer.tapeback.MainActivity
import com.f7developer.tapeback.R
import com.f7developer.tapeback.TapeBackApp
import com.f7developer.tapeback.core.Notifications
import com.f7developer.tapeback.domain.policy.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service of type `microphone`. It keeps capture alive in the background and hosts the
 * ongoing notification with Pause/Resume and Stop actions. The actual recorder lives in
 * [RecordingController]; this service only provides the process priority and the controls.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller: RecordingController get() = (application as TapeBackApp).container.recordingController
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            controller.state
                .map { NotificationKey(it.status, it.status == RecorderStatus.PAUSED, it.cassetteTitle, it.target?.side?.letter) }
                .distinctUntilChanged()
                .collect { key ->
                    when (key.status) {
                        RecorderStatus.IDLE -> shutdown()
                        RecorderStatus.RECORDING, RecorderStatus.PAUSED, RecorderStatus.STARTING ->
                            if (inForeground) notify(buildNotification())
                        RecorderStatus.SAVING -> Unit
                    }
                }
        }
    }

    private data class NotificationKey(val status: RecorderStatus, val paused: Boolean, val title: String, val side: String?)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> enterForeground()
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_STOP -> controller.stop()
            else -> if (!controller.state.value.isBusy) shutdown()
        }
        return START_NOT_STICKY
    }

    private fun enterForeground() {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, Notifications.RECORDING_ID, buildNotification(), type)
            inForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            controller.failStart(RecorderEvent.Reason.ENGINE)
            shutdown()
            return
        }
        // Foreground first, then capture: required for background mic access on Android 11+.
        controller.beginCapture()
    }

    @SuppressLint("MissingPermission") // guarded by Notifications.canPost
    private fun notify(notification: Notification) {
        if (!Notifications.canPost(this)) return
        runCatching { NotificationManagerCompat.from(this).notify(Notifications.RECORDING_ID, notification) }
    }

    private fun buildNotification(): Notification {
        val state = controller.state.value
        val paused = state.status == RecorderStatus.PAUSED
        val side = state.target?.side?.letter ?: "A"
        val title = getString(if (paused) R.string.notif_recording_paused else R.string.notif_recording_title, side)
        val text = state.cassetteTitle.ifBlank { getString(R.string.app_name) }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, Notifications.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_tape)
            .setContentTitle(title)
            .setContentText(if (paused) "$text · ${TimeFormat.lcd(state.elapsedMs)}" else text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setColor(0xFFE53935.toInt())
        if (!paused && state.status == RecorderStatus.RECORDING) {
            builder.setUsesChronometer(true).setWhen(System.currentTimeMillis() - state.elapsedMs).setShowWhen(true)
        } else {
            builder.setShowWhen(false)
        }
        if (state.isCapturing) {
            builder.addAction(
                if (paused) R.drawable.ic_notif_record else R.drawable.ic_notif_pause,
                getString(if (paused) R.string.action_resume else R.string.action_pause),
                actionIntent(this, if (paused) ACTION_RESUME else ACTION_PAUSE),
            )
            builder.addAction(R.drawable.ic_notif_stop, getString(R.string.action_stop_save), actionIntent(this, ACTION_STOP))
        }
        return builder.build()
    }

    private fun shutdown() {
        if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        // Service teardown is another stop path: make sure audio is finalized and saved.
        if (controller.state.value.isCapturing) controller.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RecordingService"
        const val ACTION_START = "com.f7developer.tapeback.record.START"
        const val ACTION_PAUSE = "com.f7developer.tapeback.record.PAUSE"
        const val ACTION_RESUME = "com.f7developer.tapeback.record.RESUME"
        const val ACTION_STOP = "com.f7developer.tapeback.record.STOP"

        private fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
