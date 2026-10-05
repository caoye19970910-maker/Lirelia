package com.cy.languagereader.mobile.readium

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.graphics.RectF
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.View
import android.widget.Magnifier
import android.webkit.JavascriptInterface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.compose.AndroidFragment
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.BookItem
import com.cy.languagereader.mobile.data.BookRepository
import com.cy.languagereader.mobile.data.DictionaryLookup
import com.cy.languagereader.mobile.data.DictionaryDisplayFormatter
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.FrenchNounGender
import com.cy.languagereader.mobile.data.LibraryOrganizer
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.data.ReadiumBookmark
import com.cy.languagereader.mobile.data.ReadingProfileStore
import com.cy.languagereader.mobile.data.ReadingStatsStore
import com.cy.languagereader.mobile.data.TextAnnotation
import com.cy.languagereader.mobile.service.SentenceTranslationManager
import com.cy.languagereader.mobile.service.TtsManager
import com.cy.languagereader.mobile.ui.rememberReadingSessionSeconds
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily as ReadiumFontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.SearchService
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.getOrElse

private val ReaderPaper = Color(0xFFF7F3EA)
private val ReaderInk = Color(0xFF28231F)
private val ReaderAccent = Color(0xFF17324A)
private val ReaderMuted = Color(0xFF786E65)
private const val DECORATION_GROUP = "lirelia-highlights"

private data class ReaderFontChoice(
    val code: String,
    val label: String,
    val note: String,
)

private val READER_FONT_CHOICES = listOf(
    ReaderFontChoice("DEFAULT", "原书字体", "保留电子书原本的字体"),
    ReaderFontChoice("SERIF", "系统衬线", "关闭原书排版并强制使用系统 serif；原书本身是衬线时仍可能风格接近"),
    ReaderFontChoice("SANS_SERIF", "清爽无衬线", "干净、现代、易扫读"),
    ReaderFontChoice("CURSIVE", "手写体", "更轻松的手写感"),
    ReaderFontChoice("MONOSPACE", "等宽字体", "字符宽度统一"),
    ReaderFontChoice("OPEN_DYSLEXIC", "OpenDyslexic", "强化字形差异，减少相似字符混淆"),
    ReaderFontChoice("ACCESSIBLE_DFA", "Accessible DfA", "面向长时间阅读的易读字体"),
    ReaderFontChoice("IA_WRITER_DUOSPACE", "iA Writer Duospace", "介于比例字体与等宽字体之间"),
)

private fun readerFontChoice(code: String): ReaderFontChoice =
    READER_FONT_CHOICES.firstOrNull { it.code == code.uppercase() } ?: READER_FONT_CHOICES.first()

private fun readiumFontFamily(code: String): ReadiumFontFamily? = when (code.uppercase()) {
    "SERIF" -> ReadiumFontFamily.SERIF
    "SANS_SERIF" -> ReadiumFontFamily.SANS_SERIF
    "CURSIVE" -> ReadiumFontFamily.CURSIVE
    "MONOSPACE" -> ReadiumFontFamily.MONOSPACE
    "OPEN_DYSLEXIC" -> ReadiumFontFamily.OPEN_DYSLEXIC
    "ACCESSIBLE_DFA" -> ReadiumFontFamily.ACCESSIBLE_DFA
    "IA_WRITER_DUOSPACE" -> ReadiumFontFamily.IA_WRITER_DUOSPACE
    else -> null
}

private data class OpenedPublication(
    val publication: Publication,
    val initialLocator: Locator?,
)

private data class QuickTap(
    val word: String,
    val sentence: String,
    val wordOffsetInSentence: Int = -1,
)

private data class SelectionUi(
    val text: String,
    val locator: Locator,
    val rect: RectF?,
)

