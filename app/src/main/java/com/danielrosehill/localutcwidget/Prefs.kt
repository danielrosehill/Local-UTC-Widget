package com.danielrosehill.localutcwidget

import android.content.Context

/** Fonts available to the widget. [key] matches res/layout/widget_<key>.xml (scripts/gen-layouts.sh). */
enum class Font(val key: String, val label: String, val layout: Int) {
    SANS("sans", "Sans (default)", R.layout.widget_sans),
    SANS_LIGHT("sans_light", "Sans light", R.layout.widget_sans_light),
    SANS_MEDIUM("sans_medium", "Sans medium", R.layout.widget_sans_medium),
    CONDENSED("condensed", "Condensed", R.layout.widget_condensed),
    SERIF("serif", "Serif", R.layout.widget_serif),
    MONO("mono", "Monospace", R.layout.widget_mono);

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: SANS
    }
}

/** Clock text size in sp; labels, dates and zone line scale from it. */
enum class Size(val key: String, val label: String, val clockSp: Float) {
    SMALL("small", "Small", 26f),
    MEDIUM("medium", "Medium", 34f),
    LARGE("large", "Large", 44f);

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

/** One settings set, shared by every instance of the widget. */
data class Settings(
    val showDate: Boolean,
    val showOffset: Boolean,
    val showZoneName: Boolean,
    val showLabels: Boolean,
    val font: Font,
    val size: Size,
)

object Prefs {
    private const val FILE = "widget"

    fun load(c: Context): Settings {
        val p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Settings(
            showDate = p.getBoolean("show_date", false),
            showOffset = p.getBoolean("show_offset", true),
            showZoneName = p.getBoolean("show_zone_name", false),
            showLabels = p.getBoolean("show_labels", true),
            font = Font.of(p.getString("font", null)),
            size = Size.of(p.getString("size", null)),
        )
    }

    fun save(c: Context, s: Settings) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("show_date", s.showDate)
            .putBoolean("show_offset", s.showOffset)
            .putBoolean("show_zone_name", s.showZoneName)
            .putBoolean("show_labels", s.showLabels)
            .putString("font", s.font.key)
            .putString("size", s.size.key)
            .apply()
    }
}
