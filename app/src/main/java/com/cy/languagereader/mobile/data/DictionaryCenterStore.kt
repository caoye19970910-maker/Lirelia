package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONArray

class DictionaryCenterStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("dictionary_center", Context.MODE_PRIVATE)

    fun history(limit: Int = 30): List<String> =
        decodeHistory().take(limit.coerceAtLeast(0))

    fun recordLookup(raw: String) {
        val word = raw.trim()
        if (word.isBlank()) return

        val merged = buildList {
            add(word)
            decodeHistory()
                .filterNot { it.equals(word, ignoreCase = true) }
                .forEach { add(it) }
        }.take(60)

        val arr = JSONArray()
        merged.forEach { arr.put(it) }
        prefs.edit().putString("history_json", arr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history_json").apply()
    }

    fun favorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet())?.toSet().orEmpty()

    fun isFavorite(raw: String): Boolean =
        favorites().any { it.equals(raw.trim(), ignoreCase = true) }

    fun toggleFavorite(raw: String): Boolean {
        val word = raw.trim()
        if (word.isBlank()) return false

        val values = favorites().toMutableSet()
        val existing = values.firstOrNull { it.equals(word, ignoreCase = true) }

        val nowFavorite = if (existing != null) {
            values.remove(existing)
            false
        } else {
            values.add(word)
            true
        }

        prefs.edit().putStringSet("favorites", values).apply()
        return nowFavorite
    }

    private fun decodeHistory(): List<String> {
        val raw = prefs.getString("history_json", "[]").orEmpty()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}
