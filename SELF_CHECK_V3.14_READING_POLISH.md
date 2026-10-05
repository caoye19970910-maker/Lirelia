# Self-check — V3.14 Reading Polish

- [x] versionCode/versionName updated to V3.14
- [x] old progress key retained for backward compatibility
- [x] new paragraph fingerprint anchor stored in `language_reader_books` (therefore covered by existing backup)
- [x] deleting a book removes its new reader-position key
- [x] page repagination follows stable paragraph anchor
- [x] publication first-line indent added to both renderer and paginator measurement
- [x] dialogue/chapter-start paragraphs excluded from automatic indent
- [x] French punctuation non-breaking substitution preserves character count
- [x] one in-book panel exposes contents/search/bookmarks/annotations
- [x] no remaining ReaderScreen references to the old `showNavigation` / `showBookSearch` states
- [x] Kotlin syntax smoke-check reports no parser errors
- [x] ZIP integrity checked after packaging
