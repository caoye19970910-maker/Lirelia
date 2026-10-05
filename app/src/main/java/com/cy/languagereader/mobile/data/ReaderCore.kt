package com.cy.languagereader.mobile.data

enum class ReaderMode { SCROLL, PAGE }

data class ReaderPage(
    val startParagraph: Int,
    val endParagraphExclusive: Int,
)

object ReaderPaginator {
    fun paginate(paragraphs: List<String>, targetChars: Int): List<ReaderPage> {
        if (paragraphs.isEmpty()) return emptyList()
        val target = targetChars.coerceIn(350, 2600)
        val pages = mutableListOf<ReaderPage>()
        var start = 0
        var chars = 0
        for (i in paragraphs.indices) {
            val addition = if (BookRepository.isImageParagraph(paragraphs[i])) {
                (target * 0.72f).toInt().coerceAtLeast(260)
            } else {
                paragraphs[i].length + 2
            }
            if (i > start && chars + addition > target) {
                pages += ReaderPage(start, i)
                start = i
                chars = 0
            }
            chars += addition
        }
        if (start < paragraphs.size) pages += ReaderPage(start, paragraphs.size)
        return pages
    }

    fun pageForParagraph(pages: List<ReaderPage>, paragraphIndex: Int): Int {
        if (pages.isEmpty()) return 0
        val safe = paragraphIndex.coerceAtLeast(0)
        val exact = pages.indexOfFirst { safe in it.startParagraph until it.endParagraphExclusive }
        return if (exact >= 0) exact else pages.lastIndex
    }
}
