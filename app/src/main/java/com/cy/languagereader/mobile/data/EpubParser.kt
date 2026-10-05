package com.cy.languagereader.mobile.data

import android.text.Html
import android.util.Base64
import org.w3c.dom.Element
import java.io.File
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

data class EpubExtract(
    val paragraphs: List<String>,
    val chapters: List<ChapterItem>,
    val coverBytes: ByteArray? = null,
    val coverExtension: String = "jpg",
)

object EpubParser {
    fun extract(file: File): List<String> = extractDetailed(file).paragraphs

    /**
     * EPUB text + inline raster image extraction.
     *
     * Image tags become standalone reader paragraphs using BookRepository's
     * private image marker. When [imageDir] is supplied, the actual EPUB image
     * bytes are copied there so the reader can display them offline.
     */
    fun extractDetailed(file: File, imageDir: File? = null): EpubExtract {
        imageDir?.mkdirs()

        ZipFile(file).use { zip ->
            val container = zip.getEntry("META-INF/container.xml")
                ?: error("EPUB invalide : META-INF/container.xml absent")
            val rootFile = zip.getInputStream(container).use { input ->
                val doc = xmlFactory().newDocumentBuilder().parse(input)
                val nodes = doc.getElementsByTagNameNS("*", "rootfile")
                if (nodes.length == 0) error("EPUB invalide : OPF introuvable")
                (nodes.item(0) as Element).getAttribute("full-path")
            }

            val opfEntry = zip.getEntry(rootFile) ?: error("EPUB invalide : fichier OPF absent")
            val opfDoc = zip.getInputStream(opfEntry).use { input ->
                xmlFactory().newDocumentBuilder().parse(input)
            }

            data class ManifestItem(
                val href: String,
                val mediaType: String,
                val properties: String,
            )

            val manifest = mutableMapOf<String, ManifestItem>()
            val itemNodes = opfDoc.getElementsByTagNameNS("*", "item")
            for (i in 0 until itemNodes.length) {
                val e = itemNodes.item(i) as Element
                manifest[e.getAttribute("id")] = ManifestItem(
                    href = e.getAttribute("href"),
                    mediaType = e.getAttribute("media-type"),
                    properties = e.getAttribute("properties"),
                )
            }

            val spineIds = mutableListOf<String>()
            val spineNodes = opfDoc.getElementsByTagNameNS("*", "itemref")
            for (i in 0 until spineNodes.length) {
                spineIds += (spineNodes.item(i) as Element).getAttribute("idref")
            }

            val base = rootFile.substringBeforeLast('/', "")
            val paragraphs = mutableListOf<String>()
            val chapters = mutableListOf<ChapterItem>()
            val extractedImages = linkedMapOf<String, String>()
            var imageCounter = 0

            fun materializeImage(chapterPath: String, rawRef: String): String? {
                val dir = imageDir ?: return null
                val ref = rawRef.trim()
                    .replace("&amp;", "&")
                    .substringBefore('#')
                    .substringBefore('?')
                if (ref.isBlank()) return null

                if (ref.startsWith("data:image/", ignoreCase = true)) {
                    val comma = ref.indexOf(',')
                    if (comma <= 0) return null
                    val header = ref.substring(0, comma).lowercase(Locale.ROOT)
                    if (!header.contains(";base64")) return null
                    val mime = header.substringAfter("data:image/").substringBefore(';')
                    val ext = when (mime) {
                        "jpeg", "jpg" -> "jpg"
                        "png" -> "png"
                        "webp" -> "webp"
                        "gif" -> "gif"
                        "bmp" -> "bmp"
                        else -> "img"
                    }
                    val cacheKey = "data:${ref.hashCode()}"
                    extractedImages[cacheKey]?.let { return it }
                    val bytes = runCatching {
                        Base64.decode(ref.substring(comma + 1), Base64.DEFAULT)
                    }.getOrNull() ?: return null
                    if (bytes.isEmpty()) return null
                    val out = File(dir, "img_${++imageCounter}.$ext")
                    out.writeBytes(bytes)
                    return out.absolutePath.also { extractedImages[cacheKey] = it }
                }

                if (ref.startsWith("http://", true) || ref.startsWith("https://", true)) {
                    return null // keep EPUB reading offline; don't hotlink remote images
                }

                val decoded = runCatching { URLDecoder.decode(ref, "UTF-8") }.getOrDefault(ref)
                val chapterBase = chapterPath.substringBeforeLast('/', "")
                val zipPath = normalizeZipPath(
                    if (chapterBase.isEmpty()) decoded else "$chapterBase/$decoded"
                )
                extractedImages[zipPath]?.let { return it }

                val entry = zip.getEntry(zipPath)
                    ?: zip.getEntry(decoded)
                    ?: return null

                val ext = extensionFor(zipPath)
                val out = File(dir, "img_${++imageCounter}.$ext")
                zip.getInputStream(entry).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                if (!out.exists() || out.length() == 0L) {
                    runCatching { out.delete() }
                    return null
                }
                return out.absolutePath.also { extractedImages[zipPath] = it }
            }

            spineIds.forEachIndexed { _, id ->
                val item = manifest[id] ?: return@forEachIndexed
                if (!item.mediaType.contains("html") &&
                    !item.href.endsWith(".xhtml", true) &&
                    !item.href.endsWith(".html", true)
                ) return@forEachIndexed

                val path = normalizeZipPath(if (base.isEmpty()) item.href else "$base/${item.href}")
                val entry = zip.getEntry(path) ?: return@forEachIndexed
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }

                val imageTokens = linkedMapOf<String, String>()
                var chapterImageToken = 0

                val readableHtml = sanitizeAndInsertImages(html, path) { ref ->
                    val localImagePath =
                        materializeImage(path, ref) ?: return@sanitizeAndInsertImages null

                    // Html.fromHtml() never sees a filesystem path.
                    val token = "LRIMGTOKEN_${chapterImageToken++}_ENDTOKEN"
                    imageTokens[token] = BookRepository.imageMarker(localImagePath)
                    token
                }

                var plainText = Html.fromHtml(readableHtml, Html.FROM_HTML_MODE_LEGACY)
                    .toString()

                // Restore real internal image markers only after HTML conversion.
                imageTokens.forEach { (token, marker) ->
                    plainText = plainText.replace(token, "\n\n$marker\n\n")
                }

                // Any image marker must be a standalone paragraph.
                plainText = IMAGE_MARKER_REGEX.replace(plainText) { match ->
                    "\n\n${match.value}\n\n"
                }

                // U+FFFC is Android's generic inline-object placeholder. If an
                // unsupported EPUB object remains, don't pretend it is text.
                plainText = plainText
                    .replace('\uFFFC', ' ')
                    .replace(Regex("""(?i)\[OBJ\]"""), " [图片暂时无法读取] ")

                val chunk = splitParagraphs(plainText)
                if (chunk.isEmpty()) return@forEachIndexed

                val title = chapterTitle(html)
                    .ifBlank { "第 ${chapters.size + 1} 章" }
                    .take(120)

                chapters += ChapterItem(title, paragraphs.size)
                paragraphs += chunk
            }

            // EPUB cover: prefer EPUB3 cover-image property, then EPUB2 <meta name="cover">.
            var coverId: String? = manifest.entries
                .firstOrNull { (_, v) -> v.properties.split(Regex("\\s+")).contains("cover-image") }
                ?.key

            if (coverId == null) {
                val metaNodes = opfDoc.getElementsByTagNameNS("*", "meta")
                for (i in 0 until metaNodes.length) {
                    val e = metaNodes.item(i) as Element
                    if (e.getAttribute("name").equals("cover", ignoreCase = true)) {
                        coverId = e.getAttribute("content").takeIf { it.isNotBlank() }
                        if (coverId != null) break
                    }
                }
            }

            val coverItem = coverId?.let { manifest[it] }
            val coverBytes = coverItem?.let { cover ->
                val path = normalizeZipPath(if (base.isEmpty()) cover.href else "$base/${cover.href}")
                zip.getEntry(path)?.let { coverEntry ->
                    zip.getInputStream(coverEntry).use { it.readBytes() }
                }
            }
            val coverExt = coverItem?.href?.substringAfterLast('.', "jpg")?.lowercase()
                ?.takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"

            return EpubExtract(
                paragraphs = paragraphs.filter { it.isNotBlank() },
                chapters = chapters.distinctBy { it.paragraphIndex },
                coverBytes = coverBytes,
                coverExtension = coverExt,
            )
        }
    }

    private fun sanitizeAndInsertImages(
        html: String,
        chapterPath: String,
        resolveImage: (String) -> String?,
    ): String {
        var out = html
            .replace(Regex("""(?is)<style\b[^>]*>.*?</style>"""), " ")
            .replace(Regex("""(?is)<script\b[^>]*>.*?</script>"""), " ")
            .replace(Regex("""(?is)<noscript\b[^>]*>.*?</noscript>"""), " ")
            .replace(Regex("""(?is)<head\b[^>]*>.*?</head>"""), " ")

        // SVG often wraps a raster <image href="...">. Preserve that image
        // before stripping the SVG container itself.
        out = Regex("""(?is)<svg\b[^>]*>(.*?)</svg>""").replace(out) { match ->
            val ref = findAttributeRef(match.value, listOf("xlink:href", "href", "src"))
            ref?.let(resolveImage)?.let { "\n\n$it\n\n" } ?: " "
        }

        // EPUB <object data="..."> was the source of the old [OBJ] placeholder.
        // Preserve actual image data; otherwise keep any fallback text inside.
        out = Regex("""(?is)<object\b([^>]*)>(.*?)</object>""").replace(out) { match ->
            val ref = findAttributeRef(match.groupValues[1], listOf("data", "src", "href"))
            val marker = ref?.let(resolveImage)
            marker?.let { "\n\n$it\n\n" } ?: match.groupValues[2]
        }

        out = Regex("""(?is)<img\b[^>]*>""").replace(out) { match ->
            val ref = findAttributeRef(match.value, listOf("src", "data-src", "href"))
            ref?.let(resolveImage)?.let { "\n\n$it\n\n" } ?: " "
        }

        // Some XHTML books use the SVG <image/> tag without a wrapping block.
        out = Regex("""(?is)<image\b[^>]*?/?>""").replace(out) { match ->
            val ref = findAttributeRef(match.value, listOf("xlink:href", "href", "src"))
            ref?.let(resolveImage)?.let { "\n\n$it\n\n" } ?: " "
        }

        return out
    }

    private fun findAttributeRef(tag: String, names: List<String>): String? {
        for (name in names) {
            val regex = Regex(
                """(?is)\b${Regex.escape(name)}\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))"""
            )
            val match = regex.find(tag) ?: continue
            return match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
        }
        return null
    }

    private fun extensionFor(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> ext
            else -> "img"
        }
    }

    private fun looksLikeCss(text: String): Boolean {
        if (BookRepository.isImageParagraph(text)) return false
        val clean = text.trim()
        if (clean.length > 1200) return false
        val lower = clean.lowercase()
        val cssSignals =
            lower.contains("@page") ||
            lower.contains("text-align:") ||
            lower.contains("padding:") ||
            lower.contains("margin:") ||
            lower.contains("font-family:") ||
            Regex("""(?i)\b(body|html|cover)\s*\{""").containsMatchIn(clean)
        return cssSignals && clean.contains('{') && clean.contains('}')
    }

    private fun chapterTitle(html: String): String {
        val candidates = listOf(
            Regex("""(?is)<h1\b[^>]*>(.*?)</h1>"""),
            Regex("""(?is)<h2\b[^>]*>(.*?)</h2>"""),
            Regex("""(?is)<title\b[^>]*>(.*?)</title>"""),
        )
        for (regex in candidates) {
            val raw = regex.find(html)?.groupValues?.getOrNull(1) ?: continue
            val clean = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
                .replace(Regex("\\s+"), " ").trim()
            if (clean.isNotBlank()) return clean
        }
        return ""
    }

    private fun splitParagraphs(text: String): List<String> =
        text.replace("\r\n", "\n")
            .split(Regex("\\n\\s*\\n+"))
            .flatMap { block ->
                val trimmed = block.trim()
                if (BookRepository.isImageParagraph(trimmed)) {
                    listOf(trimmed)
                } else if (trimmed.length > 1800) {
                    trimmed.split(Regex("(?<=[.!?…])\\s+"))
                } else {
                    listOf(trimmed)
                }
            }
            .map {
                if (BookRepository.isImageParagraph(it.trim())) it.trim()
                else it.replace(Regex("\\s*\\n\\s*"), " ").trim()
            }
            .filter { it.isNotEmpty() && !looksLikeCss(it) }

    private fun normalizeZipPath(path: String): String {
        val out = mutableListOf<String>()
        path.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (out.isNotEmpty()) out.removeAt(out.lastIndex)
                else -> out += part
            }
        }
        return out.joinToString("/")
    }

    private fun xmlFactory() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }

    private val IMAGE_MARKER_REGEX =
        Regex("""\[\[LR_IMAGE:.*?:LR_IMAGE\]\]|\u001FIMG:[^\u001F]+\u001F""")
}
