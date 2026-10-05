package com.cy.languagereader.mobile.data

import android.content.Context

class LibraryOrganizer(context: Context) {
    private val prefs = context.getSharedPreferences("library_organizer", Context.MODE_PRIVATE)

    fun tags(bookId: String): Set<String> =
        prefs.getStringSet("tags_$bookId", emptySet())?.toSet().orEmpty()

    fun setTags(bookId: String, rawTags: Collection<String>) {
        val clean = rawTags
            .map { it.trim().removePrefix("#").replace(Regex("""\s+"""), " ").take(24) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)
            .toSet()
        prefs.edit().putStringSet("tags_$bookId", clean).apply()
    }

    fun allTags(bookIds: Collection<String>): List<String> =
        bookIds.flatMap { tags(it) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun shelf(bookId: String): String =
        prefs.getString("shelf_$bookId", "").orEmpty()

    fun setShelf(bookId: String, shelf: String) {
        val clean = when (shelf) {
            "unread", "reading", "finished", "paused" -> shelf
            else -> ""
        }
        prefs.edit().putString("shelf_$bookId", clean).apply()
    }

    fun series(bookId: String): String =
        prefs.getString("series_$bookId", "").orEmpty()

    fun setSeries(bookId: String, series: String) {
        prefs.edit()
            .putString("series_$bookId", series.trim().replace(Regex("""\s+"""), " ").take(40))
            .apply()
    }

    fun allSeries(bookIds: Collection<String>): List<String> =
        bookIds.map { series(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun targetDate(bookId: String): String =
        prefs.getString("target_date_$bookId", "").orEmpty()

    fun setTargetDate(bookId: String, value: String) {
        val clean = value.trim().take(10)
        prefs.edit().putString("target_date_$bookId", clean).apply()
    }

    fun bookMemo(bookId: String): String =
        prefs.getString("book_memo_$bookId", "").orEmpty()

    fun displayTitle(bookId: String, fallback: String): String =
        prefs.getString("display_title_$bookId", "").orEmpty().trim().ifBlank { fallback }

    fun setDisplayTitle(bookId: String, value: String) {
        prefs.edit().putString(
            "display_title_$bookId",
            value.trim().replace(Regex("""\s+"""), " ").take(120),
        ).apply()
    }

    fun isCoverHidden(bookId: String): Boolean =
        prefs.getBoolean("cover_hidden_$bookId", false)

    fun setCoverHidden(bookId: String, hidden: Boolean) {
        prefs.edit().putBoolean("cover_hidden_$bookId", hidden).apply()
    }

    fun setBookMemo(bookId: String, value: String) {
        prefs.edit().putString("book_memo_$bookId", value.trim().take(4000)).apply()
    }

    fun clearBook(bookId: String) {
        prefs.edit()
            .remove("tags_$bookId")
            .remove("shelf_$bookId")
            .remove("series_$bookId")
            .remove("target_date_$bookId")
            .remove("book_memo_$bookId")
            .remove("display_title_$bookId")
            .remove("cover_hidden_$bookId")
            .apply()
    }
}
