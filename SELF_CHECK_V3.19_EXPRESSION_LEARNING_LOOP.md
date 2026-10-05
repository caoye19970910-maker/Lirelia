# V3.19 self-check

- [x] LearningStore schema migration creates expressions + expression_contexts without deleting existing data.
- [x] Smart phrase UI can add/remove an expression and forces immediate UI refresh.
- [x] Stored expression keeps original sentence and book metadata.
- [x] Training hub exposes 我的表达.
- [x] Daily adaptive mix includes personal_phrase only when saved expressions exist.
- [x] Wrong phrase answers remain on screen; correct answers advance.
- [x] Phrase pronunciation reuses the existing TtsManager routing/cache.
- [x] Stale adaptive phrase rows are pruned when an expression is removed.
- [x] Backup/restore includes expressions and their contexts.
- [x] Personal-vocab training review date follows adaptive next_due_at.
- [x] V3.18 reading profile and voice-route code retained.

Note: this source package still does not contain `gradle-wrapper.jar`, so a full Android Gradle build cannot be executed in this container without restoring the wrapper/toolchain.
