package com.danielrosehill.localutcwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) {
        val rv = WidgetRenderer.build(c, Prefs.load(c))
        for (id in ids) mgr.updateAppWidget(id, rv)
        scheduleNextTransition(c)
        scheduleTick(c)
    }

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_LOCALE_CHANGED, ACTION_REFRESH, ACTION_TICK -> updateAll(c)
            else -> super.onReceive(c, intent)
        }
    }

    override fun onDisabled(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(refreshIntent(c))
        am.cancel(tickIntent(c))
    }

    companion object {
        const val ACTION_REFRESH = "com.danielrosehill.localutcwidget.REFRESH"
        const val ACTION_TICK = "com.danielrosehill.localutcwidget.TICK"

        fun updateAll(c: Context) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, ClockWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val rv = WidgetRenderer.build(c, Prefs.load(c))
            for (id in ids) mgr.updateAppWidget(id, rv)
            scheduleNextTransition(c)
            scheduleTick(c)
        }

        private fun tickIntent(c: Context): PendingIntent = PendingIntent.getBroadcast(
            c, 2,
            Intent(c, ClockWidgetProvider::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * Bundled fonts are drawn as bitmaps, so they need a redraw at each minute boundary.
         * Non-wakeup RTC: no ticks while the screen is off; an overdue tick fires as soon as
         * the device wakes. System fonts use TextClock and need no tick, so it is cancelled.
         */
        private fun scheduleTick(c: Context) {
            val am = c.getSystemService(AlarmManager::class.java)
            val pi = tickIntent(c)
            if (Prefs.load(c).font.fontRes == 0) { am.cancel(pi); return }
            val at = (System.currentTimeMillis() / 60_000 + 1) * 60_000 + 200
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExact(AlarmManager.RTC, at, pi)
            } else {
                am.setWindow(AlarmManager.RTC, at, 5_000, pi)
            }
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
