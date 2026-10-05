# V4.2 self-check

## Passed static/source checks

- No `OllamaClient.kt` remains in the source tree.
- No UI or translation code calls Ollama. Only the upgrade cleanup removes old `ollama_url` / `ollama_model` preference keys.
- `versionCode=122`, `versionName=4.2-readium-stable-no-ollama`.
- Gradle wrapper JAR exists and contains `org/gradle/wrapper/GradleWrapperMain.class`.
- Bundled dictionary SQLite opens successfully in Python; `entries=156365`, `form_index=28878`, `phrase_index=8862`.
- `emploi du temps` is present in the bundled dictionary.
- Readium annotations are persisted through `LearningStore` with `source=readium` + `locator_json`.
- Backup exports/restores `locator_json` and `source` for annotations and includes `lirelia_readium_progress`.
- Readium reader invokes `ReadingStatsStore` and `ReadingProfileStore`.
- Volume keys are gated by `readerVolumeKeyTurnsPage`.
- Sentence extraction JavaScript derives boundaries from the tapped DOM node/offset.
- WikDict update keeps the previous database until the downloaded database has passed validation.

## Build limitation in this environment

`./gradlew --version` reaches the included wrapper and attempts to download Gradle 8.10.2, but the current container has no DNS/network access to `services.gradle.org`. Therefore this package has not been claimed as a fully compiled APK build. Android Studio/Gradle sync remains the final compile validation step on a normal development machine.
