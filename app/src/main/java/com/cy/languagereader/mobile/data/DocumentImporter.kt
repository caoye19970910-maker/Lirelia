package com.cy.languagereader.mobile.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.cy.languagereader.mobile.readium.ReadiumServices
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.UUID
import java.security.MessageDigest
import java.util.Locale
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import org.json.JSONArray
import org.json.JSONObject

class DocumentImporter(private val context: Context, private val repository: BookRepository) {
    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun import(uri: Uri): BookItem {
        val name = displayName(uri) ?: "livre_${System.currentTimeMillis()}"
        val ext = name.substringAfterLast('.', "txt").lowercase()
        require(ext in setOf("txt", "epub", "pdf")) { "Format non pris en charge : .$ext" }

        val bookDir = File(context.filesDir, "books").apply { mkdirs() }
        val token = UUID.randomUUID().toString().replace("-", "")
        val tempSource = File(bookDir, ".import_$token.$ext")
        val staging = mutableListOf<File>(tempSource)

        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Impossible de lire le fichier" }
                tempSource.outputStream().use { output -> input.copyTo(output) }
            }

            val sourceHash = sha256(tempSource)
            val aliasPrefs = context.getSharedPreferences(
                "language_reader_restore_aliases",
                Context.MODE_PRIVATE,
            )
            val restoredId = aliasPrefs.getString("sha256_$sourceHash", null)

            val existingId = repository.listBooks().firstOrNull { existing ->
                when {
                    existing.sourceHash.isNotBlank() -> existing.sourceHash == sourceHash
                    else -> {
                        val existingFile = File(existing.sourceFile)
                        existingFile.exists() &&
                            runCatching { sha256(existingFile) == sourceHash }.getOrDefault(false)
                    }
                }
            }?.id

            val id = restoredId ?: existingId ?: "b_${sourceHash.take(24)}"
            val finalImageDir = File(bookDir, "$id.images")
            val stagedImageDir = File(bookDir, ".$id.images.$token")
            var epubCover: Pair<ByteArray, String>? = null
            var chapters: List<ChapterItem> = emptyList()

            val paragraphs = when (ext) {
                "txt" -> splitText(readTextSmart(tempSource))
                "epub" -> {
                    // Readium is now the authority for whether an EPUB is readable.
                    // The legacy parser is only a best-effort metadata/text-cache helper;
                    // it must never reject a publication Readium can open.
                    runBlocking {
                        val publication = ReadiumServices(context).openEpub(tempSource)
                        try {
                            require(publication.readingOrder.isNotEmpty()) {
                                "EPUB 没有可阅读内容。"
                            }
                        } finally {
                            publication.close()
                        }
                    }

                    stagedImageDir.deleteRecursively()
                    stagedImageDir.mkdirs()
                    staging += stagedImageDir
                    val extracted = runCatching {
                        EpubParser.extractDetailed(tempSource, stagedImageDir)
                    }.getOrNull()
                    if (extracted != null) {
                        chapters = extracted.chapters
                        extracted.coverBytes?.let { epubCover = it to extracted.coverExtension }
                        // The cache must point at the committed image directory, not staging.
                        extracted.paragraphs.map { paragraph ->
                            paragraph.replace(stagedImageDir.absolutePath, finalImageDir.absolutePath)
                        }
                    } else {
                        // A valid EPUB may use constructs our old parser does not understand.
                        // Keep an empty auxiliary cache; the Readium reader uses the original EPUB.
                        emptyList()
                    }
                }
                "pdf" -> PdfPageAnalyzer.extractPageTexts(context, tempSource)
                else -> emptyList()
            }
            if (chapters.isEmpty() && ext != "pdf") chapters = detectChapters(paragraphs)
            require(paragraphs.isNotEmpty() || ext == "epub") {
                if (ext == "pdf") "PDF 无可读取页面。" else "Aucun texte lisible trouvé."
            }

            val stagedCache = File(bookDir, ".$id.reader.$token.txt").also { staging += it }
            stagedCache.writeText(paragraphs.joinToString(BookRepository.PARAGRAPH_MARK), Charsets.UTF_8)
            val stagedChapters = File(bookDir, ".$id.chapters.$token.json").also { staging += it }
            writeChapters(stagedChapters, chapters)

