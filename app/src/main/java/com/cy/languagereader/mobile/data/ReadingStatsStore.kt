package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReadingStatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("reading_stats", Context.MODE_PRIVATE)

    fun addSeconds(bookId: String, seconds: Long = 1L) {
        if (bookId.isBlank() || seconds <= 0) return
        val date = today()
        val dayKey = "day_${date}"
        val bookKey = "book_${bookId}"
        val pairKey = "pair_${date}_${bookId}"
        val dates = knownDates().toMutableSet().apply { add(date) }

        // One batched SharedPreferences commit instead of several writes per second.
        prefs.edit()
            .putLong("total_seconds", prefs.getLong("total_seconds", 0L) + seconds)
            .putLong(dayKey, prefs.getLong(dayKey, 0L) + seconds)
            .putLong(bookKey, prefs.getLong(bookKey, 0L) + seconds)
            .putLong(pairKey, prefs.getLong(pairKey, 0L) + seconds)
            .putStringSet("known_dates", dates)
            .apply()
    }

    @Deprecated("Use addSeconds so callers can batch writes")
    fun addSecond(bookId: String, seconds: Long = 1L) = addSeconds(bookId, seconds)

    fun clearBook(bookId: String) {
        if (bookId.isBlank()) return
        val editor = prefs.edit().remove("book_${bookId}")
        knownDates().forEach { date -> editor.remove("pair_${date}_${bookId}") }
        editor.apply()
    }

    fun totalSeconds(): Long = prefs.getLong("total_seconds", 0L)

    fun todaySeconds(): Long = prefs.getLong("day_${today()}", 0L)

    fun bookSeconds(bookId: String): Long = prefs.getLong("book_${bookId}", 0L)

    fun recentDays(limit: Int = 14): List<Pair<String, Long>> =
        knownDates()
            .sortedDescending()
            .take(limit)
            .map { it to prefs.getLong("day_$it", 0L) }


    fun currentStreakDays(): Int {
        val known = knownDates()
        if (known.isEmpty()) return 0

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = java.util.Calendar.getInstance()
        var streak = 0

        // If there was no reading today, allow the streak to continue from yesterday.
        val todayKey = format.format(calendar.time)
        if (todayKey !in known) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val key = format.format(calendar.time)
            if (key !in known || prefs.getLong("day_$key", 0L) <= 0L) break
            streak++
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun activeDayCount(): Int =
        knownDates().count { prefs.getLong("day_$it", 0L) > 0L }

    fun secondsInLastDays(days: Int): Long {
        if (days <= 0) return 0L
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = java.util.Calendar.getInstance()
        var total = 0L
        repeat(days) {
            val key = format.format(calendar.time)
            total += prefs.getLong("day_$key", 0L)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }


    fun calendarDays(days: Int = 7): List<Pair<String, Long>> {
        if (days <= 0) return emptyList()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = java.util.Calendar.getInstance()
        val reversed = buildList {
            repeat(days) {
                val key = format.format(calendar.time)
                add(key to prefs.getLong("day_$key", 0L))
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
        }
        return reversed.reversed()
    }

    fun bookStats(bookIds: Collection<String>): Map<String, Long> =
        bookIds.associateWith { bookSeconds(it) }

    fun exportJson(): String {
        val obj = JSONObject()
        obj.put("total_seconds", totalSeconds())
        obj.put("today_seconds", todaySeconds())

        val days = JSONArray()
        recentDays(60).forEach { (date, seconds) ->
            days.put(JSONObject().put("date", date).put("seconds", seconds))
        }
        obj.put("days", days)
        return obj.toString(2)
    }

    private fun knownDates(): Set<String> =
        prefs.getStringSet("known_dates", emptySet())?.toSet().orEmpty()

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
