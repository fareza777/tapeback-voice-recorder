package com.f7developer.tapeback.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Live microphone / notification permission state, refreshed whenever the screen resumes. */
class PermissionSnapshot(val microphone: Boolean, val notifications: Boolean)

fun readPermissions(context: Context): PermissionSnapshot = PermissionSnapshot(
    microphone = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
    notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
)

class PermissionStateHolder(initial: PermissionSnapshot, private val context: Context) {
    var value by mutableStateOf(initial)
        private set

    fun refresh() {
        value = readPermissions(context)
    }
}

@Composable
fun rememberPermissionState(): PermissionStateHolder {
    val context = LocalContext.current
    val holder = remember(context) { PermissionStateHolder(readPermissions(context), context) }
    LifecycleResumeEffect(holder) {
        holder.refresh()
        onPauseOrDispose { }
    }
    return holder
}
