package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

class BookRepository(private val context: Context) {

    private val FRENCH_STOP_WORDS = setOf(
        "les","des","une","un","dans","avec","pour","sur","par","pas","plus","que","qui","quoi",
        "aux","est","sont","être","avoir","avait","ont","elle","elles","ils","lui","leur","leurs",
        "nous","vous","tout","tous","toute","toutes","mais","donc","car","comme","sans","sous",
        "chez","entre","vers","depuis","après","avant","ainsi","alors","encore","très","bien",
        "aussi","même","cette","cet","ces","son","sa","ses","mon","ma","mes","ton","ta","tes",
        "notre","nos","votre","vos","je","tu","il","on","se","me","te","ne","de","du","la","le",
        "et","ou","où","en","au","à","ce","ça","y","d","l","qu","j","n","s","m","c"
    )


    private val prefs = context.getSharedPreferences("language_reader_books", Context.MODE_PRIVATE)

    fun listBooks(): List<BookItem> {
        val raw = prefs.getString("books", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BookItem(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            type = o.getString("type"),
                            sourceFile = o.getString("sourceFile"),
                            textCacheFile = o.getString("textCacheFile"),
                            totalChars = o.optInt("totalChars", 0),
                            totalWords = o.optInt("totalWords", 0),
                            uniqueWords = o.optInt("uniqueWords", 0),
                            totalParagraphs = o.optInt("totalParagraphs", 0),
                            coverFile = o.optString("coverFile", ""),
                            chapterFile = o.optString("chapterFile", ""),
                            sourceHash = o.optString("sourceHash", ""),
                            updatedAt = o.optLong("updatedAt", 0L),
                        )
                    )
                }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    /** One-time migration for old books; call from an IO dispatcher. */
    fun backfillWordStats(): List<BookItem> {
        val current = listBooks()
        val enriched = current.map { ensureWordStats(it) }
        if (enriched != current) saveBooks(enriched)
        return enriched.sortedByDescending { it.updatedAt }
    }

    private fun ensureWordStats(book: BookItem): BookItem {
        if (book.totalWords > 0 || book.totalChars == 0) return book
        val file = File(book.textCacheFile)
        if (!file.exists()) return book
        val text = paragraphs(book)
            .filterNot { isImageParagraph(it) }
            .joinToString("\n")
        if (text.isBlank()) return book
        val regex = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’'][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")
        var total = 0
        val unique = linkedSetOf<String>()
        regex.findAll(text).forEach { match ->
            total++
            unique += match.value.replace('’', '\'').lowercase()
        }
        return book.copy(totalWords = total, uniqueWords = unique.size)
    }

