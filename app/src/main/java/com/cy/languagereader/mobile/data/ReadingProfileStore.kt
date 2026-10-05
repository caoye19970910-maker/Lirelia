package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * V3.18 reading-language profile.
 *
 * Records only observable reading behaviour: exposed words, lookup events and
 * repeated lookup words. It deliberately avoids pretending to know the user's
 * full vocabulary size.
 */
data class ReadingProfileDay(
    val date: String,
    val exposedWords: Long,
    val lookups: Long,
) {
    val lookupsPer100: Float
        get() = if (exposedWords <= 0L) 0f else (lookups * 100f / exposedWords)
}

data class RepeatLookupWord(
    val word: String,
    val count: Int,
    val lastSeenAt: Long,
)

data class ReadingProfileSnapshot(
    val exposedWords30: Long,
    val lookups30: Long,
    val uniqueLookupWords30: Int,
    val repeatLookupEvents30: Int,
    val stubbornWords: List<RepeatLookupWord>,
    val recent14Days: List<ReadingProfileDay>,
    val recent7Rate: Float,
    val previous7Rate: Float,
) {
    val lookupRate30: Float
        get() = if (exposedWords30 <= 0L) 0f else lookups30 * 100f / exposedWords30

    val repeatRate30: Float
        get() = if (lookups30 <= 0L) 0f else repeatLookupEvents30 * 100f / lookups30
}

data class BookReadingProfile(
    val exposedWords: Long,
    val lookups: Long,
) {
    val lookupsPer100: Float
        get() = if (exposedWords <= 0L) 0f else lookups * 100f / exposedWords
}

class ReadingProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("reading_profile", Context.MODE_PRIVATE)
    private val lock = Any()

    fun recordExposure(bookId: String, segmentId: String, text: String) {
        if (bookId.isBlank() || segmentId.isBlank()) return
        val words = countFrenchWords(text)
        if (words <= 0) return
        val date = today()
        val token = "$bookId::$segmentId"

        synchronized(lock) {
            val seenKey = "seen_$date"
            val seen = prefs.getStringSet(seenKey, emptySet())?.toMutableSet() ?: mutableSetOf()
            if (!seen.add(token)) return

            val totalWords = prefs.getLong("words_$date", 0L) + words
            val bookStats = readJson("books_$date")
            val current = bookStats.optJSONObject(bookId) ?: JSONObject()
            current.put("words", current.optLong("words", 0L) + words)
            current.put("lookups", current.optLong("lookups", 0L))
            bookStats.put(bookId, current)

            prefs.edit()
                .putStringSet(seenKey, seen)
                .putLong("words_$date", totalWords)
                .putString("books_$date", bookStats.toString())
                .apply()
        }
    }

    fun recordLookup(word: String, bookId: String) {
        val normalized = normalize(word)
        if (normalized.isBlank()) return
        val display = word.trim()
        val date = today()
        val now = System.currentTimeMillis()

        synchronized(lock) {
            val dayLookups = prefs.getLong("lookups_$date", 0L) + 1L
            val dailyWordsKey = "lookup_words_$date"
            val dailyWords = prefs.getStringSet(dailyWordsKey, emptySet())?.toMutableSet() ?: mutableSetOf()
            dailyWords += normalized
            val dailyCounts = readJson("lookup_counts_$date")
            dailyCounts.put(normalized, dailyCounts.optInt(normalized, 0) + 1)

            val wordStats = readJson("word_lookup_stats")
            val old = wordStats.optJSONObject(normalized) ?: JSONObject()
            old.put("word", display.ifBlank { normalized })
            old.put("count", old.optInt("count", 0) + 1)
            old.put("last", now)
            wordStats.put(normalized, old)

            val bookStats = readJson("books_$date")
            val current = bookStats.optJSONObject(bookId) ?: JSONObject()
            current.put("words", current.optLong("words", 0L))
            current.put("lookups", current.optLong("lookups", 0L) + 1L)
            if (bookId.isNotBlank()) bookStats.put(bookId, current)

            prefs.edit()
                .putLong("lookups_$date", dayLookups)
                .putStringSet(dailyWordsKey, dailyWords)
                .putString("lookup_counts_$date", dailyCounts.toString())
                .putString("word_lookup_stats", wordStats.toString())
                .putString("books_$date", bookStats.toString())
                .apply()
        }
    }

    fun snapshot(days: Int = 30): ReadingProfileSnapshot {
        val safeDays = days.coerceIn(1, 90)
        val dates = dateKeys(safeDays)
        val exposed = dates.sumOf { prefs.getLong("words_$it", 0L) }
        val lookups = dates.sumOf { prefs.getLong("lookups_$it", 0L) }
        val unique = dates.flatMap { prefs.getStringSet("lookup_words_$it", emptySet()).orEmpty() }.toSet()

        val recent14 = dateKeys(14).reversed().map {
            ReadingProfileDay(it, prefs.getLong("words_$it", 0L), prefs.getLong("lookups_$it", 0L))
        }

        val recent7 = aggregate(dateKeys(7))
        val previous7 = aggregate(dateKeys(14).drop(7).take(7))

        val displayStats = readJson("word_lookup_stats")
        val windowCounts = linkedMapOf<String, Int>()
        dates.forEach { date ->
            val counts = readJson("lookup_counts_$date")
            val keys = counts.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                windowCounts[key] = (windowCounts[key] ?: 0) + counts.optInt(key, 0)
            }
        }
        val stubborn = windowCounts
            .filterValues { it >= 2 }
            .map { (key, count) ->
                val meta = displayStats.optJSONObject(key) ?: JSONObject()
                RepeatLookupWord(
                    word = meta.optString("word", key),
                    count = count,
                    lastSeenAt = meta.optLong("last", 0L),
                )
            }
            .sortedWith(compareByDescending<RepeatLookupWord> { it.count }.thenByDescending { it.lastSeenAt })
            .take(12)

        val repeatedEvents = windowCounts.values.sumOf { (it - 1).coerceAtLeast(0) }

        return ReadingProfileSnapshot(
            exposedWords30 = exposed,
            lookups30 = lookups,
            uniqueLookupWords30 = unique.size,
            repeatLookupEvents30 = repeatedEvents.coerceAtMost(lookups.toInt()),
            stubbornWords = stubborn,
            recent14Days = recent14,
            recent7Rate = recent7.lookupsPer100,
            previous7Rate = previous7.lookupsPer100,
        )
    }

    fun bookSnapshot(bookId: String, days: Int = 30): BookReadingProfile {
        if (bookId.isBlank()) return BookReadingProfile(0L, 0L)
        var words = 0L
        var lookups = 0L
        dateKeys(days.coerceIn(1, 90)).forEach { date ->
            val item = readJson("books_$date").optJSONObject(bookId) ?: return@forEach
            words += item.optLong("words", 0L)
            lookups += item.optLong("lookups", 0L)
        }
        return BookReadingProfile(words, lookups)
    }

    fun prune(daysToKeep: Int = 90) {
        val keep = dateKeys(daysToKeep.coerceAtLeast(30)).toSet()
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            val date = when {
                key.startsWith("seen_") -> key.removePrefix("seen_")
                key.startsWith("words_") -> key.removePrefix("words_")
                key.startsWith("lookups_") -> key.removePrefix("lookups_")
                key.startsWith("lookup_words_") -> key.removePrefix("lookup_words_")
                key.startsWith("lookup_counts_") -> key.removePrefix("lookup_counts_")
                key.startsWith("books_") -> key.removePrefix("books_")
                else -> null
            }
            if (date != null && date !in keep) editor.remove(key)
        }
        editor.apply()
    }

    private fun aggregate(dates: List<String>): BookReadingProfile = BookReadingProfile(
        exposedWords = dates.sumOf { prefs.getLong("words_$it", 0L) },
        lookups = dates.sumOf { prefs.getLong("lookups_$it", 0L) },
    )

    private fun readJson(key: String): JSONObject = runCatching {
        JSONObject(prefs.getString(key, "{}") ?: "{}")
    }.getOrElse { JSONObject() }

    private fun dateKeys(days: Int): List<String> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        return buildList {
            repeat(days) {
                add(format.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.FRENCH)
        .replace('’', '\'')
        .replace(Regex("^[^a-zà-öø-ÿœæ']+|[^a-zà-öø-ÿœæ']+$"), "")

    companion object {
        private val WORD_REGEX = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:['’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?")
        fun countFrenchWords(text: String): Int = WORD_REGEX.findAll(text).count()
    }
}
