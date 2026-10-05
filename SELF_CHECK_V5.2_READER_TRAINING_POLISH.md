# Self-check — Lirelia Mobile V5.2

## Source checks
- Gender trainer contains only masculine/feminine buttons; no `un/une` input control.
- Training session store persists a de-duplicated wrong-question list and preserves the most recently completed list.
- Reader font-size range: 14–64.
- Reader line-height range: 90–260%.
- EPUB custom typography forces publisher-style override off when needed.
- Primary marker palette uses stronger yellow/green/blue/pink/gray tints.
- Repeated vocabulary marks use lower-alpha versions of the same colors.
- Sentence translation provider order is Google first, MyMemory fallback.
- Shared French sentence segmenter is used by TXT/PDF; EPUB tap JavaScript uses French Intl.Segmenter plus fallback.
- Dual-line guide has on/off, position, height and independent top/bottom color settings.

## Validation performed
- `FrenchSentenceSegmenter.kt` compiles independently with `kotlinc` and passes abbreviation/newline smoke tests.
- EPUB tap JavaScript passes `node --check`.
- EPUB repeated-vocabulary JavaScript passes `node --check` after substituting a test payload.
- Modified Kotlin files produce no parser-level `expecting` / `unexpected tokens` diagnostics in a standalone syntax smoke check.
- `mobile_french_dictionary.db`: `PRAGMA integrity_check = ok`.
- `lirelia_lexical_engine_v2.db`: `PRAGMA integrity_check = ok`.
- No standalone font binary files were added.

## Full Android build limitation
The Gradle wrapper still needs `gradle-8.10.2-bin.zip`. This environment cannot resolve `services.gradle.org`, so full `compileDebugKotlin` / APK assembly cannot be completed here. Final integration should be verified with Android Studio Sync + Run on the development machine.
