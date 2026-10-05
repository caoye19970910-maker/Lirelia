# Lirelia Mobile V5.2 — Reader & Training Polish

V5.2 fixes the reported training and reading issues: choice-only noun gender, visible session mistakes, wider typography controls, smarter French sentence segmentation, stronger highlights with pale repeated-word radar, Google-first sentence translation, and configurable two-line reading guidance.

See `V5.2_READER_TRAINING_POLISH.md` and `SELF_CHECK_V5.2_READER_TRAINING_POLISH.md`.

---

# Lirelia Mobile V5.1 — Font Picker

V5.1 adds a compact font selector to **Aa → 字体**. The Aa panel keeps only one font row; tapping it opens a simple selection list with no category grouping. The selected font is applied immediately to reflowable EPUB text and remembered for the next reading session.

Included choices: 原书字体、书籍衬线、清爽无衬线、手写体、等宽字体、OpenDyslexic、Accessible DfA、iA Writer Duospace. No standalone external font files are bundled in this project.

---

# Lirelia Mobile V5.0 — Library & Dictionary Cleanup

V5.0 simplifies the home screen around `我的书架`, moves learning statistics into Training, adds per-book rename/cover controls, widens the EPUB magnifier safety area, and improves French dictionary gender/expression presentation.

## Fixed / improved in V5.0

- Bookshelf-first home: top import + grid/list actions, search, then books.
- Removed Continue Reading and Today Learning from the library; Today Learning now lives in Training.
- Book `•••` menu adds Rename and Hide/Show Cover alongside Favorite, Organize and Delete.
- EPUB magnifier uses a wider default/range plus horizontal safety margin to reduce clipped edge glyphs.
- Nouns display masculine/feminine information when resolved; `nids` resolves through lemma `nid` as masculine.
- Fixed expressions display Chinese glosses; dictionary examples strip inline IPA fragments and render French/Chinese on separate lines.
- Vocabulary full-screen review and auto-pronunciation from V4.9 remain unchanged.

See `V5.0_LIBRARY_DICTIONARY_CLEANUP.md` and `SELF_CHECK_V5.0_LIBRARY_DICTIONARY_CLEANUP.md`.

---

# Lirelia Mobile V4.9 — Reading Magnifier / Full-screen Review

V4.9 replaces the intrusive EPUB reading ruler with a configurable magnifier-style lens and moves all of its controls into `Aa`. Vocabulary review is now full-screen and has a persistent auto-pronunciation toggle.

## Fixed / improved in V4.9

- Removed the black EPUB ruler band and its on-page move/resize handles.
- Added a glass-like magnifier reading helper backed by the Readium navigator view.
- `Aa` now controls helper on/off, vertical position, lens height, lens width, and 110–220% zoom.
- Vocabulary review now uses the full screen instead of a centered modal card.
- Added persistent `自动发音`; when enabled each new review word is spoken once, while manual pronunciation remains available.

See `V4.9_MAGNIFIER_FULLSCREEN_REVIEW.md` and `SELF_CHECK_V4.9_MAGNIFIER_FULLSCREEN_REVIEW.md`.

---

# Lirelia Mobile V4.8 — Scroll / Ruler / Annotation Fixes

V4.8 targets the real-device EPUB/Readium interaction problems reported on 2026-09-23.

## Fixed / improved in V4.8

- Scroll mode can cross EPUB resource/chapter boundaries from the bottom/top instead of getting stuck.
- Previous/next controls scroll within the current resource first, then explicitly navigate to the adjacent reading-order resource.
- The reading ruler has direct on-page move and resize handles, with persisted position and height.
- EPUB highlight colors are stronger and easier to see.
- Selection actions include cancel annotation, add vocabulary, and clear word.
- Colored single-word annotations are saved to vocabulary automatically.
- Learning words are highlighted again at later occurrences in the current EPUB resource and re-applied after chapter/layout changes.
- Clearing a word removes its vocabulary state and exact-word annotations, preventing stale repeated highlights.
- Gray is now a persistent vocabulary marker color.

See `V4.8_SCROLL_RULER_ANNOTATION_FIXES.md` and `SELF_CHECK_V4.8_SCROLL_RULER_ANNOTATION_FIXES.md` for details.

---

# Lirelia Mobile V4.6 — Dictionary Engine 2.0

V4.6 rebuilds the dictionary resolution layer around separate lexical meanings and morphological analyses. The Readium reading engine and V4.5 translation UI remain intact.

## Fixed / improved in V4.6

