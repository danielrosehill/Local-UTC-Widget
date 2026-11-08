package com.danielrosehill.localutcwidget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Builds the widget's RemoteViews.
 *
 * System fonts: the clocks are TextClocks, so the launcher ticks them itself; only the
 * zone/offset line is static text.
 *
 * Bundled fonts: launchers do not load an app's @font resources inside RemoteViews (the
 * text silently falls back to the default face; seen on the Pixel launcher, API 34,
 * 2026-11-02). So those layouts use ImageViews and every piece of text is drawn here into
 * a bitmap. ClockWidgetProvider then redraws once a minute.
 */
object WidgetRenderer {

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val DIM = 0xFFB0BEC5.toInt()
    private const val BLUE = 0xFF81D4FA.toInt()
    private const val PIPE = 0x66FFFFFF

    /** Inline and side-by-side need more width, so their clocks run smaller. */
    private fun clockSp(s: Settings) = s.size.clockSp * if (s.template == Template.STACKED) 1f else 0.8f

    fun build(c: Context, s: Settings, zone: ZoneId = ZoneId.systemDefault()): RemoteViews {
        val rv = RemoteViews(c.packageName, Layouts.of(s.template, s.font))
        val big = clockSp(s)
        val small = s.size.clockSp * 0.36f
        val zoneText = zoneLine(zone, s.showZoneName, s.showOffset)

        if (s.font.fontRes == 0) {
            for (id in intArrayOf(R.id.clock_local, R.id.clock_utc)) {
                rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, big)
            }
            for (id in intArrayOf(R.id.label_local, R.id.label_utc, R.id.zone_local, R.id.date_local, R.id.date_utc)) {
                rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, small)
            }
            if (s.template == Template.PIPE) rv.setTextViewTextSize(R.id.pipe, TypedValue.COMPLEX_UNIT_SP, big)
            rv.setTextViewText(R.id.zone_local, zoneText)
            for (id in intArrayOf(R.id.date_local, R.id.date_utc)) {
                rv.setCharSequence(id, "setFormat24Hour", s.dateStyle.pattern)
                rv.setCharSequence(id, "setFormat12Hour", s.dateStyle.pattern)
            }
            if (s.template == Template.STACKED) {
                val face = Typeface.create(s.font.systemFamily, Typeface.NORMAL)
                val w = labelWidth(c, small, face)
                rv.setInt(R.id.label_local, "setMinWidth", w)
                rv.setInt(R.id.label_utc, "setMinWidth", w)
            }
        } else {
            val tf = c.resources.getFont(s.font.fontRes)
            val now = Instant.now()
            val local = ZonedDateTime.ofInstant(now, zone)
            val utc = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
            val hm = DateTimeFormatter.ofPattern("HH:mm")
            val day = DateTimeFormatter.ofPattern(s.dateStyle.pattern, Locale.getDefault())
            val labelW = if (s.template == Template.STACKED) labelWidth(c, small, tf) else 0
            fun put(id: Int, text: String, sp: Float, color: Int, spacing: Float = 0f, descent: Boolean = false, minW: Int = 0) =
                rv.setImageViewBitmap(id, textBitmap(c, text, sp, color, tf, spacing, descent, minW))
            put(R.id.clock_local, local.format(hm), big, WHITE)
            put(R.id.clock_utc, utc.format(hm), big, BLUE)
            put(R.id.label_local, c.getString(R.string.label_local), small, DIM, 0.1f, minW = labelW)
            put(R.id.label_utc, c.getString(R.string.label_utc), small, BLUE, 0.1f, minW = labelW)
            if (s.showDate) {
                put(R.id.date_local, local.format(day), small, DIM, descent = true)
                put(R.id.date_utc, utc.format(day), small, BLUE, descent = true)
            }
            if (zoneText.isNotEmpty()) put(R.id.zone_local, zoneText, small, DIM, descent = true)
            if (s.template == Template.PIPE) put(R.id.pipe, "|", big, PIPE)
        }

        val labels = if (s.showLabels) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.label_local, labels)
        rv.setViewVisibility(R.id.label_utc, labels)

        val dates = if (s.showDate) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.date_local, dates)
        rv.setViewVisibility(R.id.date_utc, dates)

        rv.setViewVisibility(R.id.zone_local, if (zoneText.isEmpty()) View.GONE else View.VISIBLE)

        val open = PendingIntent.getActivity(
            c, 0, Intent(c, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        rv.setOnClickPendingIntent(R.id.root, open)
        return rv
    }

    /**
     * Text drawn tight to its width. Without [descent] the bitmap ends at the baseline, so
     * ImageViews with baselineAlignBottom line up label and clock on a shared baseline.
     */
    private fun textBitmap(
        c: Context, text: String, sp: Float, color: Int, tf: Typeface,
        spacing: Float, descent: Boolean, minW: Int = 0,
    ): Bitmap {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, c.resources.displayMetrics)
            this.color = color
            letterSpacing = spacing
        }
        val fm = paint.fontMetrics
        val w = ceil(paint.measureText(text)).toInt().coerceAtLeast(minW).coerceAtLeast(1)
        val h = ceil(-fm.ascent + if (descent) fm.descent else 0f).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, 0f, -fm.ascent, paint)
        return bmp
    }

    private fun labelPaint(c: Context, sp: Float, tf: Typeface) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        typeface = tf
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, c.resources.displayMetrics)
        letterSpacing = 0.1f
    }

    /** Width of the wider of the two labels, so the stacked clocks share one left edge. */
    private fun labelWidth(c: Context, sp: Float, tf: Typeface): Int {
        val p = labelPaint(c, sp, tf)
        return ceil(maxOf(p.measureText(c.getString(R.string.label_local)), p.measureText(c.getString(R.string.label_utc)))).toInt() + 1
    }

    /** e.g. "Jerusalem · +2". Empty when both parts are off. */
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

    /** Hours from UTC, no "UTC" prefix: "+2", "-5", "+5.5", "+5.75", "±0". */
    fun formatOffset(totalSeconds: Int): String {
        if (totalSeconds == 0) return "±0"
        val sign = if (totalSeconds < 0) "-" else "+"
        val hours = java.math.BigDecimal(abs(totalSeconds)).divide(java.math.BigDecimal(3600), 2, java.math.RoundingMode.HALF_UP)
        return sign + hours.stripTrailingZeros().toPlainString()
    }
}
