package com.cy.languagereader.mobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import kotlin.math.abs
import kotlin.math.max

data class PdfWordBox(
    val word: String,
    val startOffset: Int,
    val endOffset: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class PdfPageAnalysis(
    val pageIndex: Int,
    val text: String,
    val words: List<PdfWordBox>,
    val hasTextLayer: Boolean,
)

object PdfPageAnalyzer {
    const val EMPTY_PAGE_SENTINEL = "\u241FPDF_PAGE\u241F"

    fun pageCount(context: Context, file: File): Int {
        PDFBoxResourceLoader.init(context.applicationContext)
        return PDDocument.load(file).use { it.numberOfPages }
    }

    fun extractPageTexts(context: Context, file: File): List<String> {
        PDFBoxResourceLoader.init(context.applicationContext)

        return PDDocument.load(file).use { document ->
            (0 until document.numberOfPages).map { pageIndex ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                }

                val raw = runCatching { stripper.getText(document) }.getOrDefault("")
                normalizePageText(raw).ifBlank { EMPTY_PAGE_SENTINEL }
            }
        }
    }

    fun analyzePage(
        context: Context,
        file: File,
        pageIndex: Int,
    ): PdfPageAnalysis {
        PDFBoxResourceLoader.init(context.applicationContext)

        return PDDocument.load(file).use { document ->
            if (pageIndex !in 0 until document.numberOfPages) {
                return PdfPageAnalysis(pageIndex, "", emptyList(), false)
            }

            val page = document.getPage(pageIndex)
            val crop = page.cropBox
            val width = crop.width.coerceAtLeast(1f)
            val height = crop.height.coerceAtLeast(1f)

            val collector = PositionCollector(pageIndex)
            collector.getText(document)

            buildAnalysis(
                pageIndex = pageIndex,
                positions = collector.positions,
                pageWidth = width,
                pageHeight = height,
            )
        }
    }

    fun renderPage(
        file: File,
        pageIndex: Int,
        targetWidthPx: Int,
    ): Bitmap {
        val safeWidth = targetWidthPx.coerceIn(540, 1600)
        val descriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )

        try {
            val renderer = PdfRenderer(descriptor)

            try {
                require(pageIndex in 0 until renderer.pageCount) {
                    "PDF page index out of range: $pageIndex / ${renderer.pageCount}"
                }

                val page = renderer.openPage(pageIndex)

                try {
                    val ratio =
                        page.height.toFloat() /
                            page.width.coerceAtLeast(1)

                    val targetHeight = (safeWidth * ratio)
                        .toInt()
                        .coerceIn(720, 2800)

                    val bitmap = Bitmap.createBitmap(
                        safeWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )

                    return bitmap
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        } finally {
            descriptor.close()
        }
    }

    private fun normalizePageText(raw: String): String =
        raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("""[ \t]+"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

    private data class PdfChar(
        val char: Char,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    private data class RawWord(
        val word: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    private fun buildAnalysis(
        pageIndex: Int,
        positions: List<TextPosition>,
        pageWidth: Float,
        pageHeight: Float,
    ): PdfPageAnalysis {
        val chars = mutableListOf<PdfChar>()

        positions.forEach { position ->
            val raw = position.unicode.orEmpty()
            if (raw.isEmpty()) return@forEach

            val count = raw.length.coerceAtLeast(1)
            val totalWidth = position.widthDirAdj.coerceAtLeast(0.8f)
            val charWidth = totalWidth / count
            val charHeight = position.heightDir.coerceAtLeast(3f)

            raw.forEachIndexed { index, ch ->
                chars += PdfChar(
                    char = ch,
                    x = position.xDirAdj + charWidth * index,
                    y = position.yDirAdj,
                    width = charWidth,
                    height = charHeight,
                )
            }
        }

        if (chars.none { it.char.isLetter() }) {
            return PdfPageAnalysis(
                pageIndex = pageIndex,
                text = "",
                words = emptyList(),
                hasTextLayer = false,
            )
        }

        val sorted = chars.sortedWith(
            compareBy<PdfChar> { it.y }
                .thenBy { it.x }
        )

        val lines = mutableListOf<MutableList<PdfChar>>()
        sorted.forEach { ch ->
            val current = lines.lastOrNull()
            if (current == null) {
                lines += mutableListOf(ch)
            } else {
                val lineY = current.map { it.y }.average().toFloat()
                val lineHeight = current.maxOfOrNull { it.height } ?: ch.height
                val tolerance = max(3.2f, lineHeight * 0.65f)

                if (abs(ch.y - lineY) <= tolerance) {
                    current += ch
                } else {
                    lines += mutableListOf(ch)
                }
            }
        }

        val rawWordsByLine = lines.map { line ->
            val charsInLine = line.sortedBy { it.x }
            val words = mutableListOf<RawWord>()

            var buffer = StringBuilder()
            var left = 0f
            var right = 0f
            var baseline = 0f
            var maxHeight = 0f
            var previousRight = 0f

            fun flush() {
                val token = buffer.toString()
                    .trim('\'', '’', '-')

                if (token.any { it.isLetter() }) {
                    words += RawWord(
                        word = token,
                        x = left,
                        y = baseline,
                        width = (right - left).coerceAtLeast(1f),
                        height = maxHeight.coerceAtLeast(3f),
                    )
                }

                buffer = StringBuilder()
                left = 0f
                right = 0f
                baseline = 0f
                maxHeight = 0f
                previousRight = 0f
            }

            charsInLine.forEach { ch ->
                val accepted =
                    ch.char.isLetter() ||
                        ch.char == '\'' ||
                        ch.char == '’' ||
                        ch.char == '-'

                if (!accepted) {
                    flush()
                    return@forEach
                }

                val gap = if (buffer.isEmpty()) 0f else ch.x - previousRight
                val gapThreshold = max(2.3f, ch.height * 0.52f)

                if (buffer.isNotEmpty() && gap > gapThreshold) {
                    flush()
                }

                if (buffer.isEmpty()) {
                    left = ch.x
                    baseline = ch.y
                }

                buffer.append(ch.char)
                right = max(right, ch.x + ch.width)
                maxHeight = max(maxHeight, ch.height)
                previousRight = ch.x + ch.width
            }

            flush()
            words
        }.filter { it.isNotEmpty() }

        val pageText = StringBuilder()
        val outputWords = mutableListOf<PdfWordBox>()

        rawWordsByLine.forEachIndexed { lineIndex, lineWords ->
            lineWords.forEachIndexed { wordIndex, rawWord ->
                if (wordIndex > 0) pageText.append(' ')

                val start = pageText.length
                pageText.append(rawWord.word)
                val end = pageText.length

                val topRaw = rawWord.y - rawWord.height * 1.05f
                val bottomRaw = rawWord.y + rawWord.height * 0.28f

                outputWords += PdfWordBox(
                    word = rawWord.word,
                    startOffset = start,
                    endOffset = end,
                    left = (rawWord.x / pageWidth).coerceIn(0f, 1f),
                    top = (topRaw / pageHeight).coerceIn(0f, 1f),
                    right = ((rawWord.x + rawWord.width) / pageWidth)
                        .coerceIn(0f, 1f),
                    bottom = (bottomRaw / pageHeight)
                        .coerceIn(0f, 1f),
                )
            }

            if (lineIndex < rawWordsByLine.lastIndex) {
                pageText.append('\n')
            }
        }

        return PdfPageAnalysis(
            pageIndex = pageIndex,
            text = pageText.toString(),
            words = outputWords,
            hasTextLayer = outputWords.isNotEmpty(),
        )
    }

    private class PositionCollector(
        pageIndex: Int,
    ) : PDFTextStripper() {
        val positions = mutableListOf<TextPosition>()

        init {
            sortByPosition = true
            startPage = pageIndex + 1
            endPage = pageIndex + 1
        }

        override fun processTextPosition(text: TextPosition) {
            positions += text
            super.processTextPosition(text)
        }
    }
}
