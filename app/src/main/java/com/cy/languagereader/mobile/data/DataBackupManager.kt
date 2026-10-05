package com.cy.languagereader.mobile.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class RestoreSummary(
    val vocabCount: Int,
    val highlightCount: Int,
    val preferenceCount: Int,
    val bookAliasCount: Int = 0,
    val trainingStateCount: Int = 0,
)

class DataBackupManager(
    context: Context,
    private val learningStore: LearningStore,
) {
    private val appContext = context.applicationContext
    private val adaptiveTrainingStore = AdaptiveTrainingStore(appContext)

    private val preferenceNames = listOf(
        "language_reader_settings",
        "reading_stats",
        "reading_profile",
        "library_organizer",
        "language_reader_books",
        "french_training_progress",
        "french_training_session",
        "pdf_text_boxes",
        "lirelia_readium_progress",
    )

    fun exportBackup(): String {
        val root = JSONObject()
            .put("format", "language-reader-backup")
            .put("version", 8)
            .put("created_at", System.currentTimeMillis())

        val prefsRoot = JSONObject()
        preferenceNames.forEach { name ->
            val prefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            prefsRoot.put(name, encodePreferences(name, prefs))
        }
        root.put("preferences", prefsRoot)
        root.put("learning", learningStore.exportBackupJson())
        root.put("adaptive_training", adaptiveTrainingStore.exportBackupJson())
        root.put("book_identities", exportBookIdentities())
        root.put(
            "note",
            "Book files are not embedded. Re-import the same source file; cached SHA-256 identities reconnect restored per-book data. Readium EPUB locators/bookmarks/highlights/notes, reading statistics/profile, training/grammar scheduling, paused-session progress, PDF answer boxes, multi-source vocabulary contexts, and saved reading expressions are included."
        )
        return root.toString(2)
    }

    fun restoreBackup(raw: String): RestoreSummary {
        val root = JSONObject(raw)
        require(root.optString("format") == "language-reader-backup") {
            "不是 Lirelia / Language Reader 兼容备份"
        }

        var prefCount = 0
        val prefsRoot = root.optJSONObject("preferences") ?: JSONObject()
        preferenceNames.forEach { name ->
            val encoded = prefsRoot.optJSONObject(name) ?: return@forEach
            val prefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            prefCount += restorePreferences(name, prefs, encoded)
        }

        val aliasPrefs = appContext.getSharedPreferences(
            "language_reader_restore_aliases",
            Context.MODE_PRIVATE,
        )
        val aliasEditor = aliasPrefs.edit().clear()
        var aliases = 0
        val identities = root.optJSONArray("book_identities") ?: JSONArray()
        for (i in 0 until identities.length()) {
            val item = identities.optJSONObject(i) ?: continue
            val hash = item.optString("sha256")
            val oldId = item.optString("book_id")
            if (hash.isNotBlank() && oldId.isNotBlank()) {
                aliasEditor.putString("sha256_$hash", oldId)
                aliases++
            }
        }
        aliasEditor.apply()

        val learning = root.optJSONObject("learning") ?: JSONObject()
        val (vocab, highlights) = learningStore.restoreBackupJson(learning)
        val trainingStates = root.optJSONArray("adaptive_training")
            ?.let { adaptiveTrainingStore.restoreBackupJson(it) }
            ?: 0
        return RestoreSummary(vocab, highlights, prefCount, aliases, trainingStates)
    }

    private fun exportBookIdentities(): JSONArray {
        val result = JSONArray()
        val prefs = appContext.getSharedPreferences("language_reader_books", Context.MODE_PRIVATE)
        val raw = prefs.getString("books", "[]") ?: "[]"
        val books = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        var backfilled = false

        for (i in 0 until books.length()) {
            val item = books.optJSONObject(i) ?: continue
            val source = File(item.optString("sourceFile"))
            if (!source.exists() || !source.isFile) continue

            var hash = item.optString("sourceHash")
            if (hash.isBlank()) {
                hash = runCatching { sha256(source) }.getOrNull().orEmpty()
                if (hash.isNotBlank()) {
                    item.put("sourceHash", hash)
                    backfilled = true
                }
            }
            if (hash.isBlank()) continue

            result.put(
                JSONObject()
                    .put("book_id", item.optString("id"))
                    .put("sha256", hash)
                    .put("title", item.optString("title"))
                    .put("type", item.optString("type"))
                    .put("total_chars", item.optInt("totalChars", 0))
            )
        }
        if (backfilled) prefs.edit().putString("books", books.toString()).apply()
        return result
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun encodePreferences(
        prefName: String,
        prefs: SharedPreferences,
    ): JSONObject {
        val root = JSONObject()
        prefs.all.forEach { (key, value) ->
            // Absolute file paths are device-specific.
            if (prefName == "language_reader_books" && key == "books") return@forEach

            val item = JSONObject()
            when (value) {
                is String -> {
                    item.put("type", "string")
                    item.put("value", value)
                }
                is Int -> {
                    item.put("type", "int")
                    item.put("value", value)
                }
                is Long -> {
                    item.put("type", "long")
                    item.put("value", value)
                }
                is Float -> {
                    item.put("type", "float")
                    item.put("value", value.toDouble())
                }
                is Boolean -> {
                    item.put("type", "boolean")
                    item.put("value", value)
                }
                is Set<*> -> {
                    item.put("type", "string_set")
                    val arr = JSONArray()
                    value.filterIsInstance<String>().sorted().forEach { arr.put(it) }
                    item.put("value", arr)
                }
                else -> return@forEach
            }
            root.put(key, item)
        }
        return root
    }

    private fun restorePreferences(
        prefName: String,
        prefs: SharedPreferences,
        encoded: JSONObject,
    ): Int {
        val editor = prefs.edit()
        // Restore exact backed-up state. The books catalogue itself is intentionally
        // device-local because source paths change across devices, so preserve only it.
        if (prefName == "language_reader_books") {
            prefs.all.keys.filterNot { it == "books" }.forEach { editor.remove(it) }
        } else {
            editor.clear()
        }
        var restored = 0
        val keys = encoded.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            if (prefName == "language_reader_books" && key == "books") continue

            val item = encoded.optJSONObject(key) ?: continue
            when (item.optString("type")) {
                "string" -> editor.putString(key, item.optString("value", ""))
                "int" -> editor.putInt(key, item.optInt("value", 0))
                "long" -> editor.putLong(key, item.optLong("value", 0L))
                "float" -> editor.putFloat(key, item.optDouble("value", 0.0).toFloat())
                "boolean" -> editor.putBoolean(key, item.optBoolean("value", false))
                "string_set" -> {
                    val arr = item.optJSONArray("value") ?: JSONArray()
                    val set = buildSet {
                        for (i in 0 until arr.length()) {
                            val value = arr.optString(i)
                            if (value.isNotBlank()) add(value)
                        }
                    }
                    editor.putStringSet(key, set)
                }
                else -> continue
            }
            restored++
        }
        editor.apply()
        return restored
    }
}
