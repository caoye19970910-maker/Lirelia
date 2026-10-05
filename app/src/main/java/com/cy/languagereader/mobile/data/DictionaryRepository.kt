package com.cy.languagereader.mobile.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class DictionaryRepository(private val context: Context) {
    private val dbFile = File(context.filesDir, "dictionary/mobile_french_dictionary.db")
    private val extensionDbFile = File(context.filesDir, "dictionary/wikdict_fr_zh.db")
    private val legacyExtensionDbFile = File(context.filesDir, "dictionary/wikidata_fr_zh.db")
    private val lexicalEngineDbFile = File(context.filesDir, "dictionary/lirelia_lexical_engine_v2.db")
    @Volatile private var db: SQLiteDatabase? = null
    @Volatile private var extensionDb: SQLiteDatabase? = null
    @Volatile private var lexicalEngineDb: SQLiteDatabase? = null

    init {
        recoverInterruptedExtensionSwap()
    }

    private fun recoverInterruptedExtensionSwap() {
        val parent = extensionDbFile.parentFile ?: return
        parent.mkdirs()
        val backup = File(parent, "${extensionDbFile.name}.previous")
        val temp = File(parent, "${extensionDbFile.name}.downloading")

        // A .downloading file is never authoritative. It has not passed the
        // validation-and-swap phase yet, so it is safe to discard on startup.
        runCatching { if (temp.exists()) temp.delete() }

        // Fast common path: once an update completed there is no backup file. Do
        // not synchronously open/validate SQLite on every app launch just to prove
        // an already-installed 3 MB extension is still there. Normal lookup will
        // validate it lazily when first used.
        if (extensionDbFile.exists() && !backup.exists()) return

        // A leftover backup means an update was interrupted around the swap. Only
        // this rare recovery path pays the SQLite validation cost at construction.
        if (extensionDbFile.exists() && isValidExtensionFile(extensionDbFile)) {
            runCatching { backup.delete() }
            return
        }

        // Active file missing/corrupt but the protected previous file exists:
        // restore the last known-good database. Never leave a corrupt active file
        // in front of a valid backup.
        if (backup.exists() && isValidExtensionFile(backup)) {
            runCatching { if (extensionDbFile.exists()) extensionDbFile.delete() }
            runCatching { backup.renameTo(extensionDbFile) }
        }
    }

    private fun isValidExtensionFile(file: File): Boolean {
        if (!file.exists() || file.length() < 500_000L) return false
        return runCatching {
            val opened = SQLiteDatabase.openDatabase(
                file.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            try {
                tableExists(opened, "simple_translation") &&
                    opened.rawQuery("SELECT 1 FROM simple_translation LIMIT 1", null).use { it.moveToFirst() }
            } finally {
                opened.close()
            }
        }.getOrDefault(false)
    }

    private val lookupCache = object : LinkedHashMap<String, DictionaryLookup>(320, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, DictionaryLookup>?
        ): Boolean = size > 256
    }

    private fun cached(key: String): DictionaryLookup? = synchronized(lookupCache) {
        lookupCache[key]
    }

    private fun cache(key: String, value: DictionaryLookup): DictionaryLookup {
        synchronized(lookupCache) { lookupCache[key] = value }
        return value
    }

    /**
     * Never throws to the UI.
     *
     * Primary source: bundled 156k-entry SQLite dictionary.
     * Fallback: tiny always-available core dictionary.
     */
    fun lookupSafe(input: String, context: String = "", contextOffset: Int = -1): DictionaryLookup {
        val query = clean(input)
        if (query.isBlank()) {
            return DictionaryLookup(emptyResult(input), "离线词典")
        }

        // The same surface can resolve differently in different sentences (`tâche`,
        // `est`, `son`, `livre`...). Context therefore belongs in the cache key.
        val contextKey = context.trim().replace('’', '\'').lowercase(Locale.FRENCH).take(640)
        val key = normalize(query) + "\u0001" + contextKey + "\u0001" + contextOffset.toString()
        cached(key)?.let { return it }

        val full = runCatching { lookupFull(query, context, contextOffset) }
        val resolved = full.getOrNull()
        val fullResult = resolved?.takeIf {
            it.definitions.isNotEmpty() || it.morphology.isNotEmpty() ||
                !it.lemma.equals(query, ignoreCase = true)
        }
        val wikDictResult = runCatching { lookupExtension(query, resolved?.lemma) }.getOrNull()

        if (fullResult != null) {
            val merged = if (wikDictResult != null) mergeDictionaryResults(fullResult, wikDictResult) else fullResult
            return cache(
                key,
                DictionaryLookup(
                    result = merged,
                    source = if (wikDictResult != null) {
                        "Lirelia 词典引擎 2.0 + WikDict 法中"
                    } else {
                        "Lirelia 词典引擎 2.0 · 离线"
                    },
                    error = full.exceptionOrNull()?.localizedMessage,
                ),
            )
        }

        if (wikDictResult != null) {
            return cache(
                key,
                DictionaryLookup(
                    result = wikDictResult,
                    source = "WikDict 法中 · Wiktionary/DBnary",
                    error = full.exceptionOrNull()?.localizedMessage,
                ),
            )
        }

        val fallback = CoreFrenchChineseDictionary.lookup(query)
        if (fallback != null) {
            return cache(
                key,
                DictionaryLookup(
                    result = fallback,
                    source = "内置离线词典 · 核心兜底库",
                    error = full.exceptionOrNull()?.localizedMessage,
                ),
            )
        }

        return cache(
            key,
            DictionaryLookup(
                result = emptyResult(query),
                source = "内置离线词典",
                error = full.exceptionOrNull()?.localizedMessage,
            ),
        )
    }

    fun suggest(input: String, limit: Int = 12): List<String> {
        val key = normalize(clean(input))
        if (key.isBlank()) return emptyList()

        val cap = limit.coerceIn(1, 30)
        return runCatching {
            val sqlDb = database()
            val values = linkedSetOf<String>()

            sqlDb.rawQuery(
                """
                SELECT word
                FROM entries
                WHERE word_norm LIKE ? ESCAPE '\'
                ORDER BY LENGTH(word_norm), word
                LIMIT ?
                """.trimIndent(),
                arrayOf(prefixLike(key), (cap * 2).toString()),
            ).use { c ->
                while (c.moveToNext() && values.size < cap) {
                    c.getString(0)?.takeIf { it.isNotBlank() }?.let { values += it }
                }
            }

            if (values.size < cap) {
                sqlDb.rawQuery(
                    """
                    SELECT form
                    FROM form_index
                    WHERE form_norm LIKE ? ESCAPE '\'
                    ORDER BY LENGTH(form_norm), priority DESC, form
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(prefixLike(key), (cap * 3).toString()),
                ).use { c ->
                    while (c.moveToNext() && values.size < cap) {
                        c.getString(0)?.takeIf { it.isNotBlank() }?.let { form ->
                            if (values.none { it.equals(form, ignoreCase = true) }) {
                                values += form
                            }
                        }
                    }
                }
            }

            values.take(cap)
        }.getOrDefault(emptyList())
    }

    private fun prefixLike(key: String): String =
        key
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_") + "%"

    /** Kept for callers that only need the old result type. */
    fun lookup(input: String): DictionaryResult = lookupSafe(input).result

    /**
     * V5.0: short Chinese gloss for fixed expressions.
     *
     * Prefer a real phrase entry. When the bundled phrase index only contains the
     * French expression, build a conservative noun-complement literal gloss from
     * the same offline dictionary (e.g. "nid d'aigle" -> "鹰巢（直译）").
     * Returns an empty string when no useful offline gloss can be produced so the
     * UI may optionally use its normal network translation fallback.
     */
    fun phraseMeaningOffline(input: String): String {
        val phrase = clean(input)
        if (phrase.isBlank()) return ""

        val direct = runCatching { lookupSafe(phrase).result }.getOrNull()
        direct?.definitions?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val normalized = phrase.replace('’', '\'').trim()
        val match = Regex("^(.+?)\\s+(?:de|du|des|d')\\s*(.+)$", RegexOption.IGNORE_CASE)
            .find(normalized) ?: return ""
        val head = match.groupValues[1].trim()
        val tail = match.groupValues[2].trim()
        if (head.isBlank() || tail.isBlank()) return ""

        val headMeaning = runCatching { lookupSafe(head).result.definitions.firstOrNull().orEmpty() }
            .getOrDefault("")
            .let(::compactChineseSense)
        val tailMeaning = runCatching { lookupSafe(tail).result.definitions.firstOrNull().orEmpty() }
            .getOrDefault("")
            .let(::compactChineseSense)
        if (headMeaning.isBlank() || tailMeaning.isBlank()) return ""

        return "$tailMeaning$headMeaning（直译）"
    }

    private fun compactChineseSense(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("^(?:n\\.?\\s*[mf]\\.?|nom|noun)\\s*", RegexOption.IGNORE_CASE), "")
            .split('；', ';', '，', ',', '。')
            .asSequence()
            .map { it.trim() }
            .firstOrNull { part -> part.any { ch -> ch in '\u3400'..'\u9FFF' } }
            .orEmpty()
            .replace(Regex("^[〈《（(].*?[〉》）)]\\s*"), "")
            .trim()
            .take(16)
    }

    fun stats(): String {
        return runCatching {
            val sqlDb = database()
            val map = mutableMapOf<String, String>()
            sqlDb.rawQuery("SELECT key,value FROM metadata", null).use { c ->
                while (c.moveToNext()) map[c.getString(0)] = c.getString(1)
            }
            buildString {
                append("${map["entries"] ?: "?"} 词条 · ${map["forms"] ?: "?"} 词形 · ${map["phrases"] ?: "?"} 表达")
                extensionEntryCount().takeIf { it > 0 }?.let { append(" · 扩展 $it 词条") }
                append(" · 完全离线")
            }
        }.getOrElse {
            buildString {
                append("核心离线词典可用")
                extensionEntryCount().takeIf { it > 0 }?.let { append(" · 扩展 $it 词条") }
                append(" · 完整词典暂未初始化")
            }
        }
    }


    /**
     * Optional official WikDict French→Chinese SQLite supplement.
     *
     * The bundled Lirelia lexicographic dictionary remains the primary source for IPA,
     * morphology, examples and phrases. WikDict is merged into the same card to add
     * Wiktionary/DBnary translations, and also works as a fallback when the bundled
     * dictionary misses a lemma.
     */
    fun extensionInstalled(): Boolean = extensionDbFile.exists() && extensionEntryCount() > 0

    fun extensionEntryCount(): Int = runCatching {
        extensionDatabaseOrNull()?.rawQuery("SELECT COUNT(*) FROM simple_translation", null)?.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        } ?: 0
    }.getOrDefault(0)

    fun extensionDescription(): String {
        val count = extensionEntryCount()
        return if (count > 0) {
            "WikDict 法中已安装 · $count 词头 · Wiktionary/DBnary"
        } else {
            "WikDict 法中未安装 · 官方 SQLite 约 3 MB · 可选下载"
        }
    }

    /**
     * Downloads WikDict's official rolling French→Chinese SQLite build and validates
     * the real schema before swapping it into place. Call from Dispatchers.IO.
     *
     * The rolling /2/ endpoint currently mirrors the latest published generation;
     * the dated 2026-06 path is retained as a fallback so a future mirror/layout
     * change does not immediately break installs.
     */
    fun installOrUpdateExtension(onProgress: ((Int) -> Unit)? = null): Result<Int> = runCatching {
        val parent = extensionDbFile.parentFile ?: error("词典目录不可用")
        parent.mkdirs()
        val temp = File(parent, "${extensionDbFile.name}.downloading")
        runCatching { temp.delete() }

        val urls = listOf(
            "https://download.wikdict.com/dictionaries/sqlite/2/fr-zh.sqlite3",
            "https://download.wikdict.com/dictionaries/sqlite/2_2026-06/fr-zh.sqlite3",
        )
        var lastError: Throwable? = null
        var downloaded = false
        for (address in urls) {
            try {
                downloadFile(address, temp)
                downloaded = true
                break
            } catch (t: Throwable) {
                lastError = t
                runCatching { temp.delete() }
            }
        }
        if (!downloaded) throw IllegalStateException("无法下载 WikDict 法中词典", lastError)
        check(temp.length() > 500_000L) { "WikDict 下载内容异常（${temp.length()} bytes）" }

        val verify = SQLiteDatabase.openDatabase(temp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val count = try {
            check(tableExists(verify, "simple_translation")) { "WikDict 数据库缺少 simple_translation" }
            verify.rawQuery("SELECT COUNT(*) FROM simple_translation", null).use { c ->
                check(c.moveToFirst())
                c.getInt(0)
            }
        } finally {
            verify.close()
        }
        check(count > 5_000) { "WikDict 法中词典校验失败（仅 $count 词头）" }

        synchronized(this) {
            runCatching { extensionDb?.close() }
            extensionDb = null

            // Same-directory two-phase swap. Never delete the known-good database
            // before the newly downloaded one is already verified and ready.
            val backup = File(parent, "${extensionDbFile.name}.previous")
            runCatching { backup.delete() }
            val hadOld = extensionDbFile.exists()
            if (hadOld && !extensionDbFile.renameTo(backup)) {
                error("无法保护旧 WikDict 词典，已取消更新")
            }

            val installed = temp.renameTo(extensionDbFile)
            if (!installed) {
                // Restore the old database before surfacing the error. We deliberately
                // do not fall back to copyTo(target): a crash mid-copy could leave a
                // corrupt database where the previous implementation had already
                // deleted the good one.
                if (hadOld) {
                    runCatching { backup.renameTo(extensionDbFile) }
                }
                error("WikDict 更新文件无法原子替换，旧词典已保留")
            }
            runCatching { backup.delete() }

            // V4.0 used a different experimental extension. Remove it only after
            // the official WikDict database has been committed successfully.
            runCatching { legacyExtensionDbFile.delete() }
            synchronized(lookupCache) { lookupCache.clear() }
        }
        onProgress?.invoke(count)
        count
    }

    private fun downloadFile(address: String, target: File) {
        val conn = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 45_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Lirelia/4.5 Android")
            setRequestProperty("Accept", "application/octet-stream,*/*")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, bufferSize = 256 * 1024)
                    output.flush()
                    runCatching { output.fd.sync() }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun tableExists(sqlDb: SQLiteDatabase, table: String): Boolean =
        sqlDb.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type IN ('table','view') AND name=? LIMIT 1",
            arrayOf(table),
        ).use { it.moveToFirst() }

    fun removeExtension(): Boolean = synchronized(this) {
        runCatching { extensionDb?.close() }
        extensionDb = null
        synchronized(lookupCache) { lookupCache.clear() }
        val parent = extensionDbFile.parentFile
        val backup = parent?.let { File(it, "${extensionDbFile.name}.previous") }
        val temp = parent?.let { File(it, "${extensionDbFile.name}.downloading") }
        val activeRemoved = !extensionDbFile.exists() || extensionDbFile.delete()
        val legacyRemoved = !legacyExtensionDbFile.exists() || legacyExtensionDbFile.delete()
        val backupRemoved = backup == null || !backup.exists() || backup.delete()
        val tempRemoved = temp == null || !temp.exists() || temp.delete()
        activeRemoved && legacyRemoved && backupRemoved && tempRemoved
    }

    @Synchronized
    private fun extensionDatabaseOrNull(): SQLiteDatabase? {
        extensionDb?.let { if (it.isOpen) return it }
        if (!extensionDbFile.exists()) return null
        return runCatching {
            val opened = SQLiteDatabase.openDatabase(
                extensionDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            try {
                check(tableExists(opened, "simple_translation"))
                opened.rawQuery("SELECT 1 FROM simple_translation LIMIT 1", null).use { it.moveToFirst() }
                extensionDb = opened
                opened
            } catch (t: Throwable) {
                runCatching { opened.close() }
                throw t
            }
        }.getOrNull()
    }

    private fun lookupExtension(input: String, preferredLemma: String? = null): DictionaryResult? {
        val sqlDb = extensionDatabaseOrNull() ?: return null
        val candidates = linkedSetOf<String>()
        val cleaned = clean(input)
        val preferred = preferredLemma?.trim().orEmpty()
        if (preferred.isNotBlank() && !preferred.equals(cleaned, ignoreCase = true)) {
            // A context-resolved inflection must never fall back to the surface
            // homograph in WikDict (`je tâche` must not merge noun "tâche").
            candidates += preferred
        } else {
            if (cleaned.isNotBlank()) candidates += cleaned
            FrenchConjugator.lemmaForForm(cleaned)?.takeIf { it.isNotBlank() }?.let { candidates += it }
            lexicalCore(cleaned).takeIf { it.isNotBlank() }?.let { candidates += it }
            morphologyCandidates(normalize(cleaned)).forEach { candidates += it }
        }

        for (candidate in candidates) {
            val translations = linkedSetOf<String>()
            var canonical = candidate
            sqlDb.rawQuery(
                """
                SELECT written_rep, trans_list
                FROM simple_translation
                WHERE written_rep = ? COLLATE NOCASE
                ORDER BY max_score DESC
                LIMIT 8
                """.trimIndent(),
                arrayOf(candidate),
            ).use { c ->
                while (c.moveToNext()) {
                    canonical = c.getString(0) ?: canonical
                    splitWikDictTranslations(c.getString(1)).forEach { translations += it }
                }
            }
            if (translations.isNotEmpty()) {
                return DictionaryResult(
                    query = cleaned,
                    lemma = canonical,
                    pos = "",
                    ipa = "",
                    definitions = translations.toList(),
                    examples = emptyList(),
                    related = emptyList(),
                    phrases = emptyList(),
                )
            }
        }
        return null
    }

    private fun splitWikDictTranslations(raw: String?): List<String> =
        raw.orEmpty()
            .split('|')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun mergeDictionaryResults(primary: DictionaryResult, supplement: DictionaryResult): DictionaryResult {
        val definitions = linkedSetOf<String>()
        primary.definitions.forEach { definitions += it }
        supplement.definitions.forEach { definitions += it }
        return primary.copy(
            lemma = primary.lemma.ifBlank { supplement.lemma },
            pos = primary.pos.ifBlank { supplement.pos },
            ipa = primary.ipa.ifBlank { supplement.ipa },
            definitions = definitions.toList(),
        )
    }

    /** Release SQLite handles owned by this repository. Safe to call repeatedly. */
    @Synchronized
    fun close() {
        runCatching { db?.close() }
        runCatching { extensionDb?.close() }
        runCatching { lexicalEngineDb?.close() }
        db = null
        extensionDb = null
        lexicalEngineDb = null
        synchronized(lookupCache) { lookupCache.clear() }
    }

    fun warmUp() {
        runCatching {
            database()
            lookupSafe("bonjour")
            lookupSafe("être")
            lookupSafe("faire")
        }
    }

    fun diagnosticsReport(): String = buildString {
        appendLine("词典文件：${if (dbFile.exists()) "存在" else "不存在"}")
        appendLine("词典文件大小：${if (dbFile.exists()) "%.1f MB".format(Locale.US, dbFile.length() / 1024.0 / 1024.0) else "0 MB"}")
        appendLine("缓存词条：${synchronized(lookupCache) { lookupCache.size }}/256")
        appendLine("词典统计：${stats()}")
        appendLine("WikDict：${extensionDescription()}")

        val tests = listOf("bonjour", "situations", "triste", "baissées", "inflexible", "intraitable", "pour autant")
        tests.forEach { word ->
            val lookup = runCatching { lookupSafe(word) }.getOrNull()
            val ok = lookup != null && (
                lookup.result.definitions.isNotEmpty() ||
                    lookup.result.lemma.isNotBlank()
                )
            appendLine("测试 [$word]：${if (ok) "OK" else "未命中"} · ${lookup?.source ?: "错误"}")
        }
    }.trim()

    @Synchronized
    private fun database(): SQLiteDatabase {
        db?.let { if (it.isOpen) return it }

        // Existing dictionary may be a partially written file from a previous failed
        // first lookup. Validate it instead of trusting only file size.
        if (dbFile.exists()) {
            val existing = runCatching { openAndValidate(dbFile) }.getOrNull()
            if (existing != null) {
                db = existing
                return existing
            }
            runCatching { dbFile.delete() }
        }

        ensureInstalled()
        return openAndValidate(dbFile).also { db = it }
    }

    private fun openAndValidate(file: File): SQLiteDatabase {
        val opened = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            opened.rawQuery("SELECT 1 FROM entries LIMIT 1", null).use { it.moveToFirst() }
            opened.rawQuery("SELECT 1 FROM form_index LIMIT 1", null).use { it.moveToFirst() }
            return opened
        } catch (t: Throwable) {
            runCatching { opened.close() }
            throw t
        }
    }


    @Synchronized
    private fun lexicalDatabase(): SQLiteDatabase {
        lexicalEngineDb?.let { if (it.isOpen) return it }
        lexicalEngineDbFile.parentFile?.mkdirs()

        fun openValidated(file: File): SQLiteDatabase {
            val opened = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            try {
                check(tableExists(opened, "lexeme"))
                check(tableExists(opened, "form_analysis"))
                val version = opened.rawQuery("SELECT value FROM metadata WHERE key='schema_version' LIMIT 1", null).use { c ->
                    if (c.moveToFirst()) c.getString(0) else ""
                }
                check(version == "2")
                return opened
            } catch (t: Throwable) {
                runCatching { opened.close() }
                throw t
            }
        }

        if (lexicalEngineDbFile.exists()) {
            val existing = runCatching { openValidated(lexicalEngineDbFile) }.getOrNull()
            if (existing != null) {
                lexicalEngineDb = existing
                return existing
            }
            runCatching { lexicalEngineDbFile.delete() }
        }

        val temp = File(lexicalEngineDbFile.parentFile, "${lexicalEngineDbFile.name}.installing")
        runCatching { temp.delete() }
        context.assets.open("lirelia_lexical_engine_v2.db").use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output, bufferSize = 256 * 1024)
                output.flush()
                runCatching { output.fd.sync() }
            }
        }
        check(temp.length() > 1_000_000L) { "词典引擎 2.0 数据复制失败" }
        openValidated(temp).close()
        if (lexicalEngineDbFile.exists()) lexicalEngineDbFile.delete()
        if (!temp.renameTo(lexicalEngineDbFile)) {
            temp.copyTo(lexicalEngineDbFile, overwrite = true)
            temp.delete()
        }
        return openValidated(lexicalEngineDbFile).also { lexicalEngineDb = it }
    }

    private fun lookupFull(input: String, contextText: String = "", contextOffset: Int = -1): DictionaryResult {
        val query = clean(input)
        if (query.isBlank()) return emptyResult(input)

        val resolution = runCatching { LexicalResolver.resolve(lexicalDatabase(), query, contextText, contextOffset) }.getOrNull()
            ?: return lookupFullLegacy(query)

        val sqlDb = database()
        val lemmaNorm = normalize(resolution.lemma)
        val phrases = linkedSetOf<String>()
        runCatching {
            sqlDb.rawQuery(
                """
                SELECT DISTINCT p.phrase
                FROM phrase_tokens t JOIN phrase_index p ON p.id=t.phrase_id
                WHERE t.token=? ORDER BY p.token_count, p.phrase LIMIT 18
                """.trimIndent(),
                arrayOf(lemmaNorm),
            ).use { c -> while (c.moveToNext()) c.getString(0)?.takeIf { it.isNotBlank() }?.let(phrases::add) }
        }

        val rankedPhrases = phrases
            .sortedWith(compareByDescending<String> { phraseLearningScore(it, resolution.lemma) }.thenBy { it.length })
            .take(18)
        val phraseNorms = rankedPhrases.mapTo(hashSetOf()) { normalize(it) }
        val related = resolution.related
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { normalize(it) in phraseNorms }
            // Relations should be words/derivations. Multi-word material belongs in
            // the expression block instead of being duplicated under "相关词".
            .filterNot { it.contains(' ') }
            .distinctBy { normalize(it) }
            .take(18)
            .toList()

        return DictionaryResult(
            query = query,
            lemma = resolution.lemma,
            pos = resolution.pos,
            ipa = resolution.ipa,
            definitions = resolution.definitions,
            examples = resolution.examples.take(8),
            related = related,
            phrases = rankedPhrases,
            morphology = resolution.morphology,
            alternatives = resolution.alternatives,
            resolverSource = resolution.source,
        )
    }

    private fun phraseLearningScore(phrase: String, lemma: String): Int {
        val p = normalize(phrase)
        val tokens = p.split(Regex("\\s+")).filter { it.isNotBlank() }
        var score = tokens.size.coerceAtMost(7) * 2
        if (tokens.firstOrNull() in setOf("faire", "avoir", "être", "prendre", "mettre", "tenir", "donner", "aller")) score += 35
        if (p.contains(" de ") || p.contains(" d'") || p.contains(" à ")) score += 8
        if (normalize(lemma) in tokens) score += 4
        if (p == normalize(lemma)) score -= 100
        return score
    }

    private fun lookupFullLegacy(input: String): DictionaryResult {
        val query = clean(input)
        if (query.isBlank()) return emptyResult(input)
        val sqlDb = database()
        var resolved = normalize(query)
        var lemma = query

        fun resolveForm(key: String): String? {
            sqlDb.rawQuery(
                "SELECT lemma FROM form_index WHERE form_norm=? ORDER BY priority DESC LIMIT 1",
                arrayOf(key),
            ).use { c -> return if (c.moveToFirst()) c.getString(0) else null }
        }

        if (!hasEntry(sqlDb, resolved)) {
            val directLemma = resolveForm(resolved)
            val conjugationLemma = FrenchConjugator.lemmaForForm(query)
            if (!directLemma.isNullOrBlank()) {
                lemma = directLemma
                resolved = normalize(lemma)
            } else if (!conjugationLemma.isNullOrBlank() && hasEntry(sqlDb, normalize(conjugationLemma))) {
                lemma = conjugationLemma
                resolved = normalize(lemma)
            } else {
                val core = lexicalCore(query)
                if (!core.equals(query, ignoreCase = true)) {
                    val coreKey = normalize(core)
                    val coreLemma = resolveForm(coreKey)
                    lemma = coreLemma ?: core
                    resolved = normalize(lemma)
                } else {
                    // Conservative plural fallback for reading inflected forms.
                    val candidate = morphologyCandidates(resolved).drop(1).firstOrNull { hasEntry(sqlDb, it) }
                    if (candidate != null) {
                        lemma = candidate
                        resolved = candidate
                    }
                }
            }
        }

        val positions = linkedSetOf<String>()
        val exactPositions = linkedSetOf<String>()
        val resolvedLemmaPositions = linkedSetOf<String>()
        val defs = linkedSetOf<String>()
        val examples = linkedSetOf<String>()
        val related = linkedSetOf<String>()
        var ipa = ""
        var canonical = lemma

        sqlDb.rawQuery(
            """
                SELECT word,pos,ipa,definitions,examples,related
                FROM entries
                WHERE word_norm=?
                ORDER BY CASE WHEN word=? THEN 0 ELSE 1 END, word
                LIMIT 20
            """.trimIndent(),
            arrayOf(resolved, query),
        ).use { c ->
            while (c.moveToNext()) {
                val entryWord = c.getString(0) ?: canonical
                canonical = entryWord
                c.getString(1)?.takeIf { it.isNotBlank() }?.let { pos ->
                    positions += pos
                    if (entryWord == query) exactPositions += pos
                }
                if (ipa.isBlank()) ipa = c.getString(2) ?: ""
                parseJsonArray(c.getString(3)).forEach { defs += it }
                parseJsonArray(c.getString(4)).forEach { if (examples.size < 5) examples += it }
                parseJsonArray(c.getString(5)).forEach { if (related.size < 15) related += it }
            }
        }

        val inferredLemma = inferLemmaFromMorphology(defs.toList())
        if (!inferredLemma.isNullOrBlank() &&
            !normalize(inferredLemma).equals(resolved, ignoreCase = true) &&
            hasEntry(sqlDb, normalize(inferredLemma))
        ) {
            val lemmaKey = normalize(inferredLemma)
            val lemmaDefs = linkedSetOf<String>()
            sqlDb.rawQuery(
                """
                    SELECT word,pos,ipa,definitions,examples,related
                    FROM entries
                    WHERE word_norm=?
                    ORDER BY CASE WHEN word=? THEN 0 ELSE 1 END, word
                    LIMIT 20
                """.trimIndent(),
                arrayOf(lemmaKey, inferredLemma),
            ).use { c ->
                while (c.moveToNext()) {
                    canonical = c.getString(0) ?: inferredLemma
                    c.getString(1)?.takeIf { it.isNotBlank() }?.let { pos ->
                        positions += pos
                        resolvedLemmaPositions += pos
                    }
                    if (ipa.isBlank()) ipa = c.getString(2) ?: ""
                    parseJsonArray(c.getString(3)).forEach { lemmaDefs += it }
                    parseJsonArray(c.getString(4)).forEach { if (examples.size < 6) examples += it }
                    parseJsonArray(c.getString(5)).forEach { if (related.size < 18) related += it }
                }
            }

            // For an inflected form such as `pris`, lexical meanings from the
            // resolved lemma (`prendre`) are what a reader needs first. Keep the
            // form-specific morphology notes too, but move them after the lemma
            // senses instead of letting "past participle of..." dominate the quick card.
            if (lemmaDefs.isNotEmpty()) {
                val formDefs = defs.toList()
                defs.clear()
                lemmaDefs.forEach { defs += it }
                formDefs.forEach { defs += it }
            }
        }

        val phrases = linkedSetOf<String>()
        runCatching {
            sqlDb.rawQuery(
                """
                SELECT DISTINCT p.phrase
                FROM phrase_tokens t JOIN phrase_index p ON p.id=t.phrase_id
                WHERE t.token=? ORDER BY p.token_count, p.phrase LIMIT 12
                """.trimIndent(),
                arrayOf(resolved),
            ).use { c -> while (c.moveToNext()) phrases += c.getString(0) }
        }

        return DictionaryResult(
            query = query,
            lemma = canonical,
            pos = when {
                resolvedLemmaPositions.isNotEmpty() -> resolvedLemmaPositions
                exactPositions.isNotEmpty() -> exactPositions
                else -> positions
            }.joinToString(" · "),
            ipa = ipa,
            definitions = defs.toList(),
            examples = examples.toList(),
            related = related.toList(),
            phrases = phrases.toList(),
        )
    }

    private fun hasEntry(db: SQLiteDatabase, key: String): Boolean =
        db.rawQuery("SELECT 1 FROM entries WHERE word_norm=? LIMIT 1", arrayOf(key)).use { it.moveToFirst() }

    private fun ensureInstalled() {
        dbFile.parentFile?.mkdirs()
        val temp = File(dbFile.parentFile, "${dbFile.name}.installing")
        runCatching { temp.delete() }

        context.assets.open("mobile_french_dictionary.db").use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output, bufferSize = 256 * 1024)
                output.flush()
                runCatching { output.fd.sync() }
            }
        }

        check(temp.exists() && temp.length() > 10_000_000L) { "内置完整词典复制失败。" }
        openAndValidate(temp).close()

        if (dbFile.exists() && !dbFile.delete()) throw IllegalStateException("无法替换旧词典文件。")
        if (!temp.renameTo(dbFile)) {
            temp.copyTo(dbFile, overwrite = true)
            temp.delete()
        }
        check(dbFile.exists() && dbFile.length() > 10_000_000L) { "内置完整词典安装失败。" }
    }

    private fun lexicalCore(value: String): String {
        val normalized = clean(value).replace('’', '\'')
        val prefixes = listOf("l'", "d'", "j'", "n'", "s'", "t'", "m'", "c'", "qu'")
        val lower = normalized.lowercase(Locale.FRENCH)
        val prefix = prefixes.firstOrNull { lower.startsWith(it) && normalized.length > it.length + 1 }
        return if (prefix == null) normalized else normalized.substring(prefix.length)
    }

    private fun inferLemmaFromMorphology(definitions: List<String>): String? {
        for (definition in definitions) {
            val clean = definition.trim()
            val index = clean.indexOf(" 的")
            if (index in 1..40) {
                val candidate = clean.substring(0, index).trim()
                if (candidate.matches(Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœ'’ -]{2,40}"))) return candidate
            }
        }
        return null
    }

    private fun morphologyCandidates(word: String): List<String> = buildList {
        add(word)

        // Number / basic gender. Candidates are only accepted later if the lemma
        // actually exists in the database, keeping these rules conservative.
        if (word.length > 4 && word.endsWith("s")) add(word.dropLast(1))
        if (word.length > 4 && word.endsWith("x")) add(word.dropLast(1))
        if (word.length > 5 && word.endsWith("es")) add(word.dropLast(2))
        if (word.length > 4 && word.endsWith("e")) add(word.dropLast(1))
        if (word.length > 5 && word.endsWith("aux")) add(word.dropLast(3) + "al")
        if (word.length > 5 && word.endsWith("eaux")) add(word.dropLast(1))

        // Very common adjective feminine forms.
        if (word.length > 5 && word.endsWith("ive")) add(word.dropLast(3) + "if")
        if (word.length > 6 && word.endsWith("ives")) add(word.dropLast(4) + "if")
        if (word.length > 6 && word.endsWith("euse")) add(word.dropLast(4) + "eux")
        if (word.length > 7 && word.endsWith("euses")) add(word.dropLast(5) + "eux")
        if (word.length > 5 && word.endsWith("ère")) add(word.dropLast(3) + "er")
        if (word.length > 6 && word.endsWith("ères")) add(word.dropLast(4) + "er")
        if (word.length > 6 && word.endsWith("enne")) add(word.dropLast(4) + "en")
        if (word.length > 7 && word.endsWith("ennes")) add(word.dropLast(5) + "en")
        if (word.length > 6 && word.endsWith("elle")) add(word.dropLast(4) + "el")
        if (word.length > 7 && word.endsWith("elles")) add(word.dropLast(5) + "el")

        // Regular -er past participles are extremely frequent in prose. This makes
        // baissé / baissée / baissés / baissées resolve to baisser when the form index
        // does not already contain them.
        when {
            word.length > 5 && word.endsWith("ées") -> {
                add(word.dropLast(2))
                add(word.dropLast(3) + "er")
            }
            word.length > 4 && word.endsWith("ée") -> {
                add(word.dropLast(1))
                add(word.dropLast(2) + "er")
            }
            word.length > 4 && word.endsWith("és") -> {
                add(word.dropLast(1))
                add(word.dropLast(2) + "er")
            }
            word.length > 3 && word.endsWith("é") -> add(word.dropLast(1) + "er")
        }
    }.distinct()

    private fun parseJsonArray(raw: String?): List<String> = runCatching {
        val arr = JSONArray(raw ?: "[]")
        buildList {
            for (i in 0 until arr.length()) {
                arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun clean(value: String): String =
        value.trim().trim(' ', '.', ',', '!', '?', ';', ':', '«', '»', '"', '“', '”', '(', ')', '[', ']', '{', '}')

    private fun normalize(value: String): String =
        clean(value).replace('’', '\'').lowercase(Locale.FRENCH)

    private fun emptyResult(q: String) =
        DictionaryResult(q, q, "", "", emptyList(), emptyList(), emptyList(), emptyList())
}
