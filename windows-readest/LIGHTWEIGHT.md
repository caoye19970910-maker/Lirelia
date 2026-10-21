# Lirelia Desktop lightweight revision

Based on pinned Readest 0.12.16; installer version 0.12.17.

- Native double-click in book documents opens Lirelia directly, independent of the original gesture preference.
- Word definitions and whole-sentence translations load independently with stale-result protection.
- Sentence extraction uses DOM offsets and sentence segmentation, preserving inline formatting boundaries and repeated words.
- Removed reading-page mounts/imports for original TTS, RSVP, handwriting, AI chat and whole-book translation.
- Removed original dictionary/translator/proofreader popup mounts and settings for original language engines, AI, TTS and integrations.
- Removed reader transfer/replica polling and external annotation sync hooks (BookOrbit, Readwise, Hardcover, Pagebound, Notion).
- Preserved document rendering, library, layout, themes, highlights, notes and Lirelia review/data.

Local validation: TypeScript and Biome src lint passed. Targeted Lirelia and selection regression tests: 16 passed.
Windows installer, native UI test and performance measurements must be verified for this revision; previous installer results do not validate this revision.
