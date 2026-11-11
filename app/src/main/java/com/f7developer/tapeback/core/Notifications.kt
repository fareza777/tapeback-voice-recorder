package com.f7developer.tapeback.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.f7developer.tapeback.R

object Notifications {
    const val CHANNEL_RECORDING = "recording"
    const val CHANNEL_CAPSULES = "capsules"
    const val RECORDING_ID = 1001
    const val CAPSULE_ID_BASE = 20_000

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_RECORDING,
                    context.getString(R.string.channel_recording),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.channel_recording_desc)
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_CAPSULES,
                    context.getString(R.string.channel_capsules),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.channel_capsules_desc) },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
