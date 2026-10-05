# Self-check — Lirelia Mobile V5.1

## Source audit

- `AppSettings` now persists `reader_font_family` and validates it against the supported font codes.
- `ReadiumReaderScreen` maps the saved code to Readium `FontFamily` values.
- `TypographySheet` contains a single clickable font row instead of category/button grids.
- Font picker selection is immediate and persistent.
- Font family changes are included in Readium preference submission, tap-bridge reinstall and repeated-vocabulary highlight refresh.
- No new external font binary files were added to the project.

## Supported choices

- DEFAULT
- SERIF
- SANS_SERIF
- CURSIVE
- MONOSPACE
- OPEN_DYSLEXIC
- ACCESSIBLE_DFA
- IA_WRITER_DUOSPACE

## Build limitation

The local wrapper still attempts to fetch `gradle-8.10.2-bin.zip` from `services.gradle.org`; DNS/network access is unavailable in this container. Final Android integration therefore still requires Android Studio Sync + Run on the user's computer.

## Additional checks

- Kotlin source parser smoke check: no `expecting` / unexpected-token diagnostics were found in the modified files (full compilation cannot resolve Android/Compose classes without the Gradle toolchain).
- `mobile_french_dictionary.db`: `PRAGMA integrity_check = ok`.
- `lirelia_lexical_engine_v2.db`: `PRAGMA integrity_check = ok`.
- Project tree contains no standalone `.ttf`, `.otf`, `.woff` or `.woff2` font files.
