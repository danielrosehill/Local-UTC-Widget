package com.danielrosehill.localutcwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Builds the widget's RemoteViews. The clocks are TextClocks, so the launcher ticks them
 * every minute itself; only the zone/offset line is static text, refreshed on time-zone
 * change, TIME_SET, the 30-min updatePeriod and an alarm at the next DST transition.
 */
object WidgetRenderer {

    fun build(c: Context, s: Settings, zone: ZoneId = ZoneId.systemDefault()): RemoteViews {
        val rv = RemoteViews(c.packageName, s.font.layout)

        val big = s.size.clockSp
        val small = big * 0.36f
        for (id in intArrayOf(R.id.clock_local, R.id.clock_utc)) {
            rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, big)
        }
        for (id in intArrayOf(R.id.label_local, R.id.label_utc, R.id.zone_local, R.id.date_local, R.id.date_utc)) {
            rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, small)
        }

        val labels = if (s.showLabels) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.label_local, labels)
        rv.setViewVisibility(R.id.label_utc, labels)

        val dates = if (s.showDate) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.date_local, dates)
        rv.setViewVisibility(R.id.date_utc, dates)

        val zoneText = zoneLine(zone, s.showZoneName, s.showOffset)
        rv.setTextViewText(R.id.zone_local, zoneText)
        rv.setViewVisibility(R.id.zone_local, if (zoneText.isEmpty()) View.GONE else View.VISIBLE)

        val open = PendingIntent.getActivity(
            c, 0, Intent(c, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        rv.setOnClickPendingIntent(R.id.root, open)
        return rv
    }

    /** e.g. "Jerusalem · UTC+02:00". Empty when both parts are off. */
    fun zoneLine(zone: ZoneId, name: Boolean, offset: Boolean): String {
        val parts = mutableListOf<String>()
        if (name) parts += zoneName(zone)
        if (offset) parts += formatOffset(zone.rules.getOffset(Instant.now()).totalSeconds)
        return parts.joinToString(" · ")
    }

    private fun zoneName(zone: ZoneId): String {
        val id = zone.id
        return if (id.contains('/')) id.substringAfterLast('/').replace('_', ' ')
        else zone.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun formatOffset(totalSeconds: Int): String {
        if (totalSeconds == 0) return "UTC±00:00"
        val sign = if (totalSeconds < 0) "-" else "+"
        val a = abs(totalSeconds)
        return "UTC%s%02d:%02d".format(sign, a / 3600, (a % 3600) / 60)
    }
}
