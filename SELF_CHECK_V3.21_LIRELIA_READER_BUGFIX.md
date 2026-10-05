# V3.21 self-check

- [x] Launcher label is Lirelia.
- [x] Launcher icon resources exist for mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi.
- [x] applicationId unchanged for in-place upgrade/data continuity.
- [x] Long-press palette uses `Popup`, not a full-screen dialog/bottom sheet.
- [x] Color pills call annotation persistence immediately.
- [x] Clear-all-word removes exact-word annotations across books and vocabulary state.
- [x] `on` and common pronouns have always-available core offline definitions.
- [x] Multi-word dictionary query has local-first fallback and optional translation enrichment.
- [x] Existing V3.18 reading profile, V3.19 expression loop, and V3.20 voice routing remain in project.
- [ ] Full Android Gradle build cannot be run in this container because the original project does not include `gradle-wrapper.jar` and no compatible Gradle distribution is installed locally.
