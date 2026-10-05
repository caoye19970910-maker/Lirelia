package com.cy.languagereader.mobile.readium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import com.cy.languagereader.mobile.data.LearningStore
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Locator

/** Locator-backed highlights survive reflow, font-size changes and orientation changes. */
data class ReadiumHighlight(
    val id: String,
    val locatorJson: String,
    val text: String,
    val color: String,
    val note: String,
    val createdAt: Long,
)

/**
 * V4.2 makes LearningStore the single source of truth for EPUB annotations.
 *
 * V4.0/V4.1 temporarily stored Readium highlights in SharedPreferences. The one-time
 * migration below imports those rows into learning.db and then leaves a migration flag.
 * This reconnects EPUB highlights/notes with the annotation manager and normal backups.
 */
class ReadiumAnnotationStore(
    context: Context,
    private val learningStore: LearningStore,
) {
    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences(
        "lirelia_readium_annotations",
        Context.MODE_PRIVATE,
    )
    private val migrationPrefs = appContext.getSharedPreferences(
        "lirelia_readium_migrations",
        Context.MODE_PRIVATE,
    )

    fun list(bookId: String): List<ReadiumHighlight> =
        learningStore.readiumAnnotations(bookId).map { mark ->
            ReadiumHighlight(
                id = stableId(mark.locatorJson),
                locatorJson = mark.locatorJson,
                text = mark.text,
                color = mark.color,
                note = mark.note,
                createdAt = 0L,
            )
        }

    fun add(
        bookId: String,
        locator: Locator,
        text: String,
        color: String,
        note: String = "",
    ): ReadiumHighlight {
        val locatorJson = locator.toJSON().toString()
        learningStore.upsertReadiumAnnotation(
            bookId = bookId,
            locatorJson = locatorJson,
            text = text,
            color = canonicalColor(color),
            note = note,
        )
        return ReadiumHighlight(
            id = stableId(locatorJson),
            locatorJson = locatorJson,
            text = text.trim(),
            color = canonicalColor(color),
            note = note,
            createdAt = System.currentTimeMillis(),
        )
    }

    fun updateNote(bookId: String, locatorJson: String, note: String) {
        learningStore.updateReadiumAnnotationNote(bookId, locatorJson, note.trim())
    }

    fun remove(bookId: String, locatorJson: String) {
        learningStore.removeReadiumAnnotation(bookId, locatorJson)
    }

    fun decorations(bookId: String): List<Decoration> = decorations(list(bookId))

    fun decorations(items: List<ReadiumHighlight>): List<Decoration> = items.mapNotNull { item ->
        val locator = runCatching { Locator.fromJSON(JSONObject(item.locatorJson)) }.getOrNull()
            ?: return@mapNotNull null
        val style = if (item.color == "underline") {
            Decoration.Style.Underline(tint = android.graphics.Color.rgb(41, 91, 126))
        } else {
            val tint = runCatching { android.graphics.Color.parseColor(normalizeColor(item.color)) }
                .getOrDefault(android.graphics.Color.YELLOW)
            Decoration.Style.Highlight(tint = tint)
        }
        Decoration(
            id = item.id,
            locator = locator,
            style = style,
        )
    }

    fun migrateLegacyPreferenceHighlightsIfNeeded() {
        if (migrationPrefs.getBoolean("v42_readium_pref_highlights", false)) return
        legacyPrefs.all.forEach { (key, value) ->
            if (!key.startsWith("highlights_")) return@forEach
            val bookId = key.removePrefix("highlights_")
            val raw = value as? String ?: return@forEach
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return@forEach
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val locatorJson = item.optString("locator").trim()
                if (locatorJson.isBlank()) continue
                learningStore.upsertReadiumAnnotation(
                    bookId = bookId,
                    locatorJson = locatorJson,
                    text = item.optString("text"),
                    color = canonicalColor(item.optString("color", "yellow")),
                    note = item.optString("note", ""),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }
        migrationPrefs.edit().putBoolean("v42_readium_pref_highlights", true).apply()
        // Data is now canonical in learning.db. Remove only the old highlight payloads,
        // keeping the preferences file itself harmless for downgrade diagnostics.
        val editor = legacyPrefs.edit()
        legacyPrefs.all.keys.filter { it.startsWith("highlights_") }.forEach(editor::remove)
        editor.apply()
    }

    private fun stableId(locatorJson: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(locatorJson.toByteArray(Charsets.UTF_8))
        return "lr_" + bytes.joinToString("") { "%02x".format(it) }.take(20)
    }


    private fun canonicalColor(value: String): String = when (value.trim().lowercase()) {
        "#fff1a8", "yellow" -> "yellow"
        "#cdeccf", "green" -> "green"
        "#f7cdd8", "pink" -> "pink"
        "#cfe4ff", "blue" -> "blue"
        "#dddad4" -> "gray"
        "#dddada", "gray", "grey" -> "gray"
        "underline" -> "underline"
        else -> value.trim().ifBlank { "yellow" }
    }

    private fun normalizeColor(value: String): String = when (value.lowercase()) {
        // Readium's Highlight decoration applies its own translucency. Use a
        // deliberately stronger tint here so the final on-page mark is still clear.
        "yellow" -> "#F0B429"
        "green" -> "#43A047"
        "pink" -> "#E75480"
        "blue" -> "#3B82D0"
        "gray", "grey" -> "#8A8A8A"
        else -> value.takeIf { it.startsWith("#") } ?: "#F0B429"
    }
}
