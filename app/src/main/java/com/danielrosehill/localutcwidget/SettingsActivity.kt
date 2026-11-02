package com.danielrosehill.localutcwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/**
 * Launcher entry and widget configure screen. Settings apply to every widget instance.
 * Opened as APPWIDGET_CONFIGURE it shows an "Add widget" button that returns RESULT_OK.
 */
class SettingsActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var s: Settings
    private lateinit var preview: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val configuring = widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        if (configuring) setResult(RESULT_CANCELED, resultIntent())

        s = Prefs.load(this)
        val pad = dp(20)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        col.addView(heading(getString(R.string.app_name), 22f))
        col.addView(note("24-hour local time and UTC. Settings apply to all widgets."))

        preview = FrameLayout(this).apply {
            setPadding(0, dp(16), 0, dp(16))
        }
        col.addView(preview, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        if (!configuring) {
            val mgr = getSystemService(AppWidgetManager::class.java)
            if (mgr.isRequestPinAppWidgetSupported) {
                col.addView(Button(this).apply {
                    text = "Add widget to home screen"
                    setOnClickListener {
                        mgr.requestPinAppWidget(ComponentName(this@SettingsActivity, ClockWidgetProvider::class.java), null, null)
                    }
                }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        }

        col.addView(heading("Presets", 16f))
        col.addView(note("One tap sets layout, font, size and what's shown."))
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        Preset.entries.forEach { p ->
            presetRow.addView(Button(this).apply {
                text = p.label
                isAllCaps = false
                typeface = typefaceFor(p.settings.font)
                setOnClickListener {
                    s = p.settings
                    Prefs.save(this@SettingsActivity, s)
                    ClockWidgetProvider.updateAll(this@SettingsActivity)
                    recreate()
                }
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        col.addView(presetRow)

        col.addView(heading("Layout", 16f))
        col.addView(choice(Template.entries.map { it.label }, s.template.ordinal) { s = s.copy(template = Template.entries[it]) })

        col.addView(heading("Show", 16f))
        col.addView(toggle("Date", s.showDate) { s = s.copy(showDate = it) })
        col.addView(toggle("UTC offset of local time", s.showOffset) { s = s.copy(showOffset = it) })
        col.addView(toggle("Local time zone name", s.showZoneName) { s = s.copy(showZoneName = it) })
        col.addView(toggle("LOCAL / UTC labels", s.showLabels) { s = s.copy(showLabels = it) })

        col.addView(heading("Font", 16f))
        col.addView(choice(Font.entries.map { it.label }, s.font.ordinal, Font.entries.map { typefaceFor(it) }) { s = s.copy(font = Font.entries[it]) })

        col.addView(heading("Size", 16f))
        col.addView(choice(Size.entries.map { it.label }, s.size.ordinal) { s = s.copy(size = Size.entries[it]) })

        if (configuring) {
            col.addView(Button(this).apply {
                text = "Add widget"
                setOnClickListener {
                    setResult(RESULT_OK, resultIntent())
                    finish()
                }
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })
        } else {
            col.addView(note("Add it from the home screen: long-press → Widgets → Local UTC Widget."))
        }

        setContentView(ScrollView(this).apply { addView(col) })
        // Edge-to-edge (targetSdk 35+): keep content clear of the status and nav bars.
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION")
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }
        refresh()
    }

    private fun refresh() {
        Prefs.save(this, s)
        ClockWidgetProvider.updateAll(this)
        preview.removeAllViews()
        val v = WidgetRenderer.build(this, s).apply(this, preview)
        v.setOnClickListener(null)
        preview.addView(v, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    private fun toggle(label: String, on: Boolean, set: (Boolean) -> Unit) = Switch(this).apply {
        text = label
        textSize = 16f
        isChecked = on
        setPadding(0, dp(8), 0, dp(8))
        setOnCheckedChangeListener { _, v -> set(v); refresh() }
    }

    private fun choice(labels: List<String>, selected: Int, faces: List<Typeface>? = null, set: (Int) -> Unit) = RadioGroup(this).apply {
        labels.forEachIndexed { i, l ->
            addView(RadioButton(context).apply {
                faces?.let { typeface = it[i] }
                id = View.generateViewId()
                text = l
                textSize = 16f
                tag = i
                if (i == selected) isChecked = true
            })
        }
        setOnCheckedChangeListener { g, checkedId ->
            set(g.findViewById<RadioButton>(checkedId).tag as Int); refresh()
        }
    }

    private fun heading(t: String, sp: Float) = TextView(this).apply {
        text = t
        textSize = sp
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(12), 0, dp(4))
    }

    private fun note(t: String) = TextView(this).apply {
        text = t
        textSize = 14f
        alpha = 0.7f
    }

    /** Each font option is drawn in its own face, so the list is its own preview. */
    private fun typefaceFor(f: Font): Typeface = when (f) {
        Font.SANS -> Typeface.SANS_SERIF
        Font.SANS_LIGHT -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        Font.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        Font.SERIF -> Typeface.SERIF
        Font.MONO -> Typeface.MONOSPACE
        else -> resources.getFont(f.fontRes)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
