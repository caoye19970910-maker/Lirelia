# Self-check — Lirelia Mobile V5.0

## Source checks completed

- Kotlin syntax parse check: `LanguageReaderApp.kt` — no syntax errors detected.
- Kotlin syntax parse check: `FrenchTrainingScreen.kt` — no syntax errors detected.
- Kotlin syntax parse check: `ReadiumReaderScreen.kt` — no syntax errors detected.
- Kotlin syntax parse check: `DictionaryRepository.kt` / `LibraryOrganizer.kt` — no syntax errors detected.
- Standalone Kotlin compilation succeeded for `Models.kt + FrenchTrainingData.kt + FrenchNounGender.kt + DictionaryDisplayFormatter.kt`.
- Dictionary example formatter strips inline IPA fragments and separates packed French/Chinese example text for display.
- Bundled SQLite dictionary files are checked separately during packaging.

## Functional source audit

- Library home no longer contains `Moon 风格阅读底座`, `继续阅读`, or `今日学习`.
- `今日学习` exists in the Training hub.
- List and grid book menus both contain rename + hide/show cover next to favorite/organize/delete.
- Rename metadata is included in search and propagated into opened display titles where the reader path supports it.
- EPUB dictionary meta uses shared noun-gender resolution; `nid/nids` is covered as masculine.
- Readium fixed expressions render a Chinese gloss row.
- Magnifier width default/range/safety padding were updated for edge clipping.

## Full Android build limitation

`./gradlew :app:compileDebugKotlin --offline` cannot complete in the current container because the project wrapper still needs `gradle-8.10.2-bin.zip` from `services.gradle.org`, which is not resolvable from this environment. Final Android/Readium integration therefore still needs one Android Studio Sync + Run on the user's machine.
