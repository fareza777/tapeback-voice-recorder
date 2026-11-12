package com.f7developer.tapeback.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.f7developer.tapeback.BuildConfig
import com.f7developer.tapeback.R

/**
 * Hand-offs to other apps. TapeBack itself has no network access: rating opens the store app (or the
 * browser as a fallback) and sharing goes through the system share sheet.
 */
object AppActions {

    /** The store listing id. Debug builds carry a ".debug" suffix that the store does not know. */
    val storePackage: String get() = BuildConfig.APPLICATION_ID.removeSuffix(".debug")

    val storeWebUrl: String get() = "https://play.google.com/store/apps/details?id=$storePackage"

    /** Opens the Google Play page (Play Store app first, then any store, then the browser). */
    fun rate(context: Context): Boolean {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        val marketUri = Uri.parse("market://details?id=$storePackage")
        val playStore = Intent(Intent.ACTION_VIEW, marketUri).setPackage(PLAY_STORE).addFlags(flags)
        val anyStore = Intent(Intent.ACTION_VIEW, marketUri).addFlags(flags)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse(storeWebUrl)).addFlags(flags)
        return start(context, playStore) || start(context, anyStore) || start(context, web)
    }

    private const val PLAY_STORE = "com.android.vending"

    fun share(context: Context) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, storeWebUrl))
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_app_chooser))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(context, chooser)
    }

    fun openAppSettings(context: Context): Boolean = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    fun openNotificationSettings(context: Context): Boolean = start(
        context,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    ) || openAppSettings(context)

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
