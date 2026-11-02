package com.danielrosehill.localutcwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.ZoneId

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        val rv = WidgetRenderer.build(c, Prefs.load(c))
        for (id in ids) mgr.updateAppWidget(id, rv)
        scheduleNextTransition(c)
    }

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED, ACTION_REFRESH -> updateAll(c)
            else -> super.onReceive(c, intent)
        }
    }

    override fun onDisabled(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(refreshIntent(c))
    }

    companion object {
        const val ACTION_REFRESH = "com.danielrosehill.localutcwidget.REFRESH"

        fun updateAll(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, ClockWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val rv = WidgetRenderer.build(c, Prefs.load(c))
            for (id in ids) mgr.updateAppWidget(id, rv)
            scheduleNextTransition(c)
        }

        private fun refreshIntent(c: Context): PendingIntent = PendingIntent.getBroadcast(
            c, 1,
            Intent(c, ClockWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** Inexact alarm just after the next DST/offset change, so the UTC offset line flips on time. */
        private fun scheduleNextTransition(c: Context) {
            val next = ZoneId.systemDefault().rules.nextTransition(Instant.now()) ?: return
            val at = next.instant.toEpochMilli() + 1_000
            c.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC, at, refreshIntent(c))
        }
    }
}