- Added a normalized offline lexical engine with separate `lexeme` and `form_analysis` tables.
- Context-aware ranking now disambiguates common French homographs/inflections such as `tâche`, `est`, `son`, `livre`, `pris`, `fait`, and `dit`.
- Morphology text is no longer displayed as if it were a Chinese definition.
- Quick cards can show the resolved lemma + grammatical form; full dictionary adds dedicated `词形` and `其他可能` sections.
- Common irregular forms are reverse-resolved offline; conservative regular `-er` candidates are accepted only when their lemma exists in lexical data.
- Related-word lists exclude multi-word expressions already represented as phrases.
- Fixed expressions receive a lightweight learning-oriented ranking.
- The new schema includes frequency columns so Lexique 3.83 / Morphalou 3.1 can be overlaid later without another UI rewrite.

## Validation

- New lexical SQLite integrity checked.
- Core Kotlin dictionary files passed a standalone syntax/type check with Android/SQLite stubs.
- Full `assembleDebug` still cannot run in this environment because `services.gradle.org` is not resolvable here; build it once in Android Studio as with V4.5.

---

# Lirelia Mobile V4.5 — Translation Experience

V4.5 focuses on the real-device issues visible after the Readium migration: quick lookup hierarchy, word-vs-sentence translation separation, quieter pronunciation routing, and denser reading defaults. The Readium foundation remains unchanged.

## Fixed / improved in V4.5

- Quick cards now lead with the tapped word: headword, lemma/POS/IPA, then concise definitions.
- Sentence translation is a clearly labeled second section and no longer masquerades as a word definition.
- The old offline word-by-word gloss is hidden while natural translation is loading; if it must be shown after failure, it is explicitly labeled as non-natural assistance.
- Missing local entries (for example `voyage` in the bundled dictionary) receive an independent network word-meaning fallback instead of displaying the sentence translation as the word meaning.
- Network-fallback meanings can be saved to vocabulary.
- Full dictionary keeps every stored meaning and now also shows related words, phrases, examples, and sentence context in separate sections.
- Pronunciation adds an `AUTO` route and migrates the old Google-first default once. AUTO prefers cache/system French, then Mainland-friendly Youdao/Baidu, with Google only as the last fallback.
- Sentence/word network translation also follows the selected network profile: AUTO/Mainland tries MyMemory before Google; explicit International keeps Google first.
- Normal pronunciation no longer throws success/connection toast banners over the reading page. Errors still surface when pronunciation is genuinely unavailable.
- Untouched Readium typography defaults move from margin 20 → 16 and line spacing 150% → 142%; customized settings are preserved.
- Bottom translation card uses a compact floating surface with short slide/fade transitions and animated content resizing.

## Validation

- Bundled dictionary SQLite integrity and counts rechecked.
- Kotlin parser-level check found no syntax-level errors in the edited core files; standalone `kotlinc` cannot resolve Android/Compose classes without the Android project classpath.
- Full Gradle build still cannot run in this environment because the Gradle distribution host is not resolvable here. V4.4 was confirmed by the user to build and launch in Android Studio; V4.5 still needs one Android Studio build on the user's machine.

---

# Lirelia Mobile V4.4 — Readium Stability Pass II

V4.4 continues the stabilization work. It focuses on tap/selection reliability, eliminating UI-thread database/search work, resource cleanup, cross-chapter scroll navigation, and making the full dictionary truly show every stored meaning.

## Fixed in V4.4
- Word translation becomes available immediately on newly opened EPUB resources instead of waiting behind progress-save debounce.
- Long-press selection release no longer accidentally triggers single-word translation.
- EPUB single-word taps again pronounce the word automatically while the dictionary/translation card loads.
- Scroll-mode previous/next and auto-scroll can cross chapter/resource boundaries.
- Legacy annotation search and bookmark-position migration are moved off the UI thread.
- Annotation snapshots/writes and decoration preparation avoid unnecessary UI-thread database work.
- Dictionary and learning SQLite handles are closed deterministically when activities are destroyed.
- WikDict startup skips synchronous SQLite validation in the normal no-recovery case.
- Repository-level definition caps were removed, so full dictionary screens now display all stored meanings (for example, the bundled entry `relever` contains 17). Quick cards remain intentionally concise.
- Volume-key repeat events no longer skip several pages when a key is held.
- Ollama remains removed; only stale preference cleanup exists for upgrades.

## Validation
- Bundled dictionary: `PRAGMA integrity_check = ok`; 156,365 entries / 28,878 forms / 8,862 phrases.
- XML resources parsed successfully and the Gradle wrapper jar is present.
- A parser-oriented standalone Kotlin pass found no obvious syntax-level errors in the edited core files.
- A full Android build was attempted, but this environment cannot resolve `services.gradle.org`, so Gradle 8.10.2 cannot be downloaded here.

---

# Lirelia Mobile V4.3 — Readium Foundation Fixes

