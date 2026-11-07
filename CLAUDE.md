# Local UTC Widget

Android home-screen widget: local time + UTC, 24-hour. Package `com.danielrosehill.localutcwidget`. Public repo `danielrosehill/Local-UTC-Widget`.

Release tier: **small** (`v1.0`, `v1.1`, ...). `versionCode` must increase every release.

## Signing

Release key (created 2026-11-02): `~/.config/android-signing/local-utc-widget.jks`, alias `local-utc-widget`, store and key password both in `local-utc-widget.pass`. Backed up in 1Password vault `Daniel - Personal`: document "Android signing key - Local UTC Widget", password item "Android signing key password - Local UTC Widget". Cert SHA-256 `6d2678104cd5a9dd02492c03b4e2aaecf8d2b2b88f8f67358037a09004bbc356`. Every release must use this key or installed copies cannot update.

Builds are local (no CI yet). Release build:

```bash
K=~/.config/android-signing; P=$(cat $K/local-utc-widget.pass)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 SIGNING_KEYSTORE_PATH=$K/local-utc-widget.jks \
SIGNING_STORE_PASSWORD=$P SIGNING_KEY_PASSWORD=$P SIGNING_KEY_ALIAS=local-utc-widget \
VERSION_NAME=1.x VERSION_CODE=N ./gradlew assembleRelease
```

Then `gh release create v1.x out/Local-UTC-Widget-v1.x.apk` and upload to Drive (skill `google-workspace/upload-apk`).

Distribution: public F-Droid channel since 2026-11-07 (`apps.yml` in `danielrosehill/F-Droid-Public-Repo`). After a release run `gh workflow run publish.yml -R danielrosehill/F-Droid-Public-Repo`, or it waits for the Monday 03:17 UTC run.

## Notes

- Widget layouts are generated: edit `scripts/gen_layouts.py` (fonts, templates), run `python3 -I scripts/gen_layouts.py`. It also writes `Layouts.kt` (Font/Template enums).
- Bundled fonts (`res/font/`, OFL, licences in `fonts-licenses/`) do NOT render inside RemoteViews: launchers ignore `@font/` and fall back to the default face (verified Pixel launcher, API 34, 2026-11-02). Their layouts use ImageViews; `WidgetRenderer` draws text to bitmaps and `ClockWidgetProvider` redraws each minute (exact non-wakeup RTC alarm, `USE_EXACT_ALARM`). System fonts keep live `TextClock`.
- Test on a real home screen, not just the in-app preview: the preview inflates in the app process, where `@font` works, so it hides launcher-only failures. The "Add widget to home screen" button (requestPinAppWidget) pins one in the emulator.
- Testing: AVD `hebrish34` headless (`emulator -avd hebrish34 -no-window`), never the live phone first.
- JDK 25 is the system default; build with JDK 21.
