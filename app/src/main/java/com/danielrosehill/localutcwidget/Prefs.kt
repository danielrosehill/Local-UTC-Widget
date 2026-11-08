package com.danielrosehill.localutcwidget

import android.content.Context

/** Clock text size in sp; labels, dates and zone line scale from it. */
enum class Size(val key: String, val label: String, val clockSp: Float) {
    SMALL("small", "Small", 26f),
    MEDIUM("medium", "Medium", 34f),
    LARGE("large", "Large", 44f);

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

/** Date line format. Patterns work for both TextClock and java.time. Default drops the year. */
enum class DateStyle(val key: String, val pattern: String, val hint: String) {
    SHORT("short", "EEE d MMM", ""),
    LONG("long", "EEE d MMM yyyy", ""),
    DM("dm", "dd/MM", "day/month"),
    MD("md", "MM/dd", "month/day"),
    DMY("dmy", "dd/MM/yyyy", "day/month/year"),
    MDY("mdy", "MM/dd/yyyy", "month/day/year"),
    ISO("iso", "yyyy-MM-dd", "ISO"),
    ;
    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: SHORT
    }
}

/** One settings set, shared by every instance of the widget. */
data class Settings(
    val showDate: Boolean,
    val showOffset: Boolean,
    val showZoneName: Boolean,
    val showLabels: Boolean,
    val font: Font,
    val template: Template,
    val size: Size,
    val dateStyle: DateStyle = DateStyle.SHORT,
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
            template = Template.of(p.getString("template", null)),
            size = Size.of(p.getString("size", null)),
            dateStyle = DateStyle.of(p.getString("date_style", null)),
        )
    }

    fun save(c: Context, s: Settings) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("show_date", s.showDate)
            .putBoolean("show_offset", s.showOffset)
            .putBoolean("show_zone_name", s.showZoneName)
            .putBoolean("show_labels", s.showLabels)
            .putString("font", s.font.key)
            .putString("template", s.template.key)
            .putString("size", s.size.key)
            .putString("date_style", s.dateStyle.key)
            .apply()
    }
}

/** Ready-made looks, applied in one tap from the settings screen. */
enum class Preset(val label: String, val settings: Settings) {
    MISSION_CONTROL("Mission control", Settings(showDate = false, showOffset = true, showZoneName = false, showLabels = true, font = Font.ORBITRON, size = Size.SMALL, template = Template.PIPE)),
    TERMINAL("Terminal", Settings(showDate = true, showOffset = true, showZoneName = false, showLabels = true, font = Font.SHARE_TECH_MONO, size = Size.MEDIUM, template = Template.STACKED)),
    POSTER("Poster", Settings(showDate = false, showOffset = false, showZoneName = false, showLabels = true, font = Font.BEBAS_NEUE, size = Size.LARGE, template = Template.COLUMNS)),
    MINIMAL("Minimal", Settings(showDate = false, showOffset = false, showZoneName = false, showLabels = false, font = Font.SANS_LIGHT, size = Size.MEDIUM, template = Template.PIPE)),
    TRAVELLER("Traveller", Settings(showDate = true, showOffset = true, showZoneName = true, showLabels = true, font = Font.SPACE_GROTESK, size = Size.MEDIUM, template = Template.COLUMNS)),
}