private class LireliaTapBridge(
    private val activity: ReadiumReaderActivity,
    private val onWord: (String, String, Int) -> Unit,
    private val onEdge: (Boolean) -> Unit,
) {
    @JavascriptInterface
    fun onWord(word: String?, sentence: String?, wordOffsetInSentence: Int, x: Float, y: Float) {
        val cleanWord = word.orEmpty().trim()
        if (cleanWord.isBlank()) return
        activity.runOnUiThread {
            onWord(cleanWord, sentence.orEmpty().trim().ifBlank { cleanWord }, wordOffsetInSentence)
        }
    }

    @JavascriptInterface
    fun onScrollEdge(direction: Int) {
        activity.runOnUiThread { onEdge(direction > 0) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReadiumReaderScreen(
    activity: ReadiumReaderActivity,
    book: BookItem,
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    settings: AppSettings,
    onBack: () -> Unit,
    onPageCallbacks: (() -> Unit, () -> Unit) -> Unit,
) {
    val services = remember { ReadiumServices(activity) }
    val annotations = remember { ReadiumAnnotationStore(activity, learningStore) }
    val readingStats = remember { ReadingStatsStore(activity) }
    val readingProfile = remember { ReadingProfileStore(activity) }
    val libraryOrganizer = remember { LibraryOrganizer(activity) }
    val displayBookTitle = remember(book.id, book.title) { libraryOrganizer.displayTitle(book.id, book.title) }
    val progressPrefs = remember {
        activity.getSharedPreferences("lirelia_readium_progress", Context.MODE_PRIVATE)
    }
    val migrationPrefs = remember {
        activity.getSharedPreferences("lirelia_readium_migrations", Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Reconnect EPUB reading with the same reading-time statistics used by TXT/PDF.
    rememberReadingSessionSeconds(book.id, settings, readingStats)

    var opened by remember(book.id) { mutableStateOf<OpenedPublication?>(null) }
    var openError by remember(book.id) { mutableStateOf<String?>(null) }
    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var currentLocator by remember { mutableStateOf<Locator?>(null) }
    var selectionPulse by remember { mutableIntStateOf(0) }
    var selectionUi by remember { mutableStateOf<SelectionUi?>(null) }
    var quickTap by remember { mutableStateOf<QuickTap?>(null) }
    var quickLookup by remember { mutableStateOf<DictionaryLookup?>(null) }
    var wordFallbackTranslation by remember { mutableStateOf("") }
    var sentenceTranslation by remember { mutableStateOf("") }
    var sentenceHint by remember { mutableStateOf("") }
    var sentenceSource by remember { mutableStateOf("") }
    var quickBusy by remember { mutableStateOf(false) }
    var fullDictionary by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(false) }
    var typographyVisible by remember { mutableStateOf(false) }
    var tocVisible by remember { mutableStateOf(false) }
    var highlightRefresh by remember { mutableIntStateOf(0) }
    var annotationItems by remember(book.id) { mutableStateOf<List<ReadiumHighlight>>(emptyList()) }
    var noteTarget by remember { mutableStateOf<SelectionUi?>(null) }
    var noteDraft by remember { mutableStateOf("") }
    var bookmarks by remember(book.id) { mutableStateOf(bookRepository.readiumBookmarks(book.id)) }

    var fontFamily by remember { mutableStateOf(settings.readerFontFamily) }
    var fontSize by remember { mutableIntStateOf(settings.readerFontSize) }
    var lineSpacing by remember { mutableIntStateOf(settings.readerLineSpacingPercent) }
    var pageMargin by remember { mutableIntStateOf(settings.readerMarginDp) }
    var readerTheme by remember { mutableStateOf(settings.readerTheme) }
    var scrollMode by remember { mutableStateOf(settings.readerMode == "SCROLL") }
    var autoScrollSpeed by remember { mutableIntStateOf(settings.readerAutoScrollSpeed) }
    var autoScrollRunning by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(settings.readerKeepScreenOn) }
    var twoPageLandscape by remember { mutableStateOf(settings.readerTwoPageLandscape) }
    var volumeKeyTurnsPage by remember { mutableStateOf(settings.readerVolumeKeyTurnsPage) }
    var readingMagnifierEnabled by remember { mutableStateOf(settings.readerReadingRuler) }
    var rulerPositionPercent by remember { mutableIntStateOf(settings.readerRulerPositionPercent) }
    var rulerSizeDp by remember { mutableIntStateOf(settings.readerRulerSizeDp) }
    var magnifierWidthPercent by remember { mutableIntStateOf(settings.readerMagnifierWidthPercent) }
    var magnifierZoomPercent by remember { mutableIntStateOf(settings.readerMagnifierZoomPercent) }
    var lineGuideEnabled by remember { mutableStateOf(settings.readerLineGuideEnabled) }
    var lineGuidePositionPercent by remember { mutableIntStateOf(settings.readerLineGuidePositionPercent) }
    var lineGuideHeightDp by remember { mutableIntStateOf(settings.readerLineGuideHeightDp) }
    var lineGuideTopColor by remember { mutableStateOf(settings.readerLineGuideTopColor) }
    var lineGuideBottomColor by remember { mutableStateOf(settings.readerLineGuideBottomColor) }
    var globalLearningColors by remember(book.id) { mutableStateOf<Map<String, String>>(emptyMap()) }

    val twoPage = twoPageLandscape && isLandscape && !scrollMode
    val isDark = readerTheme.uppercase() in setOf("DARK", "BLACK", "INK")
    val colorScheme = remember(readerTheme) { readerColorScheme(readerTheme) }

    // Back dismisses transient reading UI first instead of unexpectedly leaving the book.
    BackHandler(enabled = quickTap != null || selectionUi != null || chromeVisible) {
        when {
            quickTap != null -> quickTap = null
            selectionUi != null -> {
                navigator?.clearSelection()
                selectionUi = null
            }
            chromeVisible -> chromeVisible = false
        }
    }

    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(book.id) {
        bookRepository.markOpened(book.id)
        openError = null
        opened = runCatching {
            withContext(Dispatchers.IO) {
                val publication = services.openEpub(File(book.sourceFile))
                val stored = progressPrefs.getString("locator_${book.id}", null)
                    ?.let { raw -> runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull() }
                val initial = stored ?: approximateLegacyLocator(publication, book, bookRepository)
                OpenedPublication(publication, initial)
            }
        }.onFailure { openError = it.message ?: "Readium 打开失败" }.getOrNull()
    }

    DisposableEffect(opened?.publication) {
        val publication = opened?.publication
        onDispose { runCatching { publication?.close() } }
    }

    // V4.1 preference-backed highlights may require SQLite writes. Never run this
    // migration from ReadiumAnnotationStore's constructor on the Compose/UI thread.
    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            annotations.migrateLegacyPreferenceHighlightsIfNeeded()
        }
        highlightRefresh++
    }

    // Preserve bookmarks created by the old paragraph-based EPUB reader. Readium
    // positions are stable locators, so migrate each legacy paragraph bookmark once.
    LaunchedEffect(opened?.publication, book.id) {
        val publication = opened?.publication ?: return@LaunchedEffect
        val migrationKey = "legacy_bookmarks_v42_${book.id}"
        if (migrationPrefs.getBoolean(migrationKey, false)) return@LaunchedEffect
        val legacyBookmarks = bookRepository.bookmarks(book.id)

        // No legacy data means migration is genuinely complete.
        if (legacyBookmarks.isEmpty() || book.totalParagraphs <= 1) {
            migrationPrefs.edit().putBoolean(migrationKey, true).apply()
            return@LaunchedEffect
        }

        val positions = withContext(Dispatchers.IO) {
            runCatching { publication.positions() }.getOrDefault(emptyList())
        }
        // A transient Readium positions failure must not permanently mark the book
        // as migrated. We will retry on a later open.
        if (positions.isEmpty()) return@LaunchedEffect

        var migratedCount = 0
        legacyBookmarks.sorted().forEach { paragraphIndex ->
            val expected = paragraphIndex.coerceIn(0, book.totalParagraphs - 1).toDouble() /
                (book.totalParagraphs - 1).toDouble()
            val target = positions.minByOrNull {
                abs((it.locations.totalProgression ?: 0.0) - expected)
            } ?: return@forEach
            val raw = target.toJSON().toString()
            if (!bookRepository.isReadiumBookmarked(book.id, raw)) {
                bookmarks = bookRepository.toggleReadiumBookmark(
                    bookId = book.id,
                    locatorJson = raw,
                    label = "旧版书签 · ${(expected * 100).roundToInt()}%",
                )
            }
            migratedCount++
        }
        if (migratedCount == legacyBookmarks.size) {
            migrationPrefs.edit().putBoolean(migrationKey, true).apply()
        }
    }

    // One-time V3.x -> Readium highlight migration. Failed/unresolved rows remain
    // retryable instead of permanently setting the migration-complete flag.
    LaunchedEffect(opened?.publication, book.id) {
        val publication = opened?.publication ?: return@LaunchedEffect
        val migrationKey = "legacy_annotations_v42_${book.id}"
        if (migrationPrefs.getBoolean(migrationKey, false)) return@LaunchedEffect
        val legacy = withContext(Dispatchers.IO) {
            learningStore.textAnnotations(book.id).values.flatten()
        }
        if (legacy.isEmpty()) {
            migrationPrefs.edit().putBoolean(migrationKey, true).putInt("${migrationKey}_count", 0).apply()
            return@LaunchedEffect
        }

        var migrated = 0
        var unresolved = 0
        legacy.forEach { mark ->
            val query = mark.text.trim().take(300)
            if (query.length < 2) {
                // There is no useful stable text anchor to search for. Treat this row
                // as non-migratable rather than retrying forever.
                migrated++
                return@forEach
            }
            // Publication search can scan/decompress multiple EPUB resources. Keep the
            // whole iterator lifecycle off the Compose/UI thread during migration.
            val (hits, searchFailed) = withContext(Dispatchers.IO) {
                val iterator = runCatching {
                    publication.search(
                        query,
                        SearchService.Options(
                            caseSensitive = false,
                            diacriticSensitive = false,
                            wholeWord = false,
                            exact = true,
                        ),
                    )
                }.getOrNull() ?: return@withContext emptyList<Locator>() to true

                val found = mutableListOf<Locator>()
                var failed = false
                try {
                    while (found.size < 40) {
                        val next = iterator.next()
                        var pageFailed = false
                        val page = next.getOrElse {
                            pageFailed = true
                            null
                        }
                        if (pageFailed) {
                            failed = true
                            break
                        }
                        if (page == null) break
                        found += page.locators
                    }
                } finally {
                    iterator.close()
                }
                found.toList() to failed
            }
            if (searchFailed || hits.isEmpty()) {
                unresolved++
                return@forEach
            }

            val expected = if (book.totalParagraphs > 1 && mark.paragraphIndex >= 0) {
                mark.paragraphIndex.toDouble() / (book.totalParagraphs - 1).toDouble()
            } else null
            val target = if (expected == null) {
                hits.first()
            } else {
                hits.minByOrNull { abs((it.locations.totalProgression ?: 0.0) - expected) }
                    ?: hits.first()
            }
            withContext(Dispatchers.IO) {
                learningStore.promoteLegacyAnnotation(book.id, mark, target.toJSON().toString())
            }
            migrated++
        }
        migrationPrefs.edit()
            .putInt("${migrationKey}_count", migrated)
            .putInt("${migrationKey}_unresolved", unresolved)
            .apply()
        if (unresolved == 0) {
            migrationPrefs.edit().putBoolean(migrationKey, true).apply()
        }
        if (migrated > 0) highlightRefresh++
    }

    LaunchedEffect(selectionPulse, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        if (selectionPulse <= 0) return@LaunchedEffect
        delay(75)
        val selection = runCatching { nav.currentSelection() }.getOrNull()
        selectionUi = selection?.let {
            val text = it.locator.text.highlight.orEmpty().trim()
            if (text.isBlank()) null else SelectionUi(text, it.locator, it.rect)
        }
    }

    // Keep the floating toolbar attached to the actual selection while the page
    // settles or scrolls. A stale one-shot RectF is what made the old popup appear
    // to drift away from the text.
    LaunchedEffect(selectionUi?.locator?.toJSON()?.toString(), navigator) {
        val nav = navigator ?: return@LaunchedEffect
        while (selectionUi != null) {
            delay(120)
            val current = runCatching { nav.currentSelection() }.getOrNull()
            if (current == null) {
                selectionUi = null
                break
            }
            val text = current.locator.text.highlight.orEmpty().trim()
            if (text.isBlank()) {
                selectionUi = null
                break
            }
            selectionUi = SelectionUi(text, current.locator, current.rect)
        }
    }

    LaunchedEffect(quickTap) {
        val tap = quickTap ?: return@LaunchedEffect
        quickBusy = true
        quickLookup = null
        wordFallbackTranslation = ""
        sentenceTranslation = ""
        sentenceHint = ""
        sentenceSource = ""

        // One tap still means lookup + pronunciation. TTS now chooses a quieter
        // automatic route, so pronunciation never blocks the card itself.
        tts.speak(tap.word)

        // Always paint the lexical result first. Sentence translation is secondary
        // and runs afterwards/alongside network fallback so a slow network never
        // makes the word card look empty.
        val local = withContext(Dispatchers.IO) {
            readingProfile.recordLookup(tap.word, book.id)
            dictionary.lookupSafe(tap.word, tap.sentence, tap.wordOffsetInSentence)
        }
        quickLookup = local

        val sentence = tap.sentence.trim()
        coroutineScope {
            val offlineDeferred = if (sentence.isNotBlank()) {
                async(Dispatchers.IO) { offlineGloss(dictionary, sentence) }
            } else null
            val sentenceDeferred = if (sentence.isNotBlank() && !settings.safeMode) {
                async(Dispatchers.IO) {
                    SentenceTranslationManager.translate(
                        context = activity,
                        sentence = sentence,
                        allowNetwork = true,
                    )
                }
            } else null
            val wordDeferred = if (local.result.definitions.isEmpty() && !settings.safeMode) {
                async(Dispatchers.IO) {
                    SentenceTranslationManager.translate(
                        context = activity,
                        sentence = tap.word,
                        allowNetwork = true,
                    )
                }
            } else null

            sentenceHint = offlineDeferred?.await().orEmpty()
            wordDeferred?.await()?.text?.trim()?.takeIf { it.isNotBlank() }?.let {
                wordFallbackTranslation = it
            }
            sentenceDeferred?.await()?.let { translated ->
                if (translated.text.isNotBlank()) {
                    sentenceTranslation = translated.text.trim()
                    sentenceSource = translated.source
                }
            }
        }
        quickBusy = false
    }

    // UI location follows Readium immediately. Persistence, tap-bridge setup and
    // exposure sampling deliberately run as separate jobs: a slow disk write must
    // never postpone word taps on a freshly loaded chapter.
    LaunchedEffect(navigator) {
        val nav = navigator ?: return@LaunchedEffect
        nav.currentLocator.collect { locator -> currentLocator = locator }
    }

    // Install the word-tap bridge as soon as a new resource becomes current. The
    // old V4.3 path waited for the 700 ms progress debounce, leaving a short window
    // where taps on a just-opened chapter did nothing. A few short retries cover
    // the moment between currentLocator changing and the WebView finishing load.
    LaunchedEffect(
        navigator, currentLocator?.href?.toString(),
        fontFamily, fontSize, lineSpacing, pageMargin, readerTheme, scrollMode, twoPage,
    ) {
        val nav = navigator ?: return@LaunchedEffect
        if (currentLocator == null) return@LaunchedEffect
        delay(90)
        repeat(6) { attempt ->
            val result = runCatching { nav.evaluateJavascript(INSTALL_TAP_SCRIPT) }
                .getOrNull().orEmpty()
            if (result.contains("installed") || result.contains("already")) {
                return@LaunchedEffect
            }
            delay(60L + attempt * 35L)
        }
    }

    // Debounced disk persistence only.
    LaunchedEffect(navigator, opened?.publication) {
        val nav = navigator ?: return@LaunchedEffect
        nav.currentLocator.collectLatest { locator ->
            delay(700)
            progressPrefs.edit().putString("locator_${book.id}", locator.toJSON().toString()).apply()
            locator.locations.totalProgression?.let { progression ->
                if (book.totalParagraphs > 0) {
                    val index = (progression.coerceIn(0.0, 1.0) * (book.totalParagraphs - 1))
                        .roundToInt()
                    bookRepository.saveProgress(book.id, index)
                }
            }
        }
    }

    // Exposure sampling has its own debounce. It is useful when the reader settles
    // on text, but should not compete with page turns or progress persistence.
    LaunchedEffect(navigator, opened?.publication) {
        val nav = navigator ?: return@LaunchedEffect
        nav.currentLocator.collectLatest { locator ->
            delay(900)
            val visibleText = runCatching {
                decodeJavascriptString(nav.evaluateJavascript(VISIBLE_TEXT_SCRIPT))
            }.getOrDefault("").trim()
            if (visibleText.isNotBlank()) {
                val segmentId = buildString {
                    append(locator.href.toString())
                    append('#')
                    append(locator.locations.position ?: -1)
                    append(':')
                    append(visibleText.hashCode())
                }
                withContext(Dispatchers.IO) {
                    readingProfile.recordExposure(book.id, segmentId, visibleText)
                }
            }
        }
    }

    // Flush the latest position when leaving even if the 700ms debounce did not fire.
    DisposableEffect(navigator, book.id) {
        val nav = navigator
        onDispose {
            val locator = nav?.currentLocator?.value ?: currentLocator
            if (locator != null) {
                // This is the final lifecycle flush, so use synchronous commits for
                // the tiny payload instead of risking a process exit before apply().
                progressPrefs.edit()
                    .putString("locator_${book.id}", locator.toJSON().toString())
                    .commit()
                locator.locations.totalProgression?.let { progression ->
                    if (book.totalParagraphs > 0) {
                        val index = (progression.coerceIn(0.0, 1.0) * (book.totalParagraphs - 1))
                            .roundToInt()
                        bookRepository.saveProgressImmediate(book.id, index)
                    }
                }
            }
        }
    }

    LaunchedEffect(navigator, highlightRefresh, book.id) {
        val snapshot = withContext(Dispatchers.IO) { annotations.list(book.id) }
        annotationItems = snapshot
        val nav = navigator ?: return@LaunchedEffect
        val decorations = withContext(Dispatchers.Default) { annotations.decorations(snapshot) }
        runCatching { nav.applyDecorations(decorations, DECORATION_GROUP) }
    }

    // A colored single-word mark is also a vocabulary decision. Keep a lightweight
    // snapshot in memory so every later occurrence can be painted in the EPUB, not
    // only the one locator that was originally selected.
    LaunchedEffect(book.id, highlightRefresh) {
        globalLearningColors = withContext(Dispatchers.IO) {
            runCatching { learningStore.learningWordColors() }.getOrDefault(emptyMap())
        }
    }

    // Readium decorations are locator-backed and therefore intentionally local.
    // This second layer mirrors the vocabulary colors onto repeated occurrences in
    // the currently loaded resource. It is reapplied after chapter changes and after
    // add/remove operations. A few retries cover WebView resource-load races.
    LaunchedEffect(
        navigator, currentLocator?.href?.toString(), globalLearningColors,
        fontFamily, fontSize, lineSpacing, pageMargin, readerTheme, scrollMode, twoPage,
    ) {
        val nav = navigator ?: return@LaunchedEffect
        if (currentLocator == null) return@LaunchedEffect
        // Readium can rebuild the resource DOM shortly after the first successful injection
        // (pagination, publisher-style normalization, decorations). Repaint several times across
        // that settling window instead of stopping at the first "ok", otherwise later occurrences
        // can appear unmarked even though the first selected locator is still decorated.
        val repaintDelays = longArrayOf(80L, 180L, 360L, 720L, 1200L)
        repaintDelays.forEach { waitMs ->
            delay(waitMs)
            runCatching { applyVocabularyWordHighlights(nav, globalLearningColors) }
        }
    }

    LaunchedEffect(fontFamily, fontSize, lineSpacing, pageMargin, readerTheme, scrollMode, twoPage, navigator) {
        settings.readerFontFamily = fontFamily
        settings.readerFontSize = fontSize
        settings.readerLineSpacingPercent = lineSpacing
        settings.readerMarginDp = pageMargin
        settings.readerTheme = readerTheme
        settings.readerMode = if (scrollMode) "SCROLL" else "PAGE"
        navigator?.submitPreferences(
            readerPreferences(fontFamily, fontSize, lineSpacing, pageMargin, readerTheme, scrollMode, twoPage)
        )
    }

    LaunchedEffect(autoScrollRunning, autoScrollSpeed, scrollMode, navigator) {
        if (!autoScrollRunning || !scrollMode) return@LaunchedEffect
        val nav = navigator ?: return@LaunchedEffect
        val step = (0.7 + autoScrollSpeed * 0.65).coerceAtMost(8.0)
        while (autoScrollRunning && scrollMode) {
            val raw = runCatching {
                nav.evaluateJavascript(
                    """(function(){
                        const candidates = [document.scrollingElement, document.documentElement, document.body].filter(Boolean);
                        const root = candidates.sort((a,b) =>
                            ((b.scrollHeight || 0) - (b.clientHeight || 0)) -
                            ((a.scrollHeight || 0) - (a.clientHeight || 0))
                        )[0];
                        if (!root) return 'stuck';
                        const before = Number(root.scrollTop || window.scrollY || 0);
                        const viewport = Math.max(1, Number(root.clientHeight || window.innerHeight || 1));
                        const max = Math.max(0, Number(root.scrollHeight || 0) - viewport);
                        if (before >= max - 3) return 'end';
                        const target = Math.min(max, before + $step);
                        if (root.scrollTo) root.scrollTo(0, target); else window.scrollTo(0, target);
                        const after = Number(root.scrollTop || window.scrollY || 0);
                        return after >= max - 3 ? 'end' : (Math.abs(after - before) < 0.1 ? 'stuck' : 'ok');
                    })();""".trimIndent()
                )
            }.getOrNull().orEmpty()
            if (raw.contains("end") || raw.contains("stuck")) {
                if (!navigateAdjacentResource(nav, opened?.publication, currentLocator, forward = true)) {
                    autoScrollRunning = false
                    break
                }
                delay(240)
            } else {
                delay(24)
            }
        }
    }

    fun toggleBookmark() {
        val locator = currentLocator ?: return
        val label = locator.title.orEmpty().ifBlank {
            locator.locations.totalProgression?.let { "${(it * 100).roundToInt()}%" } ?: "当前位置"
        }
        bookmarks = bookRepository.toggleReadiumBookmark(
            bookId = book.id,
            locatorJson = locator.toJSON().toString(),
            label = label,
        )
    }

    fun turnBackward() {
        val nav = navigator ?: return
        autoScrollRunning = false
        if (scrollMode) {
            scope.launch {
                val movedInsideResource = runCatching {
                    scrollByViewport(nav, forward = false)
                }.getOrDefault(false)
                if (!movedInsideResource) {
                    navigateAdjacentResource(nav, opened?.publication, currentLocator, forward = false)
                }
            }
        } else {
            nav.goBackward(animated = settings.readerPageAnimation && !settings.safeMode)
        }
        chromeVisible = false
        quickTap = null
    }

    fun turnForward() {
        val nav = navigator ?: return
        autoScrollRunning = false
        if (scrollMode) {
            scope.launch {
                val movedInsideResource = runCatching {
                    scrollByViewport(nav, forward = true)
                }.getOrDefault(false)
                if (!movedInsideResource) {
                    navigateAdjacentResource(nav, opened?.publication, currentLocator, forward = true)
                }
            }
        } else {
            nav.goForward(animated = settings.readerPageAnimation && !settings.safeMode)
        }
        chromeVisible = false
        quickTap = null
    }

    fun performTapAction(action: String) {
        when (action.uppercase()) {
            "PREVIOUS" -> turnBackward()
            "NEXT" -> turnForward()
            "BOOKMARK" -> toggleBookmark()
            "NONE" -> Unit
            else -> {
                chromeVisible = !chromeVisible
                quickTap = null
            }
        }
    }

    SideEffect { onPageCallbacks(::turnBackward, ::turnForward) }

    DisposableEffect(navigator, scrollMode, settings.readerTapZonesEnabled) {
        val nav = navigator
        if (nav == null) return@DisposableEffect onDispose { }
        val listener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                if (selectionUi != null) {
                    nav.clearSelection()
                    selectionUi = null
                    return true
                }
                if (!settings.readerTapZonesEnabled) {
                    chromeVisible = !chromeVisible
                    quickTap = null
                    return true
                }
                val width = nav.publicationView.width.coerceAtLeast(1)
                val fraction = event.point.x / width.toFloat()
                when {
                    fraction < 0.30f -> performTapAction(settings.readerTapLeftAction)
                    fraction > 0.70f -> performTapAction(settings.readerTapRightAction)
                    else -> performTapAction(settings.readerTapCenterAction)
                }
                return true
            }
        }
        nav.addInputListener(listener)
        onDispose { nav.removeInputListener(listener) }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            Modifier
                .fillMaxSize()
                .background(themeBackground(readerTheme))
        ) {
            val current = opened
            when {
                openError != null -> ReaderError(openError.orEmpty(), onBack)
                current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> {
                    // The Javascript bridge itself must stay stable for the Readium WebView,
                    // but its edge action needs the latest Compose state (not the mode that was
                    // active when the fragment factory was first remembered).
                    val latestEdgeAction = rememberUpdatedState<(Boolean) -> Unit> { forward ->
                        if (scrollMode) {
                            if (forward) turnForward() else turnBackward()
                        }
                    }
                    val tapBridge = remember(current.publication) {
                        LireliaTapBridge(
                            activity = activity,
                            onWord = { word, sentence, wordOffset ->
                                chromeVisible = false
                                selectionUi = null
                                quickTap = QuickTap(word, sentence, wordOffset)
                            },
                            onEdge = { forward -> latestEdgeAction.value(forward) },
                        )
                    }
                    val fragmentFactory = remember(current.publication) {
                        EpubNavigatorFactory(current.publication).createFragmentFactory(
                            initialLocator = current.initialLocator,
                            initialPreferences = readerPreferences(
                                fontFamily, fontSize, lineSpacing, pageMargin, readerTheme, scrollMode, twoPage
                            ),
                            configuration = EpubNavigatorFragment.Configuration {
                                disablePageTurnsWhileScrolling = true
                                selectionActionModeCallback = object : ActionMode.Callback {
                                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                                        menu.clear()
                                        selectionPulse++
                                        quickTap = null
                                        chromeVisible = false
                                        return true
                                    }

                                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                                        menu.clear()
                                        selectionPulse++
                                        return true
                                    }

                                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

                                    override fun onDestroyActionMode(mode: ActionMode) {
                                        selectionUi = null
                                    }
                                }
                                registerJavascriptInterface("LireliaBridge") { tapBridge }
                            },
                        )
                    }
                    // Must be installed before AndroidFragment asks FragmentManager to instantiate
                    // EpubNavigatorFragment; its constructor is intentionally not public.
                    activity.supportFragmentManager.fragmentFactory = fragmentFactory

                    AndroidFragment<EpubNavigatorFragment>(
                        modifier = Modifier.fillMaxSize(),
                    ) { fragment ->
                        navigator = fragment
                    }

                    if (settings.readerBlueLightPercent > 0) {
                        BlueLightOverlay(settings.readerBlueLightPercent)
                    }
                    if (readingMagnifierEnabled && !typographyVisible) {
                        PersistentReadingMagnifier(
                            sourceView = navigator?.view,
                            positionPercent = rulerPositionPercent,
                            heightDp = rulerSizeDp,
                            widthPercent = magnifierWidthPercent,
                            zoomPercent = magnifierZoomPercent,
                        )
                    }
                    if (lineGuideEnabled && !typographyVisible) {
                        DualLineReadingGuide(
                            positionPercent = lineGuidePositionPercent,
                            heightDp = lineGuideHeightDp,
                            topColorCode = lineGuideTopColor,
                            bottomColorCode = lineGuideBottomColor,
                        )
                    }

                    SelectionToolbar(
                        selection = selectionUi,
                        existingAnnotation = selectionUi?.let { selected ->
                            findMatchingAnnotation(annotationItems, selected.locator)
                        },
                        wordMarked = selectionUi?.text
                            ?.let(::singleFrenchWord)
                            ?.let { word -> globalLearningColors.containsKey(normalizeFrenchKey(word)) }
                            ?: false,
                        onTranslate = { selection ->
                            quickTap = QuickTap(selection.text, selection.text)
                            navigator?.clearSelection()
                            selectionUi = null
                        },
                        onNote = { selection ->
                            val locatorJson = selection.locator.toJSON().toString()
                            noteTarget = selection
                            noteDraft = annotationItems
                                .firstOrNull { it.locatorJson == locatorJson }
                                ?.note.orEmpty()
                            navigator?.clearSelection()
                            selectionUi = null
                        },
                        onCopy = { selection ->
                            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("Lirelia", selection.text)
                            )
                            navigator?.clearSelection()
                            selectionUi = null
                        },
                        onHighlight = { selection, color ->
                            navigator?.clearSelection()
                            selectionUi = null
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    annotations.add(book.id, selection.locator, selection.text, color)
                                    if (color != "underline") {
                                        singleFrenchWord(selection.text)?.let { word ->
                                            val hit = dictionary.lookupSafe(word, selection.text).result
                                            learningStore.addVocabulary(hit, selection.text, book.id, displayBookTitle)
                                            learningStore.markLearning(word)
                                            learningStore.setVocabularyMarkerColor(word, color)
                                        }
                                    }
                                }
                                highlightRefresh++
                            }
                        },
                        onAddVocabulary = { selection ->
                            singleFrenchWord(selection.text)?.let { word ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val hit = dictionary.lookupSafe(word, selection.text).result
                                        learningStore.addVocabulary(hit, selection.text, book.id, displayBookTitle)
                                        learningStore.markLearning(word)
                                        if (learningStore.vocabularyMarkerColor(word).isNullOrBlank()) {
                                            learningStore.setVocabularyMarkerColor(word, "yellow")
                                        }
                                    }
                                    highlightRefresh++
                                }
                            }
                        },
                        onRemoveAnnotation = { selection, mark ->
                            navigator?.clearSelection()
                            selectionUi = null
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val word = singleFrenchWord(selection.text)
                                    if (word != null && mark.color != "underline") {
                                        // Colored single-word marks are vocabulary marks in Lirelia.
                                        // Removing one therefore clears the word globally, including
                                        // repeated-occurrence paint, instead of leaving a ghost color.
                                        learningStore.removeWordEverywhere(word)
                                    } else {
                                        annotations.remove(book.id, mark.locatorJson)
                                    }
                                }
                                highlightRefresh++
                            }
                        },
                        onClearWord = { selection ->
                            singleFrenchWord(selection.text)?.let { word ->
                                navigator?.clearSelection()
                                selectionUi = null
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        learningStore.removeWordEverywhere(word)
                                    }
                                    highlightRefresh++
                                }
                            }
                        },
                    )

                    val locatorJson = currentLocator?.toJSON()?.toString().orEmpty()
                    val bookmarked = locatorJson.isNotBlank() &&
                        bookRepository.isReadiumBookmarked(book.id, locatorJson)

                    ReaderChrome(
                        visible = chromeVisible,
                        title = displayBookTitle,
                        bookmarked = bookmarked,
                        scrollMode = scrollMode,
                        autoScrollRunning = autoScrollRunning,
                        onBack = onBack,
                        onBookmark = ::toggleBookmark,
                        onPrevious = ::turnBackward,
                        onNext = ::turnForward,
                        onAutoScroll = {
                            if (scrollMode) autoScrollRunning = !autoScrollRunning
                            else typographyVisible = true
                        },
                        onTypography = { typographyVisible = true },
                        onContents = { tocVisible = true },
                    )

                    TranslationDock(
                        tap = quickTap,
                        lookup = quickLookup,
                        wordMarkColor = quickTap?.word
                            ?.let(::normalizeFrenchKey)
                            ?.let(globalLearningColors::get),
                        wordFallbackTranslation = wordFallbackTranslation,
                        translation = sentenceTranslation,
                        sentenceHint = sentenceHint,
                        translationSource = sentenceSource,
                        loading = quickBusy,
                        onSpeak = { quickTap?.word?.let(tts::speak) },
                        onSave = {
                            val hit = quickLookup?.result
                            val sentence = quickTap?.sentence.orEmpty()
                            val effectiveHit = when {
                                hit == null -> null
                                hit.definitions.isNotEmpty() -> hit
                                wordFallbackTranslation.isNotBlank() -> hit.copy(
                                    lemma = hit.lemma.ifBlank { quickTap?.word.orEmpty() },
                                    definitions = listOf(wordFallbackTranslation),
                                )
                                else -> null
                            }
                            if (effectiveHit != null) {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        learningStore.addVocabulary(effectiveHit, sentence, book.id, displayBookTitle)
                                        learningStore.markLearning(effectiveHit.query)
                                    }
                                    highlightRefresh++
                                }
                            }
                        },
                        onClearWord = {
                            quickTap?.word?.let { word ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        learningStore.removeWordEverywhere(word)
                                    }
                                    highlightRefresh++
                                }
                            }
                        },
                        onDictionary = { fullDictionary = true },
                        onDismiss = { quickTap = null },
                    )
                }
            }
        }

        if (typographyVisible) {
            ModalBottomSheet(onDismissRequest = { typographyVisible = false }) {
                TypographySheet(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    lineSpacing = lineSpacing,
                    pageMargin = pageMargin,
                    theme = readerTheme,
                    scrollMode = scrollMode,
                    autoScrollSpeed = autoScrollSpeed,
                    keepScreenOn = keepScreenOn,
                    twoPageLandscape = twoPageLandscape,
                    volumeKeyTurnsPage = volumeKeyTurnsPage,
                    readingMagnifierEnabled = readingMagnifierEnabled,
                    magnifierPositionPercent = rulerPositionPercent,
                    magnifierHeightDp = rulerSizeDp,
                    magnifierWidthPercent = magnifierWidthPercent,
                    magnifierZoomPercent = magnifierZoomPercent,
                    lineGuideEnabled = lineGuideEnabled,
                    lineGuidePositionPercent = lineGuidePositionPercent,
                    lineGuideHeightDp = lineGuideHeightDp,
                    lineGuideTopColor = lineGuideTopColor,
                    lineGuideBottomColor = lineGuideBottomColor,
                    onFontFamily = { value ->
                        fontFamily = value
                        settings.readerFontFamily = value
                    },
                    onFontSize = { fontSize = it.coerceIn(14, 64) },
                    onLineSpacing = { lineSpacing = it.coerceIn(90, 260) },
                    onMargin = { pageMargin = it.coerceIn(8, 40) },
                    onTheme = { readerTheme = it },
                    onScrollMode = {
                        scrollMode = it
                        autoScrollRunning = false
                    },
                    onAutoScrollSpeed = {
                        autoScrollSpeed = it.coerceIn(1, 10)
                        settings.readerAutoScrollSpeed = autoScrollSpeed
                    },
                    onKeepScreenOn = {
                        keepScreenOn = it
                        settings.readerKeepScreenOn = it
                    },
                    onTwoPageLandscape = {
                        twoPageLandscape = it
                        settings.readerTwoPageLandscape = it
                    },
                    onVolumeKeyTurnsPage = {
                        volumeKeyTurnsPage = it
                        settings.readerVolumeKeyTurnsPage = it
                    },
                    onReadingMagnifierEnabled = { enabled ->
                        readingMagnifierEnabled = enabled
                        settings.readerReadingRuler = enabled
                    },
                    onMagnifierPositionPercent = { value ->
                        rulerPositionPercent = value.coerceIn(10, 90)
                        settings.readerRulerPositionPercent = rulerPositionPercent
                    },
                    onMagnifierHeightDp = { value ->
                        rulerSizeDp = value.coerceIn(56, 160)
                        settings.readerRulerSizeDp = rulerSizeDp
                    },
                    onMagnifierWidthPercent = { value ->
                        magnifierWidthPercent = value.coerceIn(65, 100)
                        settings.readerMagnifierWidthPercent = magnifierWidthPercent
                    },
                    onMagnifierZoomPercent = { value ->
                        magnifierZoomPercent = value.coerceIn(110, 220)
                        settings.readerMagnifierZoomPercent = magnifierZoomPercent
                    },
                    onLineGuideEnabled = { enabled ->
                        lineGuideEnabled = enabled
                        settings.readerLineGuideEnabled = enabled
                    },
                    onLineGuidePositionPercent = { value ->
                        lineGuidePositionPercent = value.coerceIn(10, 90)
                        settings.readerLineGuidePositionPercent = lineGuidePositionPercent
                    },
                    onLineGuideHeightDp = { value ->
                        lineGuideHeightDp = value.coerceIn(24, 120)
                        settings.readerLineGuideHeightDp = lineGuideHeightDp
                    },
                    onLineGuideTopColor = { value ->
                        lineGuideTopColor = value
                        settings.readerLineGuideTopColor = value
                    },
                    onLineGuideBottomColor = { value ->
                        lineGuideBottomColor = value
                        settings.readerLineGuideBottomColor = value
                    },
                )
            }
        }

        if (tocVisible) {
            ModalBottomSheet(onDismissRequest = { tocVisible = false }) {
                ContentsSheet(
                    links = opened?.publication?.tableOfContents.orEmpty(),
                    bookmarks = bookmarks,
                    annotations = annotationItems,
                    onGo = { link ->
                        navigator?.go(link, animated = false)
                        tocVisible = false
                        chromeVisible = false
                    },
                    onGoBookmark = { raw ->
                        val locator = runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull()
                        if (locator != null) navigator?.go(locator, animated = false)
                        tocVisible = false
                        chromeVisible = false
                    },
                    onDeleteBookmark = { raw ->
                        bookmarks = bookRepository.removeReadiumBookmark(book.id, raw)
                    },
                    onGoAnnotation = { raw ->
                        val locator = runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull()
                        if (locator != null) navigator?.go(locator, animated = false)
                        tocVisible = false
                        chromeVisible = false
                    },
                    onDeleteAnnotation = { raw ->
                        scope.launch {
                            withContext(Dispatchers.IO) { annotations.remove(book.id, raw) }
                            highlightRefresh++
                        }
                    },
                )
            }
        }

        if (fullDictionary) {
            ModalBottomSheet(onDismissRequest = { fullDictionary = false }) {
                FullDictionarySheet(
                    tap = quickTap,
                    lookup = quickLookup,
                    wordFallbackTranslation = wordFallbackTranslation,
                    translation = sentenceTranslation,
                    sentenceHint = sentenceHint,
                    source = sentenceSource,
                    dictionary = dictionary,
                    allowNetwork = !settings.safeMode,
                    onSpeak = { quickTap?.word?.let(tts::speak) },
                )
            }
        }

        noteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { noteTarget = null },
                title = { Text("阅读笔记") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(target.text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            label = { Text("写下理解、疑问或提醒") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val locatorJson = target.locator.toJSON().toString()
                            val existingColor = annotationItems
                                .firstOrNull { it.locatorJson == locatorJson }
                                ?.color ?: "yellow"
                            val note = noteDraft.trim()
                            noteTarget = null
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    annotations.add(
                                        bookId = book.id,
                                        locator = target.locator,
                                        text = target.text,
                                        color = existingColor,
                                        note = note,
                                    )
                                }
                                highlightRefresh++
                            }
                        }
                    ) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { noteTarget = null }) { Text("取消") } },
            )
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun scrollByViewport(
    nav: EpubNavigatorFragment,
    forward: Boolean,
): Boolean {
    val direction = if (forward) 1 else -1
    val raw = nav.evaluateJavascript(
        """(function(){
            const candidates = [document.scrollingElement, document.documentElement, document.body].filter(Boolean);
            const root = candidates.sort((a,b) =>
                ((b.scrollHeight || 0) - (b.clientHeight || 0)) -
                ((a.scrollHeight || 0) - (a.clientHeight || 0))
            )[0];
            if (!root) return 'edge';
            const before = Number(root.scrollTop || window.scrollY || 0);
            const viewport = Math.max(1, Number(root.clientHeight || window.innerHeight || 1));
            const max = Math.max(0, Number(root.scrollHeight || 0) - viewport);
            const atEdge = ${if (forward) "before >= max - 4" else "before <= 4"};
            if (atEdge) return 'edge';
            const target = Math.max(0, Math.min(max, before + ($direction * viewport * 0.86)));
            if (Math.abs(target - before) < 2) return 'edge';
            if (root.scrollTo) root.scrollTo({top: target, left: 0, behavior: 'auto'});
            else window.scrollTo(0, target);
            return 'moved';
        })();""".trimIndent()
    )
    return decodeJavascriptString(raw) == "moved"
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun navigateAdjacentResource(
    nav: EpubNavigatorFragment,
    publication: Publication?,
    locator: Locator?,
    forward: Boolean,
): Boolean {
    val order = publication?.readingOrder.orEmpty()
    val currentKey = normalizeHref(locator?.href?.toString().orEmpty())
    val currentIndex = order.indexOfFirst { link ->
        val linkKey = normalizeHref(link.href.toString())
        linkKey == currentKey ||
            (linkKey.isNotBlank() && currentKey.endsWith(linkKey)) ||
            (currentKey.isNotBlank() && linkKey.endsWith(currentKey))
    }
    val targetIndex = if (forward) currentIndex + 1 else currentIndex - 1
    val target = order.getOrNull(targetIndex)
    val moved = if (target != null) {
        nav.go(target, animated = false)
        true
    } else {
        if (forward) nav.goForward(animated = false) else nav.goBackward(animated = false)
    }
    if (!moved) return false

    // A resource change is asynchronous. Put the new chapter at the intuitive edge
    // once its document exists: top when moving forward, bottom when moving back.
    repeat(6) { attempt ->
        delay(70L + attempt * 35L)
        val raw = runCatching {
            nav.evaluateJavascript(
                """(function(){
                    const candidates = [document.scrollingElement, document.documentElement, document.body].filter(Boolean);
                    const root = candidates.sort((a,b) =>
                        ((b.scrollHeight || 0) - (b.clientHeight || 0)) -
                        ((a.scrollHeight || 0) - (a.clientHeight || 0))
                    )[0];
                    if (!root) return 'wait';
                    const viewport = Math.max(1, Number(root.clientHeight || window.innerHeight || 1));
                    const max = Math.max(0, Number(root.scrollHeight || 0) - viewport);
                    const target = ${if (forward) "0" else "max"};
                    if (root.scrollTo) root.scrollTo({top: target, left: 0, behavior: 'auto'});
                    else window.scrollTo(0, target);
                    return 'ok';
                })();""".trimIndent()
            )
        }.getOrNull()
        if (decodeJavascriptString(raw) == "ok") return true
    }
    return true
}

private fun normalizeHref(value: String): String = value
    .substringBefore('#')
    .substringBefore('?')
    .trim()
    .trimStart('/')

@OptIn(ExperimentalReadiumApi::class)
private suspend fun approximateLegacyLocator(
    publication: Publication,
    book: BookItem,
    repository: BookRepository,
): Locator? {
    if (book.totalParagraphs <= 1) return null
    val paragraph = repository.loadProgress(book.id).coerceIn(0, book.totalParagraphs - 1)
    if (paragraph <= 0) return null
    val target = paragraph.toDouble() / (book.totalParagraphs - 1).toDouble()
    val positions = publication.positions()
    return positions.minByOrNull {
        abs((it.locations.totalProgression ?: 0.0) - target)
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun readerPreferences(
    fontFamilyCode: String,
    fontSize: Int,
    lineSpacing: Int,
    marginDp: Int,
    themeName: String,
    scroll: Boolean,
    twoPage: Boolean,
): EpubPreferences {
    val preservePublisherTypography = fontFamilyCode.equals("DEFAULT", ignoreCase = true) &&
        fontSize == 20 && lineSpacing == 142 && marginDp == 16
    return EpubPreferences(
        fontFamily = readiumFontFamily(fontFamilyCode),
        fontSize = (fontSize / 20.0).coerceIn(0.7, 3.2),
        lineHeight = (lineSpacing / 100.0).coerceIn(0.90, 2.60),
        pageMargins = (marginDp / 20.0).coerceIn(0.4, 2.0),
        publisherStyles = preservePublisherTypography,
        textNormalization = !preservePublisherTypography,
        scroll = scroll,
        columnCount = if (twoPage) ColumnCount.TWO else ColumnCount.ONE,
        theme = when (themeName.uppercase()) {
            "DARK", "BLACK", "INK" -> Theme.DARK
            "SEPIA", "PARCHMENT", "PAPER" -> Theme.SEPIA
            else -> Theme.LIGHT
        },
    )
}

private fun themeBackground(theme: String): Color = when (theme.uppercase()) {
    "DARK" -> Color(0xFF1D1D1D)
    "BLACK", "INK" -> Color.Black
    "SEPIA", "PARCHMENT", "PAPER" -> Color(0xFFF1E4CC)
    else -> ReaderPaper
}

private fun readerColorScheme(theme: String) = if (theme.uppercase() in setOf("DARK", "BLACK", "INK")) {
    darkColorScheme(
        primary = Color(0xFFA9CBE5),
        onPrimary = Color(0xFF0D2639),
        surface = Color(0xFF211F1D),
        surfaceContainer = Color(0xFF2A2825),
        surfaceContainerHigh = Color(0xFF32302C),
        onSurface = Color(0xFFE9E4DB),
        onSurfaceVariant = Color(0xFFC9C0B5),
        secondaryContainer = Color(0xFF314139),
        onSecondaryContainer = Color(0xFFE1E9DF),
    )
} else {
    lightColorScheme(
        primary = ReaderAccent,
        onPrimary = Color.White,
        surface = Color(0xFFFFFCF7),
        surfaceContainer = Color(0xFFF6F1E8),
        surfaceContainerHigh = Color(0xFFEEE8DE),
        onSurface = ReaderInk,
        onSurfaceVariant = ReaderMuted,
        secondaryContainer = Color(0xFFEFF3F5),
        onSecondaryContainer = Color(0xFF31434E),
    )
}

@Composable
private fun ReaderError(message: String, onBack: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("这本书暂时打不开", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onBack) { Text("返回书库") }
        }
    }
}

@Composable
private fun ReaderChrome(
    visible: Boolean,
    title: String,
    bookmarked: Boolean,
    scrollMode: Boolean,
    autoScrollRunning: Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAutoScroll: () -> Unit,
    onTypography: () -> Unit,
    onContents: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it / 2 } + fadeIn(),
            exit = slideOutVertically { -it / 2 } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text("‹", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    TextButton(onClick = onBookmark) {
                        Text(
                            if (bookmarked) "★" else "☆",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChromeAction("‹", "上一页", onPrevious)
                    ChromeAction("≡", "目录", onContents)
                    ChromeAction("Aa", "字体", onTypography)
                    if (scrollMode) {
                        ChromeAction(if (autoScrollRunning) "Ⅱ" else "▶", if (autoScrollRunning) "停止" else "自动", onAutoScroll)
                    }
                    ChromeAction("›", "下一页", onNext)
                }
            }
        }
    }
}