            var stagedCover: File? = null
            var coverExtension = ""
            when {
                epubCover != null -> {
                    val (bytes, extension) = epubCover!!
                    coverExtension = extension.ifBlank { "png" }
                    stagedCover = File(bookDir, ".$id.cover.$token.$coverExtension").also { staging += it }
                    stagedCover!!.writeBytes(bytes)
                }
                ext == "pdf" -> {
                    runCatching {
                        val bitmap = PdfPageAnalyzer.renderPage(
                            file = tempSource,
                            pageIndex = 0,
                            targetWidthPx = 520,
                        )
                        try {
                            coverExtension = "png"
                            stagedCover = File(bookDir, ".$id.cover.$token.png").also { staging += it }
                            val ok = stagedCover!!.outputStream().use { output ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, output)
                            }
                            if (!ok) error("PDF cover encode failed")
                        } finally {
                            bitmap.recycle()
                        }
                    }.onFailure {
                        stagedCover?.let { runCatching { it.delete() } }
                        stagedCover = null
                        coverExtension = ""
                    }
                }
            }

            val textParagraphs = paragraphs
                .filterNot { BookRepository.isImageParagraph(it) }
                .filterNot { it == PdfPageAnalyzer.EMPTY_PAGE_SENTINEL }
            val stats = wordStats(textParagraphs)

            // Commit only after parsing/cache generation has completely succeeded.
            val source = File(bookDir, "$id.$ext")
            tempSource.copyTo(source, overwrite = true)
            setOf("txt", "epub", "pdf")
                .filterNot { it == ext }
                .forEach { other -> File(bookDir, "$id.$other").delete() }

            val cache = File(bookDir, "$id.reader.txt")
            stagedCache.copyTo(cache, overwrite = true)
            val chapterFile = File(bookDir, "$id.chapters.json")
            stagedChapters.copyTo(chapterFile, overwrite = true)

            if (ext == "epub") {
                finalImageDir.deleteRecursively()
                if (!stagedImageDir.renameTo(finalImageDir)) {
                    stagedImageDir.copyRecursively(finalImageDir, overwrite = true)
                }
            } else {
                finalImageDir.deleteRecursively()
            }

            bookDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("$id.cover.") }
                ?.forEach { it.delete() }
            val coverFile = if (stagedCover != null && stagedCover!!.exists()) {
                val finalCover = File(bookDir, "$id.cover.${coverExtension.ifBlank { "png" }}")
                stagedCover!!.copyTo(finalCover, overwrite = true)
                finalCover.absolutePath
            } else ""

            val book = BookItem(
                id = id,
                title = name.substringBeforeLast('.').ifBlank { name },
                type = ext.uppercase(),
                sourceFile = source.absolutePath,
                textCacheFile = cache.absolutePath,
                totalChars = textParagraphs.sumOf { it.length },
                totalWords = stats.first,
                uniqueWords = stats.second,
                totalParagraphs = paragraphs.size,
                coverFile = coverFile,
                chapterFile = chapterFile.absolutePath,
                sourceHash = sourceHash,
                updatedAt = System.currentTimeMillis(),
            )
            repository.upsert(book)
            return book
        } finally {
            staging.asReversed().forEach { file ->
                runCatching {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readTextSmart(file: File): String {
        val bytes = file.readBytes()
        val utf8 = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        return runCatching { utf8.decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF") }
            .getOrElse { Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString() }
    }


    private fun splitText(text: String): List<String> =
        text.replace("\r\n", "\n")
            .split(Regex("\\n\\s*\\n+"))
            .flatMap { block ->
                val clean = block.replace(Regex("\\s*\\n\\s*"), " ").trim()
                if (clean.length > 1800) clean.split(Regex("(?<=[.!?…])\\s+")) else listOf(clean)
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun wordStats(paragraphs: List<String>): Pair<Int, Int> {
        val regex = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’'][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")
        var total = 0
        val unique = linkedSetOf<String>()
        paragraphs.forEach { paragraph ->
            regex.findAll(paragraph).forEach { match ->
                total++
                unique += match.value.replace('’', '\'').lowercase(Locale.FRENCH)
            }
        }
        return total to unique.size
    }


    private fun writeChapters(file: File, chapters: List<ChapterItem>) {
        val arr = JSONArray()
        chapters.forEach { chapter ->
            arr.put(JSONObject().put("title", chapter.title).put("paragraphIndex", chapter.paragraphIndex))
        }
        file.writeText(arr.toString(), Charsets.UTF_8)
    }

    private fun detectChapters(paragraphs: List<String>): List<ChapterItem> {
        val heading = Regex(
            """(?i)^(chapitre|chapter|partie|part|livre|book|prologue|épilogue|epilogue)\b.*"""
        )
        val detected = buildList {
            paragraphs.forEachIndexed { index, p ->
                val clean = p.replace(Regex("\\s+"), " ").trim()
                val looksLikeHeading =
                    clean.length in 2..90 &&
                    (heading.matches(clean) ||
                     (clean.length <= 55 && clean.any(Char::isLetter) &&
                      clean == clean.uppercase(Locale.FRENCH)))
                if (looksLikeHeading) add(ChapterItem(clean.take(120), index))
            }
        }.distinctBy { it.paragraphIndex }.take(200)

        return if (detected.isNotEmpty()) detected
        else if (paragraphs.isNotEmpty()) listOf(ChapterItem("开始阅读", 0))
        else emptyList()
    }

    private fun displayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return uri.lastPathSegment
    }
}
