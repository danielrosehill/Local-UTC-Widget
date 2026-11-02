# Local UTC Widget

An Android home-screen widget that shows **local time and UTC side by side, in 24-hour format**. That's the whole app: the launcher icon only opens the widget's settings.

## Options

Set in the app (or long-press the widget → settings). They apply to every widget instance.

- **Date** under each clock (UTC's date can differ from the local one around midnight)
- **UTC offset** of local time, e.g. `UTC+02:00` (follows DST automatically)
- **Local time zone name**, e.g. `Jerusalem`
- **LOCAL / UTC labels** on or off
- **Layout**: Stacked, Side by side (thin divider), Inline with pipe (`LOCAL 14:32 | UTC 12:32`)
- **Font**: system Sans, Sans light, Condensed, Serif, Monospace, plus bundled Orbitron, Audiowide, Share Tech Mono, JetBrains Mono, Space Grotesk, Bebas Neue, Rajdhani, Major Mono Display
- **Size**: small, medium, large
- **Presets**: one tap: Mission control (Orbitron, pipe), Terminal (Share Tech Mono, stacked), Poster (Bebas Neue, side by side), Minimal (Sans light, pipe, no labels), Traveller (Space Grotesk, zone + date)

The settings screen also has an **Add widget to home screen** button.

Tapping the widget opens the settings.

## Install (sideload)

Download the APK from [Releases](../../releases/latest), open it on the phone and allow installs from that source. Then long-press the home screen → Widgets → **Local UTC Widget**.

Or over adb: `adb install Local-UTC-Widget-v1.1.apk`

Requires Android 8.0 (API 26) or later. No network. The only permission is exact alarms (`USE_EXACT_ALARM`), used to redraw bundled-font clocks on the minute.

## How it works

- The clocks are `TextClock` views with a fixed `HH:mm` format, so the launcher ticks them every minute; the app never wakes up to redraw them. The UTC clock has `timeZone="UTC"`.
- Only the zone/offset line is static text. It is refreshed on `TIMEZONE_CHANGED`, `TIME_SET`, `LOCALE_CHANGED`, the 30-minute `updatePeriodMillis`, and an inexact alarm set for just after the next DST transition (`ZoneRules.nextTransition`).
- `RemoteViews` cannot change a typeface at runtime, so each template × font has its own layout, `res/layout/widget_<template>_<font>.xml`, generated with `Layouts.kt` by `scripts/gen_layouts.py`. Edit the script, not the generated files.
- Launchers ignore an app's bundled `@font/` resources inside `RemoteViews` (text silently falls back to the default face). So bundled-font layouts use ImageViews: the text is drawn into bitmaps and redrawn each minute by a non-wakeup exact alarm (no redraws while the screen is off). System fonts keep the live `TextClock` path.

## Build

```bash
# JDK 17 or 21; SDK path in local.properties (sdk.dir=...)
./gradlew assembleDebug                       # debug-signed
SIGNING_KEYSTORE_PATH=... SIGNING_STORE_PASSWORD=... SIGNING_KEY_ALIAS=... SIGNING_KEY_PASSWORD=... \
VERSION_NAME=1.0 VERSION_CODE=1 ./gradlew assembleRelease
```

Without the `SIGNING_*` variables the release build falls back to the debug key.

Stack: Kotlin, AGP 8.13.2, Gradle 8.14.3, compileSdk/targetSdk 36, no third-party or AndroidX dependencies.

## License

MIT. Bundled fonts are under the SIL Open Font License 1.1, from [google/fonts](https://github.com/google/fonts); licence texts are in `fonts-licenses/`.