V4.3 focuses on the foundation bugs found in the V4.2 audit. No new study module was added.

## Fixed in V4.3
- EPUB import is Readium-authoritative: a valid EPUB is no longer rejected just because Lirelia's legacy helper parser cannot extract a cache.
- Readium position persistence is debounced (700 ms) and flushed on exit to avoid excessive SharedPreferences writes during scroll/auto-scroll.
- Legacy bookmark/highlight migrations are retryable; transient Readium/search failures no longer permanently mark migration as complete.
- V4.1 preference-backed highlight migration is moved off the Compose/UI thread.
- Selection toolbar re-reads the active Readium selection while visible, reducing stale popup drift during page settling/scrolling.
- EPUB navigation now includes an annotation tab; tapping an annotation jumps to its Locator and annotations can be deleted there.
- WikDict startup recovery restores the `.previous` database if the app was interrupted between the old/new atomic swap.
- Volume-key switch now has Compose state and updates immediately.
- Cleartext HTTP traffic is disabled; current app network paths are HTTPS-only.
- Ollama remains removed from the app. Only one-time cleanup of stale old preference keys remains.

## Validation
- Source/archive integrity checks were run.
- Bundled dictionary SQLite integrity was checked.
- Gradle wrapper jar is included.
- Full Android compilation could not be completed in this environment because Gradle 8.10.2 cannot be downloaded here (DNS/network unavailable).


---

# Lirelia Mobile V4.2 — Readium Stabilization / No Ollama

V4.2 is a stabilization release. It does not add a new study feature set; it reconnects the Readium EPUB reader to Lirelia's existing data, settings, backup, reading statistics and annotation workflow, and removes Ollama completely.

## Core reader

- EPUB uses Readium for parsing, pagination/scrolling, locators, selection and decorations.
- PDF and text/subtitle readers keep their existing implementations.
- EPUB reading progress is stored as an exact Readium Locator and is also mapped back to the legacy paragraph progress for compatibility with existing library views.
- Legacy paragraph bookmarks are migrated once into Readium Locator bookmarks.
- Legacy EPUB highlights/notes are migrated once by searching the EPUB and choosing the occurrence nearest the old reading progression.
- Readium highlights/notes now live in `learning.db` instead of an isolated SharedPreferences store, so the annotation manager and backup system see the same records.

## Translation-first behavior

- Tapping a French word keeps the page in place and opens the translation dock.
- The tapped sentence is derived from the actual DOM character offset, rather than by searching for the first sentence containing the word.
- Offline dictionary assistance appears first. Network sentence translation updates the same card in place.
- Ollama/local-AI translation has been removed. Old Ollama preference keys are cleaned during upgrade.

## Reading controls restored for EPUB

- Font size, line spacing, page margins, theme, pagination/scroll mode.
- Landscape two-page mode.
- Keep-screen-on.
- Optional volume-key page turns (respects the existing setting).
- Custom left/center/right tap-zone actions.
- Auto-scroll in scroll mode.
- Reading ruler and blue-light overlay.
- Dark reader chrome and dark translation/selection surfaces follow the reader theme.
- Reader activity runs immersive with transient system bars.

## Bookmarks, notes and statistics

- Readium EPUB bookmarks have their own Locator-backed format and are included in normal preference backup.
- Notes are stored on the same Locator-backed annotation row as the highlight.
- EPUB reading time is recorded through the same `ReadingStatsStore` used elsewhere.
- Visible EPUB text exposure and word lookups are fed back into `ReadingProfileStore`.

## Hybrid French→Chinese dictionary

- Bundled primary database remains the main lexicon: 156,365 entries, 28,878 indexed forms, and 8,862 indexed phrases.
- The small core fallback still catches common function words missing from the large database.
- Optional official WikDict French→Chinese SQLite can be installed from Dictionary Center and merged into the same word card.
- WikDict updates use a verified same-directory two-phase swap: the known-good database is preserved until the new database has passed schema/count validation and has been atomically moved into place.

## Backup / upgrade

The application id remains `com.cy.languagereader.mobile`. Existing books, vocabulary, training state, settings and databases are preserved on normal upgrade.

Backup format version 8 includes Readium progress through `lirelia_readium_progress`, Locator-backed highlights/notes through `learning.db`, and Readium bookmarks through the normal `language_reader_books` preferences (the physical book catalogue itself remains device-local because source paths differ across devices).

## Build note

The project now contains `gradle/wrapper/gradle-wrapper.jar`, so the wrapper is structurally complete. The working environment used for this package has no external network access to `services.gradle.org`, so a full Android `assembleDebug` could not be completed here. Android Studio on a normal network can download Gradle 8.10.2 and Maven dependencies on first sync.
