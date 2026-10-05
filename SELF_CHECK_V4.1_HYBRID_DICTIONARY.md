# Self-check — V4.1 Hybrid Dictionary

- Package/application ID unchanged.
- Version bumped to 121 / `4.1-readium-hybrid-dictionary`.
- Bundled 156k dictionary remains the primary lexical database.
- Optional extension now uses official WikDict `fr-zh.sqlite3`, not the former experimental Wikidata text list.
- Installer writes to a temporary file, verifies SQLite schema and requires >5,000 `simple_translation` rows before atomic replacement.
- Rolling WikDict URL plus dated 2026-06 fallback configured.
- Lookup merges WikDict translations into existing definitions instead of hiding/replacing Lirelia lexical metadata.
- Inflected forms are normalized through `FrenchConjugator` and conservative morphology candidates before WikDict lookup.
- Old `wikidata_fr_zh.db` is deleted only after a new WikDict database is successfully installed.
- Existing PDF/TXT reader paths and learning stores were not changed.
- Full Android compilation still requires Android SDK/Gradle dependency resolution on the user's machine.

## Local data checks

- Bundled dictionary: 156,365 entries / 28,878 indexed forms / 8,862 phrases.
- `emploi du temps` is present in the bundled database.
- `on` is intentionally handled by the core fallback when absent from the large bundled DB.
- Mocked official WikDict `simple_translation(written_rep, trans_list, max_score, rel_importance)` query returns expected `|`-separated translations.
- WikDict current rolling directory lists `fr-zh.sqlite3` at about 3 MB (2026-06 generation).
