# Self-check — V4.0 Readium Translation First

## Static checks completed

- New Kotlin files pass Kotlin parser/syntax stage (Android/Compose symbols cannot resolve without Android SDK classpath in this container).
- Existing bundled dictionary asset opens with SQLite and reports 156,365 entries.
- Verified `emploi du temps` exists in the bundled dictionary.
- Added and JVM-tested irregular reverse conjugation: `devrait -> devoir`, `pourrions -> pouvoir`.
- EPUB routing is limited to `book.type == EPUB`; PDF and text readers remain unchanged.
- Package/application ID unchanged.
- Manifest includes the new non-exported Readium activity.
- New Wikidata extension is built in a temp database and only swapped into place after `>100,000` rows and final count verification.
- Readium highlights persist as Locator JSON and are reapplied through the Decoration API.

## Environment limitation

A full Android/Gradle compilation was not possible in this execution container because it has no Android SDK and the original project does not contain `gradle-wrapper.jar`. Android Studio should perform the real dependency sync/build on the user's machine.
