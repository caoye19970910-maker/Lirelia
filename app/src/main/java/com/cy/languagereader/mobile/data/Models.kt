package com.cy.languagereader.mobile.data

data class BookItem(
    val id: String,
    val title: String,
    val type: String,
    val sourceFile: String,
    val textCacheFile: String,
    val totalChars: Int,
    val totalWords: Int = 0,
    val uniqueWords: Int = 0,
    val totalParagraphs: Int = 0,
    val coverFile: String = "",
    val chapterFile: String = "",
    val sourceHash: String = "",
    val updatedAt: Long,
)

data class ChapterItem(
    val title: String,
    val paragraphIndex: Int,
)

data class DictionaryAlternative(
    val lemma: String,
    val pos: String,
    val morphology: String = "",
    val preview: String = "",
)

data class DictionaryResult(
    val query: String,
    val lemma: String,
    val pos: String,
    val ipa: String,
    val definitions: List<String>,
    val examples: List<String>,
    val related: List<String>,
    val phrases: List<String>,
    val morphology: List<String> = emptyList(),
    val alternatives: List<DictionaryAlternative> = emptyList(),
    val resolverSource: String = "",
)

data class DictionaryLookup(
    val result: DictionaryResult,
    val source: String,
    val error: String? = null,
)

data class VocabularyItem(
    val word: String,
    val lemma: String,
    val definition: String,
    val context: String,
    val createdAt: Long,
    val bookId: String = "",
    val bookTitle: String = "",
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L,
    val nextReviewAt: Long = 0L,
    val status: String = "learning",
)


data class PhraseItem(
    val expression: String,
    val definition: String,
    val context: String,
    val createdAt: Long,
    val bookId: String = "",
    val bookTitle: String = "",
    val status: String = "learning",
)

data class VocabularyContext(
    val text: String,
    val bookId: String = "",
    val bookTitle: String = "",
    val createdAt: Long = 0L,
)

data class BookWordCandidate(
    val word: String,
    val count: Int,
)


data class TextAnnotation(
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val color: String,
    val note: String = "",
    val locatorJson: String = "",
    val source: String = "legacy",
)


enum class ReaderAction {
    PREVIOUS,
    NEXT,
    TOGGLE_CHROME,
    BOOKMARK,
    NONE,
}

enum class ReaderRulerStyle {
    BAND,
    LINE,
    DOUBLE_LINE,
}


data class ReadiumBookmark(
    val locatorJson: String,
    val label: String = "",
    val createdAt: Long = 0L,
)

data class ReadingStat(
    val date: String,
    val bookId: String,
    val seconds: Long,
)
