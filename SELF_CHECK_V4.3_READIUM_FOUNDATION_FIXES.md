# V4.3 Self-check

- [x] V4.2 archive unpacked successfully.
- [x] Gradle wrapper jar is present.
- [x] No Ollama client/translation path exists; only stale preference-key cleanup remains.
- [x] EPUB importer validates with Readium before optional legacy extraction.
- [x] EPUB import no longer requires legacy parser paragraphs.
- [x] Locator writes debounced to 700 ms.
- [x] Latest Locator flushes on dispose.
- [x] Legacy bookmark migration remains retryable if Readium positions are unavailable.
- [x] Legacy annotation migration tracks unresolved rows and does not falsely complete.
- [x] V4.1 preference highlight migration moved off UI thread.
- [x] Selection toolbar refreshes active selection bounds while visible.
- [x] Annotation list can jump to exact Readium Locator and delete.
- [x] WikDict interrupted `.previous` swap recovery added.
- [x] Volume-key switch immediately reflects state.
- [x] Android manifest no longer opts into cleartext HTTP.
- [ ] Full `:app:compileDebugKotlin` / APK build: blocked in this environment because `services.gradle.org` cannot be resolved.
