package com.cy.languagereader.mobile.data

import java.text.BreakIterator
import java.util.Locale

/**
 * Locale-aware French sentence extraction shared by TXT and PDF readers.
 *
 * BreakIterator is substantially safer than treating every newline or period as a hard stop:
 * PDF line wraps no longer become fake sentences, and common French abbreviations/quotes are
 * handled by the platform sentence iterator. EPUB uses the equivalent Intl.Segmenter path in JS.
 */
object FrenchSentenceSegmenter {
    fun range(text: String, rawOffset: Int): IntRange? {
        if (text.isBlank()) return null
        val safe = rawOffset.coerceIn(0, text.lastIndex)
        val iterator = BreakIterator.getSentenceInstance(Locale.FRENCH).apply { setText(text) }

        var start = iterator.preceding((safe + 1).coerceAtMost(text.length))
        if (start == BreakIterator.DONE) start = 0
        var end = iterator.following(safe)
        if (end == BreakIterator.DONE) end = text.length

        // Java's sentence iterator can split immediately after abbreviations such as
        // “M.” / “Mme.”. If the fragment right before the computed start is only a known
        // abbreviation or an initial, pull it back into the same sentence.
        repeat(3) {
            if (start <= 0) return@repeat
            val previous = iterator.preceding(start)
            if (previous == BreakIterator.DONE || previous >= start) return@repeat
            val fragment = text.substring(previous, start).trim()
            if (isAbbreviationFragment(fragment)) start = previous else return@repeat
        }

        // Conversely, if the iterator lands *on* an abbreviation fragment, join the following
        // chunk instead of returning a fake one-word sentence.
        if (end - start < 14 && isAbbreviationFragment(text.substring(start, end).trim())) {
            val next = iterator.following(end.coerceAtMost(text.length - 1))
            if (next != BreakIterator.DONE) end = next
        }

        while (start < end && text[start].isWhitespace()) start++
        while (end > start && text[end - 1].isWhitespace()) end--
        if (end <= start) return null
        return start..(end - 1)
    }

    fun sentence(text: String, rawOffset: Int, maxChars: Int = 520): String {
        val range = range(text, rawOffset) ?: return text.trim().replace(Regex("\\s+"), " ").take(maxChars)
        val safe = rawOffset.coerceIn(range.first, range.last)
        val raw = text.substring(range.first, range.last + 1)
        val normalized = raw.replace(Regex("\\s+"), " ").trim()
        if (normalized.length <= maxChars) return normalized

        // Long literary sentences are clipped around the tapped word, preferably at a clause
        // boundary so the translation still receives a coherent chunk.
        val prefixRaw = text.substring(range.first, safe.coerceAtMost(range.last + 1))
        val focus = prefixRaw.replace(Regex("\\s+"), " ").length.coerceIn(0, normalized.length)
        var start = (focus - maxChars / 2).coerceIn(0, (normalized.length - maxChars).coerceAtLeast(0))
        var end = (start + maxChars).coerceAtMost(normalized.length)

        fun isClauseBoundary(c: Char): Boolean = c in charArrayOf(',', ';', ':', '—', '–', '.', '!', '?', '…')
        val left = (start downTo (start - 70).coerceAtLeast(0)).firstOrNull { isClauseBoundary(normalized[it]) }
        if (left != null && left + 1 < focus) start = left + 1
        val right = (end until (end + 70).coerceAtMost(normalized.length)).firstOrNull { isClauseBoundary(normalized[it]) }
        if (right != null && right > focus) end = right + 1

        return normalized.substring(start.coerceAtMost(end), end).trim().take(maxChars + 80)
    }

    private fun isAbbreviationFragment(raw: String): Boolean {
        val cleaned = raw
            .trim()
            .trim('«', '»', '“', '”', '\'', '"', '(', ')', '[', ']')
            .lowercase(Locale.FRENCH)
        if (!cleaned.endsWith('.')) return false
        val stem = cleaned.removeSuffix(".").trim()
        return stem in COMMON_ABBREVIATIONS || stem.matches(Regex("[a-zà-öø-ÿ]") )
    }

    private val COMMON_ABBREVIATIONS = setOf(
        "m", "mme", "mmes", "mlle", "dr", "pr", "prof", "etc", "env", "av", "apr",
        "janv", "févr", "avr", "juill", "sept", "oct", "nov", "déc", "n", "no",
    )
}
