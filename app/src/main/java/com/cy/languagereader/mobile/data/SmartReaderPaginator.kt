package com.cy.languagereader.mobile.data

import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan

/**
 * Height-aware paginator for the paper reader.
 *
 * V3.14 keeps pagination tied to the real viewport and reader typography, while
 * also accounting for publication-style first-line indentation.  Paragraphs stay
 * atomic so dictionary/highlight offsets continue to point at the original source
 * paragraph; the fallback character paginator is used only when measurements are
 * unavailable.
 */
object SmartReaderPaginator {
    fun paginate(
        paragraphs: List<String>,
        contentWidthPx: Int,
        contentHeightPx: Int,
        fontSizePx: Float,
        lineHeightPx: Float,
        paragraphGapPx: Int,
        verticalTextPaddingPx: Int,
        firstLineIndentPx: Int = 0,
        chapterTitles: Map<Int, String> = emptyMap(),
        chapterLabelHeightPx: Int = 0,
        chapterVerticalPaddingPx: Int = 0,
        imageHeightPx: Int = 0,
        fallbackChars: Int = 650,
    ): List<ReaderPage> {
        if (paragraphs.isEmpty()) return emptyList()
        if (contentWidthPx <= 0 || contentHeightPx <= 0 || fontSizePx <= 0f) {
            return ReaderPaginator.paginate(paragraphs, fallbackChars)
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizePx
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizePx * 1.30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        fun isDialogue(text: String): Boolean {
            val t = text.trimStart()
            return t.startsWith("—") || t.startsWith("–") || t.startsWith("-") ||
                t.startsWith("«") || t.startsWith("\"")
        }

        fun measuredHeight(
            text: String,
            paint: TextPaint,
            requestedLineHeight: Float,
            firstIndent: Int = 0,
        ): Int {
            if (text.isBlank()) return requestedLineHeight.toInt().coerceAtLeast(1)
            val base = paint.fontMetrics.run { (descent - ascent).coerceAtLeast(1f) }
            val multiplier = (requestedLineHeight / base).coerceIn(0.8f, 2.8f)
            val source: CharSequence = if (firstIndent > 0 && text.length > 1) {
                SpannableString(text).apply {
                    setSpan(
                        LeadingMarginSpan.Standard(firstIndent, 0),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            } else text
            return StaticLayout.Builder
                .obtain(source, 0, source.length, paint, contentWidthPx)
                .setIncludePad(false)
                .setLineSpacing(0f, multiplier)
                .build()
                .height
                .coerceAtLeast(1)
        }

        fun blockHeight(index: Int, paragraph: String): Int {
            val chapterStart = chapterTitles.containsKey(index)
            val indent = if (
                firstLineIndentPx > 0 &&
                !chapterStart &&
                !isDialogue(paragraph) &&
                paragraph.length >= 45
            ) firstLineIndentPx else 0

            var h = if (BookRepository.isImageParagraph(paragraph)) {
                imageHeightPx.coerceAtLeast((contentHeightPx * 0.34f).toInt())
            } else {
                measuredHeight(paragraph, bodyPaint, lineHeightPx, indent) + verticalTextPaddingPx
            }

            val chapterTitle = chapterTitles[index]
            if (!chapterTitle.isNullOrBlank()) {
                val titleLineHeight = lineHeightPx * 1.35f
                h += chapterLabelHeightPx +
                    measuredHeight(chapterTitle, titlePaint, titleLineHeight) +
                    chapterVerticalPaddingPx
            }
            return h
        }

        val pages = ArrayList<ReaderPage>()
        var start = 0
        var used = 0

        for (i in paragraphs.indices) {
            val block = blockHeight(i, paragraphs[i])
            val gap = if (i == start) 0 else paragraphGapPx
            if (i > start && used + gap + block > contentHeightPx) {
                pages += ReaderPage(start, i)
                start = i
                used = block
            } else {
                used += gap + block
            }
        }

        if (start < paragraphs.size) pages += ReaderPage(start, paragraphs.size)
        return pages.ifEmpty { ReaderPaginator.paginate(paragraphs, fallbackChars) }
    }
}