    private fun saveBooks(items: List<BookItem>) {
        val arr = JSONArray()
        items.sortedByDescending { it.updatedAt }.forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("title", b.title)
                    .put("type", b.type)
                    .put("sourceFile", b.sourceFile)
                    .put("textCacheFile", b.textCacheFile)
                    .put("totalChars", b.totalChars)
                    .put("totalWords", b.totalWords)
                    .put("uniqueWords", b.uniqueWords)
                    .put("totalParagraphs", b.totalParagraphs)
                    .put("coverFile", b.coverFile)
                    .put("chapterFile", b.chapterFile)
                    .put("sourceHash", b.sourceHash)
                    .put("updatedAt", b.updatedAt)
            )
        }
        prefs.edit().putString("books", arr.toString()).apply()
    }

    fun upsert(book: BookItem) {
        saveBooks(listBooks().filterNot { it.id == book.id } + book)
    }

    fun remove(book: BookItem) {
        runCatching { File(book.sourceFile).delete() }
        runCatching { File(book.textCacheFile).delete() }
        if (book.coverFile.isNotBlank()) runCatching { File(book.coverFile).delete() }
        if (book.chapterFile.isNotBlank()) runCatching { File(book.chapterFile).delete() }
        runCatching {
            File(File(book.sourceFile).parentFile, "${book.id}.images").deleteRecursively()
        }

        // Remove all per-book state so re-importing the same source cannot revive
        // old progress, bookmarks, annotations or PDF answer boxes. Vocabulary is
        // kept, but detached from the deleted book so learned words are not lost.
        prefs.edit()
            .remove("progress_${book.id}")
            .remove("bookmarks_${book.id}")
            .remove("readium_bookmarks_${book.id}")
            .remove("favorite_${book.id}")
            .remove("last_opened_${book.id}")
            .remove("reader_position_${book.id}")
            .remove("image_repair_attempt_${book.id}")
            .apply()
        LibraryOrganizer(context).clearBook(book.id)
        PdfTextBoxStore(context).clear(book.id)
        ReadingStatsStore(context).clearBook(book.id)
        LearningStore(context).let { store ->
            try {
                store.removeBookState(book.id)
            } finally {
                store.close()
            }
        }
        context.getSharedPreferences("lirelia_readium_progress", Context.MODE_PRIVATE)
            .edit().remove("locator_${book.id}").apply()
        context.getSharedPreferences("lirelia_readium_migrations", Context.MODE_PRIVATE)
            .edit()
            .remove("legacy_annotations_v42_${book.id}")
            .remove("legacy_annotations_v42_${book.id}_count")
            .remove("legacy_annotations_v42_${book.id}_unresolved")
            .remove("legacy_bookmarks_v42_${book.id}")
            .apply()
        if (book.sourceHash.isNotBlank()) {
            context.getSharedPreferences("language_reader_restore_aliases", Context.MODE_PRIVATE)
                .edit().remove("sha256_${book.sourceHash}").apply()
        }

        saveBooks(listBooks().filterNot { it.id == book.id })
    }

    fun paragraphs(book: BookItem): List<String> {
        val file = File(book.textCacheFile)
        if (!file.exists()) return emptyList()

        var raw = runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")

        // V2.3 migration: old EPUB caches used [OBJ]/U+FFFC where an image
        // should have been. Re-open the original EPUB once and reconstruct
        // text + images instead of silently deleting that content.
        val hasOldObjectPlaceholder =
            raw.contains('\uFFFC') || Regex("""(?i)\[OBJ\]""").containsMatchIn(raw)

        if (
            book.type.equals("EPUB", ignoreCase = true) &&
            hasOldObjectPlaceholder &&
            !prefs.getBoolean("image_repair_attempt_${book.id}", false)
        ) {
            val source = File(book.sourceFile)
            if (source.exists()) {
                runCatching {
                    val imageDir = File(source.parentFile, "${book.id}.images").apply {
                        deleteRecursively()
                        mkdirs()
                    }
                    val extracted = EpubParser.extractDetailed(source, imageDir)
                    if (extracted.paragraphs.isNotEmpty()) {
                        raw = extracted.paragraphs.joinToString(PARAGRAPH_MARK)
                        file.writeText(raw, Charsets.UTF_8)
                        if (book.chapterFile.isNotBlank()) {
                            val arr = JSONArray()
                            extracted.chapters.forEach { chapter ->
                                arr.put(
                                    JSONObject()
                                        .put("title", chapter.title)
                                        .put("paragraphIndex", chapter.paragraphIndex)
                                )
                            }
                            File(book.chapterFile).writeText(arr.toString(), Charsets.UTF_8)
                        }
                    }
                }
            }
            prefs.edit().putBoolean("image_repair_attempt_${book.id}", true).apply()
        }

        return raw
            .split(PARAGRAPH_MARK)
            .map { paragraph ->
                val clean = paragraph.trim()
                if (isImageParagraph(clean)) {
                    clean
                } else {
                    clean
                        .replace('\uFFFC', ' ')
                        .replace(
                            Regex("""(?i)\[OBJ\]"""),
                            " [图片占位：原图未能从 EPUB 恢复] ",
                        )
                        .replace(Regex("""(?is)<object\b[^>]*>.*?</object>"""), " ")
                        .trim()
                }
            }
            .filter { it.isNotEmpty() }
    }


    fun topWordCandidates(
        book: BookItem,
        excluded: Set<String> = emptySet(),
        limit: Int = 24,
    ): List<BookWordCandidate> {
        val text = paragraphs(book)
            .filterNot { isImageParagraph(it) }
            .joinToString("\n")
        if (text.isBlank()) return emptyList()

        val counts = linkedMapOf<String, Int>()
        val regex = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’'][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")
        regex.findAll(text).forEach { match ->
            val word = match.value.replace('’', '\'').lowercase(Locale.FRENCH)
            if (word.length < 3 || word in FRENCH_STOP_WORDS || word in excluded) return@forEach
            counts[word] = (counts[word] ?: 0) + 1
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { BookWordCandidate(it.key, it.value) }
    }

    fun saveProgress(bookId: String, paragraphIndex: Int) {
        prefs.edit().putInt("progress_$bookId", paragraphIndex.coerceAtLeast(0)).apply()
    }

    /** Final lifecycle flush used when a reader is leaving the screen. */
    fun saveProgressImmediate(bookId: String, paragraphIndex: Int): Boolean =
        prefs.edit().putInt("progress_$bookId", paragraphIndex.coerceAtLeast(0)).commit()

    fun loadProgress(bookId: String): Int = prefs.getInt("progress_$bookId", 0)

    /**
     * V3.14 reading anchor: paragraph index + a normalized text fingerprint.
     * The index is fast for normal reopen; the fingerprint recovers the same
     * paragraph if pagination, orientation, or a lightly-normalized EPUB cache
     * changes around it.
     */
    fun saveReaderPosition(bookId: String, paragraphIndex: Int, paragraphText: String) {
        val index = paragraphIndex.coerceAtLeast(0)
        saveProgress(bookId, index)
        val anchor = paragraphAnchor(paragraphText)
        val json = JSONObject()
            .put("paragraphIndex", index)
            .put("anchor", anchor)
            .put("savedAt", System.currentTimeMillis())
        prefs.edit().putString("reader_position_$bookId", json.toString()).apply()
    }

    fun resolveReaderPosition(bookId: String, paragraphs: List<String>): Int {
        if (paragraphs.isEmpty()) return 0
        val fallback = loadProgress(bookId).coerceIn(0, paragraphs.lastIndex)
        val raw = prefs.getString("reader_position_$bookId", null) ?: return fallback
        val saved = runCatching { JSONObject(raw) }.getOrNull() ?: return fallback
        val storedIndex = saved.optInt("paragraphIndex", fallback).coerceIn(0, paragraphs.lastIndex)
        val anchor = saved.optString("anchor", "").trim()
        if (anchor.isBlank()) return storedIndex
        if (paragraphAnchor(paragraphs[storedIndex]) == anchor) return storedIndex

        // Search near the old position first so repeated sentences do not jump
        // across the book; fall back to a full scan only if necessary.
        val radius = 80
        val from = (storedIndex - radius).coerceAtLeast(0)
        val to = (storedIndex + radius).coerceAtMost(paragraphs.lastIndex)
        for (i in from..to) {
            if (paragraphAnchor(paragraphs[i]) == anchor) return i
        }
        val exact = paragraphs.indexOfFirst { paragraphAnchor(it) == anchor }
        return if (exact >= 0) exact else fallback
    }

    private fun paragraphAnchor(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase(Locale.FRENCH)
        .take(180)


    fun chapters(book: BookItem): List<ChapterItem> {
        if (book.chapterFile.isBlank()) return emptyList()
        val file = File(book.chapterFile)
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(ChapterItem(o.optString("title", "章节 ${i + 1}"), o.optInt("paragraphIndex", 0)))
                }
            }.distinctBy { it.paragraphIndex }
        }.getOrDefault(emptyList())
    }

    fun bookmarks(bookId: String): Set<Int> {
        val raw = prefs.getString("bookmarks_$bookId", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildSet { for (i in 0 until arr.length()) add(arr.getInt(i)) }
        }.getOrDefault(emptySet())
    }

    fun toggleBookmark(bookId: String, paragraphIndex: Int): Set<Int> {
        val current = bookmarks(bookId).toMutableSet()
        if (!current.add(paragraphIndex)) current.remove(paragraphIndex)
        val arr = JSONArray()
        current.sorted().forEach { arr.put(it) }
        prefs.edit().putString("bookmarks_$bookId", arr.toString()).apply()
        return current
    }


    fun readiumBookmarks(bookId: String): List<ReadiumBookmark> {
        val raw = prefs.getString("readium_bookmarks_$bookId", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val locator = o.optString("locator").trim()
                    if (locator.isBlank()) continue
                    add(
                        ReadiumBookmark(
                            locatorJson = locator,
                            label = o.optString("label", ""),
                            createdAt = o.optLong("createdAt", 0L),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun isReadiumBookmarked(bookId: String, locatorJson: String): Boolean {
        val key = readiumBookmarkKey(locatorJson)
        return key.isNotBlank() && readiumBookmarks(bookId).any { readiumBookmarkKey(it.locatorJson) == key }
    }

    fun toggleReadiumBookmark(bookId: String, locatorJson: String, label: String = ""): List<ReadiumBookmark> {
        if (bookId.isBlank() || locatorJson.isBlank()) return readiumBookmarks(bookId)
        val key = readiumBookmarkKey(locatorJson)
        if (key.isBlank()) return readiumBookmarks(bookId)
        val current = readiumBookmarks(bookId).toMutableList()
        val index = current.indexOfFirst { readiumBookmarkKey(it.locatorJson) == key }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current += ReadiumBookmark(locatorJson, label.trim(), System.currentTimeMillis())
        }
        saveReadiumBookmarks(bookId, current)
        return current
    }

    fun removeReadiumBookmark(bookId: String, locatorJson: String): List<ReadiumBookmark> {
        val key = readiumBookmarkKey(locatorJson)
        val next = readiumBookmarks(bookId).filterNot { readiumBookmarkKey(it.locatorJson) == key }
        saveReadiumBookmarks(bookId, next)
        return next
    }

    private fun saveReadiumBookmarks(bookId: String, items: List<ReadiumBookmark>) {
        val arr = JSONArray()
        items.sortedBy { it.createdAt }.forEach { item ->
            arr.put(
                JSONObject()
                    .put("locator", item.locatorJson)
                    .put("label", item.label)
                    .put("createdAt", item.createdAt)
            )
        }
        prefs.edit().putString("readium_bookmarks_$bookId", arr.toString()).apply()
    }

    private fun readiumBookmarkKey(locatorJson: String): String = runCatching {
        val root = JSONObject(locatorJson)
        val href = root.optString("href")
        val locations = root.optJSONObject("locations") ?: JSONObject()
        val position = locations.optInt("position", -1)
        val progression = locations.optDouble("progression", Double.NaN)
        when {
            href.isBlank() -> ""
            position >= 0 -> "$href#p$position"
            progression.isFinite() -> "$href#g${(progression * 1000.0).toInt()}"
            else -> href
        }
    }.getOrDefault("")


    fun isFavorite(bookId: String): Boolean = prefs.getBoolean("favorite_$bookId", false)

    fun toggleFavorite(bookId: String): Boolean {
        val next = !isFavorite(bookId)
        prefs.edit().putBoolean("favorite_$bookId", next).apply()
        return next
    }

    fun markOpened(bookId: String) {
        prefs.edit().putLong("last_opened_$bookId", System.currentTimeMillis()).apply()
    }

    fun lastOpened(bookId: String): Long = prefs.getLong("last_opened_$bookId", 0L)

    companion object {
        const val PARAGRAPH_MARK = "\n\u241E\n"

        // Printable marker: safe across Html.fromHtml() / text normalization.
        private const val IMAGE_PREFIX = "[[LR_IMAGE:"
        private const val IMAGE_SUFFIX = ":LR_IMAGE]]"

        // Legacy V2.3 marker: keep compatibility with already imported books.
        private const val LEGACY_IMAGE_PREFIX = "\u001FIMG:"
        private const val LEGACY_IMAGE_SUFFIX = "\u001F"

        private val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "avif"
        )

        fun imageMarker(path: String): String =
            IMAGE_PREFIX + path + IMAGE_SUFFIX

        fun isImageParagraph(text: String): Boolean =
            imagePath(text) != null

        fun imagePath(text: String): String? {
            val clean = text.trim()

            if (clean.startsWith(IMAGE_PREFIX) && clean.endsWith(IMAGE_SUFFIX)) {
                return clean
                    .removePrefix(IMAGE_PREFIX)
                    .removeSuffix(IMAGE_SUFFIX)
                    .takeIf { it.isNotBlank() }
            }

            if (
                clean.startsWith(LEGACY_IMAGE_PREFIX) &&
                clean.endsWith(LEGACY_IMAGE_SUFFIX)
            ) {
                return clean
                    .removePrefix(LEGACY_IMAGE_PREFIX)
                    .removeSuffix(LEGACY_IMAGE_SUFFIX)
                    .takeIf { it.isNotBlank() }
            }

            // Repair V2.3 cached books where only the local image path survived.
            val localPath = when {
                clean.startsWith("file://") -> runCatching {
                    android.net.Uri.parse(clean).path
                }.getOrNull()
                clean.startsWith("/") -> clean
                else -> null
            } ?: return null

            val file = java.io.File(localPath)
            return localPath.takeIf {
                file.exists() &&
                    file.isFile &&
                    file.extension.lowercase() in IMAGE_EXTENSIONS
            }
        }
    }
}
