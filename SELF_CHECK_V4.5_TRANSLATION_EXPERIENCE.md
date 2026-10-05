# SELF CHECK — V4.5 Translation Experience

- [x] Version bumped to 125 / 4.5-translation-experience.
- [x] Ollama remains absent from runtime code.
- [x] Quick word and sentence translation are visually separated.
- [x] Missing local word entry has independent network word fallback.
- [x] Full dictionary still iterates all definitions and adds related/phrases/examples.
- [x] Pronunciation route accepts AUTO/MAINLAND/GOOGLE/OFFLINE.
- [x] Old Google-first default migrates once to AUTO.
- [x] Sentence/word translation respects the shared network profile instead of hard-coding Google first.
- [x] Normal TTS success/Google-connecting toast removed.
- [x] System-TTS failure marks engine unavailable before fallback, preventing fallback loops.
- [x] Untouched margin/line-spacing defaults migrate to 16 / 142.
- [x] Gradle wrapper files present.
- [ ] Full assembleDebug in this environment (blocked by services.gradle.org DNS).
