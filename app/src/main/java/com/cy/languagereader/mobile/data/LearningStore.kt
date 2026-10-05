package com.cy.languagereader.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class LearningStore(context: Context) : SQLiteOpenHelper(context, "learning.db", null, 11) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE vocab(
                word_norm TEXT PRIMARY KEY,
                word TEXT NOT NULL,
                lemma TEXT,
                definition TEXT,
                context TEXT,
                created_at INTEGER NOT NULL,
                book_id TEXT DEFAULT '',
                book_title TEXT DEFAULT '',
                review_count INTEGER DEFAULT 0,
                last_reviewed_at INTEGER DEFAULT 0,
                next_review_at INTEGER DEFAULT 0,
                status TEXT DEFAULT 'learning',
                marker_color TEXT DEFAULT 'yellow'
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE highlights(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id TEXT NOT NULL,
                paragraph_index INTEGER NOT NULL,
                text TEXT NOT NULL,
                color TEXT DEFAULT 'yellow',
                note TEXT DEFAULT '',
                start_offset INTEGER DEFAULT -1,
                end_offset INTEGER DEFAULT -1,
                created_at INTEGER NOT NULL,
                locator_json TEXT DEFAULT '',
                source TEXT DEFAULT 'legacy'
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_highlights_book ON highlights(book_id)")
        db.execSQL("CREATE UNIQUE INDEX idx_highlights_legacy_range ON highlights(book_id,paragraph_index,start_offset,end_offset) WHERE source='legacy'")
        db.execSQL("CREATE UNIQUE INDEX idx_highlights_readium_locator ON highlights(book_id,locator_json) WHERE source='readium' AND locator_json<>''")
        db.execSQL(
            """CREATE TABLE vocab_contexts(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word_norm TEXT NOT NULL,
                context TEXT NOT NULL,
                book_id TEXT DEFAULT '',
                book_title TEXT DEFAULT '',
                created_at INTEGER NOT NULL,
                UNIQUE(word_norm, context, book_id)
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_vocab_contexts_word ON vocab_contexts(word_norm, created_at DESC)")

        db.execSQL(
            """CREATE TABLE expressions(
                expression_norm TEXT PRIMARY KEY,
                expression TEXT NOT NULL,
                definition TEXT DEFAULT '',
                context TEXT DEFAULT '',
                created_at INTEGER NOT NULL,
                book_id TEXT DEFAULT '',
                book_title TEXT DEFAULT '',
                status TEXT DEFAULT 'learning'
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE expression_contexts(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                expression_norm TEXT NOT NULL,
                context TEXT NOT NULL,
                book_id TEXT DEFAULT '',
                book_title TEXT DEFAULT '',
                created_at INTEGER NOT NULL,
                UNIQUE(expression_norm, context, book_id)
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_expression_contexts_expr ON expression_contexts(expression_norm, created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN book_id TEXT DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN book_title TEXT DEFAULT ''") }
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN review_count INTEGER DEFAULT 0") }
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN last_reviewed_at INTEGER DEFAULT 0") }
        }
        if (oldVersion < 3) {
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN next_review_at INTEGER DEFAULT 0") }
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("ALTER TABLE highlights ADD COLUMN color TEXT DEFAULT 'yellow'") }
            runCatching { db.execSQL("ALTER TABLE highlights ADD COLUMN note TEXT DEFAULT ''") }
        }
        if (oldVersion < 5) {
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN status TEXT DEFAULT 'learning'") }
        }
        if (oldVersion < 6) {
            runCatching { db.execSQL("ALTER TABLE highlights ADD COLUMN start_offset INTEGER DEFAULT -1") }
            runCatching { db.execSQL("ALTER TABLE highlights ADD COLUMN end_offset INTEGER DEFAULT -1") }
        }
        if (oldVersion < 7) {
            // Rebuild instead of ALTER: the old table still had the legacy UNIQUE
            // constraint (book_id, paragraph_index, text), which could silently reject
            // valid precise-range annotations.
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS highlights_v7(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id TEXT NOT NULL,
                    paragraph_index INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    color TEXT DEFAULT 'yellow',
                    note TEXT DEFAULT '',
                    start_offset INTEGER DEFAULT -1,
                    end_offset INTEGER DEFAULT -1,
                    created_at INTEGER NOT NULL,
                    UNIQUE(book_id, paragraph_index, start_offset, end_offset)
                )""".trimIndent()
            )
            db.execSQL(
                """INSERT OR REPLACE INTO highlights_v7(
                       book_id,paragraph_index,text,color,note,start_offset,end_offset,created_at
                   )
                   SELECT book_id,paragraph_index,text,
                          COALESCE(color,'yellow'),
                          COALESCE(note,''),
                          COALESCE(start_offset,-1),
                          COALESCE(end_offset,-1),
                          created_at
                   FROM highlights""".trimIndent()
            )
            db.execSQL("DROP TABLE highlights")
            db.execSQL("ALTER TABLE highlights_v7 RENAME TO highlights")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_highlights_book ON highlights(book_id)")
        }
        if (oldVersion < 8) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS vocab_contexts(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    word_norm TEXT NOT NULL,
                    context TEXT NOT NULL,
                    book_id TEXT DEFAULT '',
                    book_title TEXT DEFAULT '',
                    created_at INTEGER NOT NULL,
                    UNIQUE(word_norm, context, book_id)
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_vocab_contexts_word ON vocab_contexts(word_norm, created_at DESC)")
            // Preserve the single source sentence stored by older versions.
            db.execSQL(
                """INSERT OR IGNORE INTO vocab_contexts(word_norm,context,book_id,book_title,created_at)
                   SELECT word_norm,context,COALESCE(book_id,''),COALESCE(book_title,''),created_at
                   FROM vocab WHERE TRIM(COALESCE(context,''))<>''""".trimIndent()
            )
        }
        if (oldVersion < 9) {
            runCatching { db.execSQL("ALTER TABLE vocab ADD COLUMN marker_color TEXT DEFAULT 'yellow'") }
        }

        if (oldVersion < 10) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS expressions(
                    expression_norm TEXT PRIMARY KEY,
                    expression TEXT NOT NULL,
                    definition TEXT DEFAULT '',
                    context TEXT DEFAULT '',
                    created_at INTEGER NOT NULL,
                    book_id TEXT DEFAULT '',
                    book_title TEXT DEFAULT '',
                    status TEXT DEFAULT 'learning'
                )""".trimIndent()
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS expression_contexts(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expression_norm TEXT NOT NULL,
                    context TEXT NOT NULL,
                    book_id TEXT DEFAULT '',
                    book_title TEXT DEFAULT '',
                    created_at INTEGER NOT NULL,
                    UNIQUE(expression_norm, context, book_id)
                )""".trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_expression_contexts_expr ON expression_contexts(expression_norm, created_at DESC)")
        }
        if (oldVersion < 11) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS highlights_v11(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id TEXT NOT NULL,
                    paragraph_index INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    color TEXT DEFAULT 'yellow',
                    note TEXT DEFAULT '',
                    start_offset INTEGER DEFAULT -1,
                    end_offset INTEGER DEFAULT -1,
                    created_at INTEGER NOT NULL,
                    locator_json TEXT DEFAULT '',
                    source TEXT DEFAULT 'legacy'
                )""".trimIndent()
            )
            db.execSQL(
                """INSERT INTO highlights_v11(
                       id,book_id,paragraph_index,text,color,note,start_offset,end_offset,created_at,locator_json,source
                   )
                   SELECT id,book_id,paragraph_index,text,COALESCE(color,'yellow'),COALESCE(note,''),
                          COALESCE(start_offset,-1),COALESCE(end_offset,-1),created_at,'','legacy'
                   FROM highlights""".trimIndent()
            )
            db.execSQL("DROP TABLE highlights")
            db.execSQL("ALTER TABLE highlights_v11 RENAME TO highlights")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_highlights_book ON highlights(book_id)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_highlights_legacy_range ON highlights(book_id,paragraph_index,start_offset,end_offset) WHERE source='legacy'")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_highlights_readium_locator ON highlights(book_id,locator_json) WHERE source='readium' AND locator_json<>''")
        }
    }

    fun addVocabulary(result: DictionaryResult, contextText: String, bookId: String = "", bookTitle: String = "") {
        val key = normalize(result.query)
        data class Existing(
            val reviewCount: Int,
            val lastReviewedAt: Long,
            val nextReviewAt: Long,
            val status: String,
            val context: String,
            val bookId: String,
            val bookTitle: String,
            val markerColor: String,
        )
        val existing = readableDatabase.rawQuery(
            """SELECT review_count,last_reviewed_at,COALESCE(next_review_at,0),
                      COALESCE(status,'learning'),COALESCE(context,''),
                      COALESCE(book_id,''),COALESCE(book_title,''),COALESCE(marker_color,'yellow')
               FROM vocab WHERE word_norm=?""".trimIndent(),
            arrayOf(key),
        ).use { c ->
            if (c.moveToFirst()) Existing(c.getInt(0), c.getLong(1), c.getLong(2), c.getString(3) ?: "learning", c.getString(4) ?: "", c.getString(5) ?: "", c.getString(6) ?: "", c.getString(7) ?: "yellow")
            else Existing(0, 0L, 0L, "learning", "", "", "", "yellow")
        }

        val cleanContext = contextText.trim()
        val effectiveContext = cleanContext.ifBlank { existing.context }
        val effectiveBookId = if (cleanContext.isNotBlank() && bookId.isNotBlank()) bookId else existing.bookId.ifBlank { bookId }
        val effectiveBookTitle = if (cleanContext.isNotBlank() && bookTitle.isNotBlank()) bookTitle else existing.bookTitle.ifBlank { bookTitle }
        val now = System.currentTimeMillis()

        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("word_norm", key)
                put("word", result.query)
                put("lemma", result.lemma)
                put("definition", result.definitions.firstOrNull().orEmpty())
                put("context", effectiveContext)
                put("created_at", now)
                put("book_id", effectiveBookId)
                put("book_title", effectiveBookTitle)
                put("review_count", existing.reviewCount)
                put("last_reviewed_at", existing.lastReviewedAt)
                put("next_review_at", existing.nextReviewAt)
                put("status", existing.status)
                put("marker_color", existing.markerColor)
            }
            db.insertWithOnConflict("vocab", null, values, SQLiteDatabase.CONFLICT_REPLACE)

            if (cleanContext.isNotBlank()) {
                val contextValues = ContentValues().apply {
                    put("word_norm", key)
                    put("context", cleanContext)
                    put("book_id", bookId)
                    put("book_title", bookTitle)
                    put("created_at", now)
                }
                db.insertWithOnConflict("vocab_contexts", null, contextValues, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun vocabularyContexts(word: String, limit: Int = 6): List<VocabularyContext> = buildList {
        readableDatabase.rawQuery(
            """SELECT context,COALESCE(book_id,''),COALESCE(book_title,''),created_at
               FROM vocab_contexts WHERE word_norm=?
               ORDER BY created_at DESC LIMIT ?""".trimIndent(),
            arrayOf(normalize(word), limit.coerceIn(1, 20).toString()),
        ).use { c ->
            while (c.moveToNext()) {
                add(VocabularyContext(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3)))
            }
        }
    }

    fun containsVocabulary(word: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM vocab WHERE word_norm=? LIMIT 1", arrayOf(normalize(word))
    ).use { it.moveToFirst() }

    fun removeVocabulary(word: String) {
        val key = normalize(word)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("vocab", "word_norm=?", arrayOf(key))
            db.delete("vocab_contexts", "word_norm=?", arrayOf(key))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Removes this lexical item everywhere, not only the tapped occurrence.
     * Notes/highlights whose selected text is exactly the same normalized word are
     * deleted across all books; sentence/phrase annotations are left untouched.
     */
    fun removeWordEverywhere(word: String): Int {
        val key = normalize(word)
        if (key.isBlank()) return 0
        val db = writableDatabase
        val ids = mutableListOf<Long>()
        db.rawQuery("SELECT id,text FROM highlights", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val text = c.getString(1) ?: ""
                if (normalize(text) == key) ids += id
            }
        }
        db.beginTransaction()
        try {
            ids.forEach { id -> db.delete("highlights", "id=?", arrayOf(id.toString())) }
            db.delete("vocab", "word_norm=?", arrayOf(key))
            db.delete("vocab_contexts", "word_norm=?", arrayOf(key))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ids.size
    }

    fun markReviewed(word: String) {
        val key = normalize(word)
        val current = readableDatabase.rawQuery(
            "SELECT COALESCE(review_count,0) FROM vocab WHERE word_norm=?", arrayOf(key)
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        val newCount = current + 1
        val days = when {
            newCount <= 1 -> 1
            newCount == 2 -> 3
            newCount == 3 -> 7
            newCount == 4 -> 14
            else -> 30
        }
        val now = System.currentTimeMillis()
        val next = now + days * 24L * 60L * 60L * 1000L
        writableDatabase.execSQL(
            "UPDATE vocab SET review_count=?,last_reviewed_at=?,next_review_at=? WHERE word_norm=?",
            arrayOf(newCount, now, next, key),
        )
    }


    /** V3.19: personal-vocabulary scheduling is owned by AdaptiveTrainingStore. */
    fun syncVocabularyReview(word: String, nextDueAt: Long) {
        val key = normalize(word)
        val now = System.currentTimeMillis()
        writableDatabase.execSQL(
            "UPDATE vocab SET review_count=COALESCE(review_count,0)+1,last_reviewed_at=?,next_review_at=? WHERE word_norm=?",
            arrayOf(now, nextDueAt.coerceAtLeast(0L), key),
        )
    }

    fun vocabulary(): List<VocabularyItem> = buildList {
        readableDatabase.rawQuery(
            """SELECT word,lemma,definition,context,created_at,
               COALESCE(book_id,''),COALESCE(book_title,''),COALESCE(review_count,0),COALESCE(last_reviewed_at,0),
               COALESCE(next_review_at,0),COALESCE(status,'learning')
               FROM vocab ORDER BY created_at DESC""".trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) add(
                VocabularyItem(
                    word = c.getString(0), lemma = c.getString(1) ?: "", definition = c.getString(2) ?: "",
                    context = c.getString(3) ?: "", createdAt = c.getLong(4), bookId = c.getString(5) ?: "",
                    bookTitle = c.getString(6) ?: "", reviewCount = c.getInt(7), lastReviewedAt = c.getLong(8),
                    nextReviewAt = c.getLong(9), status = c.getString(10) ?: "learning",
                )
            )
        }
    }

    fun bookVocabularyCount(bookId: String): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM vocab WHERE book_id=?", arrayOf(bookId)
    ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun highlightCount(bookId: String): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM highlights WHERE book_id=?", arrayOf(bookId)
    ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun dueVocabulary(now: Long = System.currentTimeMillis()): List<VocabularyItem> =
        vocabulary().filter { it.status != "known" && (it.nextReviewAt == 0L || it.nextReviewAt <= now) }


    fun vocabularyStatus(word: String): String? = readableDatabase.rawQuery(
        "SELECT COALESCE(status,'learning') FROM vocab WHERE word_norm=? LIMIT 1",
        arrayOf(normalize(word)),
    ).use { c -> if (c.moveToFirst()) c.getString(0) ?: "learning" else null }

    fun markKnown(word: String) {
        val now = System.currentTimeMillis()
        writableDatabase.execSQL(
            "UPDATE vocab SET status='known',last_reviewed_at=?,next_review_at=0 WHERE word_norm=?",
            arrayOf(now, normalize(word)),
        )
    }

    fun markLearning(word: String) {
        writableDatabase.execSQL(
            "UPDATE vocab SET status='learning',next_review_at=0 WHERE word_norm=?",
            arrayOf(normalize(word)),
        )
    }

    fun bookLearningCount(bookId: String): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM vocab WHERE book_id=? AND COALESCE(status,'learning')='learning'",
        arrayOf(bookId),
    ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun bookKnownCount(bookId: String): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM vocab WHERE book_id=? AND status='known'",
        arrayOf(bookId),
    ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }


    fun reviewedTodayCount(): Int {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        return readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM vocab WHERE last_reviewed_at>=?",
            arrayOf(start.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    fun knownNormalizedWords(): Set<String> = buildSet {
        readableDatabase.rawQuery(
            "SELECT word_norm FROM vocab WHERE status='known'",
            null,
        ).use { c -> while (c.moveToNext()) add(c.getString(0)) }
    }

    fun trackedNormalizedWords(): Set<String> = buildSet {
        readableDatabase.rawQuery("SELECT word_norm FROM vocab", null).use { c ->
            while (c.moveToNext()) add(c.getString(0))
        }
    }

    /** V3.16 global vocabulary radar: learning words reappear softly across every book. */
    fun learningWordColors(): Map<String, String> = buildMap {
        readableDatabase.rawQuery(
            "SELECT word_norm,COALESCE(marker_color,'yellow') FROM vocab WHERE COALESCE(status,'learning')='learning'",
            null,
        ).use { c ->
            while (c.moveToNext()) put(c.getString(0), c.getString(1).ifBlank { "yellow" })
        }
    }

    fun setVocabularyMarkerColor(word: String, color: String) {
        val safe = when (color.trim().lowercase(Locale.ROOT)) {
            "yellow", "green", "blue", "pink", "gray" -> color.trim().lowercase(Locale.ROOT)
            "grey" -> "gray"
            else -> "yellow"
        }
        writableDatabase.execSQL(
            "UPDATE vocab SET marker_color=?,status='learning' WHERE word_norm=?",
            arrayOf(safe, normalize(word)),
        )
    }

    fun vocabularyMarkerColor(word: String): String? = readableDatabase.rawQuery(
        "SELECT COALESCE(marker_color,'yellow') FROM vocab WHERE word_norm=? LIMIT 1",
        arrayOf(normalize(word)),
    ).use { c -> if (c.moveToFirst()) c.getString(0) else null }


    fun addExpression(expression: String, definition: String, contextText: String, bookId: String = "", bookTitle: String = "") {
        val clean = expression.trim()
        if (clean.isBlank()) return
        val key = normalize(clean)
        val now = System.currentTimeMillis()
        val old = readableDatabase.rawQuery(
            "SELECT COALESCE(status,'learning'),COALESCE(context,''),COALESCE(book_id,''),COALESCE(book_title,'') FROM expressions WHERE expression_norm=?",
            arrayOf(key),
        ).use { c -> if (c.moveToFirst()) listOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3)) else null }
        val context = contextText.trim().ifBlank { old?.getOrNull(1).orEmpty() }
        val values = ContentValues().apply {
            put("expression_norm", key); put("expression", clean); put("definition", definition.trim()); put("context", context)
            put("created_at", now); put("book_id", bookId.ifBlank { old?.getOrNull(2).orEmpty() }); put("book_title", bookTitle.ifBlank { old?.getOrNull(3).orEmpty() })
            put("status", old?.getOrNull(0) ?: "learning")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("expressions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            if (contextText.isNotBlank()) {
                val cv = ContentValues().apply {
                    put("expression_norm", key); put("context", contextText.trim()); put("book_id", bookId); put("book_title", bookTitle); put("created_at", now)
                }
                db.insertWithOnConflict("expression_contexts", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun containsExpression(expression: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM expressions WHERE expression_norm=? LIMIT 1", arrayOf(normalize(expression))
    ).use { it.moveToFirst() }

    fun expressions(): List<PhraseItem> = buildList {
        readableDatabase.rawQuery(
            """SELECT expression,COALESCE(definition,''),COALESCE(context,''),created_at,
                      COALESCE(book_id,''),COALESCE(book_title,''),COALESCE(status,'learning')
               FROM expressions ORDER BY created_at DESC""".trimIndent(), null
        ).use { c ->
            while (c.moveToNext()) add(PhraseItem(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getString(5), c.getString(6)))
        }
    }

    fun expressionContexts(expression: String, limit: Int = 8): List<VocabularyContext> = buildList {
        readableDatabase.rawQuery(
            """SELECT context,COALESCE(book_id,''),COALESCE(book_title,''),created_at
               FROM expression_contexts WHERE expression_norm=? ORDER BY created_at DESC LIMIT ?""".trimIndent(),
            arrayOf(normalize(expression), limit.coerceIn(1, 20).toString()),
        ).use { c -> while (c.moveToNext()) add(VocabularyContext(c.getString(0), c.getString(1), c.getString(2), c.getLong(3))) }
    }

    fun markExpressionKnown(expression: String) {
        writableDatabase.execSQL("UPDATE expressions SET status='known' WHERE expression_norm=?", arrayOf(normalize(expression)))
    }

    fun markExpressionLearning(expression: String) {
        writableDatabase.execSQL("UPDATE expressions SET status='learning' WHERE expression_norm=?", arrayOf(normalize(expression)))
    }

    fun removeExpression(expression: String) {
        val key = normalize(expression)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("expressions", "expression_norm=?", arrayOf(key))
            db.delete("expression_contexts", "expression_norm=?", arrayOf(key))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun addHighlight(bookId: String, paragraphIndex: Int, text: String) {
        val values = ContentValues().apply {
            put("book_id", bookId); put("paragraph_index", paragraphIndex); put("text", text); put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("highlights", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeHighlight(bookId: String, paragraphIndex: Int) {
        writableDatabase.delete("highlights", "book_id=? AND paragraph_index=?", arrayOf(bookId, paragraphIndex.toString()))
    }

    fun highlightedParagraphs(bookId: String): Set<Int> = buildSet {
        readableDatabase.rawQuery("SELECT DISTINCT paragraph_index FROM highlights WHERE book_id=?", arrayOf(bookId)).use { c ->
            while (c.moveToNext()) add(c.getInt(0))
        }
    }

    fun setAnnotation(bookId: String, paragraphIndex: Int, text: String, color: String, note: String = "") {
        writableDatabase.delete(
            "highlights",
            "book_id=? AND paragraph_index=?",
            arrayOf(bookId, paragraphIndex.toString()),
        )
        val values = ContentValues().apply {
            put("book_id", bookId)
            put("paragraph_index", paragraphIndex)
            put("text", text)
            put("color", color)
            put("note", note)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("highlights", null, values)
    }

    fun annotationColors(bookId: String): Map<Int, String> = buildMap {
        readableDatabase.rawQuery(
            "SELECT paragraph_index,COALESCE(color,'yellow') FROM highlights WHERE book_id=?",
            arrayOf(bookId),
        ).use { c -> while (c.moveToNext()) put(c.getInt(0), c.getString(1) ?: "yellow") }
    }

    fun annotationNote(bookId: String, paragraphIndex: Int): String =
        readableDatabase.rawQuery(
            "SELECT COALESCE(note,'') FROM highlights WHERE book_id=? AND paragraph_index=? LIMIT 1",
            arrayOf(bookId, paragraphIndex.toString()),
        ).use { c -> if (c.moveToFirst()) c.getString(0) ?: "" else "" }


    fun addTextAnnotation(
        bookId: String,
        paragraphIndex: Int,
        fullParagraph: String,
        startOffset: Int,
        endOffset: Int,
        color: String = "yellow",
        note: String = "",
    ) {
        if (fullParagraph.isEmpty()) return
        val start = startOffset.coerceIn(0, fullParagraph.length)
        val end = endOffset.coerceIn(start, fullParagraph.length)
        if (end <= start) return
        val selected = fullParagraph.substring(start, end)

        // Replace only the same range, not every highlight in the paragraph.
        writableDatabase.delete(
            "highlights",
            "book_id=? AND paragraph_index=? AND start_offset=? AND end_offset=?",
            arrayOf(bookId, paragraphIndex.toString(), start.toString(), end.toString()),
        )

        val values = ContentValues().apply {
            put("book_id", bookId)
            put("paragraph_index", paragraphIndex)
            put("text", selected)
            put("color", color)
            put("note", note)
            put("start_offset", start)
            put("end_offset", end)
            put("created_at", System.currentTimeMillis())
        }
        val rowId = writableDatabase.insertWithOnConflict(
            "highlights",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        if (rowId == -1L) {
            // One more deterministic fallback instead of silently losing a mark.
            writableDatabase.delete(
                "highlights",
                "book_id=? AND paragraph_index=? AND start_offset=? AND end_offset=?",
                arrayOf(bookId, paragraphIndex.toString(), start.toString(), end.toString()),
            )
            writableDatabase.insert("highlights", null, values)
        }
    }

    fun removeTextAnnotation(bookId: String, paragraphIndex: Int, startOffset: Int, endOffset: Int) {
        writableDatabase.delete(
            "highlights",
            "book_id=? AND paragraph_index=? AND start_offset=? AND end_offset=?",
            arrayOf(bookId, paragraphIndex.toString(), startOffset.toString(), endOffset.toString()),
        )
    }

    fun textAnnotations(bookId: String): Map<Int, List<TextAnnotation>> {
        val grouped = linkedMapOf<Int, MutableList<TextAnnotation>>()
        readableDatabase.rawQuery(
            """SELECT paragraph_index,text,COALESCE(color,'yellow'),COALESCE(note,''),
                      COALESCE(start_offset,-1),COALESCE(end_offset,-1)
               FROM highlights WHERE book_id=? AND COALESCE(source,'legacy')='legacy' ORDER BY paragraph_index,start_offset,id""",
            arrayOf(bookId),
        ).use { c ->
            while (c.moveToNext()) {
                val paragraph = c.getInt(0)
                val item = TextAnnotation(
                    paragraphIndex = paragraph,
                    text = c.getString(1) ?: "",
                    color = c.getString(2) ?: "yellow",
                    note = c.getString(3) ?: "",
                    startOffset = c.getInt(4),
                    endOffset = c.getInt(5),
                )
                grouped.getOrPut(paragraph) { mutableListOf() }.add(item)
            }
        }
        return grouped
    }

    fun hasTextAnnotation(bookId: String, paragraphIndex: Int, startOffset: Int, endOffset: Int): Boolean =
        readableDatabase.rawQuery(
            """SELECT 1 FROM highlights
               WHERE book_id=? AND COALESCE(source,'legacy')='legacy' AND paragraph_index=? AND start_offset=? AND end_offset=? LIMIT 1""",
            arrayOf(bookId, paragraphIndex.toString(), startOffset.toString(), endOffset.toString()),
        ).use { it.moveToFirst() }


    fun allAnnotations(): List<Pair<String, TextAnnotation>> = buildList {
        readableDatabase.rawQuery(
            """SELECT book_id,paragraph_index,text,COALESCE(color,'yellow'),COALESCE(note,''),
                      COALESCE(start_offset,-1),COALESCE(end_offset,-1),
                      COALESCE(locator_json,''),COALESCE(source,'legacy')
               FROM highlights
               ORDER BY created_at DESC,id DESC""",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                add(
                    c.getString(0) to TextAnnotation(
                        paragraphIndex = c.getInt(1),
                        text = c.getString(2) ?: "",
                        color = c.getString(3) ?: "yellow",
                        note = c.getString(4) ?: "",
                        startOffset = c.getInt(5),
                        endOffset = c.getInt(6),
                        locatorJson = c.getString(7) ?: "",
                        source = c.getString(8) ?: "legacy",
                    )
                )
            }
        }
    }


    fun updateAnnotationNote(
        bookId: String,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
        note: String,
    ) {
        val values = ContentValues().apply { put("note", note) }
        writableDatabase.update(
            "highlights",
            values,
            "book_id=? AND paragraph_index=? AND start_offset=? AND end_offset=?",
            arrayOf(
                bookId,
                paragraphIndex.toString(),
                startOffset.toString(),
                endOffset.toString(),
            ),
        )
    }

    fun removeAnnotationExact(
        bookId: String,
        paragraphIndex: Int,
        startOffset: Int,
        endOffset: Int,
    ) {
        writableDatabase.delete(
            "highlights",
            "book_id=? AND paragraph_index=? AND start_offset=? AND end_offset=?",
            arrayOf(
                bookId,
                paragraphIndex.toString(),
                startOffset.toString(),
                endOffset.toString(),
            ),
        )
    }

    fun removeAnnotationByText(bookId: String, paragraphIndex: Int, text: String) {
        writableDatabase.delete(
            "highlights",
            "book_id=? AND paragraph_index=? AND text=?",
            arrayOf(bookId, paragraphIndex.toString(), text),
        )
    }


    /** Locator-backed EPUB annotation used by the Readium reader. */
    fun upsertReadiumAnnotation(
        bookId: String,
        locatorJson: String,
        text: String,
        color: String,
        note: String = "",
        createdAt: Long = System.currentTimeMillis(),
    ) {
        if (bookId.isBlank() || locatorJson.isBlank()) return
        val existing = readableDatabase.rawQuery(
            "SELECT id,created_at,COALESCE(note,'') FROM highlights WHERE book_id=? AND source='readium' AND locator_json=? LIMIT 1",
            arrayOf(bookId, locatorJson),
        ).use { c ->
            if (c.moveToFirst()) Triple(c.getLong(0), c.getLong(1), c.getString(2) ?: "") else null
        }
        val values = ContentValues().apply {
            put("book_id", bookId)
            put("paragraph_index", -1)
            put("text", text.trim())
            put("color", color)
            put("note", note.ifBlank { existing?.third.orEmpty() })
            put("start_offset", -1)
            put("end_offset", -1)
            put("created_at", existing?.second ?: createdAt)
            put("locator_json", locatorJson)
            put("source", "readium")
        }
        if (existing != null) {
            writableDatabase.update("highlights", values, "id=?", arrayOf(existing.first.toString()))
        } else {
            writableDatabase.insertWithOnConflict("highlights", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun readiumAnnotations(bookId: String): List<TextAnnotation> = buildList {
        readableDatabase.rawQuery(
            """SELECT paragraph_index,text,COALESCE(color,'yellow'),COALESCE(note,''),
                      COALESCE(start_offset,-1),COALESCE(end_offset,-1),
                      COALESCE(locator_json,''),COALESCE(source,'readium')
               FROM highlights WHERE book_id=? AND source='readium'
               ORDER BY created_at,id""".trimIndent(),
            arrayOf(bookId),
        ).use { c ->
            while (c.moveToNext()) add(
                TextAnnotation(
                    paragraphIndex = c.getInt(0),
                    text = c.getString(1) ?: "",
                    color = c.getString(2) ?: "yellow",
                    note = c.getString(3) ?: "",
                    startOffset = c.getInt(4),
                    endOffset = c.getInt(5),
                    locatorJson = c.getString(6) ?: "",
                    source = c.getString(7) ?: "readium",
                )
            )
        }
    }

    /** Promote an old paragraph-offset EPUB annotation to a Readium locator in-place. */
    fun promoteLegacyAnnotation(bookId: String, mark: TextAnnotation, locatorJson: String) {
        if (bookId.isBlank() || locatorJson.isBlank()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existingReadiumId = db.rawQuery(
                "SELECT id FROM highlights WHERE book_id=? AND source='readium' AND locator_json=? LIMIT 1",
                arrayOf(bookId, locatorJson),
            ).use { c -> if (c.moveToFirst()) c.getLong(0) else null }

            if (existingReadiumId != null) {
                val values = ContentValues().apply {
                    if (mark.note.isNotBlank()) put("note", mark.note)
                    if (mark.color.isNotBlank()) put("color", mark.color)
                    if (mark.text.isNotBlank()) put("text", mark.text)
                }
                if (values.size() > 0) {
                    db.update("highlights", values, "id=?", arrayOf(existingReadiumId.toString()))
                }
                db.delete(
                    "highlights",
                    "book_id=? AND source='legacy' AND paragraph_index=? AND start_offset=? AND end_offset=?",
                    arrayOf(bookId, mark.paragraphIndex.toString(), mark.startOffset.toString(), mark.endOffset.toString()),
                )
            } else {
                val values = ContentValues().apply {
                    put("locator_json", locatorJson)
                    put("source", "readium")
                }
                db.update(
                    "highlights", values,
                    "book_id=? AND source='legacy' AND paragraph_index=? AND start_offset=? AND end_offset=?",
                    arrayOf(bookId, mark.paragraphIndex.toString(), mark.startOffset.toString(), mark.endOffset.toString()),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateReadiumAnnotationNote(bookId: String, locatorJson: String, note: String) {
        val values = ContentValues().apply { put("note", note) }
        writableDatabase.update(
            "highlights", values,
            "book_id=? AND source='readium' AND locator_json=?",
            arrayOf(bookId, locatorJson),
        )
    }

    fun removeReadiumAnnotation(bookId: String, locatorJson: String) {
        writableDatabase.delete(
            "highlights",
            "book_id=? AND source='readium' AND locator_json=?",
            arrayOf(bookId, locatorJson),
        )
    }




    fun removeBookState(bookId: String) {
        if (bookId.isBlank()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("highlights", "book_id=?", arrayOf(bookId))
            db.execSQL(
                "UPDATE vocab SET book_id='',book_title='' WHERE book_id=?",
                arrayOf(bookId),
            )
            db.delete("vocab_contexts", "book_id=?", arrayOf(bookId))
            db.execSQL("UPDATE expressions SET book_id='',book_title='' WHERE book_id=?", arrayOf(bookId))
            db.delete("expression_contexts", "book_id=?", arrayOf(bookId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun exportBackupJson(): JSONObject {
        val root = JSONObject()

        val vocab = JSONArray()
        readableDatabase.rawQuery(
            """SELECT word_norm,word,COALESCE(lemma,''),COALESCE(definition,''),
                      COALESCE(context,''),created_at,COALESCE(book_id,''),
                      COALESCE(book_title,''),COALESCE(review_count,0),
                      COALESCE(last_reviewed_at,0),COALESCE(next_review_at,0),
                      COALESCE(status,'learning'),COALESCE(marker_color,'yellow')
               FROM vocab""".trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) {
                vocab.put(
                    JSONObject()
                        .put("word_norm", c.getString(0))
                        .put("word", c.getString(1))
                        .put("lemma", c.getString(2))
                        .put("definition", c.getString(3))
                        .put("context", c.getString(4))
                        .put("created_at", c.getLong(5))
                        .put("book_id", c.getString(6))
                        .put("book_title", c.getString(7))
                        .put("review_count", c.getInt(8))
                        .put("last_reviewed_at", c.getLong(9))
                        .put("next_review_at", c.getLong(10))
                        .put("status", c.getString(11))
                        .put("marker_color", c.getString(12))
                )
            }
        }
        root.put("vocab", vocab)

        val highlights = JSONArray()
        readableDatabase.rawQuery(
            """SELECT book_id,paragraph_index,text,COALESCE(color,'yellow'),
                      COALESCE(note,''),COALESCE(start_offset,-1),
                      COALESCE(end_offset,-1),created_at,
                      COALESCE(locator_json,''),COALESCE(source,'legacy')
               FROM highlights""".trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) {
                highlights.put(
                    JSONObject()
                        .put("book_id", c.getString(0))
                        .put("paragraph_index", c.getInt(1))
                        .put("text", c.getString(2))
                        .put("color", c.getString(3))
                        .put("note", c.getString(4))
                        .put("start_offset", c.getInt(5))
                        .put("end_offset", c.getInt(6))
                        .put("created_at", c.getLong(7))
                        .put("locator_json", c.getString(8))
                        .put("source", c.getString(9))
                )
            }
        }
        root.put("highlights", highlights)

        val contexts = JSONArray()
        readableDatabase.rawQuery(
            """SELECT word_norm,context,COALESCE(book_id,''),COALESCE(book_title,''),created_at
               FROM vocab_contexts ORDER BY created_at""".trimIndent(),
            null,
        ).use { c ->
            while (c.moveToNext()) {
                contexts.put(
                    JSONObject()
                        .put("word_norm", c.getString(0))
                        .put("context", c.getString(1))
                        .put("book_id", c.getString(2))
                        .put("book_title", c.getString(3))
                        .put("created_at", c.getLong(4))
                )
            }
        }
        root.put("vocab_contexts", contexts)

        val expressions = JSONArray()
        readableDatabase.rawQuery(
            """SELECT expression_norm,expression,COALESCE(definition,''),COALESCE(context,''),created_at,
                      COALESCE(book_id,''),COALESCE(book_title,''),COALESCE(status,'learning') FROM expressions""".trimIndent(), null
        ).use { c ->
            while (c.moveToNext()) expressions.put(JSONObject()
                .put("expression_norm", c.getString(0)).put("expression", c.getString(1)).put("definition", c.getString(2))
                .put("context", c.getString(3)).put("created_at", c.getLong(4)).put("book_id", c.getString(5))
                .put("book_title", c.getString(6)).put("status", c.getString(7)))
        }
        root.put("expressions", expressions)

        val expressionContexts = JSONArray()
        readableDatabase.rawQuery(
            """SELECT expression_norm,context,COALESCE(book_id,''),COALESCE(book_title,''),created_at FROM expression_contexts ORDER BY created_at""".trimIndent(), null
        ).use { c ->
            while (c.moveToNext()) expressionContexts.put(JSONObject()
                .put("expression_norm", c.getString(0)).put("context", c.getString(1)).put("book_id", c.getString(2))
                .put("book_title", c.getString(3)).put("created_at", c.getLong(4)))
        }
        root.put("expression_contexts", expressionContexts)
        return root
    }

    fun restoreBackupJson(root: JSONObject): Pair<Int, Int> {
        val db = writableDatabase
        var vocabCount = 0
        var highlightCount = 0

        db.beginTransaction()
        try {
            // A restore is a snapshot replacement, not an additive merge.
            db.delete("vocab", null, null)
            db.delete("highlights", null, null)
            db.delete("vocab_contexts", null, null)
            db.delete("expressions", null, null)
            db.delete("expression_contexts", null, null)
            val vocab = root.optJSONArray("vocab") ?: JSONArray()
            for (i in 0 until vocab.length()) {
                val o = vocab.optJSONObject(i) ?: continue
                val word = o.optString("word").trim()
                if (word.isBlank()) continue

                val values = ContentValues().apply {
                    put("word_norm", o.optString("word_norm", normalize(word)))
                    put("word", word)
                    put("lemma", o.optString("lemma", ""))
                    put("definition", o.optString("definition", ""))
                    put("context", o.optString("context", ""))
                    put("created_at", o.optLong("created_at", System.currentTimeMillis()))
                    put("book_id", o.optString("book_id", ""))
                    put("book_title", o.optString("book_title", ""))
                    put("review_count", o.optInt("review_count", 0))
                    put("last_reviewed_at", o.optLong("last_reviewed_at", 0L))
                    put("next_review_at", o.optLong("next_review_at", 0L))
                    put("status", o.optString("status", "learning"))
                    put("marker_color", o.optString("marker_color", "yellow"))
                }
                if (db.insertWithOnConflict(
                        "vocab", null, values, SQLiteDatabase.CONFLICT_REPLACE
                    ) != -1L
                ) {
                    vocabCount++
                }
            }

            val highlights = root.optJSONArray("highlights") ?: JSONArray()
            for (i in 0 until highlights.length()) {
                val o = highlights.optJSONObject(i) ?: continue
                val bookId = o.optString("book_id")
                if (bookId.isBlank()) continue

                val values = ContentValues().apply {
                    put("book_id", bookId)
                    put("paragraph_index", o.optInt("paragraph_index", 0))
                    put("text", o.optString("text", ""))
                    put("color", o.optString("color", "yellow"))
                    put("note", o.optString("note", ""))
                    put("start_offset", o.optInt("start_offset", -1))
                    put("end_offset", o.optInt("end_offset", -1))
                    put("created_at", o.optLong("created_at", System.currentTimeMillis()))
                    put("locator_json", o.optString("locator_json", ""))
                    put("source", o.optString("source", if (o.optString("locator_json").isBlank()) "legacy" else "readium"))
                }
                if (db.insertWithOnConflict(
                        "highlights", null, values, SQLiteDatabase.CONFLICT_REPLACE
                    ) != -1L
                ) {
                    highlightCount++
                }
            }

            val contexts = root.optJSONArray("vocab_contexts") ?: JSONArray()
            for (i in 0 until contexts.length()) {
                val o = contexts.optJSONObject(i) ?: continue
                val wordNorm = o.optString("word_norm").trim()
                val context = o.optString("context").trim()
                if (wordNorm.isBlank() || context.isBlank()) continue
                val values = ContentValues().apply {
                    put("word_norm", wordNorm)
                    put("context", context)
                    put("book_id", o.optString("book_id", ""))
                    put("book_title", o.optString("book_title", ""))
                    put("created_at", o.optLong("created_at", System.currentTimeMillis()))
                }
                db.insertWithOnConflict("vocab_contexts", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }


            val expressions = root.optJSONArray("expressions") ?: JSONArray()
            for (i in 0 until expressions.length()) {
                val o = expressions.optJSONObject(i) ?: continue
                val expression = o.optString("expression").trim()
                if (expression.isBlank()) continue
                val values = ContentValues().apply {
                    put("expression_norm", o.optString("expression_norm", normalize(expression))); put("expression", expression)
                    put("definition", o.optString("definition", "")); put("context", o.optString("context", ""))
                    put("created_at", o.optLong("created_at", System.currentTimeMillis())); put("book_id", o.optString("book_id", ""))
                    put("book_title", o.optString("book_title", "")); put("status", o.optString("status", "learning"))
                }
                db.insertWithOnConflict("expressions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            val expressionContexts = root.optJSONArray("expression_contexts") ?: JSONArray()
            for (i in 0 until expressionContexts.length()) {
                val o = expressionContexts.optJSONObject(i) ?: continue
                val expressionNorm = o.optString("expression_norm").trim(); val context = o.optString("context").trim()
                if (expressionNorm.isBlank() || context.isBlank()) continue
                val values = ContentValues().apply {
                    put("expression_norm", expressionNorm); put("context", context); put("book_id", o.optString("book_id", ""))
                    put("book_title", o.optString("book_title", "")); put("created_at", o.optLong("created_at", System.currentTimeMillis()))
                }
                db.insertWithOnConflict("expression_contexts", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }

            // Backward compatibility: old backups had only vocab.context.
            if (contexts.length() == 0) {
                db.execSQL(
                    """INSERT OR IGNORE INTO vocab_contexts(word_norm,context,book_id,book_title,created_at)
                       SELECT word_norm,context,COALESCE(book_id,''),COALESCE(book_title,''),created_at
                       FROM vocab WHERE TRIM(COALESCE(context,''))<>''""".trimIndent()
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return vocabCount to highlightCount
    }

    fun exportAnnotationsMarkdown(
        bookNames: Map<String, String>,
        onlyBookId: String = "",
        notesOnly: Boolean = false,
    ): String {
        val selected = allAnnotations().filter { (bookId, mark) ->
            (onlyBookId.isBlank() || bookId == onlyBookId) &&
                (!notesOnly || mark.note.isNotBlank())
        }
        val grouped = selected.groupBy { it.first }

        return buildString {
            appendLine("# Lirelia 标注与笔记")
            appendLine()
            grouped.forEach { (bookId, items) ->
                appendLine("## ${bookNames[bookId] ?: bookId}")
                appendLine()
                items.sortedWith(compareBy<Pair<String, TextAnnotation>> { it.second.source != "readium" }
                    .thenBy { it.second.paragraphIndex }).forEach { (_, mark) ->
                    append("- ")
                    append(mark.text.replace("\n", " ").trim())
                    appendLine()
                    if (mark.note.isNotBlank()) {
                        append("  - 笔记：")
                        appendLine(mark.note.replace("\n", " ").trim())
                    }
                    if (mark.source == "readium") {
                        appendLine("  - 位置：EPUB 精确定位")
                    } else {
                        append("  - 位置：第 ")
                        append(mark.paragraphIndex + 1)
                        appendLine(" 段")
                    }
                }
                appendLine()
            }
        }
    }

    fun exportAnnotationsCsv(
        bookNames: Map<String, String>,
        onlyBookId: String = "",
    ): String {
        fun esc(value: String): String =
            "\"" + value.replace("\"", "\"\"").replace("\n", " ") + "\""

        return buildString {
            appendLine("book,location,text,color,note,source")
            allAnnotations()
                .filter { onlyBookId.isBlank() || it.first == onlyBookId }
                .forEach { (bookId, mark) ->
                    append(esc(bookNames[bookId] ?: bookId)); append(',')
                    append(esc(if (mark.source == "readium") "EPUB 精确定位" else "第 ${mark.paragraphIndex + 1} 段")); append(',')
                    append(esc(mark.text)); append(',')
                    append(esc(mark.color)); append(',')
                    append(esc(mark.note)); append(',')
                    append(esc(mark.source))
                    appendLine()
                }
        }
    }

    private fun normalize(value: String) = value.trim().replace('’', '\'').lowercase(Locale.FRENCH)
}
