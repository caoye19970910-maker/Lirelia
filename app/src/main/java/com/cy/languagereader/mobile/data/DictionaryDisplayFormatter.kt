package com.cy.languagereader.mobile.data

/**
 * Small display-only cleanup for dictionary examples.
 *
 * The bundled sources occasionally contain an IPA fragment or a bilingual example
 * packed into one line with an em dash and slash separators. The dictionary UI
 * should show a readable French example plus its Chinese translation, not raw
 * source punctuation or an extra pronunciation line.
 */
data class DictionaryDisplayExample(
    val french: String,
    val chinese: String = "",
)

object DictionaryDisplayFormatter {
    private val inlineIpa = Regex(
        """\s*/[A-Za-zɑ-ʯœŒøØəɛɔɑɥʁɲŋːˈˌ̃̃.-]{1,40}/\s*"""
    )

    fun example(raw: String): DictionaryDisplayExample {
        val cleaned = raw
            .replace(inlineIpa, " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        if (cleaned.isBlank()) return DictionaryDisplayExample("")

        val separator = listOf(" — ", " – ", " -- ")
            .mapNotNull { marker -> cleaned.indexOf(marker).takeIf { it >= 0 }?.let { it to marker } }
            .minByOrNull { it.first }

        if (separator == null) {
            return DictionaryDisplayExample(
                french = cleaned.replace(" / ", "\n").trim(),
            )
        }

        val (index, marker) = separator
        val french = cleaned.substring(0, index)
            .replace(" / ", "\n")
            .trim()
        val chinese = cleaned.substring(index + marker.length)
            .replace(" / ", "\n")
            .trim()

        return DictionaryDisplayExample(french = french, chinese = chinese)
    }
}
