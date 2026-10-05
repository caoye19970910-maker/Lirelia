# V4.9 self-check

## Source checks

- `AppSettings`: added persistent magnifier width/zoom and vocabulary-review auto-pronunciation settings.
- `ReadiumReaderScreen`: removed V4.8 on-page ruler handles from the EPUB path and added `Aa` magnifier controls.
- `ReadiumReaderScreen`: persistent lens uses the current Readium navigator view and refreshes while content scrolls.
- `LanguageReaderApp`: vocabulary review is full-screen and retains reveal/rating behavior.
- `LanguageReaderApp`: auto pronunciation is keyed to the active review word, so recomposition does not repeatedly speak the same word.
- Standalone Kotlin parser pass reported no syntax/parser errors; unresolved Android/Compose symbols are expected without the Android classpath.

## Build limitation in this environment

`./gradlew :app:compileDebugKotlin --offline` cannot proceed because this project wrapper still needs to download Gradle 8.10.2 and `services.gradle.org` is unavailable in the execution environment. Run one normal Sync/Run in Android Studio for the final Android build check.

## Device test checklist

1. Open an EPUB and tap `Aa`.
2. Enable `阅读放大镜`; change vertical position, lens height, width, and magnification; close `Aa`.
3. Confirm there is no black ruler and no floating move/resize handles.
4. Scroll continuously and confirm the magnified lens refreshes with the text.
5. Open 生词 → 复习 and confirm review occupies the screen rather than a centered card.
6. Toggle `自动发音` on, advance several words, and confirm every new word is read once.
7. Turn `自动发音` off and confirm only the manual speaker button reads the word.