@Composable
private fun ChromeAction(icon: String, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 19.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionToolbar(
    selection: SelectionUi?,
    existingAnnotation: ReadiumHighlight?,
    wordMarked: Boolean,
    onTranslate: (SelectionUi) -> Unit,
    onNote: (SelectionUi) -> Unit,
    onCopy: (SelectionUi) -> Unit,
    onHighlight: (SelectionUi, String) -> Unit,
    onAddVocabulary: (SelectionUi) -> Unit,
    onRemoveAnnotation: (SelectionUi, ReadiumHighlight) -> Unit,
    onClearWord: (SelectionUi) -> Unit,
) {
    val active = selection ?: return
    val appear = remember(active.locator.toJSON().toString()) { Animatable(0.86f) }
    LaunchedEffect(active.locator.toJSON().toString()) {
        appear.animateTo(1f, spring(dampingRatio = 0.74f, stiffness = 430f))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val safe = WindowInsets.safeDrawing
        val safeLeft = safe.getLeft(density, layoutDirection).toFloat()
        val safeRight = safe.getRight(density, layoutDirection).toFloat()
        val safeTop = safe.getTop(density).toFloat()
        val safeBottom = safe.getBottom(density).toFloat()
        val rect = active.rect
        val availableDp = (maxWidth - 16.dp).coerceAtLeast(220.dp)
        val toolbarWidthDp = if (availableDp < 330.dp) availableDp else 330.dp
        val toolbarWidth = with(density) { toolbarWidthDp.toPx() }
        val toolbarHeight = with(density) { 108.dp.toPx() }
        val gap = with(density) { 8.dp.toPx() }
        val maxX = with(density) { maxWidth.toPx() }
        val maxY = with(density) { maxHeight.toPx() }
        val minX = safeLeft + gap
        val maxAllowedX = (maxX - safeRight - toolbarWidth - gap).coerceAtLeast(minX)
        val x = if (rect != null) {
            (rect.centerX() - toolbarWidth / 2f).coerceIn(minX, maxAllowedX)
        } else {
            ((maxX - toolbarWidth) / 2f).coerceIn(minX, maxAllowedX)
        }
        val minY = safeTop + gap
        val maxAllowedY = (maxY - safeBottom - toolbarHeight - gap).coerceAtLeast(minY)
        val y = if (rect != null) {
            val above = rect.top - toolbarHeight - gap
            if (above > minY) above else (rect.bottom + gap).coerceAtMost(maxAllowedY)
        } else {
            (maxY * 0.28f).coerceIn(minY, maxAllowedY)
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 9.dp,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .width(toolbarWidthDp)
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                    alpha = ((appear.value - 0.86f) / 0.14f).coerceIn(0f, 1f)
                },
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    listOf(
                        "yellow" to "#F0B429",
                        "green" to "#43A047",
                        "pink" to "#E75480",
                        "blue" to "#3B82D0",
                        "gray" to "#8A8A8A",
                    ).forEach { (code, hex) ->
                        Box(
                            Modifier
                                .size(31.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { onHighlight(active, code) }
                        )
                    }
                    TextButton(onClick = { onHighlight(active, "underline") }) {
                        Text("U̲", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (existingAnnotation != null) {
                        TextButton(onClick = { onRemoveAnnotation(active, existingAnnotation) }) {
                            Text("取消标注", fontSize = 11.sp)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    TextButton(onClick = { onTranslate(active) }) { Text("翻译", fontSize = 11.sp) }
                    TextButton(onClick = { onNote(active) }) { Text("笔记", fontSize = 11.sp) }
                    TextButton(onClick = { onCopy(active) }) { Text("复制", fontSize = 11.sp) }
                    if (singleFrenchWord(active.text) != null) {
                        TextButton(onClick = { onAddVocabulary(active) }) {
                            Text(if (wordMarked) "✓ 生词" else "＋生词", fontSize = 11.sp)
                        }
                        if (wordMarked) {
                            TextButton(onClick = { onClearWord(active) }) {
                                Text("清除该词", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun prettyDictionaryPos(raw: String): String {
    val mapped = raw.split("·", ",", ";")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() && it != "unknown" }
        .mapNotNull { token ->
            when (token) {
                "noun", "nom" -> "名词"
                "verb", "verbe" -> "动词"
                "adj", "adjective", "adjectif" -> "形容词"
                "adv", "adverb", "adverbe" -> "副词"
                "pron", "pronoun", "pronom" -> "代词"
                "prep", "preposition", "préposition" -> "介词"
                "conj", "conjunction", "conjonction" -> "连词"
                "interj", "interjection" -> "感叹词"
                "det", "determiner", "déterminant" -> "限定词"
                "name", "proper noun" -> "专名/称谓"
                else -> token.takeIf { it.length <= 24 }
            }
        }
        .distinct()
    return mapped.joinToString(" · ")
}

private fun dictionaryMeta(tap: QuickTap?, lookup: DictionaryLookup?): String {
    val result = lookup?.result ?: return ""
    val word = tap?.word.orEmpty().trim()
    val parts = mutableListOf<String>()
    result.lemma.trim().takeIf {
        it.isNotBlank() && word.isNotBlank() && !it.equals(word, ignoreCase = true)
    }?.let { parts += "原形 $it" }
    result.morphology.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { morphology ->
        parts += when (morphology.lowercase(Locale.ROOT)) {
            "plural" -> "复数"
            "singular" -> "单数"
            else -> morphology
        }
    }
    prettyDictionaryPos(result.pos).takeIf { it.isNotBlank() }?.let { pos ->
        val gender = FrenchNounGender.label(result, tap?.sentence.orEmpty(), word)
        parts += if (pos.split(" · ").contains("名词")) {
            if (gender != null) "名词（$gender）" else "名词（性别未收录）"
        } else pos
    }
    result.ipa.trim().takeIf { it.isNotBlank() }?.let { parts += it }
    return parts.joinToString(" · ")
}

@Composable
private fun TranslationDock(
    tap: QuickTap?,
    lookup: DictionaryLookup?,
    wordMarkColor: String?,
    wordFallbackTranslation: String,
    translation: String,
    sentenceHint: String,
    translationSource: String,
    loading: Boolean,
    onSpeak: () -> Unit,
    onSave: () -> Unit,
    onClearWord: () -> Unit,
    onDictionary: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = tap != null,
        enter = slideInVertically(animationSpec = tween(180)) { it / 3 } + fadeIn(animationSpec = tween(120)),
        exit = slideOutVertically(animationSpec = tween(150)) { it / 3 } + fadeOut(animationSpec = tween(100)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 14.dp,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .animateContentSize(animationSpec = tween(180))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tap?.word.orEmpty(),
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Serif,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = onSpeak) { Text("🔊", fontSize = 16.sp) }
                        TextButton(onClick = onDismiss) { Text("×", fontSize = 20.sp) }
                    }

                    val definitions = lookup?.result?.definitions.orEmpty()
                    val meta = dictionaryMeta(tap, lookup)
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    when {
                        definitions.isNotEmpty() -> {
                            Text(
                                definitions.take(2).joinToString("；"),
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (definitions.size > 2) {
                                Text(
                                    "另有 ${definitions.size - 2} 个义项 · 完整词典中查看",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        wordFallbackTranslation.isNotBlank() -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                                    Text("联网补充词义", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(wordFallbackTranslation, fontSize = 15.sp, lineHeight = 21.sp)
                                }
                            }
                        }
                        lookup != null -> {
                            Text(
                                "本地词典暂未收录这个词。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val sentence = tap?.sentence.orEmpty().trim()
                    val hasSentenceContext = sentence.isNotBlank() &&
                        !sentence.equals(tap?.word.orEmpty().trim(), ignoreCase = true)
                    if (hasSentenceContext) {
                        HorizontalDivider(Modifier.padding(top = 2.dp))
                        Text("本句", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            sentence,
                            fontFamily = FontFamily.Serif,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )

                        when {
                            translation.isNotBlank() -> {
                                Text(
                                    translation,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (translationSource.isNotBlank()) {
                                    Text(
                                        when (translationSource) {
                                            "翻译缓存" -> "整句翻译 · 缓存"
                                            "网络翻译" -> "整句翻译"
                                            "网络翻译备用" -> "整句翻译 · 备用线路"
                                            else -> translationSource
                                        },
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Text("正在翻译本句…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            sentenceHint.isNotBlank() -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("离线逐词辅助 · 不是自然整句翻译", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(sentenceHint, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onSave,
                            enabled = definitions.isNotEmpty() || wordFallbackTranslation.isNotBlank(),
                        ) { Text(if (wordMarkColor != null) "✓ 生词" else "＋生词") }
                        if (wordMarkColor != null) {
                            TextButton(onClick = onClearWord) { Text("清除") }
                        }
                        TextButton(onClick = onDictionary) { Text("完整词典 ›") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullDictionarySheet(
    tap: QuickTap?,
    lookup: DictionaryLookup?,
    wordFallbackTranslation: String,
    translation: String,
    sentenceHint: String,
    source: String,
    dictionary: DictionaryRepository,
    allowNetwork: Boolean,
    onSpeak: () -> Unit,
) {
    val result = lookup?.result
    val definitions = result?.definitions.orEmpty()
    val meta = dictionaryMeta(tap, lookup)
    val context = LocalContext.current
    var phraseMeanings by remember(result?.phrases) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(result?.phrases, allowNetwork) {
        val phrases = result?.phrases.orEmpty().take(12)
        val offline = withContext(Dispatchers.IO) {
            phrases.associateWith { phrase ->
                runCatching { dictionary.phraseMeaningOffline(phrase) }.getOrDefault("").trim()
            }
        }
        phraseMeanings = offline.mapValues { (_, value) -> value.ifBlank { "中文释义暂缺" } }

        if (allowNetwork) {
            phrases.filter { offline[it].isNullOrBlank() }.forEach { phrase ->
                val translated = withContext(Dispatchers.IO) {
                    runCatching {
                        SentenceTranslationManager.translate(context, phrase, allowNetwork = true).text.trim()
                    }.getOrDefault("")
                }
                if (translated.isNotBlank()) phraseMeanings = phraseMeanings + (phrase to translated)
            }
        }
    }
    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tap?.word.orEmpty(),
                        fontFamily = FontFamily.Serif,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (meta.isNotBlank()) {
                        Text(meta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onSpeak) { Text("🔊 发音") }
            }
        }

        if (definitions.isNotEmpty() || wordFallbackTranslation.isNotBlank()) {
            item { HorizontalDivider(); Text("词义", fontWeight = FontWeight.SemiBold) }
            definitions.forEachIndexed { index, definition ->
                item {
                    Row {
                        Text(
                            "${index + 1}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(26.dp),
                        )
                        Text(definition, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
            if (definitions.isEmpty() && wordFallbackTranslation.isNotBlank()) {
                item {
                    Column {
                        Text(wordFallbackTranslation, fontSize = 14.sp, lineHeight = 21.sp)
                        Text("联网补充词义", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                Text("本地词典暂未收录这个词。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        result?.morphology?.takeIf { it.isNotEmpty() }?.let { morphology ->
            item { HorizontalDivider(); Text("词形", fontWeight = FontWeight.SemiBold) }
            items(morphology.distinct().take(6)) { detail ->
                Text("• $detail", fontSize = 13.sp, lineHeight = 19.sp)
            }
        }

        result?.alternatives?.takeIf { it.isNotEmpty() }?.let { alternatives ->
            item { HorizontalDivider(); Text("其他可能", fontWeight = FontWeight.SemiBold) }
            items(alternatives) { alternative ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        buildString {
                            append(alternative.lemma)
                            prettyDictionaryPos(alternative.pos).takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    alternative.morphology.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    alternative.preview.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        result?.phrases?.takeIf { it.isNotEmpty() }?.let { phrases ->
            item { HorizontalDivider(); Text("固定表达", fontWeight = FontWeight.SemiBold) }
            items(phrases.take(12)) { phrase ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(phrase, fontFamily = FontFamily.Serif, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        phraseMeanings[phrase] ?: "正在补充中文…",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        result?.related?.takeIf { it.isNotEmpty() }?.let { related ->
            item { HorizontalDivider(); Text("相关词", fontWeight = FontWeight.SemiBold) }
            items(related) { relatedWord -> Text("• $relatedWord", fontSize = 13.sp) }
        }
        result?.examples?.takeIf { it.isNotEmpty() }?.let { examples ->
            item { HorizontalDivider(); Text("例句", fontWeight = FontWeight.SemiBold) }
            items(examples) { example ->
                val display = DictionaryDisplayFormatter.example(example)
                if (display.french.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            display.french,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                        if (display.chinese.isNotBlank()) {
                            Text(
                                display.chinese,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        val sentence = tap?.sentence.orEmpty().trim()
        if (sentence.isNotBlank() && !sentence.equals(tap?.word.orEmpty().trim(), ignoreCase = true)) {
            item { HorizontalDivider(); Text("本句", fontWeight = FontWeight.SemiBold) }
            item {
                Text(sentence, fontFamily = FontFamily.Serif, fontSize = 13.sp, lineHeight = 20.sp)
            }
            when {
                translation.isNotBlank() -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(translation, fontSize = 15.sp, lineHeight = 22.sp)
                            if (source.isNotBlank()) {
                                Text(source, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                sentenceHint.isNotBlank() -> item {
                    Column {
                        Text(sentenceHint, fontSize = 13.sp, lineHeight = 20.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        Text("离线逐词辅助 · 非自然整句翻译", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            lookup?.source?.let {
                Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TypographySheet(
    fontFamily: String,
    fontSize: Int,
    lineSpacing: Int,
    pageMargin: Int,
    theme: String,
    scrollMode: Boolean,
    autoScrollSpeed: Int,
    keepScreenOn: Boolean,
    twoPageLandscape: Boolean,
    volumeKeyTurnsPage: Boolean,
    readingMagnifierEnabled: Boolean,
    magnifierPositionPercent: Int,
    magnifierHeightDp: Int,
    magnifierWidthPercent: Int,
    magnifierZoomPercent: Int,
    lineGuideEnabled: Boolean,
    lineGuidePositionPercent: Int,
    lineGuideHeightDp: Int,
    lineGuideTopColor: String,
    lineGuideBottomColor: String,
    onFontFamily: (String) -> Unit,
    onFontSize: (Int) -> Unit,
    onLineSpacing: (Int) -> Unit,
    onMargin: (Int) -> Unit,
    onTheme: (String) -> Unit,
    onScrollMode: (Boolean) -> Unit,
    onAutoScrollSpeed: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onTwoPageLandscape: (Boolean) -> Unit,
    onVolumeKeyTurnsPage: (Boolean) -> Unit,
    onReadingMagnifierEnabled: (Boolean) -> Unit,
    onMagnifierPositionPercent: (Int) -> Unit,
    onMagnifierHeightDp: (Int) -> Unit,
    onMagnifierWidthPercent: (Int) -> Unit,
    onMagnifierZoomPercent: (Int) -> Unit,
    onLineGuideEnabled: (Boolean) -> Unit,
    onLineGuidePositionPercent: (Int) -> Unit,
    onLineGuideHeightDp: (Int) -> Unit,
    onLineGuideTopColor: (String) -> Unit,
    onLineGuideBottomColor: (String) -> Unit,
) {
    var fontPickerVisible by remember { mutableStateOf(false) }

    if (fontPickerVisible) {
        AlertDialog(
            onDismissRequest = { fontPickerVisible = false },
            title = { Text("选择字体", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    READER_FONT_CHOICES.forEach { option ->
                        val selected = option.code == fontFamily.uppercase()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onFontFamily(option.code)
                                    fontPickerVisible = false
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (selected) "✓" else "",
                                modifier = Modifier.width(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(option.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                                Text(option.note, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { fontPickerVisible = false }) { Text("关闭") } },
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("阅读排版", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = { fontPickerVisible = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text("字体", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text(readerFontChoice(fontFamily).label, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Stepper("字号", fontSize.toString(), { onFontSize(fontSize - 1) }, { onFontSize(fontSize + 1) })
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { onFontSize(it.roundToInt()) },
            valueRange = 14f..64f,
        )
        Stepper("行距", "$lineSpacing%", { onLineSpacing(lineSpacing - 5) }, { onLineSpacing(lineSpacing + 5) })
        Slider(
            value = lineSpacing.toFloat(),
            onValueChange = { onLineSpacing(it.roundToInt()) },
            valueRange = 90f..260f,
        )
        Text("选择非“原书字体”或修改字号/行距后，会关闭出版社排版覆盖，确保调整真正生效。", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Stepper("页边距", "${pageMargin}dp", { onMargin(pageMargin - 2) }, { onMargin(pageMargin + 2) })
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("PAPER" to "纸张", "SEPIA" to "暖色", "LIGHT" to "白色", "DARK" to "深色").forEach { (value, label) ->
                if (theme == value) Button(onClick = { onTheme(value) }) { Text(label) }
                else OutlinedButton(onClick = { onTheme(value) }) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!scrollMode) Button(onClick = { onScrollMode(false) }) { Text("分页") }
            else OutlinedButton(onClick = { onScrollMode(false) }) { Text("分页") }
            if (scrollMode) Button(onClick = { onScrollMode(true) }) { Text("滚动") }
            else OutlinedButton(onClick = { onScrollMode(true) }) { Text("滚动") }
        }
        if (scrollMode) {
            Stepper(
                "自动滚动速度",
                "$autoScrollSpeed / 10",
                { onAutoScrollSpeed(autoScrollSpeed - 1) },
                { onAutoScrollSpeed(autoScrollSpeed + 1) },
            )
        }
        ToggleRow("阅读时屏幕常亮", keepScreenOn, onKeepScreenOn)
        ToggleRow("横屏双页", twoPageLandscape, onTwoPageLandscape)
        ToggleRow("音量键翻页", volumeKeyTurnsPage, onVolumeKeyTurnsPage)

        HorizontalDivider()
        ToggleRow("阅读放大镜", readingMagnifierEnabled, onReadingMagnifierEnabled)
        Text(
            "开启后只显示放大的阅读镜片，不再覆盖黑色阅读尺；位置、大小和倍率都在这里调整。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
        )
        if (readingMagnifierEnabled) {
            Text("垂直位置  $magnifierPositionPercent%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = magnifierPositionPercent.toFloat(),
                onValueChange = { onMagnifierPositionPercent(it.roundToInt()) },
                valueRange = 10f..90f,
            )
            Text("镜片高度  ${magnifierHeightDp}dp", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = magnifierHeightDp.toFloat(),
                onValueChange = { onMagnifierHeightDp(it.roundToInt()) },
                valueRange = 56f..160f,
            )
            Text("镜片宽度  $magnifierWidthPercent%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = magnifierWidthPercent.toFloat(),
                onValueChange = { onMagnifierWidthPercent(it.roundToInt()) },
                valueRange = 65f..100f,
            )
            Text("放大比例  $magnifierZoomPercent%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = magnifierZoomPercent.toFloat(),
                onValueChange = { onMagnifierZoomPercent(it.roundToInt()) },
                valueRange = 110f..220f,
                steps = 10,
            )
        }

        HorizontalDivider()
        ToggleRow("双线行跟随", lineGuideEnabled, onLineGuideEnabled)
        Text(
            "用两条不同颜色的横线夹住当前阅读区域，中间用浅色高亮区分；不会挡住点词。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
        )
        if (lineGuideEnabled) {
            Text("垂直位置  $lineGuidePositionPercent%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = lineGuidePositionPercent.toFloat(),
                onValueChange = { onLineGuidePositionPercent(it.roundToInt()) },
                valueRange = 10f..90f,
            )
            Text("跟随区高度  ${lineGuideHeightDp}dp", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = lineGuideHeightDp.toFloat(),
                onValueChange = { onLineGuideHeightDp(it.roundToInt()) },
                valueRange = 24f..120f,
            )
            Text("上方线颜色", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            GuideColorPicker(lineGuideTopColor, onLineGuideTopColor)
            Text("下方线颜色", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            GuideColorPicker(lineGuideBottomColor, onLineGuideBottomColor)
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val GUIDE_COLOR_OPTIONS = listOf("BLUE", "ORANGE", "GREEN", "RED", "PURPLE", "GRAY")

private fun guideColor(code: String): Color = when (code.uppercase()) {
    "ORANGE" -> Color(0xFFE67E22)
    "GREEN" -> Color(0xFF2E8B57)
    "RED" -> Color(0xFFD64545)
    "PURPLE" -> Color(0xFF7E57C2)
    "GRAY" -> Color(0xFF616161)
    else -> Color(0xFF2F6FB0)
}

@Composable
private fun GuideColorPicker(selected: String, onSelected: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GUIDE_COLOR_OPTIONS.forEach { code ->
            val active = code == selected.uppercase()
            Box(
                Modifier
                    .size(if (active) 34.dp else 30.dp)
                    .clip(CircleShape)
                    .background(guideColor(code))
                    .clickable { onSelected(code) }
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun Stepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        OutlinedButton(onClick = minus, contentPadding = PaddingValues(horizontal = 13.dp)) { Text("−") }
        Text(value, modifier = Modifier.width(82.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = plus, contentPadding = PaddingValues(horizontal = 13.dp)) { Text("＋") }
    }
}

@Composable
private fun ContentsSheet(
    links: List<Link>,
    bookmarks: List<ReadiumBookmark>,
    annotations: List<ReadiumHighlight>,
    onGo: (Link) -> Unit,
    onGoBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onGoAnnotation: (String) -> Unit,
    onDeleteAnnotation: (String) -> Unit,
) {
    var tab by remember { mutableStateOf("toc") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("书籍导航", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tab == "toc") Button(onClick = { tab = "toc" }) { Text("目录") }
            else OutlinedButton(onClick = { tab = "toc" }) { Text("目录") }
            if (tab == "bookmarks") Button(onClick = { tab = "bookmarks" }) { Text("书签 ${bookmarks.size}") }
            else OutlinedButton(onClick = { tab = "bookmarks" }) { Text("书签 ${bookmarks.size}") }
            if (tab == "annotations") Button(onClick = { tab = "annotations" }) { Text("标注 ${annotations.size}") }
            else OutlinedButton(onClick = { tab = "annotations" }) { Text("标注 ${annotations.size}") }
        }
        Spacer(Modifier.height(8.dp))
        if (tab == "toc") {
            if (links.isEmpty()) {
                Text("这本 EPUB 没有提供目录。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                    items(links) { link ->
                        TextButton(onClick = { onGo(link) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                link.title ?: "未命名章节",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("›")
                        }
                    }
                }
            }
        } else if (tab == "bookmarks") {
            if (bookmarks.isEmpty()) {
                Text("还没有书签。点阅读页顶部的 ☆ 即可保存当前位置。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                    items(bookmarks, key = { it.locatorJson }) { bookmark ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onGoBookmark(bookmark.locatorJson) }, modifier = Modifier.weight(1f)) {
                                Text(
                                    bookmark.label.ifBlank { bookmarkProgressLabel(bookmark.locatorJson) },
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("›")
                            }
                            TextButton(onClick = { onDeleteBookmark(bookmark.locatorJson) }) { Text("删除") }
                        }
                    }
                }
            }
        } else {
            if (annotations.isEmpty()) {
                Text("还没有 EPUB 标注或笔记。长按正文即可添加。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 36.dp)) {
                    items(annotations, key = { it.id }) { mark ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { onGoAnnotation(mark.locatorJson) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        mark.text.ifBlank { "标注" },
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (mark.note.isNotBlank()) {
                                        Text(
                                            mark.note,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text("›")
                            }
                            TextButton(onClick = { onDeleteAnnotation(mark.locatorJson) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueLightOverlay(percent: Int) {
    val alpha = (percent.coerceIn(0, 55) / 55f) * 0.16f
    Box(Modifier.fillMaxSize().background(Color(0xFFFFA33A).copy(alpha = alpha)))
}

@SuppressLint("NewApi")
@Composable
private fun DualLineReadingGuide(
    positionPercent: Int,
    heightDp: Int,
    topColorCode: String,
    bottomColorCode: String,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bandHeightPx = with(density) { heightDp.coerceIn(24, 120).dp.roundToPx() }
        val maxTop = (constraints.maxHeight - bandHeightPx).coerceAtLeast(0)
        val center = (constraints.maxHeight * (positionPercent.coerceIn(10, 90) / 100f)).roundToInt()
        val top = (center - bandHeightPx / 2).coerceIn(0, maxTop)
        val topColor = guideColor(topColorCode)
        val bottomColor = guideColor(bottomColorCode)

        Box(
            Modifier
                .fillMaxWidth()
                .height(heightDp.coerceIn(24, 120).dp)
                .offset { IntOffset(0, top) }
                .background(topColor.copy(alpha = 0.055f))
        ) {
            Box(Modifier.fillMaxSize().background(bottomColor.copy(alpha = 0.025f)))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(topColor.copy(alpha = 0.92f))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(bottomColor.copy(alpha = 0.92f))
            )
        }
    }
}

@Composable
private fun PersistentReadingMagnifier(
    sourceView: View?,
    positionPercent: Int,
    heightDp: Int,
    widthPercent: Int,
    zoomPercent: Int,
) {
    val density = LocalDensity.current
    val lensHeightPx = with(density) { heightDp.coerceIn(56, 160).dp.roundToPx() }
    val cornerRadiusPx = with(density) { 12.dp.toPx() }
    val elevationPx = with(density) { 4.dp.toPx() }
    val horizontalSafetyPx = with(density) { 28.dp.roundToPx() }

    LaunchedEffect(sourceView, positionPercent, lensHeightPx, widthPercent, zoomPercent) {
        val view = sourceView ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@LaunchedEffect

        while (view.width <= 0 || view.height <= 0) delay(40)

        // V5.0: the visible lens gets an extra side safety margin. On high-DPI
        // emulators the previous percentage-only width could clip the first/last
        // glyphs of a line after magnification.
        val requestedWidth = (view.width * (widthPercent.coerceIn(65, 100) / 100f)).roundToInt()
        val lensWidthPx = (requestedWidth + horizontalSafetyPx)
            .coerceAtMost((view.width - with(density) { 6.dp.roundToPx() }).coerceAtLeast(1))
            .coerceAtLeast(1)
        val zoom = zoomPercent.coerceIn(110, 220) / 100f

        val magnifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Magnifier.Builder(view)
                .setSize(lensWidthPx, lensHeightPx)
                .setInitialZoom(zoom)
                .setCornerRadius(cornerRadiusPx)
                .setElevation(elevationPx)
                .setClippingEnabled(true)
                .build()
        } else {
            Magnifier(view)
        }

        try {
            while (true) {
                if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
                    delay(90)
                    continue
                }

                val centerX = view.width / 2f
                val centerY = (view.height * (positionPercent.coerceIn(10, 90) / 100f))
                    .coerceIn(1f, (view.height - 1).coerceAtLeast(1).toFloat())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    magnifier.setZoom(zoom)
                    // Put the glass directly on the reading line. The source is the
                    // Readium view itself, so the popup never re-captures its own pixels.
                    magnifier.show(centerX, centerY, centerX, centerY)
                } else {
                    magnifier.show(centerX, centerY)
                }
                delay(90)
            }
        } finally {
            magnifier.dismiss()
        }
    }
}

private val SINGLE_FRENCH_WORD = Regex(
    "^[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’'][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*$"
)

private fun singleFrenchWord(text: String): String? = text
    .trim()
    .takeIf { it.isNotBlank() && SINGLE_FRENCH_WORD.matches(it) }

private fun normalizeFrenchKey(word: String): String = word
    .trim()
    .replace('’', '\'')
    .lowercase(Locale.FRENCH)

private fun findMatchingAnnotation(
    items: List<ReadiumHighlight>,
    locator: Locator,
): ReadiumHighlight? {
    val targetRaw = locator.toJSON().toString()
    items.firstOrNull { it.locatorJson == targetRaw }?.let { return it }

    val target = runCatching { JSONObject(targetRaw) }.getOrNull() ?: return null
    val targetHref = normalizeHref(target.optString("href"))
    val targetText = target.optJSONObject("text")?.optString("highlight").orEmpty().trim()
    val targetLocations = target.optJSONObject("locations")
    val targetFragment = targetLocations?.optJSONArray("fragments")?.optString(0).orEmpty()
    val targetProgress = targetLocations?.optDouble("totalProgression", Double.NaN) ?: Double.NaN

    return items.firstOrNull { mark ->
        val stored = runCatching { JSONObject(mark.locatorJson) }.getOrNull() ?: return@firstOrNull false
        val sameHref = normalizeHref(stored.optString("href")) == targetHref
        val sameText = stored.optJSONObject("text")?.optString("highlight").orEmpty().trim() == targetText
        if (!sameHref || !sameText) return@firstOrNull false
        val locations = stored.optJSONObject("locations")
        val fragment = locations?.optJSONArray("fragments")?.optString(0).orEmpty()
        if (targetFragment.isNotBlank() && fragment.isNotBlank()) return@firstOrNull targetFragment == fragment
        val progress = locations?.optDouble("totalProgression", Double.NaN) ?: Double.NaN
        targetProgress.isFinite() && progress.isFinite() && abs(targetProgress - progress) < 0.0015
    }
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun applyVocabularyWordHighlights(
    nav: EpubNavigatorFragment,
    colors: Map<String, String>,
): Boolean {
    val payload = JSONObject().apply {
        colors.entries
            .asSequence()
            .map { normalizeFrenchKey(it.key) to it.value.trim().lowercase(Locale.ROOT) }
            .filter { (word, color) -> SINGLE_FRENCH_WORD.matches(word) && color != "underline" }
            .distinctBy { it.first }
            .take(3000)
            .forEach { (word, color) -> put(word, color) }
    }.toString()

    val raw = nav.evaluateJavascript(
        """(function(){
            const vocab = $payload;
            const old = Array.from(document.querySelectorAll('span[data-lirelia-vocab="1"]'));
            for (const span of old) {
              const parent = span.parentNode;
              if (!parent) continue;
              while (span.firstChild) parent.insertBefore(span.firstChild, span);
              parent.removeChild(span);
              parent.normalize();
            }

            if (!Object.keys(vocab).length || !document.body) return 'ok:0';
            const tokenRe = /[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:['’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*/gu;
            const normalize = s => (s || '').replace(/’/g, "'").toLocaleLowerCase('fr');
            const css = code => {
              switch ((code || '').toLowerCase()) {
                case 'green': return 'rgba(67, 160, 71, .20)';
                case 'pink': return 'rgba(231, 84, 128, .18)';
                case 'blue': return 'rgba(59, 130, 208, .18)';
                case 'gray': case 'grey': return 'rgba(90, 90, 90, .15)';
                default: return 'rgba(240, 180, 41, .22)';
              }
            };

            const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
            const nodes = [];
            let node;
            while ((node = walker.nextNode())) {
              const p = node.parentElement;
              if (!p || !node.nodeValue || !node.nodeValue.trim()) continue;
              if (p.closest('script,style,textarea,input,button,select,option,[contenteditable="true"],[data-lirelia-vocab="1"]')) continue;
              nodes.push(node);
            }

            let painted = 0;
            for (const textNode of nodes) {
              const text = textNode.nodeValue || '';
              tokenRe.lastIndex = 0;
              let match, last = 0, changed = false;
              const frag = document.createDocumentFragment();
              while ((match = tokenRe.exec(text)) !== null) {
                const key = normalize(match[0]);
                const code = vocab[key];
                if (!code) continue;
                const start = match.index, end = start + match[0].length;
                if (start > last) frag.appendChild(document.createTextNode(text.slice(last, start)));
                const span = document.createElement('span');
                span.setAttribute('data-lirelia-vocab', '1');
                span.style.setProperty('background-color', css(code), 'important');
                span.style.borderRadius = '.12em';
                span.style.boxDecorationBreak = 'clone';
                span.style.webkitBoxDecorationBreak = 'clone';
                span.textContent = match[0];
                frag.appendChild(span);
                last = end;
                changed = true;
                painted++;
              }
              if (changed) {
                if (last < text.length) frag.appendChild(document.createTextNode(text.slice(last)));
                textNode.parentNode.replaceChild(frag, textNode);
              }
            }
            return 'ok:' + painted;
        })();""".trimIndent()
    )
    return decodeJavascriptString(raw).startsWith("ok:")
}

private fun offlineGloss(dictionary: DictionaryRepository, sentence: String): String {
    val words = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[’'][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?")
        .findAll(sentence)
        .map { it.value }
        .distinctBy { it.lowercase() }
        .take(7)
        .toList()
    return words.mapNotNull { word ->
        dictionary.lookupSafe(word, sentence).result.definitions.firstOrNull()?.let { "$word：$it" }
    }.joinToString("；")
}

private fun decodeJavascriptString(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank() || value == "null") return ""
    return runCatching { JSONArray("[$value]").optString(0) }.getOrDefault("")
}

private fun bookmarkProgressLabel(locatorJson: String): String = runCatching {
    val root = JSONObject(locatorJson)
    val locations = root.optJSONObject("locations") ?: JSONObject()
    val total = locations.optDouble("totalProgression", Double.NaN)
    if (total.isFinite()) "${(total * 100).roundToInt()}%" else "书签"
}.getOrDefault("书签")

/**
 * Installed inside each reflowable EPUB resource. A normal tap on a French word is consumed by
 * Lirelia and becomes a translation request; taps on whitespace still belong to the reader.
 *
 * V5.3 derives the sentence from the exact DOM character offset and chapter context. It no longer
 * searches for the first sentence containing the same word, so repeated pronouns such as il/on
 * in one paragraph do not jump the translation to an earlier sentence.
 */
private val INSTALL_TAP_SCRIPT = """
(function() {
  if (window.__lireliaTapInstalled) return 'already';
  window.__lireliaTapInstalled = true;
  const letter = /[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ'’-]/;
  const wordRe = /^[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:['’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)?(?:-[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*$/;

  function caretAt(x, y) {
    if (document.caretRangeFromPoint) return document.caretRangeFromPoint(x, y);
    if (document.caretPositionFromPoint) {
      const p = document.caretPositionFromPoint(x, y);
      if (!p) return null;
      const r = document.createRange();
      r.setStart(p.offsetNode, p.offset); r.collapse(true); return r;
    }
    return null;
  }

  function sentenceAt(node, localOffset, word) {
    // V5.3: never use an arbitrary DIV as the sentence container. A surprising number of
    // EPUBs use one DIV per visual line; that was the reason a tap on "remporté" returned
    // only "Nicolas a", while a tap a few words later returned another line fragment.
    // Prefer true semantic text blocks. If the publisher did not use them, flatten the chapter
    // body and segment the sentence around the exact DOM character offset.
    const parent = node && node.parentElement ? node.parentElement : null;
    const semantic = parent && parent.closest
      ? parent.closest('p,li,blockquote,dd,dt,figcaption,h1,h2,h3,h4,h5,h6,td,th')
      : null;
    const root = semantic || document.body || parent;
    if (!root) return { text: word, focus: 0 };

    const blockedTags = new Set(['SCRIPT','STYLE','NOSCRIPT','SVG','MATH','TEMPLATE']);
    const blockTags = new Set([
      'P','DIV','LI','BLOCKQUOTE','DD','DT','FIGCAPTION','H1','H2','H3','H4','H5','H6',
      'TD','TH','SECTION','ARTICLE','ASIDE','HEADER','FOOTER'
    ]);

    function shouldSkip(textNode) {
      let el = textNode && textNode.parentElement;
      while (el && el !== root) {
        if (blockedTags.has(el.tagName)) return true;
        el = el.parentElement;
      }
      return false;
    }

    function blockOwner(textNode) {
      let el = textNode && textNode.parentElement;
      while (el && el !== root) {
        if (blockTags.has(el.tagName)) return el;
        el = el.parentElement;
      }
      return root;
    }

    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let full = '';
    let absolute = -1;
    let previousBlock = null;
    let current;
    while ((current = walker.nextNode())) {
      if (shouldSkip(current)) continue;
      const value = current.nodeValue || '';
      if (!value) continue;
      const owner = blockOwner(current);
      // Preserve only a soft space between publisher-created line/block containers. Do not
      // inject a newline here: Intl.Segmenter may treat EPUB visual-line DIV breaks as sentence
      // breaks even when there is no sentence-ending punctuation.
      if (full && previousBlock && owner !== previousBlock && !/\s$/.test(full) && !/^\s/.test(value)) {
        full += ' ';
      }
      if (current === node) {
        absolute = full.length + Math.max(0, Math.min(localOffset, value.length));
      }
      full += value;
      previousBlock = owner;
    }

    if (!full) return { text: word, focus: 0 };
    if (absolute < 0) {
      const lowerFull = full.toLocaleLowerCase('fr');
      const lowerWord = String(word || '').toLocaleLowerCase('fr');
      absolute = Math.max(0, lowerFull.indexOf(lowerWord));
    }

    let start = 0, end = full.length;
    let segmented = false;
    try {
      if (typeof Intl !== 'undefined' && Intl.Segmenter) {
        const segmenter = new Intl.Segmenter('fr', { granularity: 'sentence' });
        for (const part of segmenter.segment(full)) {
          const a = Number(part.index || 0);
          const b = a + String(part.segment || '').length;
          if (absolute >= a && (absolute < b || (b === full.length && absolute === b))) {
            start = a; end = b; segmented = true; break;
          }
        }
      }
    } catch (_) {}

    if (!segmented) {
      const abbreviations = new Set(['m','mme','mmes','mlle','dr','pr','prof','etc','env','av','apr','janv','févr','avr','juill','sept','oct','nov','déc','n','no']);
      function isBoundaryAt(i) {
        const ch = full.charAt(i);
        if (ch === '!' || ch === '?' || ch === '…') return true;
        if (ch !== '.') return false;
        const prev = full.charAt(i - 1), next = full.charAt(i + 1);
        if (/\d/.test(prev) && /\d/.test(next)) return false;
        const before = full.slice(Math.max(0, i - 16), i).match(/([A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)$/u);
        if (before && abbreviations.has(before[1].toLocaleLowerCase('fr'))) return false;
        if (before && before[1].length === 1 && /[A-ZÀ-ÖØ-Þ]/u.test(before[1])) return false;
        return true;
      }
      start = Math.max(0, Math.min(absolute, full.length));
      while (start > 0 && !isBoundaryAt(start - 1)) start--;
      end = Math.max(0, Math.min(absolute, full.length));
      while (end < full.length && !isBoundaryAt(end)) end++;
      if (end < full.length) end++;
      const closingMarks = `»”"')]} `;
      while (end < full.length && closingMarks.includes(full.charAt(end))) end++;
    }

    const raw = full.slice(start, end);
    const rawFocus = Math.max(0, absolute - start);
    const normalizedRaw = raw.replace(/\s+/g, ' ');
    const normalizedBefore = raw.slice(0, rawFocus).replace(/\s+/g, ' ');
    const leading = normalizedRaw.length - normalizedRaw.trimStart().length;
    let sentence = normalizedRaw.trim();
    let focus = Math.max(0, normalizedBefore.length - leading);
    if (!sentence) return { text: word, focus: 0 };

    // Keep complete literary sentences whenever practical. V5.2 used 520 characters, which could
    // itself turn a grammatically valid long sentence into a translation fragment.
    const maxSentence = 1000;
    if (sentence.length > maxSentence) {
      const windowStart = Math.max(0, Math.min(focus - 420, sentence.length - maxSentence));
      let clipStart = windowStart;
      let clipEnd = Math.min(sentence.length, windowStart + maxSentence);
      const boundary = /[,;:!?…]/;
      for (let i = windowStart; i > Math.max(0, windowStart - 100); i--) {
        if (boundary.test(sentence.charAt(i))) { clipStart = i + 1; break; }
      }
      for (let i = clipEnd; i < Math.min(sentence.length, clipEnd + 100); i++) {
        if (boundary.test(sentence.charAt(i))) { clipEnd = i + 1; break; }
      }
      const clippedRaw = sentence.slice(clipStart, clipEnd);
      const clipLeading = clippedRaw.length - clippedRaw.trimStart().length;
      sentence = clippedRaw.trim();
      focus = Math.max(0, focus - clipStart - clipLeading);
    }
    return { text: sentence, focus: Math.min(focus, Math.max(0, sentence.length - 1)) };
  }

  // In Readium scroll mode one spine resource ends before the next chapter is
  // loaded. A second upward swipe at the bottom (or downward swipe at the top)
  // should feel like continuous reading instead of hitting a dead wall.
  function scrollRoot() {
    const candidates = [document.scrollingElement, document.documentElement, document.body].filter(Boolean);
    return candidates.sort((a,b) =>
      ((b.scrollHeight || 0) - (b.clientHeight || 0)) -
      ((a.scrollHeight || 0) - (a.clientHeight || 0))
    )[0] || null;
  }
  let edgeTouch = null;
  document.addEventListener('touchstart', function(ev) {
    if (!ev.touches || ev.touches.length !== 1) { edgeTouch = null; return; }
    const root = scrollRoot();
    if (!root) { edgeTouch = null; return; }
    const top = Number(root.scrollTop || window.scrollY || 0);
    const viewport = Math.max(1, Number(root.clientHeight || window.innerHeight || 1));
    const max = Math.max(0, Number(root.scrollHeight || 0) - viewport);
    edgeTouch = {
      y: ev.touches[0].clientY,
      atTop: top <= 4,
      atBottom: top >= max - 4,
      time: Date.now()
    };
  }, {passive:true});
  document.addEventListener('touchend', function(ev) {
    const start = edgeTouch; edgeTouch = null;
    if (!start || !ev.changedTouches || !ev.changedTouches.length) return;
    const dy = ev.changedTouches[0].clientY - start.y;
    if (Date.now() - start.time > 1200 || Math.abs(dy) < 42) return;
    try {
      if (start.atBottom && dy < 0) LireliaBridge.onScrollEdge(1);
      else if (start.atTop && dy > 0) LireliaBridge.onScrollEdge(-1);
    } catch (_) {}
  }, {passive:true});

  document.addEventListener('click', function(ev) {
    if (ev.defaultPrevented || ev.button !== 0) return;
    // Android WebView can emit a click when a long-press selection is released.
    // Never turn that release into a word translation request.
    const activeSelection = window.getSelection ? window.getSelection() : null;
    if (activeSelection && !activeSelection.isCollapsed) return;
    const target = ev.target;
    if (target && target.closest && target.closest('a,button,input,textarea,select,[contenteditable=true]')) return;
    const r = caretAt(ev.clientX, ev.clientY);
    if (!r || !r.startContainer || r.startContainer.nodeType !== Node.TEXT_NODE) return;
    const node = r.startContainer;
    const text = node.nodeValue || '';
    if (!text) return;
    let o = Math.max(0, Math.min(r.startOffset, text.length));
    if (o === text.length && o > 0) o--;
    if (!letter.test(text.charAt(o)) && o > 0 && letter.test(text.charAt(o - 1))) o--;
    if (!letter.test(text.charAt(o))) return;
    let a = o, b = o + 1;
    while (a > 0 && letter.test(text.charAt(a - 1))) a--;
    while (b < text.length && letter.test(text.charAt(b))) b++;
    let word = text.slice(a, b).replace(/^[’'-]+|[’'-]+$/g, '');
    if (!wordRe.test(word)) return;
    const sentenceInfo = sentenceAt(node, a, word);
    try {
      LireliaBridge.onWord(word, sentenceInfo.text, sentenceInfo.focus, ev.clientX, ev.clientY);
      ev.preventDefault();
      ev.stopImmediatePropagation();
      ev.stopPropagation();
    } catch (_) {}
  }, true);
  return 'installed';
})();
""".trimIndent()

private val VISIBLE_TEXT_SCRIPT = """
(function() {
  const nodes = Array.from(document.querySelectorAll('p,li,blockquote,dd,dt,figcaption,h1,h2,h3,h4,h5,h6'));
  const visible = [];
  for (const el of nodes) {
    const r = el.getBoundingClientRect();
    if (r.bottom >= 0 && r.top <= window.innerHeight) {
      const text = (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();
      if (text) visible.push(text);
    }
    if (visible.join(' ').length > 1800) break;
  }
  return visible.join(' ').slice(0, 2200);
})();
""".trimIndent()
