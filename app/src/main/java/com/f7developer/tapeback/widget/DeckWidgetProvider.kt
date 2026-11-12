package com.f7developer.tapeback.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.f7developer.tapeback.MainActivity
import com.f7developer.tapeback.R

/** Home-screen mini deck: a big REC key plus shortcuts into the diary and capsules. */
class DeckWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views(context)) }
    }

    private fun views(context: Context): RemoteViews = RemoteViews(context.packageName, R.layout.widget_deck).apply {
        setOnClickPendingIntent(R.id.widget_rec, open(context, MainActivity.ACTION_RECORD, 51))
        setOnClickPendingIntent(R.id.widget_diary, open(context, MainActivity.ACTION_DIARY, 52))
        setOnClickPendingIntent(R.id.widget_capsules, open(context, MainActivity.ACTION_OPEN_CAPSULES, 53))
        setOnClickPendingIntent(R.id.widget_root, open(context, null, 54))
    }

    private fun open(context: Context, action: String?, request: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action ?: Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
