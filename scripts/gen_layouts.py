#!/usr/bin/env python3
"""Generate one widget layout per (template, font), plus Layouts.kt mapping them.

RemoteViews has no remotable setTypeface, so every font needs its own layout file.
System fonts (fontFamily names) use live TextClock/TextView. Bundled fonts (@font/...) are
ignored by launchers (verified 2026-11-02, Pixel launcher API 34), so their layouts use
ImageViews with the same ids, and WidgetRenderer draws the text into bitmaps each minute.
Run from anywhere: python3 -I scripts/gen_layouts.py
Edit FONTS / TEMPLATES here, never the generated XML.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/layout"
KT = ROOT / "app/src/main/java/com/danielrosehill/localutcwidget/Layouts.kt"

# key -> (fontFamily value, label shown in settings)
FONTS = {
    "sans": ("sans-serif", "Sans"),
    "sans_light": ("sans-serif-light", "Sans light"),
    "condensed": ("sans-serif-condensed", "Condensed"),
    "serif": ("serif", "Serif"),
    "mono": ("monospace", "Monospace"),
    "orbitron": ("@font/orbitron", "Orbitron"),
    "audiowide": ("@font/audiowide", "Audiowide"),
    "share_tech_mono": ("@font/share_tech_mono", "Share Tech Mono"),
    "jetbrains_mono": ("@font/jetbrains_mono", "JetBrains Mono"),
    "space_grotesk": ("@font/space_grotesk", "Space Grotesk"),
    "bebas_neue": ("@font/bebas_neue", "Bebas Neue"),
    "rajdhani": ("@font/rajdhani", "Rajdhani"),
    "major_mono_display": ("@font/major_mono_display", "Major Mono Display"),
}

TEMPLATES = {
    "stacked": "Stacked",
    "columns": "Side by side",
    "pipe": "Inline with pipe",
}

LOCAL_C, UTC_C, DIM_C = "#FFFFFF", "#81D4FA", "#B0BEC5"
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'


def img(i, extra=""):
    return (f'<ImageView android:id="@+id/{i}" android:layout_width="wrap_content" '
            f'android:layout_height="wrap_content" android:baselineAlignBottom="true" {extra}/>')


def bundled(f):
    return f.startswith("@font/")


def label(i, text, color, f, extra=""):
    if bundled(f):
        return img(i, extra)
    return f'''<TextView android:id="@+id/{i}" android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:fontFamily="{f}" android:letterSpacing="0.1" android:text="@string/{text}"
    android:textColor="{color}" android:textSize="12sp" {extra}/>'''


def clock(i, color, f, utc, fmt="HH:mm", size="34sp", extra=""):
    if bundled(f):
        return img(i, extra)
    tz = 'android:timeZone="UTC"' if utc else ""
    return f'''<TextClock android:id="@+id/{i}" android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:fontFamily="{f}" android:format12Hour="{fmt}" android:format24Hour="{fmt}" android:includeFontPadding="false" android:maxLines="1"
    android:textColor="{color}" android:textSize="{size}" {tz} {extra}/>'''


def date(i, color, f, utc):
    return clock(i, color if utc else DIM_C, f, utc, "EEE d MMM", "12sp")


def zone(f):
    if bundled(f):
        return img("zone_local")
    return f'''<TextView android:id="@+id/zone_local" android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:fontFamily="{f}" android:textColor="{DIM_C}" android:textSize="12sp" />'''


def row(*children, extra=""):
    return ('<LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" '
            f'android:orientation="horizontal" android:baselineAligned="true" {extra}>\n'
            + "\n".join(children) + "\n</LinearLayout>")


def col(*children, extra=""):
    return ('<LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" '
            f'android:orientation="vertical" {extra}>\n' + "\n".join(children) + "\n</LinearLayout>")


def root(orientation, *children):
    return (f'<LinearLayout {NS} android:id="@+id/root" android:layout_width="match_parent" '
            f'android:layout_height="match_parent" android:background="@drawable/widget_bg" '
            f'android:gravity="center" android:orientation="{orientation}" android:padding="14dp">\n'
            + "\n".join(children) + "\n</LinearLayout>")


def stacked(f):
    # Label | (clock, zone, date): clocks and the lines under them share one left edge.
    # WidgetRenderer gives both labels the same min width so the two clocks line up.
    m = 'android:layout_marginEnd="10dp"'
    b = 'android:baselineAlignedChildIndex="0"'
    return root("vertical", col(
        row(label("label_local", "label_local", DIM_C, f, m),
            col(clock("clock_local", LOCAL_C, f, False), zone(f), date("date_local", LOCAL_C, f, False), extra=b)),
        row(label("label_utc", "label_utc", UTC_C, f, m),
            col(clock("clock_utc", UTC_C, f, True), date("date_utc", UTC_C, f, True), extra=b),
            extra='android:layout_marginTop="6dp"')))


def columns(f):
    # A plain <View> is not allowed in RemoteViews (InflateException, seen 2026-11-02); FrameLayout is.
    divider = ('<FrameLayout android:layout_width="1.5dp" android:layout_height="match_parent" '
               'android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:background="#55FFFFFF" />')
    return root("horizontal",
                col(label("label_local", "label_local", DIM_C, f), clock("clock_local", LOCAL_C, f, False),
                    zone(f), date("date_local", LOCAL_C, f, False)),
                divider,
                col(label("label_utc", "label_utc", UTC_C, f), clock("clock_utc", UTC_C, f, True),
                    date("date_utc", UTC_C, f, True)))


def pipe(f):
    m = 'android:layout_marginEnd="8dp"'
    bar = img("pipe", 'android:layout_marginStart="12dp" android:layout_marginEnd="12dp"') if bundled(f) else (f'<TextView android:id="@+id/pipe" android:layout_width="wrap_content" android:layout_height="wrap_content" '
           f'android:layout_marginStart="12dp" android:layout_marginEnd="12dp" android:fontFamily="{f}" '
           f'android:includeFontPadding="false" android:text="|" android:textColor="#66FFFFFF" android:textSize="34sp" />')
    return root("vertical",
                row(label("label_local", "label_local", DIM_C, f, m), clock("clock_local", LOCAL_C, f, False),
                    bar,
                    label("label_utc", "label_utc", UTC_C, f, m), clock("clock_utc", UTC_C, f, True)),
                row(col(zone(f), date("date_local", LOCAL_C, f, False)),
                    date("date_utc", UTC_C, f, True).replace("Clock ", 'Clock android:layout_marginStart="16dp" ', 1).replace("<ImageView ", '<ImageView android:layout_marginStart="16dp" ', 1),
                    extra='android:layout_marginTop="2dp"'))


BUILDERS = {"stacked": stacked, "columns": columns, "pipe": pipe}

for old in RES.glob("widget_*.xml"):
    old.unlink()
for t in TEMPLATES:
    for k, (fam, _) in FONTS.items():
        body = BUILDERS[t](fam)
        (RES / f"widget_{t}_{k}.xml").write_text(
            f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by scripts/gen_layouts.py. Do not edit. -->\n{body}\n')


def enum_name(k):
    return k.upper()


kt = ["package com.danielrosehill.localutcwidget", "",
      "// Generated by scripts/gen_layouts.py. Do not edit.", "",
      "/** [fontRes] is 0 for system fonts (live TextClock); otherwise text is drawn to bitmaps. */",
       "enum class Font(val key: String, val label: String, val fontRes: Int, val systemFamily: String?) {"]
kt += [f'    {enum_name(k)}("{k}", "{lbl}", {("R.font." + fam[6:]) if fam.startswith("@font/") else 0}, {"null" if fam.startswith("@font/") else '"' + fam + '"'}),' for k, (fam, lbl) in FONTS.items()]
kt += ["    ;", "    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: SANS_LIGHT }", "}", "",
       "enum class Template(val key: String, val label: String) {"]
kt += [f'    {enum_name(k)}("{k}", "{lbl}"),' for k, lbl in TEMPLATES.items()]
kt += ["    ;", "    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: STACKED }", "}", "",
       "object Layouts {", "    fun of(t: Template, f: Font): Int = when (t) {"]
for t in TEMPLATES:
    kt.append(f"        Template.{enum_name(t)} -> when (f) {{")
    kt += [f"            Font.{enum_name(k)} -> R.layout.widget_{t}_{k}" for k in FONTS]
    kt.append("        }")
kt += ["    }", "}", ""]
KT.write_text("\n".join(kt))
print(f"generated {len(TEMPLATES) * len(FONTS)} layouts and {KT.name}")
