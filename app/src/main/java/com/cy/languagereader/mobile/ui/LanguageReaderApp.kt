package com.cy.languagereader.mobile.ui

import android.net.Uri
import android.graphics.BitmapFactory
import android.app.Activity
import com.cy.languagereader.mobile.ReaderKeyBridge
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets as ComposeWindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.BookItem
import com.cy.languagereader.mobile.data.BookWordCandidate
import com.cy.languagereader.mobile.data.ChapterItem
import com.cy.languagereader.mobile.data.BookRepository
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.DictionaryCenterStore
import com.cy.languagereader.mobile.data.DataBackupManager
import com.cy.languagereader.mobile.data.DictionaryResult
import com.cy.languagereader.mobile.data.DictionaryLookup
import com.cy.languagereader.mobile.data.DictionaryDisplayFormatter
import com.cy.languagereader.mobile.data.DocumentImporter
import com.cy.languagereader.mobile.data.FrenchConjugator
import com.cy.languagereader.mobile.data.FrenchNounGender
import com.cy.languagereader.mobile.data.FrenchSentenceSegmenter
import com.cy.languagereader.mobile.data.FrenchTrainingData
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.data.LibraryOrganizer
import com.cy.languagereader.mobile.data.ReaderMode
import com.cy.languagereader.mobile.data.ReaderRulerStyle
import com.cy.languagereader.mobile.data.ReadingStatsStore
import com.cy.languagereader.mobile.data.ReadingProfileStore
import com.cy.languagereader.mobile.data.ReadingProfileSnapshot
import com.cy.languagereader.mobile.data.ReaderPage
import com.cy.languagereader.mobile.data.ReaderPaginator
import com.cy.languagereader.mobile.data.SmartReaderPaginator
import com.cy.languagereader.mobile.data.VocabularyItem
import com.cy.languagereader.mobile.data.TextAnnotation
import com.cy.languagereader.mobile.service.SentenceTranslationManager
import com.cy.languagereader.mobile.service.TtsManager
import com.cy.languagereader.mobile.readium.ReadiumReaderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import java.io.File

private val Paper = Color(0xFFF7F3EA)
private val Ink = Color(0xFF2E2A26)
private val Accent = Color(0xFF755A43)
private val AccentSoft = Color(0xFFE9DED1)
private val Highlight = Color(0xFFFFEDB3)



private fun friendlyDictionaryPos(result: DictionaryResult): String {
    val raw = result.pos
        .lowercase(Locale.ROOT)
        .replace("/", " ")
        .replace(",", " ")
        .replace(";", " ")
        .replace("·", " ")

    val mapped = linkedSetOf<String>()

    fun addIf(vararg needles: String, label: String) {
        if (needles.any { raw.contains(it) }) mapped += label
    }

    // Inflected verb forms such as "tombe" -> "tomber" should not be displayed
    // as noun/unknown just because the source dictionary merged POS metadata.
    val resolvedToDifferentLemma =
        result.lemma.isNotBlank() &&
            !result.lemma.equals(result.query, ignoreCase = true)

    if (resolvedToDifferentLemma &&
        (raw.contains("verb") || raw.contains("verbe"))
    ) {
        return "动词"
    }

    addIf("verb", "verbe", label = "动词")
    addIf("noun", "nom", label = "名词")
    addIf("adjective", "adj", "adjectif", label = "形容词")
    addIf("adverb", "adv", "adverbe", label = "副词")
    addIf("pronoun", "pronom", label = "代词")
    addIf("preposition", "préposition", "preposition", label = "介词")
    addIf("conjunction", "conjonction", label = "连词")
    addIf("article", "determiner", "déterminant", "determinant", label = "冠词/限定词")
    addIf("interjection", "interj", label = "感叹词")

    return mapped.joinToString(" · ")
}

private fun annotationComposeColor(name: String?, palette: ReaderPalette): Color {
    if (name.isNullOrBlank()) return Color.Transparent
    if (name.startsWith("#")) {
        return runCatching { Color(android.graphics.Color.parseColor(name)).copy(alpha = 0.78f) }
            .getOrElse { palette.highlight.copy(alpha = 0.78f) }
    }
    return when (name) {
        "yellow" -> Color(0xFFF0B429).copy(alpha = 0.90f)
        "green" -> Color(0xFF43A047).copy(alpha = 0.86f)
        "blue" -> Color(0xFF3B82D0).copy(alpha = 0.84f)
        "pink" -> Color(0xFFE75480).copy(alpha = 0.86f)
        "gray" -> Color(0xFF8A8A8A).copy(alpha = 0.72f)
        else -> palette.highlight.copy(alpha = 0.78f)
    }
}

private fun globalVocabularyComposeColor(name: String?, palette: ReaderPalette): Color = when (name) {
    "yellow" -> Color(0xFFF0B429).copy(alpha = 0.22f)
    "green" -> Color(0xFF43A047).copy(alpha = 0.19f)
    "blue" -> Color(0xFF3B82D0).copy(alpha = 0.18f)
    "pink" -> Color(0xFFE75480).copy(alpha = 0.19f)
    "gray" -> Color(0xFF8A8A8A).copy(alpha = 0.16f)
    else -> palette.highlight.copy(alpha = 0.22f)
}

private fun normalizeReaderWord(value: String): String =
    value.trim().replace('’', '\'').lowercase(Locale.FRENCH)


private fun offlineSentenceGloss(
    dictionary: DictionaryRepository,
    sentence: String,
    focusWord: String,
): String {
    val stop = setOf(
        "le", "la", "les", "un", "une", "des", "du", "de", "d", "à", "au", "aux",
        "et", "ou", "mais", "que", "qui", "dont", "où", "il", "elle", "ils", "elles",
        "je", "tu", "nous", "vous", "on", "ce", "cet", "cette", "ces", "se", "sa", "son",
        "ses", "mes", "tes", "nos", "vos", "leur", "leurs", "en", "y", "ne", "pas",
    )
    val words = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[-'’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")
        .findAll(sentence)
        .map { it.value }
        .distinctBy { normalizeReaderWord(it) }
        .toList()

    val ordered = buildList {
        words.firstOrNull { it.equals(focusWord, ignoreCase = true) }?.let { add(it) }
        words.filterNot { it.equals(focusWord, ignoreCase = true) }
            .filterNot { normalizeReaderWord(it) in stop }
            .forEach { add(it) }
    }

    return ordered.take(10).mapNotNull { word ->
        val hit = runCatching { dictionary.lookupSafe(word, sentence).result }.getOrNull() ?: return@mapNotNull null
        val meaning = hit.definitions.firstOrNull()?.trim().orEmpty()
        if (meaning.isBlank()) null else "$word：$meaning"
    }.distinct().joinToString(" · ")
}

private val READER_FRENCH_TOKEN = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[-'’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")

/**
 * V3.17: build short n-grams around the tapped word. We only query the bundled
 * dictionary for these candidates; no network/AI is used for detection.
 */
private fun phraseCandidatesAround(sentence: String, wordOffset: Int, selectedWord: String): List<String> {
    if (sentence.isBlank()) return emptyList()
    val tokens = READER_FRENCH_TOKEN.findAll(sentence).toList()
    if (tokens.size < 2) return emptyList()

    val selectedIndex = if (wordOffset >= 0) {
        tokens.indexOfFirst { wordOffset in it.range }
    } else -1
    val byWord = tokens.indexOfFirst { normalizeReaderWord(it.value) == normalizeReaderWord(selectedWord) }
    val anchor = when {
        selectedIndex >= 0 -> selectedIndex
        byWord >= 0 -> byWord
        else -> 0
    }
    val out = linkedSetOf<String>()

    for (length in 5 downTo 2) {
        if (tokens.size < length) continue
        val minStart = (anchor - length + 1).coerceAtLeast(0)
        val maxStart = anchor.coerceAtMost(tokens.size - length)
        for (start in minStart..maxStart) {
            val end = start + length - 1
            if (anchor !in start..end) continue
            val from = tokens[start].range.first
            val to = tokens[end].range.last + 1
            val candidate = sentence.substring(from, to)
                .replace(Regex("\\s+"), " ")
                .trim(' ', ',', ';', ':', '.', '!', '?', '«', '»', '“', '”', '"')
            if (candidate.length in 3..72) out += candidate

            // French elision: a fixed expression stored as "... que" appears in text
            // as "... qu'il / qu'elle / qu'on". Add the canonical lookup form.
            val lastToken = tokens[end].value.replace('’', '\'')
            if (lastToken.lowercase(Locale.FRENCH).startsWith("qu'") && end > start) {
                val prefix = sentence.substring(from, tokens[end - 1].range.last + 1)
                    .replace(Regex("\\s+"), " ").trim()
                out += "$prefix que"
            }
        }
    }
    return out.take(18)
}

private fun nounGenderLabel(result: DictionaryResult, sentence: String, selectedWord: String): String? =
    FrenchNounGender.label(result, sentence, selectedWord)

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
internal fun Activity.setReaderImmersive(hidden: Boolean) {
    val window = window
    val paper = android.graphics.Color.rgb(247, 243, 234)

    // Huawei/HarmonyOS can hide the status bar but still reserve the display-cutout
    // area, leaving a black strip. Full screen therefore needs BOTH system-bar
    // hiding and permission to lay out into the cutout/system-bar area.
    val attrs = window.attributes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attrs.layoutInDisplayCutoutMode =
            if (hidden) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        window.attributes = attrs
    }

    if (hidden) {
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = paper
        window.navigationBarColor = paper
    }

    val immersiveFlags =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

    // Keep the legacy flags as a Huawei fallback even on API 30+.
    val lightBarFlags =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0) or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
    window.decorView.systemUiVisibility =
        if (hidden) immersiveFlags else View.SYSTEM_UI_FLAG_LAYOUT_STABLE or lightBarFlags

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(!hidden)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.insetsController?.let { controller ->
            if (hidden) {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsets.Type.systemBars())
                controller.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            }
        }
    }
}

private data class ReaderViewportMetrics(
    val widthPx: Int,
    val heightPx: Int,
    val cutoutTopPx: Int,
)

@Suppress("DEPRECATION")
private fun Activity.readerViewportMetrics(): ReaderViewportMetrics {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics = windowManager.currentWindowMetrics
        ReaderViewportMetrics(
            widthPx = metrics.bounds.width(),
            heightPx = metrics.bounds.height(),
            cutoutTopPx = metrics.windowInsets.displayCutout?.safeInsetTop ?: 0,
        )
    } else {
        val dm = resources.displayMetrics
        ReaderViewportMetrics(
            widthPx = dm.widthPixels,
            heightPx = dm.heightPixels,
            cutoutTopPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.decorView.rootWindowInsets?.displayCutout?.safeInsetTop ?: 0
            } else 0,
        )
    }
}

private enum class ReaderTheme { PAPER, EINK, NIGHT, PARCHMENT }
private enum class BookPanelTab { CONTENTS, SEARCH, BOOKMARKS, ANNOTATIONS }

private data class ReaderPalette(val background: Color, val text: Color, val chrome: Color, val highlight: Color)

private fun readerPalette(theme: ReaderTheme): ReaderPalette = when (theme) {
    ReaderTheme.PAPER -> ReaderPalette(Color(0xFFF7F3EA), Color(0xFF2E2A26), Color(0xFFF1EADF), Color(0xFFFFEDB3))
    ReaderTheme.EINK -> ReaderPalette(Color(0xFFF0F0EC), Color(0xFF202020), Color(0xFFE5E5E0), Color(0xFFDDE5C8))
    ReaderTheme.NIGHT -> ReaderPalette(Color(0xFF1D1C1A), Color(0xFFDED8CF), Color(0xFF292724), Color(0xFF6A5A34))
    ReaderTheme.PARCHMENT -> ReaderPalette(Color(0xFFF2E3C6), Color(0xFF3B2D20), Color(0xFFE4D0AA), Color(0xFFF2CF77))
}

private fun readerOuterBackground(theme: ReaderTheme): Color = when (theme) {
    ReaderTheme.PAPER -> Color(0xFFE9DFD1)
    ReaderTheme.EINK -> Color(0xFFDADAD6)
    ReaderTheme.NIGHT -> Color(0xFF11110F)
    ReaderTheme.PARCHMENT -> Color(0xFFD7C09B)
}

/**
 * V3.11 full-bleed paper surface + optional touch-led curved page curl.
 *
 * The fold is no longer a fixed triangular mask.  The live pointer Y position and
 * pager progress drive three separate layers: the visible front sheet, the paper
 * backside, and the already-composed incoming page underneath.  The fold boundary
 * itself is a cubic curve, which keeps the turn organic in both directions.
 */
private fun Modifier.bookPaperPage(
    theme: ReaderTheme,
    pageColor: Color,
    textColor: Color,
    curlProgress: Float = 0f,
    curlFromRight: Boolean = true,
    curlTouchYFraction: Float = 0.78f,
): Modifier = drawWithContent {
    val p = curlProgress.coerceIn(0f, 1f)
    val w = size.width
    val h = size.height

    fun drawPaperBase() {
        drawRect(pageColor)
        val fibre = if (theme == ReaderTheme.NIGHT) Color.White.copy(alpha = 0.006f) else textColor.copy(alpha = 0.016f)
        var y = 11f
        var band = 0
        while (y < h) {
            val x0 = ((band * 37) % 83).toFloat()
            val len = 42f + ((band * 29) % 112)
            drawLine(
                color = fibre,
                start = Offset(x0, y),
                end = Offset((x0 + len).coerceAtMost(w), y + ((band % 3) - 1) * 0.45f),
                strokeWidth = 0.58f,
            )
            y += 18f + (band % 4) * 3f
            band++
        }
        val edgeAlpha = if (theme == ReaderTheme.NIGHT) 0.052f else 0.055f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    textColor.copy(alpha = edgeAlpha * 0.45f),
                    Color.Transparent,
                    Color.Transparent,
                    textColor.copy(alpha = edgeAlpha),
                )
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    textColor.copy(alpha = edgeAlpha * 0.28f),
                    Color.Transparent,
                    Color.Transparent,
                    textColor.copy(alpha = edgeAlpha * 0.45f),
                )
            )
        )
    }

    if (p < 0.012f || w <= 1f || h <= 1f) {
        drawPaperBase()
        drawContent()
        return@drawWithContent
    }

    // Local X is measured inward from the turning edge.  Mirroring this single
    // geometry gives exactly the same curl when paging backwards.
    fun edgeX(inset: Float): Float = if (curlFromRight) w - inset else inset

    val depth = (w * (0.025f + 0.86f * p)).coerceIn(w * 0.025f, w * 0.90f)
    val requestedTouchY = h * curlTouchYFraction.coerceIn(0.28f, 0.95f)
    val touchY = requestedTouchY.coerceIn(h * 0.24f, h * 0.94f)
    val edgeLift = h * (0.035f + 0.16f * p)
    val edgeY = (touchY - edgeLift).coerceIn(h * 0.035f, h * 0.82f)
    val bottomInset = (depth * (0.68f + 0.10f * p)).coerceIn(w * 0.02f, w * 0.78f)
    val verticalSpan = (h - edgeY).coerceAtLeast(h * 0.16f)

    val a = Offset(edgeX(0f), edgeY)
    val b = Offset(edgeX(bottomInset), h)
    val touch = Offset(edgeX(depth), touchY)

    val foldC1 = Offset(edgeX(depth * 0.18f), edgeY + verticalSpan * 0.29f)
    val foldC2 = Offset(edgeX((bottomInset - depth * 0.08f).coerceAtLeast(depth * 0.14f)), h - verticalSpan * 0.18f)

    val foldPath = Path().apply {
        moveTo(a.x, a.y)
        cubicTo(foldC1.x, foldC1.y, foldC2.x, foldC2.y, b.x, b.y)
    }

    // Front of the current page.  Everything beyond the curved fold is cut away,
    // allowing the incoming pager page to be visible underneath.
    val frontPath = Path().apply {
        if (curlFromRight) {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(a.x, a.y)
            cubicTo(foldC1.x, foldC1.y, foldC2.x, foldC2.y, b.x, b.y)
            lineTo(0f, h)
            close()
        } else {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(b.x, b.y)
            cubicTo(foldC2.x, foldC2.y, foldC1.x, foldC1.y, a.x, a.y)
            lineTo(0f, 0f)
            close()
        }
    }

    val pageCanvas = drawContext.canvas
    pageCanvas.save()
    pageCanvas.clipPath(frontPath)
    try {
        drawPaperBase()
        drawContent()
    } finally {
        pageCanvas.restore()
    }

    // The free paper edge deliberately bows before reaching the finger.  This gives
    // the lifted sheet a rolled profile instead of the old straight triangular flap.
    val outerA1 = Offset(edgeX(depth * 0.045f), edgeY + (touchY - edgeY) * 0.40f)
    val outerA2 = Offset(edgeX(depth * 0.58f), touchY - verticalSpan * 0.13f)
    val outerB1 = Offset(edgeX(depth * 0.93f), touchY + verticalSpan * 0.10f)
    val outerB2 = Offset(edgeX((bottomInset + depth * 0.08f).coerceAtMost(depth * 0.98f)), h - verticalSpan * 0.16f)

    val backPath = Path().apply {
        moveTo(a.x, a.y)
        cubicTo(outerA1.x, outerA1.y, outerA2.x, outerA2.y, touch.x, touch.y)
        cubicTo(outerB1.x, outerB1.y, outerB2.x, outerB2.y, b.x, b.y)
        cubicTo(foldC2.x, foldC2.y, foldC1.x, foldC1.y, a.x, a.y)
        close()
    }

    val outerEdge = Path().apply {
        moveTo(a.x, a.y)
        cubicTo(outerA1.x, outerA1.y, outerA2.x, outerA2.y, touch.x, touch.y)
        cubicTo(outerB1.x, outerB1.y, outerB2.x, outerB2.y, b.x, b.y)
    }

    val deepShadow = if (theme == ReaderTheme.NIGHT) {
        Color.Black.copy(alpha = 0.52f)
    } else {
        Color(0xFF3D342D).copy(alpha = 0.22f)
    }
    val back = if (theme == ReaderTheme.NIGHT) Color(0xFF312F2B) else Color(0xFFF0ECE3)

    // Soft cast shadow lies half outside the fold and therefore lands on the page
    // underneath.  A broad low-alpha stroke looks much closer to paper than the old
    // hard diagonal line.
    drawPath(
        path = foldPath,
        color = deepShadow.copy(alpha = if (theme == ReaderTheme.NIGHT) 0.36f else 0.16f),
        style = Stroke(width = 10f + 18f * p),
    )

    drawPath(
        path = backPath,
        brush = Brush.linearGradient(
            colors = if (curlFromRight) {
                listOf(
                    deepShadow.copy(alpha = 0.14f),
                    back,
                    pageColor.copy(alpha = 0.985f),
                    Color.White.copy(alpha = if (theme == ReaderTheme.NIGHT) 0.025f else 0.28f),
                )
            } else {
                listOf(
                    Color.White.copy(alpha = if (theme == ReaderTheme.NIGHT) 0.025f else 0.28f),
                    pageColor.copy(alpha = 0.985f),
                    back,
                    deepShadow.copy(alpha = 0.14f),
                )
            },
            start = touch,
            end = a,
        ),
    )

    // Very faint reverse-print impression on the paper backside.  It is deliberately
    // subtle; at normal reading distance it reads as paper translucency, not text.
    clipPath(backPath) {
        val ghost = textColor.copy(alpha = if (theme == ReaderTheme.NIGHT) 0.018f else 0.027f)
        val minInset = (depth * 0.18f).coerceAtLeast(w * 0.03f)
        val maxInset = (depth * 0.82f).coerceAtMost(w * 0.82f)
        var gy = (edgeY + verticalSpan * 0.30f).coerceAtMost(h * 0.86f)
        repeat(6) { row ->
            val leftInset = minInset + (row % 2) * depth * 0.055f
            val rightInset = maxInset - (row % 3) * depth * 0.025f
            val x1 = edgeX(leftInset)
            val x2 = edgeX(rightInset)
            drawLine(ghost, Offset(x1, gy), Offset(x2, gy), strokeWidth = 0.85f)
            gy += h * 0.038f
        }
    }

    // Fold highlight + outside edge.  These two hairlines are what make the page read
    // as a thin sheet rather than a grey overlay.
    drawPath(
        path = foldPath,
        color = if (theme == ReaderTheme.NIGHT) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.50f),
        style = Stroke(width = 1.1f + 0.9f * p),
    )
    drawPath(
        path = outerEdge,
        color = textColor.copy(alpha = if (theme == ReaderTheme.NIGHT) 0.10f else 0.075f),
        style = Stroke(width = 0.9f),
    )
}

private enum class HomeTab { LIBRARY, DICTIONARY, TRAINING, VOCAB, ANNOTATIONS, SETTINGS }
private enum class VocabFilter { DUE, LEARNING, KNOWN, ALL }

internal data class WordSelection(
    val word: String,
    val sentence: String,
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    // Offset of the selected word inside sentence. -1 keeps older/PDF callers compatible.
    val wordOffsetInSentence: Int = -1,
)

internal data class SentenceSelection(
    val sentence: String,
    val paragraphIndex: Int,
    val sentenceStart: Int,
    val sentenceEnd: Int,
    val word: String,
    val wordStart: Int,
    val wordEnd: Int,
    // Window-space anchor for the floating long-press palette. PDF callers can
    // omit it and will fall back to a centered palette.
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
)

@Composable
fun LanguageReaderApp(
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
) {
    val scheme = lightColorScheme(
        primary = Accent,
        onPrimary = Color.White,
        background = Paper,
        surface = Paper,
        onSurface = Ink,
        secondaryContainer = AccentSoft,
    )
    MaterialTheme(colorScheme = scheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            AppRoot(bookRepository, dictionary, learningStore, tts)
        }
    }
}

@Composable
private fun AppRoot(
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val readingStats = remember { ReadingStatsStore(context) }
    val dictionaryCenterStore = remember { DictionaryCenterStore(context) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedBook by remember { mutableStateOf<BookItem?>(null) }
    var pendingReaderParagraph by remember { mutableStateOf<Int?>(null) }
    var tab by rememberSaveable { mutableStateOf(HomeTab.LIBRARY) }
    var books by remember { mutableStateOf(bookRepository.listBooks()) }
    var importing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tts.configure(
            networkFallbackEnabled = !settings.safeMode && settings.pronunciationRoute != "OFFLINE",
            persistentCacheEnabled = settings.pronunciationCacheEnabled,
            pronunciationRoute = settings.pronunciationRoute,
            fallbackEnabled = settings.pronunciationFallbackEnabled,
        )
        tts.prepare()
        withContext(Dispatchers.IO) {
            dictionary.warmUp()
            ReadingProfileStore(context).prune(90)
        }
        val migratedBooks = withContext(Dispatchers.IO) { bookRepository.backfillWordStats() }
        if (migratedBooks != books) books = migratedBooks
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                importing = true
                val result = runCatching { withContext(Dispatchers.IO) { DocumentImporter(context, bookRepository).import(uri) } }
                importing = false
                result.onSuccess { book ->
                    books = bookRepository.listBooks()
                    selectedBook = book
                }.onFailure { error ->
                    snackbar.showSnackbar(error.message ?: "Import impossible")
                }
            }
        }
    }

    selectedBook?.let { book ->
        val closeReader = {
            selectedBook = null
            pendingReaderParagraph = null
        }

        if (book.type.equals("PDF", ignoreCase = true)) {
            PdfReaderScreen(
                book = book,
                bookRepository = bookRepository,
                dictionary = dictionary,
                learningStore = learningStore,
                tts = tts,
                settings = settings,
                readingStats = readingStats,
                initialPage = pendingReaderParagraph,
                onBack = closeReader,
            )
        } else if (book.type.equals("EPUB", ignoreCase = true)) {
            // EPUB is now rendered by Readium in its own FragmentActivity. Keeping it
            // separate from the library shell prevents reader overlays, WebView selection
            // state and page animations from forcing the whole app to recompose.
            LaunchedEffect(book.id, pendingReaderParagraph) {
                pendingReaderParagraph?.let { bookRepository.saveProgress(book.id, it) }
                context.startActivity(ReadiumReaderActivity.intent(context, book.id))
                closeReader()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // TXT/subtitle/imported text formats keep the proven text reader. Readium is
            // introduced only where it has a mature EPUB navigator, so this migration does
            // not sacrifice any of Lirelia's existing document compatibility.
            ReaderScreen(
                book = book,
                bookRepository = bookRepository,
                dictionary = dictionary,
                learningStore = learningStore,
                tts = tts,
                settings = settings,
                readingStats = readingStats,
                initialParagraph = pendingReaderParagraph,
                onBack = closeReader,
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF1EADF)) {
                NavigationBarItem(
                    selected = tab == HomeTab.LIBRARY,
                    onClick = { tab = HomeTab.LIBRARY },
                    icon = { Text("📚") },
                    label = { Text("书库") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.DICTIONARY,
                    onClick = { tab = HomeTab.DICTIONARY },
                    icon = { Text("🔎") },
                    label = { Text("词典") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.TRAINING,
                    onClick = { tab = HomeTab.TRAINING },
                    icon = { Text("🎯") },
                    label = { Text("训练") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.VOCAB,
                    onClick = { tab = HomeTab.VOCAB },
                    icon = { Text("📝") },
                    label = { Text("生词") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.ANNOTATIONS,
                    onClick = { tab = HomeTab.ANNOTATIONS },
                    icon = { Text("🖍") },
                    label = { Text("标注") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.SETTINGS,
                    onClick = { tab = HomeTab.SETTINGS },
                    icon = { Text("⚙️") },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            HomeTab.LIBRARY -> LibraryScreen(
                books = books,
                bookRepository = bookRepository,
                readingStats = readingStats,
                learningStore = learningStore,
                importing = importing,
                modifier = Modifier.padding(padding),
                onImport = { launcher.launch(arrayOf("*/*")) },
                onOpen = {
                    pendingReaderParagraph = null
                    bookRepository.markOpened(it.id)
                    selectedBook = it
                },
                onDelete = { book ->
                    bookRepository.remove(book)
                    books = bookRepository.listBooks()
                },
            )
            HomeTab.DICTIONARY -> DictionaryCenterScreen(
                dictionary = dictionary,
                learningStore = learningStore,
                tts = tts,
                settings = settings,
                store = dictionaryCenterStore,
                modifier = Modifier.padding(padding),
            )
            HomeTab.TRAINING -> FrenchTrainingScreen(
                tts = tts,
                learningStore = learningStore,
                dictionary = dictionary,
                settings = settings,
                readingStats = readingStats,
                modifier = Modifier.padding(padding),
            )
            HomeTab.VOCAB -> VocabularyScreen(learningStore, tts, bookRepository, dictionary, settings, Modifier.padding(padding))
            HomeTab.ANNOTATIONS -> AnnotationManagerScreen(
                learningStore = learningStore,
                books = books,
                modifier = Modifier.padding(padding),
                onOpenAnnotation = { bookId, paragraphIndex ->
                    books.firstOrNull { it.id == bookId }?.let { book ->
                        pendingReaderParagraph = paragraphIndex
                        bookRepository.markOpened(book.id)
                        selectedBook = book
                    }
                },
            )
            HomeTab.SETTINGS -> SettingsScreen(
                settings = settings,
                dictionary = dictionary,
                readingStats = readingStats,
                learningStore = learningStore,
                tts = tts,
                books = books,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    books: List<BookItem>,
    bookRepository: BookRepository,
    readingStats: ReadingStatsStore,
    learningStore: LearningStore,
    importing: Boolean,
    modifier: Modifier = Modifier,
    onImport: () -> Unit,
    onOpen: (BookItem) -> Unit,
    onDelete: (BookItem) -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var deleteTarget by remember { mutableStateOf<BookItem?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var gridMode by remember { mutableStateOf(settings.libraryGridMode) }
    var favoriteRefresh by remember { mutableStateOf(0) }
    val organizer = remember { LibraryOrganizer(context) }
    var tagRefresh by remember { mutableStateOf(0) }
    var libraryMetaRefresh by remember { mutableStateOf(0) }
    var renameTarget by remember { mutableStateOf<BookItem?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var tagTarget by remember { mutableStateOf<BookItem?>(null) }
    var tagDraft by remember { mutableStateOf("") }
    var seriesDraft by remember { mutableStateOf("") }
    var shelfDraft by remember { mutableStateOf("") }
    var targetDateDraft by remember { mutableStateOf("") }
    var bookMemoDraft by remember { mutableStateOf("") }


    fun effectiveShelf(book: BookItem): String {
        val saved = organizer.shelf(book.id)
        if (saved.isNotBlank()) return saved
        val progress = bookRepository.loadProgress(book.id)
        return when {
            book.totalParagraphs > 0 && progress >= book.totalParagraphs - 2 -> "finished"
            progress > 0 -> "reading"
            else -> "unread"
        }
    }

    // V5.0: the library is the home screen. Search includes user-renamed titles.
    val shown = remember(books, query, favoriteRefresh, tagRefresh, libraryMetaRefresh) {
        val needle = query.trim()
        books
            .filter { book ->
                val displayTitle = organizer.displayTitle(book.id, book.title)
                needle.isBlank() ||
                    displayTitle.contains(needle, ignoreCase = true) ||
                    book.title.contains(needle, ignoreCase = true) ||
                    organizer.series(book.id).contains(needle, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<BookItem> { bookRepository.lastOpened(it.id) }
                    .thenByDescending { it.updatedAt }
            )
    }

    // V3.6.1: the whole library body scrolls as one surface.
    // Previously only the final book list was lazy/scrollable; as the dashboard grew,
    // it could consume the viewport and leave the book list with almost no height.
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "library_dashboard") {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "我的书架",
                        modifier = Modifier.weight(1f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    OutlinedButton(
                        onClick = onImport,
                        enabled = !importing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        if (importing) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                        } else {
                            Text("＋ 导入")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            gridMode = !gridMode
                            settings.libraryGridMode = gridMode
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(if (gridMode) "☰" else "▦") }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索书名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    if (query.isBlank()) "${books.size} 本书" else "搜索结果 · ${shown.size}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (shown.isEmpty()) {
            item(key = "library_empty") {
                Box(
                    Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (books.isEmpty()) EmptyLibrary()
                    else Text("没有找到匹配的书。", color = Color.Gray)
                }
            }
        } else if (gridMode) {
            items(
                items = shown.chunked(2),
                key = { row -> row.joinToString("|") { it.id } },
            ) { rowBooks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowBooks.forEach { book ->
                        Box(Modifier.weight(1f)) {
                            BookGridCard(
                                book = book,
                                displayTitle = organizer.displayTitle(book.id, book.title),
                                coverHidden = organizer.isCoverHidden(book.id),
                                bookRepository = bookRepository,
                                readingStats = readingStats,
                                tags = organizer.tags(book.id),
                                shelf = effectiveShelf(book),
                                series = organizer.series(book.id),
                                targetDate = organizer.targetDate(book.id),
                                hasMemo = organizer.bookMemo(book.id).isNotBlank(),
                                onOpen = { onOpen(book.copy(title = organizer.displayTitle(book.id, book.title))) },
                                onDelete = { deleteTarget = book },
                                onRename = {
                                    renameTarget = book
                                    renameDraft = organizer.displayTitle(book.id, book.title)
                                },
                                onToggleCover = {
                                    organizer.setCoverHidden(book.id, !organizer.isCoverHidden(book.id))
                                    libraryMetaRefresh++
                                },
                                onEditTags = {
                                    tagTarget = book
                                    tagDraft = organizer.tags(book.id).joinToString(", ")
                                    seriesDraft = organizer.series(book.id)
                                    shelfDraft = effectiveShelf(book)
                                    targetDateDraft = organizer.targetDate(book.id)
                                    bookMemoDraft = organizer.bookMemo(book.id)
                                },
                                onFavoriteChanged = { favoriteRefresh++ },
                            )
                        }
                    }
                    if (rowBooks.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            items(shown, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    displayTitle = organizer.displayTitle(book.id, book.title),
                    coverHidden = organizer.isCoverHidden(book.id),
                    bookRepository = bookRepository,
                    readingStats = readingStats,
                    tags = organizer.tags(book.id),
                    shelf = effectiveShelf(book),
                    series = organizer.series(book.id),
                    targetDate = organizer.targetDate(book.id),
                    hasMemo = organizer.bookMemo(book.id).isNotBlank(),
                    onOpen = { onOpen(book.copy(title = organizer.displayTitle(book.id, book.title))) },
                    onDelete = { deleteTarget = book },
                    onRename = {
                        renameTarget = book
                        renameDraft = organizer.displayTitle(book.id, book.title)
                    },
                    onToggleCover = {
                        organizer.setCoverHidden(book.id, !organizer.isCoverHidden(book.id))
                        libraryMetaRefresh++
                    },
                    onEditTags = {
                        tagTarget = book
                        tagDraft = organizer.tags(book.id).joinToString(", ")
                        seriesDraft = organizer.series(book.id)
                        shelfDraft = effectiveShelf(book)
                        targetDateDraft = organizer.targetDate(book.id)
                        bookMemoDraft = organizer.bookMemo(book.id)
                    },
                    onFavoriteChanged = { favoriteRefresh++ },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    deleteTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这本书？") },
            text = { Text("“${organizer.displayTitle(book.id, book.title)}”的本地副本会删除；生词本仍然保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        organizer.clearBook(book.id)
                        onDelete(book)
                        deleteTarget = null
                        tagRefresh++
                    }
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }

    renameTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名书籍") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(120) },
                    label = { Text("显示书名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        organizer.setDisplayTitle(book.id, renameDraft.ifBlank { book.title })
                        libraryMetaRefresh++
                        renameTarget = null
                    },
                    enabled = renameDraft.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }

    tagTarget?.let { book ->
        AlertDialog(
            onDismissRequest = { tagTarget = null },
            title = { Text("整理《${organizer.displayTitle(book.id, book.title)}》") },
            text = {
                Column {
                    Text("书架状态", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(
                            "unread" to "待读",
                            "reading" to "在读",
                            "finished" to "读完",
                            "paused" to "搁置",
                        ).forEach { entry ->
                            val (code, label) = entry
                            if (shelfDraft == code) {
                                Button(onClick = { shelfDraft = code }) { Text(label, fontSize = 10.sp) }
                            } else {
                                OutlinedButton(onClick = { shelfDraft = code }) { Text(label, fontSize = 10.sp) }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = seriesDraft,
                        onValueChange = { seriesDraft = it },
                        label = { Text("系列（可选）") },
                        placeholder = { Text("例如：Le Petit Nicolas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetDateDraft,
                        onValueChange = { targetDateDraft = it.take(10) },
                        label = { Text("计划完成日期（可选）") },
                        placeholder = { Text("2026-10-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookMemoDraft,
                        onValueChange = { bookMemoDraft = it.take(4000) },
                        label = { Text("全书备注（可选）") },
                        placeholder = { Text("例如：考试前重读第 3、5 章") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("标签用逗号分隔，例如：小说, B1, TCF", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(
                        value = tagDraft,
                        onValueChange = { tagDraft = it },
                        label = { Text("书籍标签") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        organizer.setTags(
                            book.id,
                            tagDraft.split(',', '，', ';', '；'),
                        )
                        organizer.setSeries(book.id, seriesDraft)
                        organizer.setShelf(book.id, shelfDraft)
                        organizer.setTargetDate(book.id, targetDateDraft)
                        organizer.setBookMemo(book.id, bookMemoDraft)
                        tagRefresh++
                        tagTarget = null
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { tagTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📖", fontSize = 52.sp)
        Spacer(Modifier.height(10.dp))
        Text("还没有书", fontWeight = FontWeight.Medium)
        Text("先导入一本法语 EPUB、TXT 或可复制文字的 PDF。", color = Color.Gray, fontSize = 13.sp)
    }
}

@Composable
private fun BookCover(book: BookItem, modifier: Modifier = Modifier, hidden: Boolean = false) {
    val cover by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = book.coverFile) {
        value = if (book.coverFile.isNotBlank()) {
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(book.coverFile) }.getOrNull() }
        } else null
    }

    if (!hidden && cover != null) {
        Image(
            bitmap = cover!!.asImageBitmap(),
            contentDescription = "${book.title} 封面",
            contentScale = ContentScale.Crop,
            modifier = modifier.background(AccentSoft, RoundedCornerShape(9.dp)),
        )
    } else {
        Box(
            modifier.background(AccentSoft, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (book.type == "PDF") "PDF" else "FR", fontWeight = FontWeight.Bold, color = Accent)
                Text(book.type, fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    book: BookItem,
    displayTitle: String,
    coverHidden: Boolean,
    bookRepository: BookRepository,
    readingStats: ReadingStatsStore,
    tags: Set<String>,
    shelf: String,
    series: String,
    targetDate: String,
    hasMemo: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onToggleCover: () -> Unit,
    onEditTags: () -> Unit,
    onFavoriteChanged: () -> Unit,
) {
    val paragraphIndex = remember(book.id) { bookRepository.loadProgress(book.id) }
    val progress = if (book.totalParagraphs > 0) {
        ((paragraphIndex + 1) / book.totalParagraphs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val chapterCount = remember(book.id, book.chapterFile) { bookRepository.chapters(book).size }
    var favorite by remember(book.id) { mutableStateOf(bookRepository.isFavorite(book.id)) }
    var showActions by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showActions = true },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book, Modifier.size(width = 60.dp, height = 86.dp), hidden = coverHidden)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    displayTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    buildString {
                        append(book.type)
                        if (chapterCount > 1) append(" · $chapterCount 章")
                        if (book.totalWords > 0) append(" · ${formatCount(book.totalWords)} 词")
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF887B70),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book.totalParagraphs > 0) {
                    Spacer(Modifier.height(11.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = Accent,
                        trackColor = AccentSoft.copy(alpha = 0.72f),
                    )
                    Spacer(Modifier.height(5.dp))
                    val readingSeconds = readingStats.bookSeconds(book.id)
                    Text(
                        buildString {
                            append("${(progress * 100).toInt()}%")
                            if (readingSeconds > 0L) append("  ·  ${formatDuration(readingSeconds)}")
                        },
                        fontSize = 10.sp,
                        color = Color(0xFF8A7E74),
                    )
                }
            }
            Box {
                TextButton(
                    onClick = { showActions = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("•••", color = Color(0xFF8A7E74), fontSize = 16.sp)
                }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(
                        text = { Text(if (favorite) "取消收藏" else "收藏") },
                        onClick = {
                            favorite = bookRepository.toggleFavorite(book.id)
                            onFavoriteChanged()
                            showActions = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            showActions = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (coverHidden) "显示封面" else "隐藏封面") },
                        onClick = {
                            showActions = false
                            onToggleCover()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("整理书籍") },
                        onClick = {
                            showActions = false
                            onEditTags()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showActions = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridCard(
    book: BookItem,
    displayTitle: String,
    coverHidden: Boolean,
    bookRepository: BookRepository,
    readingStats: ReadingStatsStore,
    tags: Set<String>,
    shelf: String,
    series: String,
    targetDate: String,
    hasMemo: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onToggleCover: () -> Unit,
    onEditTags: () -> Unit,
    onFavoriteChanged: () -> Unit,
) {
    val paragraphIndex = remember(book.id) { bookRepository.loadProgress(book.id) }
    val progress = if (book.totalParagraphs > 0) {
        ((paragraphIndex + 1) / book.totalParagraphs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    var favorite by remember(book.id) { mutableStateOf(bookRepository.isFavorite(book.id)) }
    var showActions by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showActions = true },
            ),
    ) {
        Column(Modifier.fillMaxWidth().padding(9.dp)) {
            Box {
                BookCover(book, Modifier.fillMaxWidth().height(188.dp), hidden = coverHidden)
                Box(Modifier.align(Alignment.TopEnd)) {
                    TextButton(
                        onClick = { showActions = true },
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text("•••", fontSize = 15.sp, color = Color(0xFF6F655D))
                    }
                    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                        DropdownMenuItem(
                            text = { Text(if (favorite) "取消收藏" else "收藏") },
                            onClick = {
                                favorite = bookRepository.toggleFavorite(book.id)
                                onFavoriteChanged()
                                showActions = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = {
                                showActions = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (coverHidden) "显示封面" else "隐藏封面") },
                            onClick = {
                                showActions = false
                                onToggleCover()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("整理书籍") },
                            onClick = {
                                showActions = false
                                onEditTags()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                showActions = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                displayTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = AccentSoft.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(progress * 100).toInt()}%",
                fontSize = 10.sp,
                color = Color(0xFF8A7E74),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ReaderScreen(
    book: BookItem,
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    settings: AppSettings,
    readingStats: ReadingStatsStore,
    initialParagraph: Int? = null,
    onBack: () -> Unit,
) {
    val paragraphs by produceState<List<String>?>(initialValue = null, key1 = book.id) {
        value = withContext(Dispatchers.IO) { bookRepository.paragraphs(book) }
    }
    val data = paragraphs
    if (data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val readerContext = LocalContext.current
    val activity = readerContext.findActivity()
    val readingProfile = remember { ReadingProfileStore(readerContext) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val viewportWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val startParagraph = (initialParagraph ?: bookRepository.resolveReaderPosition(book.id, data))
        .coerceIn(0, (data.size - 1).coerceAtLeast(0))

    var readerMode by remember {
        mutableStateOf(runCatching { ReaderMode.valueOf(settings.readerMode) }.getOrDefault(ReaderMode.SCROLL))
    }
    var pageChars by remember { mutableStateOf(settings.readerPageChars) }
    var autoScrollSpeed by remember { mutableStateOf(settings.readerAutoScrollSpeed) }
    var autoScrollRunning by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(settings.readerKeepScreenOn) }
    var twoPageLandscape by remember { mutableStateOf(settings.readerTwoPageLandscape) }
    var volumeKeyTurnsPage by remember { mutableStateOf(settings.readerVolumeKeyTurnsPage) }
    var readingRuler by remember { mutableStateOf(settings.readerReadingRuler) }
    var blueLightPercent by remember { mutableStateOf(settings.readerBlueLightPercent) }
    var pageAnimation by remember { mutableStateOf(settings.readerPageAnimation && !settings.safeMode) }
    var rulerStyle by remember {
        mutableStateOf(runCatching { ReaderRulerStyle.valueOf(settings.readerRulerStyle) }.getOrDefault(ReaderRulerStyle.BAND))
    }
    var rulerPositionPercent by remember { mutableStateOf(settings.readerRulerPositionPercent) }
    var rulerTopColor by remember { mutableStateOf(settings.readerLineGuideTopColor) }
    var rulerBottomColor by remember { mutableStateOf(settings.readerLineGuideBottomColor) }

    // Typography must exist before page calculation because V3.12 measures
    // each page using the current reader settings.
    var fontSize by remember { mutableStateOf(settings.readerFontSize) }
    var lineSpacing by remember { mutableStateOf(settings.readerLineSpacingPercent) }
    var marginDp by remember { mutableStateOf(settings.readerMarginDp) }
    val chapters = remember(book.id, book.chapterFile) { bookRepository.chapters(book) }

    val spreadSize = if (readerMode == ReaderMode.PAGE && isLandscape && twoPageLandscape) 2 else 1

    // V3.12: the paper still fills the physical screen, but text lives inside a
    // cutout-safe reading rectangle. Pagination is based on that rectangle's real
    // pixel dimensions and the current typography instead of a fixed char count.
    val viewportMetrics = remember(
        activity,
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        activity?.readerViewportMetrics() ?: ReaderViewportMetrics(
            widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() },
            heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() },
            cutoutTopPx = 0,
        )
    }
    val minimumTopPx = with(density) { 24.dp.roundToPx() }
    val cutoutBreathingPx = with(density) { 10.dp.roundToPx() }
    val readerTopInsetPx = maxOf(
        minimumTopPx,
        viewportMetrics.cutoutTopPx + cutoutBreathingPx,
    )
    val readerBottomInsetPx = with(density) { 26.dp.roundToPx() }
    val readerTopInsetDp = with(density) { readerTopInsetPx.toDp() }
    val readerBottomInsetDp = with(density) { readerBottomInsetPx.toDp() }

    val spreadGapPx = if (spreadSize == 2) with(density) { 8.dp.roundToPx() } else 0
    val physicalPageWidthPx = if (spreadSize == 2) {
        ((viewportMetrics.widthPx - spreadGapPx).coerceAtLeast(1) / 2)
    } else viewportMetrics.widthPx.coerceAtLeast(1)
    val horizontalTextPaddingPx = with(density) { ((marginDp * 2) + 10).dp.roundToPx() }
    val smartContentWidthPx = (physicalPageWidthPx - horizontalTextPaddingPx).coerceAtLeast(80)
    val smartContentHeightPx = (
        viewportMetrics.heightPx - readerTopInsetPx - readerBottomInsetPx
    ).coerceAtLeast(120)
    val fontSizePx = with(density) { fontSize.sp.toPx() }
    val lineHeightPx = with(density) { (fontSize * lineSpacing / 100f).sp.toPx() }
    val paragraphGapPx = (lineHeightPx * 0.18f).toInt().coerceAtLeast(1)
    val firstLineIndentPx = (fontSizePx * 1.35f).toInt().coerceAtLeast(0)
    val verticalTextPaddingPx = with(density) { 2.dp.roundToPx() }
    val chapterLabelHeightPx = with(density) { 12.sp.toPx().toInt() }
    val chapterVerticalPaddingPx = with(density) { 25.dp.roundToPx() }
    val imageHeightPx = minOf(
        (smartContentWidthPx * 0.72f).toInt(),
        (smartContentHeightPx * 0.48f).toInt(),
    ).coerceAtLeast(with(density) { 140.dp.roundToPx() })
    val chapterTitleMap = remember(chapters, book.type) {
        if (book.type == "EPUB") {
            chapters.filter { it.title.isNotBlank() }.associate { it.paragraphIndex to it.title }
        } else emptyMap()
    }

    val pages = remember(
        data, pageChars, fontSize, lineSpacing, marginDp, spreadSize,
        smartContentWidthPx, smartContentHeightPx, chapterTitleMap,
    ) {
        SmartReaderPaginator.paginate(
            paragraphs = data,
            contentWidthPx = smartContentWidthPx,
            contentHeightPx = smartContentHeightPx,
            fontSizePx = fontSizePx,
            lineHeightPx = lineHeightPx,
            paragraphGapPx = paragraphGapPx,
            verticalTextPaddingPx = verticalTextPaddingPx,
            firstLineIndentPx = firstLineIndentPx,
            chapterTitles = chapterTitleMap,
            chapterLabelHeightPx = chapterLabelHeightPx,
            chapterVerticalPaddingPx = chapterVerticalPaddingPx,
            imageHeightPx = imageHeightPx,
            fallbackChars = pageChars,
        )
    }
    val spreadCount = ((pages.size + spreadSize - 1) / spreadSize).coerceAtLeast(1)
    val initialPage = (
        ReaderPaginator.pageForParagraph(pages, startParagraph) / spreadSize
    ).coerceIn(0, (spreadCount - 1).coerceAtLeast(0))

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startParagraph)
    val pagerState = rememberPagerState(initialPage = initialPage) { spreadCount }
    var stableReadingAnchor by remember(book.id) { mutableStateOf(startParagraph) }

    var annotations by remember(book.id) { mutableStateOf(learningStore.textAnnotations(book.id)) }
    var globalLearningColors by remember(book.id) { mutableStateOf(learningStore.learningWordColors()) }
    var chromeVisible by remember { mutableStateOf(false) }
    var curlTouchYFraction by remember { mutableStateOf(0.78f) }
    // Translation-first reader: a normal tap opens a compact translation dock.
    // The complete dictionary is a second-level surface so the page stays visible.
    var selection by remember { mutableStateOf<WordSelection?>(null) }
    var dictionarySelection by remember { mutableStateOf<WordSelection?>(null) }
    var sentenceSelection by remember { mutableStateOf<SentenceSelection?>(null) }
    var readerTheme by remember {
        mutableStateOf(runCatching { ReaderTheme.valueOf(settings.readerTheme) }.getOrDefault(ReaderTheme.PAPER))
    }
    var showReaderSettings by remember { mutableStateOf(false) }
    var showBookStats by remember { mutableStateOf(false) }
    var showBookPanel by remember { mutableStateOf(false) }
    var bookPanelTab by remember { mutableStateOf(BookPanelTab.CONTENTS) }
    var bookmarks by remember(book.id) { mutableStateOf(bookRepository.bookmarks(book.id)) }
    val readerScope = rememberCoroutineScope()
    val palette = readerPalette(readerTheme)
    val readerAccent = if (readerTheme == ReaderTheme.NIGHT) Color(0xFFC8B58E) else Accent
    val readerChromeTapSource = remember { MutableInteractionSource() }

    val currentParagraph = when (readerMode) {
        ReaderMode.SCROLL -> listState.firstVisibleItemIndex
        ReaderMode.PAGE -> {
            val physicalPage = (pagerState.currentPage * spreadSize).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            pages.getOrNull(physicalPage)?.startParagraph ?: 0
        }
    }.coerceIn(0, (data.size - 1).coerceAtLeast(0))

    val progress = if (data.isEmpty()) 0 else (
        ((currentParagraph + 1) * 100f / data.size).toInt()
    ).coerceIn(0, 100)

    // Reading-language profile: count a paragraph/page only once per day.
    // This records observable exposure without inventing a vocabulary-size estimate.
    LaunchedEffect(book.id, readerMode, currentParagraph, pagerState.currentPage, spreadSize, pages, data) {
        withContext(Dispatchers.IO) {
            if (readerMode == ReaderMode.SCROLL) {
                data.getOrNull(currentParagraph)?.let { text ->
                    readingProfile.recordExposure(book.id, "p:$currentParagraph", text)
                }
            } else if (pages.isNotEmpty()) {
                val firstPhysical = (pagerState.currentPage * spreadSize).coerceIn(0, pages.lastIndex)
                val lastPhysical = (firstPhysical + spreadSize - 1).coerceAtMost(pages.lastIndex)
                for (physical in firstPhysical..lastPhysical) {
                    val page = pages[physical]
                    for (index in page.startParagraph until page.endParagraphExclusive) {
                        data.getOrNull(index)?.let { text ->
                            readingProfile.recordExposure(book.id, "p:$index", text)
                        }
                    }
                }
            }
        }
    }

    val currentChapterTitle = chapters
        .lastOrNull { it.paragraphIndex <= currentParagraph }
        ?.title
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "阅读"
    val readerProgressFraction = when (readerMode) {
        ReaderMode.SCROLL -> if (data.isEmpty()) 0f else ((currentParagraph + 1f) / data.size).coerceIn(0f, 1f)
        ReaderMode.PAGE -> if (pages.isEmpty()) 0f else {
            val visibleEnd = ((pagerState.currentPage + 1) * spreadSize).coerceAtMost(pages.size)
            (visibleEnd.toFloat() / pages.size).coerceIn(0f, 1f)
        }
    }

    val sessionSeconds = rememberReadingSessionSeconds(
        bookId = book.id,
        settings = settings,
        readingStats = readingStats,
    )

    // Reader-only window behavior. Brightness deliberately follows Android's
    // system setting; the reader no longer overrides the window brightness.
    DisposableEffect(activity) {
        activity?.window?.attributes = activity.window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        // Reader opens in immersive mode by default. The in-app chrome can be
        // summoned when needed, while Android system bars remain transient via edge swipe.
        activity?.setReaderImmersive(true)
        onDispose {
            activity?.setReaderImmersive(false)
            activity?.window?.attributes = activity.window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(keepScreenOn, activity) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Scroll mode: hide chrome when reading down, reveal on upward movement.
    LaunchedEffect(listState, readerMode) {
        if (readerMode != ReaderMode.SCROLL) return@LaunchedEffect
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress,
            )
        }.collect { (index, offset, scrolling) ->
            if (scrolling) {
                val movingDown = index > previousIndex || (index == previousIndex && offset > previousOffset + 8)
                val movingUp = index < previousIndex || (index == previousIndex && offset < previousOffset - 8)
                if (movingDown) chromeVisible = false
                if (movingUp) chromeVisible = true
            }
            previousIndex = index
            previousOffset = offset
        }
    }

    // Keep Android system bars hidden for the entire reader session.  The in-app
    // chrome is an overlay and must never shrink the page or leave a bottom strip.
    LaunchedEffect(chromeVisible) { activity?.setReaderImmersive(true) }

    // Word lookup and sentence annotation are reader overlays, not navigation.
    // They must preserve the user's current immersive/fullscreen state.
    LaunchedEffect(
        showReaderSettings,
        showBookStats,
        showBookPanel,
    ) {
        if (showReaderSettings || showBookStats || showBookPanel) {
            chromeVisible = true
        }
    }

    // Some Huawei/HarmonyOS builds briefly reveal system bars when a Material
    // bottom sheet takes focus. Re-assert immersive mode after the overlay opens.
    LaunchedEffect(selection, dictionarySelection, sentenceSelection, chromeVisible, activity) {
        if (!chromeVisible && (selection != null || dictionarySelection != null || sentenceSelection != null)) {
            repeat(4) {
                activity?.setReaderImmersive(true)
                delay(70)
            }
        }
    }

    // Persist a stable paragraph anchor rather than only a page number.  Page
    // boundaries move when font size, margins, orientation or spread mode change;
    // the anchor keeps the reader near the same sentence after repagination.
    LaunchedEffect(book.id, readerMode, listState, pagerState, spreadSize, pages, data) {
        if (readerMode == ReaderMode.SCROLL) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { index ->
                    val safe = index.coerceIn(0, (data.size - 1).coerceAtLeast(0))
                    stableReadingAnchor = safe
                    bookRepository.saveReaderPosition(book.id, safe, data.getOrNull(safe).orEmpty())
                }
        } else {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { spread ->
                    val physicalPage = (spread * spreadSize).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                    pages.getOrNull(physicalPage)?.let { page ->
                        val safe = page.startParagraph.coerceIn(0, (data.size - 1).coerceAtLeast(0))
                        stableReadingAnchor = safe
                        bookRepository.saveReaderPosition(book.id, safe, data.getOrNull(safe).orEmpty())
                    }
                }
        }
    }

    // If typography, orientation or two-page mode changes, recompute page
    // boundaries but keep the same paragraph under the reader instead of keeping
    // the old numeric page index.
    LaunchedEffect(pages, spreadSize, readerMode) {
        if (readerMode == ReaderMode.PAGE && pages.isNotEmpty()) {
            val physical = ReaderPaginator.pageForParagraph(pages, stableReadingAnchor)
            val target = (physical / spreadSize).coerceIn(0, (spreadCount - 1).coerceAtLeast(0))
            if (pagerState.currentPage != target) pagerState.scrollToPage(target)
        }
    }

    // Moon-style auto-scroll. Only active in continuous-scroll mode.
    LaunchedEffect(autoScrollRunning, autoScrollSpeed, readerMode) {
        if (!autoScrollRunning || readerMode != ReaderMode.SCROLL) return@LaunchedEffect
        val pxPerTick = with(density) { (0.45f + autoScrollSpeed * 0.55f).dp.toPx() }
        while (autoScrollRunning) {
            val consumed = listState.scrollBy(pxPerTick)
            if (consumed == 0f) {
                autoScrollRunning = false
                break
            }
            delay(24)
        }
    }

    // Keep page index valid if font/page density or landscape spread changes.
    LaunchedEffect(spreadCount) {
        if (readerMode == ReaderMode.PAGE && pagerState.currentPage > spreadCount - 1) {
            pagerState.scrollToPage((spreadCount - 1).coerceAtLeast(0))
        }
    }

    fun selectWordAt(paragraph: String, paragraphIndex: Int, word: String, start: Int, end: Int, offset: Int) {
        readerScope.launch(Dispatchers.IO) {
            readingProfile.recordLookup(word, book.id)
        }
        // Keep the page still: replace the content inside the same compact dock
        // instead of launching a new full-height sheet for every word.
        chromeVisible = false
        sentenceSelection = null
        dictionarySelection = null
        val sr = sentenceRange(paragraph, offset)
        val rawSentence = paragraph.substring(sr.first, sr.last + 1)
        val leadingWhitespace = rawSentence.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
        selection = WordSelection(
            word = word,
            sentence = rawSentence.trim(),
            paragraphIndex = paragraphIndex,
            startOffset = start,
            endOffset = end,
            wordOffsetInSentence = (start - sr.first - leadingWhitespace).coerceAtLeast(0),
        )
    }

    fun selectSentenceAt(paragraph: String, paragraphIndex: Int, offset: Int, anchor: Offset = Offset.Zero) {
        chromeVisible = false
        selection = null
        dictionarySelection = null
        val sr = sentenceRange(paragraph, offset)
        val wr = frenchWordRange(paragraph, offset)
        val wordStart = wr?.first ?: sr.first
        val wordEnd = (wr?.last?.plus(1) ?: (sr.first + 1)).coerceAtMost(paragraph.length)
        sentenceSelection = SentenceSelection(
            sentence = paragraph.substring(sr.first, sr.last + 1).trim(),
            paragraphIndex = paragraphIndex,
            sentenceStart = sr.first,
            sentenceEnd = sr.last + 1,
            word = paragraph.substring(wordStart, wordEnd),
            wordStart = wordStart,
            wordEnd = wordEnd,
            anchorX = anchor.x,
            anchorY = anchor.y,
        )
    }

    fun jumpToParagraph(index: Int) {
        val safe = index.coerceIn(0, (data.size - 1).coerceAtLeast(0))
        readerScope.launch {
            if (readerMode == ReaderMode.SCROLL) {
                listState.animateScrollToItem(safe)
            } else {
                val physical = ReaderPaginator.pageForParagraph(pages, safe)
                // Chapter/search jumps are navigation, not page-turn gestures; jump directly
                // so a long-distance seek does not play dozens of curl frames.
                pagerState.scrollToPage((physical / spreadSize).coerceIn(0, (spreadCount - 1).coerceAtLeast(0)))
            }
        }
    }
    DisposableEffect(readerMode, volumeKeyTurnsPage, spreadCount, spreadSize) {
        if (volumeKeyTurnsPage) {
            ReaderKeyBridge.onVolumeDown = {
                readerScope.launch {
                    if (readerMode == ReaderMode.PAGE) {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(spreadCount - 1))
                    } else {
                        listState.scrollBy(700f)
                    }
                }
            }
            ReaderKeyBridge.onVolumeUp = {
                readerScope.launch {
                    if (readerMode == ReaderMode.PAGE) {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    } else {
                        listState.scrollBy(-700f)
                    }
                }
            }
        } else {
            ReaderKeyBridge.clear()
        }
        onDispose { ReaderKeyBridge.clear() }
    }

    val readerOuter = readerOuterBackground(readerTheme)

    Scaffold(
        // The paper surface itself is the fullscreen background.  Reader controls
        // are drawn later as overlays so showing them never changes pagination.
        containerColor = palette.background,
        contentWindowInsets = ComposeWindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .padding(padding)
                .background(readerOuter)
                .clickable(
                    interactionSource = readerChromeTapSource,
                    indication = null,
                ) {
                    // Empty-space tap first dismisses translation/selection overlays.
                    // Only a second clean tap toggles the reader chrome. This mirrors
                    // a dedicated reading canvas instead of navigating away from it.
                    when {
                        selection != null -> selection = null
                        sentenceSelection != null -> sentenceSelection = null
                        else -> chromeVisible = !chromeVisible
                    }
                }
        ) {
            if (readerMode == ReaderMode.SCROLL) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        Modifier.fillMaxSize().bookPaperPage(
                            theme = readerTheme,
                            pageColor = palette.background,
                            textColor = palette.text,
                        )
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = marginDp.dp,
                                end = marginDp.dp,
                                top = readerTopInsetDp,
                                bottom = readerBottomInsetDp,
                            ),
                            verticalArrangement = Arrangement.spacedBy((fontSize * 0.24f).dp),
                        ) {
                            itemsIndexed(data, key = { index, _ -> index }) { index, paragraph ->
                                val chapter = chapters.firstOrNull { it.paragraphIndex == index }
                                if (book.type == "EPUB" && chapter != null && chapter.title.isNotBlank()) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            "CHAPITRE",
                                            color = palette.text.copy(alpha = 0.52f),
                                            fontSize = 10.sp,
                                            letterSpacing = 2.sp,
                                            fontFamily = FontFamily.Serif,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            chapter.title,
                                            color = palette.text,
                                            fontSize = (fontSize + 7).sp,
                                            lineHeight = (fontSize + 11).sp,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                val imagePath = BookRepository.imagePath(paragraph)
                                if (imagePath != null) {
                                    ReaderImageParagraph(imagePath = imagePath, palette = palette)
                                } else {
                                    ReaderParagraph(
                                        text = paragraph,
                                        annotations = annotations[index].orEmpty(),
                                        globalLearningColors = globalLearningColors,
                                        fontSize = fontSize,
                                        lineSpacingPercent = lineSpacing,
                                        palette = palette,
                                        isChapterStart = chapter != null,
                                        temporarySelection = sentenceSelection
                                            ?.takeIf { it.paragraphIndex == index }
                                            ?.let { it.wordStart to it.wordEnd },
                                        onWord = { word, start, end, offset ->
                                            selectWordAt(paragraph, index, word, start, end, offset)
                                        },
                                        onSentence = { offset, anchor -> selectSentenceAt(paragraph, index, offset, anchor) },
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(84.dp)) }
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageAnimation, readerMode) {
                            // Observe the live pointer without consuming it. HorizontalPager
                            // still owns the drag, while the curl can follow the finger Y.
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressed = event.changes.lastOrNull { it.pressed }
                                    if (pressed != null && size.height > 0) {
                                        val next = (pressed.position.y / size.height.toFloat()).coerceIn(0.28f, 0.95f)
                                        if ((next - curlTouchYFraction).absoluteValue > 0.006f) {
                                            curlTouchYFraction = next
                                        }
                                    }
                                }
                            }
                        },
                    pageSpacing = 0.dp,
                    contentPadding = PaddingValues(0.dp),
                ) { spread ->
                    val firstPhysical = spread * spreadSize
                    val settled = pagerState.settledPage
                    val settledOffset = ((pagerState.currentPage - settled) + pagerState.currentPageOffsetFraction)
                    val curlProgress = if (pageAnimation && pagerState.isScrollInProgress && spread == settled) {
                        settledOffset.absoluteValue.coerceIn(0f, 1f)
                    } else 0f
                    val curlFromRight = settledOffset >= 0f
                    val incomingSpread = when {
                        settledOffset > 0.001f -> (settled + 1).coerceAtMost(spreadCount - 1)
                        settledOffset < -0.001f -> (settled - 1).coerceAtLeast(0)
                        else -> settled
                    }
                    val activeCurlPair = pageAnimation && pagerState.isScrollInProgress &&
                        (spread == settled || spread == incomingSpread)
                    val relativePagerOffset = ((pagerState.currentPage - spread) + pagerState.currentPageOffsetFraction)

                    Row(
                        Modifier.fillMaxSize()
                            .zIndex(
                                when {
                                    pageAnimation && pagerState.isScrollInProgress && spread == settled -> 2f
                                    pageAnimation && pagerState.isScrollInProgress && spread == incomingSpread -> 1f
                                    else -> 0f
                                }
                            )
                            .graphicsLayer {
                                // Stack both sheets during the live turn. The incoming page is already
                                // fully underneath, so clipping the outgoing page reveals the real next
                                // page instead of a narrow sliding strip.
                                translationX = if (activeCurlPair) {
                                    relativePagerOffset * viewportWidthPx
                                } else 0f
                            }
                            .padding(
                                start = if (spreadSize == 2) 3.dp else 0.dp,
                                end = if (spreadSize == 2) 3.dp else 0.dp,
                                top = 0.dp,
                                bottom = 0.dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(if (spreadSize == 2) 8.dp else 0.dp),
                    ) {
                        repeat(spreadSize) { slot ->
                            val page = pages.getOrNull(firstPhysical + slot)
                            val slotCurlProgress = when {
                                spreadSize == 1 -> curlProgress
                                curlFromRight && slot == spreadSize - 1 -> curlProgress
                                !curlFromRight && slot == 0 -> curlProgress
                                else -> 0f
                            }
                            if (page == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(0.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    Box(
                                        Modifier.fillMaxSize().bookPaperPage(
                                            theme = readerTheme,
                                            pageColor = palette.background,
                                            textColor = palette.text,
                                            curlProgress = slotCurlProgress,
                                            curlFromRight = curlFromRight,
                                            curlTouchYFraction = curlTouchYFraction,
                                        )
                                    ) {
                                        Column(
                                            Modifier.fillMaxSize().padding(
                                                start = marginDp.dp,
                                                end = marginDp.dp,
                                                top = readerTopInsetDp,
                                                bottom = readerBottomInsetDp,
                                            ),
                                            verticalArrangement = Arrangement.spacedBy((fontSize * 0.18f).dp),
                                        ) {
                                            for (index in page.startParagraph until page.endParagraphExclusive) {
                                                val paragraph = data[index]
                                                val chapter = chapters.firstOrNull { it.paragraphIndex == index }
                                                if (book.type == "EPUB" && chapter != null && chapter.title.isNotBlank()) {
                                                    Column(
                                                        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                    ) {
                                                        Text(
                                                            "CHAPITRE",
                                                            color = palette.text.copy(alpha = 0.50f),
                                                            fontSize = 9.sp,
                                                            letterSpacing = 1.8.sp,
                                                            fontFamily = FontFamily.Serif,
                                                        )
                                                        Spacer(Modifier.height(7.dp))
                                                        Text(
                                                            chapter.title,
                                                            color = palette.text,
                                                            fontSize = (fontSize + 7).sp,
                                                            lineHeight = (fontSize + 11).sp,
                                                            fontFamily = FontFamily.Serif,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                        Spacer(Modifier.height(5.dp))
                                                        Box(
                                                            Modifier.width(38.dp).height(1.dp)
                                                                .background(palette.text.copy(alpha = 0.22f))
                                                        )
                                                    }
                                                }
                                                val imagePath = BookRepository.imagePath(paragraph)
                                                if (imagePath != null) {
                                                    ReaderImageParagraph(imagePath = imagePath, palette = palette)
                                                } else {
                                                    ReaderParagraph(
                                                        text = paragraph,
                                                        annotations = annotations[index].orEmpty(),
                                                        globalLearningColors = globalLearningColors,
                                                        fontSize = fontSize,
                                                        lineSpacingPercent = lineSpacing,
                                                        palette = palette,
                                                        isChapterStart = chapter != null,
                                                        temporarySelection = sentenceSelection
                                                            ?.takeIf { it.paragraphIndex == index }
                                                            ?.let { it.wordStart to it.wordEnd },
                                                        onWord = { word, start, end, offset ->
                                                            selectWordAt(paragraph, index, word, start, end, offset)
                                                        },
                                                        onSentence = { offset, anchor -> selectSentenceAt(paragraph, index, offset, anchor) },
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reader UI 2.0: controls are true overlays. Showing them never
            // changes pagination or moves the text, and the same short motion
            // language is used for top and bottom chrome.
            val showChrome = chromeVisible && selection == null && sentenceSelection == null && dictionarySelection == null

            AnimatedVisibility(
                visible = showChrome,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 3 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(170)) { -it / 4 },
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = readerTopInsetDp, start = 10.dp, end = 10.dp),
                    color = palette.chrome.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 7.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("‹", fontSize = 31.sp, color = readerAccent, fontWeight = FontWeight.Light) }

                        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                            Text(
                                currentChapterTitle,
                                color = palette.text,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "$progress% · ${if (readerMode == ReaderMode.PAGE) "翻页" else "滚动"}",
                                color = palette.text.copy(alpha = 0.54f),
                                fontSize = 9.sp,
                            )
                        }

                        TextButton(
                            onClick = { bookmarks = bookRepository.toggleBookmark(book.id, currentParagraph) },
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(if (currentParagraph in bookmarks) "★" else "☆", fontSize = 20.sp, color = readerAccent)
                        }
                        TextButton(
                            onClick = { showReaderSettings = true },
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("⋯", fontSize = 24.sp, color = readerAccent) }
                    }
                }
            }

            AnimatedVisibility(
                visible = showChrome,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(170)) + slideInVertically(tween(220)) { it / 2 },
                exit = fadeOut(tween(130)) + slideOutVertically(tween(170)) { it / 3 },
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    color = palette.chrome.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 9.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderChromeAction("☰", "目录", palette.text, readerAccent) {
                            bookPanelTab = BookPanelTab.CONTENTS
                            showBookPanel = true
                        }
                        ReaderChromeAction("Aa", "排版", palette.text, readerAccent) { showReaderSettings = true }
                        ReaderChromeAction("◐", "主题", palette.text, readerAccent) {
                            val all = ReaderTheme.entries
                            val next = all[(all.indexOf(readerTheme) + 1) % all.size]
                            readerTheme = next
                            settings.readerTheme = next.name
                        }
                        ReaderChromeAction("◷", "统计", palette.text, readerAccent) { showBookStats = true }
                    }
                }
            }

            if (readingRuler) {
                val alignment = when {
                    rulerPositionPercent < 35 -> Alignment.TopCenter
                    rulerPositionPercent > 65 -> Alignment.BottomCenter
                    else -> Alignment.Center
                }
                when (rulerStyle) {
                    ReaderRulerStyle.BAND -> Box(
                        Modifier.fillMaxWidth()
                            .height(44.dp)
                            .align(alignment)
                            .background(Color(0x2EFFD54F))
                            .border(1.dp, Color(0x55C49A2C))
                    )
                    ReaderRulerStyle.LINE -> Box(
                        Modifier.fillMaxWidth()
                            .height(2.dp)
                            .align(alignment)
                            .background(Color(0x99D69E2E))
                    )
                    ReaderRulerStyle.DOUBLE_LINE -> Box(
                        Modifier.fillMaxWidth()
                            .height(48.dp)
                            .align(alignment)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        readerGuideColor(rulerTopColor).copy(alpha = 0.10f),
                                        readerGuideColor(rulerBottomColor).copy(alpha = 0.08f),
                                    )
                                )
                            )
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                                .background(readerGuideColor(rulerTopColor).copy(alpha = 0.88f))
                        )
                        Box(
                            Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter)
                                .background(readerGuideColor(rulerBottomColor).copy(alpha = 0.88f))
                        )
                    }
                }
            }

            if (blueLightPercent > 0) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color(0xFFFFA94D).copy(alpha = (blueLightPercent / 100f) * 0.42f))
                )
            }

            AnimatedContent(
                targetState = selection,
                modifier = Modifier.align(Alignment.BottomCenter),
                label = "translation-dock",
            ) { sel ->
                if (sel != null) {
                    QuickTranslationDock(
                        selection = sel,
                        sourceBook = book,
                        dictionary = dictionary,
                        tts = tts,
                        settings = settings,
                        palette = palette,
                        accent = readerAccent,
                        onOpenDictionary = {
                            dictionarySelection = sel
                            selection = null
                        },
                        onClose = { selection = null },
                    )
                }
            }
        }
    }

    if (showBookPanel) {
        BookInsidePanel(
            initialTab = bookPanelTab,
            chapters = chapters,
            bookmarks = bookmarks,
            annotations = annotations,
            paragraphs = data,
            currentIndex = currentParagraph,
            onDismiss = { showBookPanel = false },
            onJump = { index ->
                stableReadingAnchor = index.coerceIn(0, (data.size - 1).coerceAtLeast(0))
                jumpToParagraph(index)
                showBookPanel = false
            },
        )
    }

    if (showReaderSettings) {
        ReaderSettingsSheet(
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            marginDp = marginDp,
            theme = readerTheme,
            mode = readerMode,
            pageChars = pageChars,
            autoScrollSpeed = autoScrollSpeed,
            keepScreenOn = keepScreenOn,
            twoPageLandscape = twoPageLandscape,
            isLandscape = isLandscape,
            volumeKeyTurnsPage = volumeKeyTurnsPage,
            readingRuler = readingRuler,
            blueLightPercent = blueLightPercent,
            pageAnimation = pageAnimation,
            rulerStyle = rulerStyle,
            rulerPositionPercent = rulerPositionPercent,
            rulerTopColor = rulerTopColor,
            rulerBottomColor = rulerBottomColor,
            tts = tts,
            settings = settings,
            onDismiss = { showReaderSettings = false },
            onFontSize = { fontSize = it; settings.readerFontSize = it },
            onLineSpacing = { lineSpacing = it; settings.readerLineSpacingPercent = it },
            onMargin = { marginDp = it; settings.readerMarginDp = it },
            onApplyTypographyPreset = { size, spacing, margin ->
                fontSize = size
                lineSpacing = spacing
                marginDp = margin
                settings.readerFontSize = size
                settings.readerLineSpacingPercent = spacing
                settings.readerMarginDp = margin
            },
            onTheme = { readerTheme = it; settings.readerTheme = it.name },
            onMode = {
                readerMode = it
                settings.readerMode = it.name
                autoScrollRunning = false
            },
            onPageChars = { pageChars = it; settings.readerPageChars = it },
            onAutoScrollSpeed = { autoScrollSpeed = it; settings.readerAutoScrollSpeed = it },
            onKeepScreenOn = { keepScreenOn = it; settings.readerKeepScreenOn = it },
            onTwoPageLandscape = { twoPageLandscape = it; settings.readerTwoPageLandscape = it },
            onVolumeKeyTurnsPage = { volumeKeyTurnsPage = it; settings.readerVolumeKeyTurnsPage = it },
            onReadingRuler = { readingRuler = it; settings.readerReadingRuler = it },
            onBlueLightPercent = { blueLightPercent = it; settings.readerBlueLightPercent = it },
            onPageAnimation = { pageAnimation = it; settings.readerPageAnimation = it },
            onRulerStyle = { rulerStyle = it; settings.readerRulerStyle = it.name },
            onRulerPositionPercent = { rulerPositionPercent = it; settings.readerRulerPositionPercent = it },
            onRulerTopColor = { rulerTopColor = it; settings.readerLineGuideTopColor = it },
            onRulerBottomColor = { rulerBottomColor = it; settings.readerLineGuideBottomColor = it },
        )
    }

    if (showBookStats) {
        BookStatsSheet(
            book = book,
            progress = progress,
            learningStore = learningStore,
            bookRepository = bookRepository,
            dictionary = dictionary,
            onDismiss = { showBookStats = false },
        )
    }

    dictionarySelection?.let { sel ->
        DictionarySheet(
            selection = sel,
            sourceBook = book,
            dictionary = dictionary,
            learningStore = learningStore,
            tts = tts,
            settings = settings,
            initialMarkColor = annotations[sel.paragraphIndex]
                .orEmpty()
                .firstOrNull { it.startOffset == sel.startOffset && it.endOffset == sel.endOffset }
                ?.color,
            onDismiss = { dictionarySelection = null },
            onSaveVocab = { result -> learningStore.addVocabulary(result, sel.sentence, book.id, book.title) },
            onWordMark = { color ->
                val paragraph = data.getOrNull(sel.paragraphIndex).orEmpty()
                if (color == null) {
                    learningStore.removeTextAnnotation(book.id, sel.paragraphIndex, sel.startOffset, sel.endOffset)
                } else {
                    learningStore.addTextAnnotation(
                        bookId = book.id,
                        paragraphIndex = sel.paragraphIndex,
                        fullParagraph = paragraph,
                        startOffset = sel.startOffset,
                        endOffset = sel.endOffset,
                        color = color,
                    )
                }
                annotations = learningStore.textAnnotations(book.id)
                globalLearningColors = learningStore.learningWordColors()
            },
        )
    }

    sentenceSelection?.let { sel ->
        SentenceSheet(
            selection = sel,
            paragraph = data.getOrNull(sel.paragraphIndex).orEmpty(),
            existingAnnotations = annotations[sel.paragraphIndex].orEmpty(),
            settings = settings,
            tts = tts,
            onDictionary = {
                dictionarySelection = WordSelection(
                    word = sel.word,
                    sentence = sel.sentence,
                    paragraphIndex = sel.paragraphIndex,
                    startOffset = sel.wordStart,
                    endOffset = sel.wordEnd,
                    wordOffsetInSentence = (sel.wordStart - sel.sentenceStart).coerceAtLeast(0),
                )
                sentenceSelection = null
            },
            onAnnotation = { start, end, color, note ->
                val paragraph = data.getOrNull(sel.paragraphIndex).orEmpty()
                if (color == null) {
                    learningStore.removeTextAnnotation(book.id, sel.paragraphIndex, start, end)
                } else {
                    learningStore.addTextAnnotation(
                        book.id, sel.paragraphIndex, paragraph, start, end, color, note
                    )
                    // Colored word highlights are vocabulary decisions; underline stays local-only.
                    val markedText = paragraph.substring(start.coerceIn(0, paragraph.length), end.coerceIn(start.coerceAtMost(paragraph.length), paragraph.length)).trim()
                    if (color != "underline" && markedText.matches(Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[-'’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*"))) {
                        readerScope.launch {
                            val lookup = withContext(Dispatchers.IO) { dictionary.lookupSafe(markedText).result }
                            withContext(Dispatchers.IO) {
                                learningStore.addVocabulary(lookup, sel.sentence, book.id, book.title)
                                learningStore.markLearning(markedText)
                                learningStore.setVocabularyMarkerColor(markedText, color)
                            }
                            globalLearningColors = learningStore.learningWordColors()
                        }
                    }
                }
                annotations = learningStore.textAnnotations(book.id)
            },
            onDismiss = { sentenceSelection = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookInsidePanel(
    initialTab: BookPanelTab,
    chapters: List<ChapterItem>,
    bookmarks: Set<Int>,
    annotations: Map<Int, List<TextAnnotation>>,
    paragraphs: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    var query by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    val annotationItems = remember(annotations) {
        annotations.entries
            .flatMap { (paragraphIndex, marks) -> marks.map { paragraphIndex to it } }
            .sortedWith(compareBy<Pair<Int, TextAnnotation>> { it.first }.thenBy { it.second.startOffset })
    }

    LaunchedEffect(query, paragraphs, tab) {
        if (tab != BookPanelTab.SEARCH) return@LaunchedEffect
        val q = query.trim()
        if (q.length < 2) {
            hits = emptyList()
            searching = false
        } else {
            searching = true
            hits = withContext(Dispatchers.Default) {
                buildList {
                    for ((index, paragraph) in paragraphs.withIndex()) {
                        if (paragraph.contains(q, ignoreCase = true)) {
                            add(index to searchPreview(paragraph, q))
                            if (size >= 80) break
                        }
                    }
                }
            }
            searching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFCF7),
        modifier = Modifier.imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            Text("书内面板", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "目录、搜索、书签和标注集中在这里，不占阅读页面。",
                fontSize = 11.sp,
                color = Color.Gray,
            )
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    BookPanelTab.CONTENTS to "目录",
                    BookPanelTab.SEARCH to "搜索",
                    BookPanelTab.BOOKMARKS to "书签",
                    BookPanelTab.ANNOTATIONS to "标注",
                ).forEach { (target, label) ->
                    TextButton(
                        onClick = { tab = target },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (tab == target) Accent else Color(0xFF8A7E74)
                        ),
                    ) {
                        Text(
                            label,
                            fontWeight = if (tab == target) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE7DED4))
            Spacer(Modifier.height(8.dp))

            when (tab) {
                BookPanelTab.CONTENTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            Text(
                                "当前位置：第 ${currentIndex + 1} 段 · ${chapters.size} 个章节",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        if (chapters.isEmpty()) {
                            item {
                                Text(
                                    "这本书没有可识别的章节目录。可以切到“搜索”定位内容。",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                )
                            }
                        } else {
                            itemsIndexed(chapters) { chapterIndex, chapter ->
                                val nextStart = chapters.getOrNull(chapterIndex + 1)?.paragraphIndex ?: Int.MAX_VALUE
                                val isCurrent = currentIndex >= chapter.paragraphIndex && currentIndex < nextStart
                                Card(
                                    onClick = { onJump(chapter.paragraphIndex) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) AccentSoft else Color.Transparent
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            chapter.title,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (isCurrent) Text("正在读", fontSize = 10.sp, color = Accent)
                                    }
                                }
                            }
                        }
                    }
                }

                BookPanelTab.SEARCH -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("书内搜索") },
                        placeholder = { Text("法语单词或短语") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        query.trim().length < 2 -> Text("至少输入 2 个字符。", color = Color.Gray, fontSize = 12.sp)
                        searching -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("正在搜索…", color = Color.Gray, fontSize = 12.sp)
                        }
                        hits.isEmpty() -> Text("没有找到。", color = Color.Gray, fontSize = 12.sp)
                        else -> Text(
                            "找到 ${hits.size}${if (hits.size >= 80) "+" else ""} 处",
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(hits, key = { it.first }) { (index, preview) ->
                            Card(
                                onClick = { onJump(index) },
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text("第 ${index + 1} 段", color = Accent, fontSize = 11.sp)
                                    Text(
                                        preview,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                BookPanelTab.BOOKMARKS -> {
                    if (bookmarks.isEmpty()) {
                        Text("还没有书签。阅读时点顶部 ☆ 即可添加当前书签。", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(bookmarks.sorted()) { index ->
                                Card(
                                    onClick = { onJump(index) },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                                        Text("🔖 第 ${index + 1} 段", fontSize = 11.sp, color = Accent)
                                        Text(
                                            paragraphs.getOrNull(index)?.take(220).orEmpty(),
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                BookPanelTab.ANNOTATIONS -> {
                    if (annotationItems.isEmpty()) {
                        Text("还没有标注。长按句子即可高亮、记笔记或做学习标记。", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(annotationItems) { (index, mark) ->
                                Card(
                                    onClick = { onJump(index) },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                                        Text("第 ${index + 1} 段 · ${when (mark.color) { "yellow" -> "黄"; "green" -> "绿"; "blue" -> "蓝"; "pink" -> "粉"; "underline" -> "下划线"; else -> mark.color }}", fontSize = 11.sp, color = Accent)
                                        Text(
                                            mark.text.ifBlank { paragraphs.getOrNull(index).orEmpty() }.take(240),
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (mark.note.isNotBlank()) {
                                            Text(
                                                mark.note,
                                                fontSize = 12.sp,
                                                color = Color(0xFF7A6F66),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}


private fun resolvedAnnotationRange(mark: TextAnnotation, text: String): IntRange? {
    if (text.isEmpty()) return null

    val start = mark.startOffset
    val end = mark.endOffset
    if (start >= 0 && end > start && end <= text.length) {
        // Trust precise offsets when they still point to the same text.
        val current = text.substring(start, end)
        if (mark.text.isBlank() || current == mark.text) {
            return start until end
        }
    }

    // Compatibility path for annotations created before character offsets existed,
    // or when an imported paragraph changed slightly after re-import.
    val target = mark.text.trim()
    if (target.isNotEmpty()) {
        val exact = text.indexOf(target)
        if (exact >= 0) return exact until (exact + target.length)
    }

    // Last resort: keep valid offsets even if text normalization changed.
    if (start >= 0 && end > start && start < text.length) {
        return start until end.coerceAtMost(text.length)
    }
    return null
}

@Composable
private fun ReaderImageParagraph(
    imagePath: String,
    palette: ReaderPalette,
) {
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = imagePath,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(imagePath) }.getOrNull()
        }
    }

    val decoded = bitmap
    if (decoded == null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = palette.chrome.copy(alpha = 0.65f)
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "🖼 图片加载失败",
                modifier = Modifier.padding(12.dp),
                color = palette.text.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
        return
    }

    val ratio = (decoded.width.toFloat() / decoded.height.coerceAtLeast(1))
        .coerceIn(0.45f, 2.8f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            bitmap = decoded.asImageBitmap(),
            contentDescription = "EPUB 图片",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio),
        )
    }
}

private fun frenchPublicationText(text: String): String = text
    // Same-length substitutions keep selection/highlight offsets stable while
    // preventing French punctuation from being stranded at the next line.
    .replace(Regex(" (?=[;?!»])"), "\u202F")
    .replace(Regex(" (?=:)"), "\u00A0")
    .replace("« ", "«\u202F")

@Composable
private fun ReaderParagraph(
    text: String,
    annotations: List<TextAnnotation>,
    globalLearningColors: Map<String, String>,
    fontSize: Int,
    lineSpacingPercent: Int,
    palette: ReaderPalette,
    isChapterStart: Boolean = false,
    temporarySelection: Pair<Int, Int>? = null,
    onWord: (String, Int, Int, Int) -> Unit,
    onSentence: (Int, Offset) -> Unit,
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var windowOrigin by remember(text) { mutableStateOf(Offset.Zero) }

    fun offsetAt(pos: Offset): Int? {
        val result = layout ?: return null
        return result.getOffsetForPosition(pos).coerceIn(0, (text.length - 1).coerceAtLeast(0))
    }

    val displayText = remember(text) { frenchPublicationText(text) }
    val annotatedText = remember(displayText, text, annotations, globalLearningColors, palette, temporarySelection) {
        buildAnnotatedString {
            append(displayText)
            // Global vocabulary radar: words that are still being learned receive a
            // deliberately pale version of their chosen highlight in every book.
            if (globalLearningColors.isNotEmpty()) {
                Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[-'’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")
                    .findAll(text)
                    .forEach { match ->
                        val colorName = globalLearningColors[normalizeReaderWord(match.value)] ?: return@forEach
                        addStyle(
                            SpanStyle(background = globalVocabularyComposeColor(colorName, palette)),
                            match.range.first,
                            match.range.last + 1,
                        )
                    }
            }
            temporarySelection?.let { (rawStart, rawEnd) ->
                val start = rawStart.coerceIn(0, text.length)
                val end = rawEnd.coerceIn(start, text.length)
                if (end > start) {
                    addStyle(
                        SpanStyle(
                            background = Color(0xFF2196D3).copy(alpha = 0.72f),
                            color = Color.White,
                        ),
                        start,
                        end,
                    )
                }
            }
            annotations.forEach { mark ->
                val range = resolvedAnnotationRange(mark, text) ?: return@forEach
                val start = range.first.coerceIn(0, text.length)
                val end = (range.last + 1).coerceIn(start, text.length)
                if (end > start) {
                    addStyle(
                        if (mark.color == "underline") {
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium,
                            )
                        } else {
                            SpanStyle(
                                background = annotationComposeColor(mark.color, palette),
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        start,
                        end,
                    )
                }
            }
        }
    }

    val trimmed = text.trimStart()
    val isDialogue = trimmed.startsWith("—") || trimmed.startsWith("–") ||
        trimmed.startsWith("-") || trimmed.startsWith("«") || trimmed.startsWith("\"")
    val firstLineIndent = if (!isChapterStart && !isDialogue && text.length >= 45) {
        (fontSize * 1.35f).sp
    } else 0.sp

    Text(
        text = annotatedText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .onGloballyPositioned { windowOrigin = it.positionInWindow() }
            .pointerInput(text) {
                detectTapGestures(
                    onTap = { pos ->
                        val offset = offsetAt(pos) ?: return@detectTapGestures
                        val range = frenchWordRange(text, offset) ?: nearestFrenchWordRange(text, offset) ?: return@detectTapGestures
                        onWord(
                            text.substring(range.first, range.last + 1),
                            range.first,
                            range.last + 1,
                            offset,
                        )
                    },
                    onLongPress = { pos ->
                        val offset = offsetAt(pos) ?: return@detectTapGestures
                        onSentence(offset, windowOrigin + pos)
                    },
                )
            },
        color = palette.text,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * lineSpacingPercent / 100f).sp,
        fontFamily = FontFamily.Serif,
        style = MaterialTheme.typography.bodyLarge.copy(
            textIndent = TextIndent(firstLine = firstLineIndent),
        ),
        onTextLayout = { layout = it },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookStatsSheet(
    book: BookItem,
    progress: Int,
    learningStore: LearningStore,
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val profileStore = remember { ReadingProfileStore(context) }
    val bookProfile by produceState(
        initialValue = com.cy.languagereader.mobile.data.BookReadingProfile(0L, 0L),
        key1 = book.id,
    ) {
        value = withContext(Dispatchers.IO) { profileStore.bookSnapshot(book.id, 30) }
    }
    val trackedCount by produceState(initialValue = 0, key1 = book.id) {
        value = withContext(Dispatchers.IO) { learningStore.bookVocabularyCount(book.id) }
    }
    val learningCount by produceState(initialValue = 0, key1 = book.id) {
        value = withContext(Dispatchers.IO) { learningStore.bookLearningCount(book.id) }
    }
    val knownCount by produceState(initialValue = 0, key1 = book.id) {
        value = withContext(Dispatchers.IO) { learningStore.bookKnownCount(book.id) }
    }
    val highlightCount by produceState(initialValue = 0, key1 = book.id) {
        value = withContext(Dispatchers.IO) { learningStore.highlightCount(book.id) }
    }
    val candidates by produceState<List<BookWordCandidate>>(initialValue = emptyList(), key1 = book.id, key2 = trackedCount) {
        value = withContext(Dispatchers.IO) {
            bookRepository.topWordCandidates(book, learningStore.trackedNormalizedWords(), 16)
        }
    }
    val markedCoverage = if (book.uniqueWords > 0) knownCount * 100f / book.uniqueWords else 0f

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFFFFCF7)) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("本书学习统计", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(book.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                StatLine("阅读进度", "$progress%")
                StatLine("总词数", if (book.totalWords > 0) formatCount(book.totalWords) else "—")
                StatLine("去重词数", if (book.uniqueWords > 0) formatCount(book.uniqueWords) else "—")
                StatLine("学习中", learningCount.toString())
                StatLine("已标记掌握", knownCount.toString())
                StatLine("全部追踪词", trackedCount.toString())
                StatLine("彩色标注", highlightCount.toString())
                if (bookProfile.exposedWords > 0L) {
                    StatLine("近30天实际读过", "${formatCount(bookProfile.exposedWords.toInt())} 词")
                    StatLine("查词密度", String.format(Locale.US, "%.1f / 100词", bookProfile.lookupsPer100))
                }
                if (book.uniqueWords > 0) {
                    StatLine("已标记掌握 / 去重词", String.format(Locale.US, "%.1f%%", markedCoverage))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "“掌握率”只统计你主动标记为“已掌握”的词，不会假装推断你认识整本书的多少词。",
                    fontSize = 11.sp, color = Color.Gray, lineHeight = 17.sp
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("全书高频候选词", fontWeight = FontWeight.SemiBold, color = Accent)
                Text("已在生词本/已掌握列表中的词会自动排除。适合快速预习一本书。", fontSize = 11.sp, color = Color.Gray)
            }

            if (candidates.isEmpty()) {
                item { Text("暂时没有新的高频候选词。", color = Color.Gray, fontSize = 13.sp) }
            } else {
                items(candidates) { candidate ->
                    val meaning by produceState(initialValue = "", key1 = candidate.word) {
                        value = withContext(Dispatchers.IO) {
                            dictionary.lookupSafe(candidate.word).result.definitions.firstOrNull().orEmpty()
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(candidate.word, fontWeight = FontWeight.SemiBold)
                                if (meaning.isNotBlank()) Text(meaning, fontSize = 12.sp, color = Color(0xFF6B625C), maxLines = 2)
                            }
                            Text("${candidate.count}×", color = Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF6B625C), modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private val READER_GUIDE_COLOR_OPTIONS = listOf("BLUE", "ORANGE", "GREEN", "RED", "PURPLE", "GRAY")

private fun readerGuideColor(code: String): Color = when (code.uppercase()) {
    "ORANGE" -> Color(0xFFF08A24)
    "GREEN" -> Color(0xFF43A047)
    "RED" -> Color(0xFFE14B4B)
    "PURPLE" -> Color(0xFF8E5AC7)
    "GRAY" -> Color(0xFF6F7780)
    else -> Color(0xFF3B82D0)
}

@Composable
private fun ReaderGuideColorPicker(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        READER_GUIDE_COLOR_OPTIONS.forEach { code ->
            val chosen = selected.equals(code, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(if (chosen) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(readerGuideColor(code))
                    .border(
                        width = if (chosen) 3.dp else 1.dp,
                        color = if (chosen) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.75f),
                        shape = CircleShape,
                    )
                    .clickable { onSelected(code) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    fontSize: Int,
    lineSpacing: Int,
    marginDp: Int,
    theme: ReaderTheme,
    mode: ReaderMode,
    pageChars: Int,
    autoScrollSpeed: Int,
    keepScreenOn: Boolean,
    twoPageLandscape: Boolean,
    isLandscape: Boolean,
    volumeKeyTurnsPage: Boolean,
    readingRuler: Boolean,
    blueLightPercent: Int,
    pageAnimation: Boolean,
    rulerStyle: ReaderRulerStyle,
    rulerPositionPercent: Int,
    rulerTopColor: String,
    rulerBottomColor: String,
    tts: TtsManager,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onFontSize: (Int) -> Unit,
    onLineSpacing: (Int) -> Unit,
    onMargin: (Int) -> Unit,
    onApplyTypographyPreset: (Int, Int, Int) -> Unit,
    onTheme: (ReaderTheme) -> Unit,
    onMode: (ReaderMode) -> Unit,
    onPageChars: (Int) -> Unit,
    onAutoScrollSpeed: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onTwoPageLandscape: (Boolean) -> Unit,
    onVolumeKeyTurnsPage: (Boolean) -> Unit,
    onReadingRuler: (Boolean) -> Unit,
    onBlueLightPercent: (Int) -> Unit,
    onPageAnimation: (Boolean) -> Unit,
    onRulerStyle: (ReaderRulerStyle) -> Unit,
    onRulerPositionPercent: (Int) -> Unit,
    onRulerTopColor: (String) -> Unit,
    onRulerBottomColor: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFFFFCF7)) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("阅读控制中心", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("V4.5 · Translation Experience / Readium", fontSize = 12.sp, color = Color.Gray)
            }

            item {
                var pronunciationRoute by remember { mutableStateOf(settings.pronunciationRoute) }
                var pronunciationCache by remember { mutableStateOf(settings.pronunciationCacheEnabled) }
                var pronunciationFallback by remember { mutableStateOf(settings.pronunciationFallbackEnabled) }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("🌐 网络与发音", fontWeight = FontWeight.SemiBold, color = Color(0xFF557A55))
                        Text("默认用自动路线：优先本地与系统法语，再按网络可用性回退。也可以手动锁定路线。", fontSize = 11.sp, color = Color(0xFF6B625C))
                        Spacer(Modifier.height(8.dp))
                        val routes = listOf(
                            "AUTO" to "✨ 自动",
                            "MAINLAND" to "🇨🇳 大陆",
                            "GOOGLE" to "🌍 国际",
                            "OFFLINE" to "📴 离线",
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            routes.forEach { (route, label) ->
                                val selected = pronunciationRoute == route
                                if (selected) {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp),
                                    ) { Text(label, fontSize = 10.sp) }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            pronunciationRoute = route
                                            settings.pronunciationRoute = route
                                            tts.configure(
                                                networkFallbackEnabled = !settings.safeMode && route != "OFFLINE",
                                                persistentCacheEnabled = pronunciationCache,
                                                pronunciationRoute = route,
                                                fallbackEnabled = pronunciationFallback,
                                            )
                                            tts.refreshEngineDiscovery()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp),
                                    ) { Text(label, fontSize = 10.sp) }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (pronunciationRoute) {
                                "AUTO" -> "当前：缓存 → 可用的系统法语 → 有道/百度 → Google 最后备用。正常情况不再弹出“正在连接 Google”。"
                                "MAINLAND" -> "当前：缓存未命中时优先有道/百度；必要时使用系统法语 TTS，不主动连接 Google。"
                                "OFFLINE" -> "当前：只使用本机法语 TTS 与已缓存音频。"
                                else -> "当前：国际网络模式，缓存未命中时允许使用 Google 法语发音。"
                            },
                            fontSize = 10.sp, color = Color.Gray,
                        )
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("保存成功发音", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("再次遇到同一词时直接本地播放。", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(checked = pronunciationCache, onCheckedChange = {
                                pronunciationCache = it
                                settings.pronunciationCacheEnabled = it
                                tts.configure(!settings.safeMode && pronunciationRoute != "OFFLINE", it, pronunciationRoute, pronunciationFallback)
                            })
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("失败后才切备用源", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("正常发音不会多做网络测试。", fontSize = 10.sp, color = Color.Gray)
                            }
                            Switch(checked = pronunciationFallback, onCheckedChange = {
                                pronunciationFallback = it
                                settings.pronunciationFallbackEnabled = it
                                tts.configure(!settings.safeMode && pronunciationRoute != "OFFLINE", pronunciationCache, pronunciationRoute, it)
                            })
                        }
                        OutlinedButton(onClick = { tts.speak("bonjour") }, modifier = Modifier.fillMaxWidth()) {
                            Text("🔊 试听 Bonjour · ${tts.routeLabel()}", fontSize = 10.sp)
                        }
                    }
                }
            }

            item {
                Text("阅读方式", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == ReaderMode.SCROLL) {
                        Button(onClick = { onMode(ReaderMode.SCROLL) }, modifier = Modifier.weight(1f)) { Text("连续滚动") }
                    } else {
                        OutlinedButton(onClick = { onMode(ReaderMode.SCROLL) }, modifier = Modifier.weight(1f)) { Text("连续滚动") }
                    }
                    if (mode == ReaderMode.PAGE) {
                        Button(onClick = { onMode(ReaderMode.PAGE) }, modifier = Modifier.weight(1f)) { Text("纸页翻页") }
                    } else {
                        OutlinedButton(onClick = { onMode(ReaderMode.PAGE) }, modifier = Modifier.weight(1f)) { Text("纸页翻页") }
                    }
                }
            }

            item {
                Text("排版预设", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                val presets = listOf(
                    Triple("紧凑", 18, Pair(135, 16)),
                    Triple("标准", 20, Pair(150, 20)),
                    Triple("舒展", 22, Pair(165, 26)),
                    Triple("大字", 26, Pair(170, 24)),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { (label, size, rest) ->
                        val spacing = rest.first
                        val margin = rest.second
                        val selected = fontSize == size && lineSpacing == spacing && marginDp == margin
                        if (selected) {
                            Button(
                                onClick = { onApplyTypographyPreset(size, spacing, margin) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp),
                            ) { Text(label, fontSize = 11.sp) }
                        } else {
                            OutlinedButton(
                                onClick = { onApplyTypographyPreset(size, spacing, margin) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp),
                            ) { Text(label, fontSize = 11.sp) }
                        }
                    }
                }
                Text(
                    "预设会同时调整字号、行距和左右边距；下面仍可继续微调。",
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
            }

            item {
                SettingStepper("字号", fontSize.toString(),
                    { onFontSize((fontSize - 1).coerceAtLeast(14)) },
                    { onFontSize((fontSize + 1).coerceAtMost(64)) })
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { onFontSize(it.toInt().coerceIn(14, 64)) },
                    valueRange = 14f..64f,
                )
                SettingStepper("行距", "$lineSpacing%",
                    { onLineSpacing((lineSpacing - 5).coerceAtLeast(90)) },
                    { onLineSpacing((lineSpacing + 5).coerceAtMost(260)) })
                Slider(
                    value = lineSpacing.toFloat(),
                    onValueChange = { onLineSpacing(it.toInt().coerceIn(90, 260)) },
                    valueRange = 90f..260f,
                )
                SettingStepper("左右边距", "${marginDp}dp",
                    { onMargin((marginDp - 2).coerceAtLeast(8)) },
                    { onMargin((marginDp + 2).coerceAtMost(40)) })
            }

            if (mode == ReaderMode.PAGE) {
                item {
                    Text("智能分页", fontWeight = FontWeight.Medium)
                    Text(
                        "已按当前屏幕、摄像头安全区、字号、行距和边距自动计算每页内容。",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }

                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("横屏双页", fontWeight = FontWeight.Medium)
                            Text(
                                if (isLandscape) "当前横屏：可同时显示左右两页。"
                                else "切到横屏时自动使用左右双页。",
                                fontSize = 11.sp,
                                color = Color.Gray,
                            )
                        }
                        Switch(checked = twoPageLandscape, onCheckedChange = onTwoPageLandscape)
                    }
                }
            } else {
                item {
                    Text("自动滚动速度", fontWeight = FontWeight.Medium)
                    Slider(
                        value = autoScrollSpeed.toFloat(),
                        onValueChange = { onAutoScrollSpeed(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                    Text("$autoScrollSpeed / 10", fontSize = 12.sp, color = Color.Gray)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("屏幕亮度", fontWeight = FontWeight.Medium)
                        Text("跟随系统亮度，不再由阅读器单独覆盖。", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("系统", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("阅读时屏幕常亮", fontWeight = FontWeight.Medium)
                        Text("自动滚动或长时间阅读时不自动熄屏。", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOn)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("音量键翻页", fontWeight = FontWeight.Medium)
                        Text("音量+上一页 / 音量-下一页；滚动模式则上下移动。", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = volumeKeyTurnsPage, onCheckedChange = onVolumeKeyTurnsPage)
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("正文轻点：翻译优先", fontWeight = FontWeight.SemiBold, color = Color(0xFF557A55))
                        Text(
                            "滚动和翻页模式都一样：轻点法语词只浮出小型快译卡，正文不移动；需要所有释义时再进入完整词典。长按只负责标注工具。",
                            fontSize = 11.sp,
                            color = Color(0xFF6B625C),
                            lineHeight = 17.sp,
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("行跟随", fontWeight = FontWeight.Medium)
                        Text("双线模式会用两条不同颜色的线夹住当前阅读区域，中间用浅色高亮。", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = readingRuler, onCheckedChange = onReadingRuler)
                }
                if (readingRuler) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReaderRulerStyle.entries.forEach { style ->
                            val label = when (style) {
                                ReaderRulerStyle.BAND -> "高亮带"
                                ReaderRulerStyle.LINE -> "单线"
                                ReaderRulerStyle.DOUBLE_LINE -> "双线"
                            }
                            if (style == rulerStyle) {
                                Button(onClick = { onRulerStyle(style) }) { Text(label, fontSize = 11.sp) }
                            } else {
                                OutlinedButton(onClick = { onRulerStyle(style) }) { Text(label, fontSize = 11.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("位置", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = rulerPositionPercent.toFloat(),
                        onValueChange = { onRulerPositionPercent(it.toInt()) },
                        valueRange = 15f..85f,
                        steps = 13,
                    )
                    if (rulerStyle == ReaderRulerStyle.DOUBLE_LINE) {
                        Text("上边线颜色", fontSize = 12.sp, color = Color.Gray)
                        ReaderGuideColorPicker(rulerTopColor, onRulerTopColor)
                        Text("下边线颜色", fontSize = 12.sp, color = Color.Gray)
                        ReaderGuideColorPicker(rulerBottomColor, onRulerBottomColor)
                    }
                }
            }

            item {
                Text("护眼暖色层", fontWeight = FontWeight.Medium)
                Slider(
                    value = blueLightPercent.toFloat(),
                    onValueChange = { onBlueLightPercent(it.toInt()) },
                    valueRange = 0f..55f,
                    steps = 10,
                )
                Text(if (blueLightPercent == 0) "关闭" else "$blueLightPercent%", fontSize = 12.sp, color = Color.Gray)
            }

            if (mode == ReaderMode.PAGE) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("翻页动画", fontWeight = FontWeight.Medium)
                            Text("开启后使用纸张卷角、背面与阴影效果；关闭则保留基础滑页。", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = pageAnimation, onCheckedChange = onPageAnimation)
                    }
                }
            }

            item {
                Text("主题", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ReaderTheme.entries.forEach { item ->
                        val label = when (item) {
                            ReaderTheme.PAPER -> "纸张"
                            ReaderTheme.EINK -> "墨水屏"
                            ReaderTheme.NIGHT -> "夜间纸页"
                            ReaderTheme.PARCHMENT -> "羊皮纸"
                        }
                        if (item == theme) {
                            Button(onClick = { onTheme(item) }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 12.sp) }
                        } else {
                            OutlinedButton(onClick = { onTheme(item) }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 12.sp) }
                        }
                    }
                }
            }

            item {
                Text("屏幕方向", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                val activity = LocalContext.current.findActivity()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            Toast.makeText(activity, "屏幕方向：自动", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("自动", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            Toast.makeText(activity, "已切换竖屏", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("竖屏", fontSize = 12.sp) }
                    Button(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            Toast.makeText(activity, "已强制切换横屏", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("横屏", fontSize = 12.sp) }
                }
            }

            item {
                OutlinedButton(onClick = { tts.openTtsSettings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔊 打开手机语音引擎设置")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "轻点词 = 快译；完整词典 = 二级展开；长按 = 标注。翻页模式继续使用左右滑动或音量键。",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
@Composable
private fun SettingStepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = minus) { Text("−") }
        Text(value, modifier = Modifier.width(68.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedButton(onClick = plus) { Text("＋") }
    }
}

@Composable
private fun ReaderChromeAction(
    icon: String,
    label: String,
    textColor: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(1.dp))
            Text(label, color = textColor.copy(alpha = 0.74f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun QuickTranslationDock(
    selection: WordSelection,
    sourceBook: BookItem,
    dictionary: DictionaryRepository,
    tts: TtsManager,
    settings: AppSettings,
    palette: ReaderPalette,
    accent: Color,
    modifier: Modifier = Modifier,
    onOpenDictionary: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var lookup by remember(selection.word, selection.sentence, selection.wordOffsetInSentence) { mutableStateOf<DictionaryLookup?>(null) }
    var sentenceTranslation by remember(selection.sentence) { mutableStateOf("") }
    var sentenceSource by remember(selection.sentence) { mutableStateOf("") }
    var sentenceError by remember(selection.sentence) { mutableStateOf("") }
    var offlineGloss by remember(selection.sentence) { mutableStateOf("") }
    var loading by remember(selection.sentence) { mutableStateOf(true) }

    LaunchedEffect(selection.word, selection.sentence, selection.wordOffsetInSentence) {
        lookup = withContext(Dispatchers.IO) {
            runCatching { dictionary.lookupSafe(selection.word, selection.sentence, selection.wordOffsetInSentence) }.getOrNull()
        }
    }

    LaunchedEffect(selection.sentence) {
        loading = true
        sentenceTranslation = ""
        sentenceSource = ""
        sentenceError = ""
        offlineGloss = withContext(Dispatchers.IO) {
            runCatching { offlineSentenceGloss(dictionary, selection.sentence, selection.word) }.getOrDefault("")
        }
        val translated = withContext(Dispatchers.IO) {
            SentenceTranslationManager.translate(
                context = context,
                sentence = selection.sentence,
                allowNetwork = !settings.safeMode,
            )
        }
        sentenceTranslation = translated.text
        sentenceSource = translated.source
        sentenceError = translated.error
        loading = false
    }

    val definitions = lookup?.result?.definitions.orEmpty()
    val quickMeaning = definitions.take(3).joinToString("；")
    val night = palette.background == Color(0xFF1D1C1A)
    val cardColor = if (night) Color(0xFF2A2825) else Color(0xFFFFFCF7)
    val translationColor = if (night) Color(0xFF34312D) else Color(0xFFF0F5EA)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        selection.word,
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                    )
                    Text(
                        quickMeaning.ifBlank { if (lookup == null) "正在查词…" else "离线词典暂未命中" },
                        color = palette.text.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = { tts.speak(selection.word) },
                    modifier = Modifier.size(42.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("🔊", fontSize = 18.sp) }
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.size(38.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("×", color = palette.text.copy(alpha = 0.58f), fontSize = 23.sp) }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = palette.text.copy(alpha = 0.10f))
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("整句翻译", color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { tts.speak(selection.sentence) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                ) { Text("🔊 阅读整句", fontSize = 11.sp, color = accent) }
            }
            Text(
                selection.sentence,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = palette.text.copy(alpha = 0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = translationColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    when {
                        sentenceTranslation.isNotBlank() -> {
                            Text(
                                sentenceTranslation,
                                color = if (night) Color(0xFFE4E0D7) else Color(0xFF314A31),
                                fontSize = 16.sp,
                                lineHeight = 23.sp,
                            )
                            if (sentenceSource.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text("${sentenceSource} · ${sourceBook.title}", fontSize = 9.sp, color = palette.text.copy(alpha = 0.46f), maxLines = 1)
                            }
                        }
                        offlineGloss.isNotBlank() -> {
                            Text(
                                offlineGloss,
                                color = palette.text.copy(alpha = 0.76f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (loading) "离线词典先显示 · 自然中文翻译载入中…" else "离线词典辅助",
                                color = palette.text.copy(alpha = 0.44f),
                                fontSize = 9.sp,
                            )
                        }
                        loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = accent)
                            Spacer(Modifier.width(8.dp))
                            Text("正在翻译…", fontSize = 11.sp, color = palette.text.copy(alpha = 0.56f))
                        }
                        else -> Text(
                            sentenceError.ifBlank { "整句翻译暂时不可用" },
                            fontSize = 11.sp,
                            color = palette.text.copy(alpha = 0.58f),
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 3.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenDictionary) {
                    Text("查看全部词义与词典  ›", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SentenceSheet(
    selection: SentenceSelection,
    paragraph: String,
    existingAnnotations: List<TextAnnotation>,
    settings: AppSettings,
    tts: TtsManager,
    onDictionary: (() -> Unit)? = null,
    onAnnotation: (Int, Int, String?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedColor by remember(selection.paragraphIndex, selection.wordStart) { mutableStateOf("yellow") }
    var showNote by remember(selection.paragraphIndex, selection.wordStart) { mutableStateOf(false) }
    var showMore by remember(selection.paragraphIndex, selection.wordStart) { mutableStateOf(false) }
    var note by remember(selection.paragraphIndex, selection.wordStart) { mutableStateOf("") }

    val wordStart = selection.wordStart.coerceIn(0, paragraph.length)
    val wordEnd = selection.wordEnd.coerceIn(wordStart, paragraph.length)
    val sentenceStart = selection.sentenceStart.coerceIn(0, paragraph.length)
    val sentenceEnd = selection.sentenceEnd.coerceIn(sentenceStart, paragraph.length)
    val selectedText = paragraph.substring(wordStart, wordEnd).trim().ifBlank { selection.word }
    val existingWord = existingAnnotations.firstOrNull {
        it.startOffset == wordStart && it.endOffset == wordEnd
    }

    LaunchedEffect(existingWord?.color, existingWord?.note) {
        selectedColor = existingWord?.color?.takeIf { it != "underline" } ?: "yellow"
        note = existingWord?.note.orEmpty()
    }

    fun copySelected() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Lirelia", selectedText))
        Toast.makeText(context, "已复制：$selectedText", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    // True floating tool palette: anchor it near the pressed word whenever
    // window coordinates are available. It never dims or replaces the page.
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val popupWidthPx = with(density) { 340.dp.roundToPx() }
    val edgePx = with(density) { 10.dp.roundToPx() }
    val estimatedHeightPx = with(density) { 175.dp.roundToPx() }
    val screenWidthPx = with(density) { config.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.roundToPx() }
    val anchored = selection.anchorX > 0f && selection.anchorY > 0f
    val anchorOffset = if (anchored) {
        val x = (selection.anchorX.toInt() - popupWidthPx / 2)
            .coerceIn(edgePx, (screenWidthPx - popupWidthPx - edgePx).coerceAtLeast(edgePx))
        val yBelow = selection.anchorY.toInt() + with(density) { 22.dp.roundToPx() }
        val yAbove = selection.anchorY.toInt() - estimatedHeightPx - with(density) { 22.dp.roundToPx() }
        val y = if (yBelow + estimatedHeightPx < screenHeightPx - edgePx) yBelow else yAbove.coerceAtLeast(edgePx)
        IntOffset(x, y)
    } else IntOffset.Zero

    Popup(
        alignment = if (anchored) Alignment.TopStart else Alignment.Center,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 286.dp, max = 340.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        selectedText,
                        modifier = Modifier.weight(1f),
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("长按工具", fontSize = 10.sp, color = Color.Gray)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        "green" to Color(0xFFA9C47F),
                        "yellow" to Color(0xFFFFB21A),
                        "pink" to Color(0xFFD895BF),
                        "gray" to Color(0xFFB9BBC0),
                        "blue" to Color(0xFF75C7E8),
                    ).forEach { (name, color) ->
                        Box(
                            Modifier
                                .width(52.dp)
                                .height(22.dp)
                                .background(color, RoundedCornerShape(99.dp))
                                .border(
                                    if (selectedColor == name) 2.dp else 0.dp,
                                    if (selectedColor == name) Color(0xFF5E5A55) else Color.Transparent,
                                    RoundedCornerShape(99.dp),
                                )
                                .clickable {
                                    selectedColor = name
                                    onAnnotation(wordStart, wordEnd, name, note)
                                    Toast.makeText(context, "已标记 $selectedText", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFDADBE1))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CompactSelectionAction("复制") { copySelected() }
                    CompactSelectionAction("划线") {
                        onAnnotation(wordStart, wordEnd, selectedColor, note)
                        onDismiss()
                    }
                    CompactSelectionAction("笔记") {
                        showNote = !showNote
                        showMore = false
                    }
                    CompactSelectionAction("词典") {
                        if (onDictionary != null) onDictionary() else onDismiss()
                    }
                    CompactSelectionAction("更多") {
                        showMore = !showMore
                        showNote = false
                    }
                }

                if (showNote) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("给“$selectedText”做笔记") },
                        minLines = 2,
                        maxLines = 4,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onAnnotation(wordStart, wordEnd, selectedColor, note)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("保存") }
                        if (existingWord != null) {
                            TextButton(
                                onClick = {
                                    onAnnotation(wordStart, wordEnd, null, "")
                                    onDismiss()
                                }
                            ) { Text("移除标注") }
                        }
                    }
                }

                if (showMore) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAnnotation(sentenceStart, sentenceEnd, selectedColor, "")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) { Text("标整句", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = {
                                onAnnotation(wordStart, wordEnd, "underline", note)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) { Text("下划线", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = {
                                onAnnotation(wordStart, wordEnd, null, "")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) { Text("清除当前位置", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSelectionAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF30343B))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DictionarySheet(
    selection: WordSelection,
    sourceBook: BookItem,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    settings: AppSettings,
    initialMarkColor: String?,
    onDismiss: () -> Unit,
    onSaveVocab: (DictionaryResult) -> Unit,
    onWordMark: (String?) -> Unit,
) {
    var activeWord by remember(selection.word) { mutableStateOf(selection.word) }
    var result by remember(selection.word, selection.sentence, selection.wordOffsetInSentence) { mutableStateOf<DictionaryResult?>(null) }
    var dictionarySource by remember(selection.word) { mutableStateOf("内置离线词典") }
    var dictionaryError by remember(selection.word) { mutableStateOf<String?>(null) }
    var wordFallbackLoading by remember(selection.word) { mutableStateOf(false) }
    var wordFallbackError by remember(selection.word) { mutableStateOf("") }
    var saved by remember(selection.word) { mutableStateOf(false) }
    var masteryStatus by remember(selection.word) { mutableStateOf<String?>(null) }
    var markColor by remember(selection.paragraphIndex, selection.startOffset, selection.endOffset) { mutableStateOf(initialMarkColor) }
    var phraseMeanings by remember(selection.word) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var relatedMeanings by remember(selection.word) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var autoDetectedPhrases by remember(selection.word, selection.sentence) { mutableStateOf<List<DictionaryResult>>(emptyList()) }
    var sentenceTranslation by remember(selection.sentence) { mutableStateOf("") }
    var sentenceTranslationSource by remember(selection.sentence) { mutableStateOf("") }
    var sentenceTranslationError by remember(selection.sentence) { mutableStateOf("") }
    var sentenceTranslationLoading by remember(selection.sentence) { mutableStateOf(false) }
    var sentenceDictionaryGloss by remember(selection.sentence) { mutableStateOf("") }
    var expressionRefresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(activeWord, selection.sentence, selection.wordOffsetInSentence) {
        // Core UX rule: one tap = lookup + pronunciation. The button remains for replay only.
        tts.speak(activeWord)
        result = null
        dictionaryError = null

        val lookup = withContext(Dispatchers.IO) {
            runCatching {
                dictionary.lookupSafe(
                    activeWord,
                    if (activeWord.equals(selection.word, ignoreCase = true)) selection.sentence else "",
                    if (activeWord.equals(selection.word, ignoreCase = true)) selection.wordOffsetInSentence else -1,
                )
            }
                .getOrElse {
                    DictionaryLookup(
                        result = DictionaryResult(
                            query = activeWord,
                            lemma = activeWord,
                            pos = "",
                            ipa = "",
                            definitions = emptyList(),
                            examples = emptyList(),
                            related = emptyList(),
                            phrases = emptyList(),
                        ),
                        source = "内置离线词典",
                        error = it.message ?: "未知词典错误",
                    )
                }
        }

        result = lookup.result
        dictionarySource = lookup.source
        dictionaryError = lookup.error
        wordFallbackError = ""

        // V3.15: the compact bundled dictionary is broad but still has real holes
        // (even common words such as "triste" may be absent). When every local
        // source misses, reuse the existing free translation fallback for the word
        // itself. This is a dictionary fallback, not an AI action.
        // Successful network results are cached by
        // SentenceTranslationManager, so later lookups work without the network.
        if (lookup.result.definitions.isEmpty()) {
            wordFallbackLoading = true
            val fallback = withContext(Dispatchers.IO) {
                SentenceTranslationManager.translate(
                    context = context,
                    sentence = activeWord,
                    allowNetwork = !settings.safeMode,
                )
            }
            val useful = fallback.text.trim().takeIf { translated ->
                translated.isNotBlank() &&
                    translated.any { ch -> ch in '\u3400'..'\u9FFF' } &&
                    !translated.equals(activeWord, ignoreCase = true)
            }
            if (useful != null) {
                result = lookup.result.copy(definitions = listOf(useful))
                dictionarySource = when (fallback.source) {
                    "翻译缓存" -> "本地词义缓存"
                    "网络翻译" -> "联网补充词义 · 已缓存"
                    "网络翻译备用" -> "联网备用词义 · 已缓存"
                    else -> fallback.source.ifBlank { "补充词义" }
                }
            } else {
                wordFallbackError = fallback.error
            }
            wordFallbackLoading = false
        } else {
            wordFallbackLoading = false
        }

        saved = withContext(Dispatchers.IO) {
            runCatching { learningStore.containsVocabulary(activeWord) }.getOrDefault(false)
        }
        masteryStatus = withContext(Dispatchers.IO) {
            runCatching { learningStore.vocabularyStatus(activeWord) }.getOrNull()
        }
    }

    fun requestSentenceTranslation() {
        if (sentenceTranslationLoading) return
        scope.launch {
            sentenceTranslationLoading = true
            sentenceTranslationError = ""
            val translated = withContext(Dispatchers.IO) {
                SentenceTranslationManager.translate(
                    context = context,
                    sentence = selection.sentence,
                    allowNetwork = !settings.safeMode,
                )
            }
            sentenceTranslation = translated.text
            sentenceTranslationSource = translated.source
            sentenceTranslationError = translated.error
            sentenceTranslationLoading = false
        }
    }

    suspend fun compactChineseMeaning(item: String): String {
        val offline = withContext(Dispatchers.IO) { runCatching { dictionary.phraseMeaningOffline(item) }.getOrDefault("") }
        if (offline.isNotBlank()) return offline
        if (settings.safeMode) return "中文释义暂缺"
        val translated = withContext(Dispatchers.IO) {
            SentenceTranslationManager.translate(
                context = context, sentence = item, allowNetwork = true,
            )
        }
        return translated.text.trim().takeIf { it.any { ch -> ch in '\u3400'..'\u9FFF' } } ?: "中文释义暂缺"
    }

    LaunchedEffect(result?.phrases, result?.related) {
        val r = result ?: return@LaunchedEffect
        phraseMeanings = r.phrases.take(8).associateWith { compactChineseMeaning(it) }
        relatedMeanings = r.related.take(12).associateWith { compactChineseMeaning(it) }
    }

    LaunchedEffect(selection.word, selection.sentence, selection.wordOffsetInSentence) {
        val candidates = phraseCandidatesAround(selection.sentence, selection.wordOffsetInSentence, selection.word)
        autoDetectedPhrases = withContext(Dispatchers.IO) {
            candidates.mapNotNull { phrase ->
                val hit = runCatching { dictionary.lookupSafe(phrase).result }.getOrNull() ?: return@mapNotNull null
                if (hit.definitions.isEmpty()) return@mapNotNull null
                val normalizedPhrase = normalizeReaderWord(phrase)
                val normalizedHit = normalizeReaderWord(hit.query.ifBlank { hit.lemma })
                if (normalizedHit == normalizedPhrase || normalizeReaderWord(hit.lemma) == normalizedPhrase) hit else null
            }.distinctBy { normalizeReaderWord(it.query.ifBlank { it.lemma }) }.take(5)
        }
    }

    LaunchedEffect(selection.sentence) {
        sentenceDictionaryGloss = withContext(Dispatchers.IO) {
            offlineSentenceGloss(dictionary, selection.sentence, selection.word)
        }
        requestSentenceTranslation()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFCF7),
        modifier = Modifier.imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(activeWord, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                        result?.let { r ->
                            val gender = nounGenderLabel(r, selection.sentence, activeWord)
                            val posWithGender = friendlyDictionaryPos(r).takeIf { it.isNotBlank() }?.let { pos ->
                                if (pos.split(" · ").contains("名词")) "名词（${gender ?: "性别未收录"}）" else pos
                            }
                            val meta = listOf(
                                r.lemma.takeIf { !it.equals(activeWord, true) },
                                posWithGender,
                                r.ipa.takeIf { it.isNotBlank() },
                            ).filterNotNull().joinToString("  ·  ")
                            if (meta.isNotBlank()) Text(meta, color = Color.Gray, fontSize = 13.sp)
                        }
                        Text(
                            when {
                                dictionarySource.contains("联网") -> "🌐 $dictionarySource"
                                dictionarySource.contains("缓存") -> "💾 $dictionarySource · 可离线复用"
                                else -> "📚 $dictionarySource · 查词完全离线"
                            },
                            color = if (dictionarySource.contains("联网")) Color(0xFF7A6547) else Color(0xFF557A55),
                            fontSize = 11.sp,
                        )
                        if (!activeWord.equals(selection.word, ignoreCase = true)) {
                            TextButton(
                                onClick = { activeWord = selection.word },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) { Text("← 返回原词 ${selection.word}", fontSize = 12.sp) }
                        }
                    }
                }
            }

            result?.let { r ->
                if (activeWord.equals(selection.word, ignoreCase = true)) {
                    item {
                        Text("快速标记", fontWeight = FontWeight.SemiBold, color = Accent)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(
                                "yellow" to "黄", "green" to "绿", "blue" to "蓝", "pink" to "粉",
                            ).forEach { (colorName, label) ->
                                OutlinedButton(
                                    onClick = {
                                        if (markColor == colorName) {
                                            onWordMark(null)
                                            learningStore.removeVocabulary(activeWord)
                                            saved = false
                                            masteryStatus = null
                                            markColor = null
                                        } else {
                                            onSaveVocab(r)
                                            learningStore.markLearning(activeWord)
                                            learningStore.setVocabularyMarkerColor(activeWord, colorName)
                                            saved = true
                                            masteryStatus = "learning"
                                            markColor = colorName
                                            onWordMark(colorName)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp),
                                ) { Text(if (markColor == colorName) "✓$label" else label, fontSize = 12.sp) }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (markColor == "underline") {
                                        onWordMark(null); markColor = null
                                    } else {
                                        markColor = "underline"
                                        onWordMark("underline")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp),
                            ) { Text(if (markColor == "underline") "✓U" else "U", textDecoration = TextDecoration.Underline, fontSize = 12.sp) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { tts.speak(activeWord) }, modifier = Modifier.weight(1f)) { Text("🔊 发音") }
                            OutlinedButton(
                                onClick = {
                                    val removed = learningStore.removeWordEverywhere(activeWord)
                                    // Refresh the current reader caches after the global deletion.
                                    onWordMark(null)
                                    saved = false
                                    masteryStatus = null
                                    markColor = null
                                    Toast.makeText(
                                        context,
                                        if (removed > 0) "已清除“$activeWord”的全部标记（$removed 处）" else "已清除“$activeWord”的生词状态",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("⌫ 清除该词全部") }
                        }
                        Text("颜色高亮会自动加入生词本，并在其他书中浅色提醒；U 下划线只标当前位置。“清除该词全部”会移除所有书中这个词的单词标记与生词状态，不会误删包含它的整句笔记。", fontSize = 10.sp, color = Color.Gray, lineHeight = 15.sp)
                    }
                }

                if (!dictionaryError.isNullOrBlank()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D9))) {
                            Column(Modifier.padding(12.dp)) {
                                Text("完整词典暂时没有成功打开", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "已自动切换到内置核心词典，阅读器不会退出。${dictionaryError?.let { "（$it）" } ?: ""}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B625C),
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }

                if (r.definitions.isEmpty()) {
                    item {
                        when {
                            wordFallbackLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("离线词典未命中，正在补充词义…", color = Color.Gray, fontSize = 13.sp)
                            }
                            settings.safeMode -> Text(
                                "离线词典暂未找到这个词。安全模式已开启，因此不会联网补充词义。",
                                color = Color.Gray,
                                fontSize = 13.sp,
                            )
                            else -> Text(
                                wordFallbackError.ifBlank { "离线词典和联网补充暂时都没有找到这个词。" },
                                color = Color.Gray,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    item { Text("中文释义", fontWeight = FontWeight.SemiBold, color = Accent) }
                    items(r.definitions) { d -> Text("• $d", fontSize = 16.sp, lineHeight = 24.sp) }
                }

                item {
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("整句翻译", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Accent)
                        OutlinedButton(
                            onClick = { tts.speak(selection.sentence) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) { Text("🔊 阅读整句", fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        selection.sentence,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFF6B625C),
                    )
                    Spacer(Modifier.height(7.dp))

                    when {
                        sentenceTranslationLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("正在翻译整句…", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        sentenceTranslation.isNotBlank() -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    sentenceTranslation,
                                    Modifier.padding(12.dp),
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = Color(0xFF314A31),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "翻译来源：${sentenceTranslationSource.ifBlank { "缓存/本地" }} · ${sourceBook.title}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                            )
                        }
                        else -> {
                            Text(
                                sentenceTranslationError.ifBlank { "整句翻译暂时不可用。" },
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                            TextButton(onClick = { requestSentenceTranslation() }) {
                                Text("重试整句翻译")
                            }
                        }
                    }

                    if (sentenceDictionaryGloss.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F1EA)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("离线词典辅助", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                                Text(
                                    sentenceDictionaryGloss,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF625A53),
                                )
                                Text("逐词词义辅助，断网时也能看；它不会冒充整句机器翻译。", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }


                FrenchConjugator.conjugate(r.lemma, r.pos)?.let { card ->
                    item {
                        HorizontalDivider()
                        Text("动词变位", fontWeight = FontWeight.SemiBold, color = Accent)
                        Text(card.note, color = Color.Gray, fontSize = 11.sp)
                    }
                    items(card.sections) { section ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(section.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.height(5.dp))
                                section.forms.forEach { (subject, form) ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(subject, modifier = Modifier.width(92.dp), color = Color.Gray, fontSize = 13.sp)
                                        Text(form, fontFamily = FontFamily.Serif, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (r.examples.isNotEmpty()) {
                    item { HorizontalDivider(); Text("例句", fontWeight = FontWeight.SemiBold, color = Accent) }
                    items(r.examples.take(4)) { ex -> Text(ex, fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF544C46)) }
                }

                val phraseRows = buildList<Pair<String, String>> {
                    autoDetectedPhrases.forEach { hit ->
                        val phrase = hit.query.ifBlank { hit.lemma }
                        val meaning = hit.definitions.firstOrNull().orEmpty().ifBlank { "中文释义暂缺" }
                        if (phrase.isNotBlank()) add(phrase to meaning)
                    }
                    r.phrases
                        .filter { selection.sentence.contains(it, ignoreCase = true) }
                        .forEach { phrase -> add(phrase to (phraseMeanings[phrase] ?: "正在补充中文…")) }
                }.distinctBy { normalizeReaderWord(it.first) }.take(6)

                if (phraseRows.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("自动识别短语", fontWeight = FontWeight.SemiBold, color = Color(0xFF557A55))
                                Text(
                                    "围绕你点到的词检查 2–5 词固定表达；只查离线词典，不会为识别调用 AI。",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                )
                                Spacer(Modifier.height(6.dp))
                                phraseRows.forEach { (phrase, meaning) ->
                                    val savedExpression = expressionRefresh >= 0 && learningStore.containsExpression(phrase)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(
                                            onClick = { activeWord = phrase },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(phrase, fontFamily = FontFamily.Serif)
                                                Text(meaning, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                                            }
                                            Text("›")
                                        }
                                        TextButton(
                                            onClick = {
                                                if (savedExpression) {
                                                    learningStore.removeExpression(phrase)
                                                } else {
                                                    learningStore.addExpression(
                                                        expression = phrase,
                                                        definition = meaning,
                                                        contextText = selection.sentence,
                                                        bookId = sourceBook.id,
                                                        bookTitle = sourceBook.title,
                                                    )
                                                }
                                                expressionRefresh++
                                            },
                                        ) { Text(if (savedExpression) "✓ 已加入" else "+ 表达") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (r.phrases.isNotEmpty()) {
                    item { Text("相关表达", fontWeight = FontWeight.SemiBold, color = Accent) }
                    items(r.phrases.take(8)) { p ->
                        Text(
                            "· $p  ——  ${phraseMeanings[p] ?: "正在补充中文…"}",
                            fontSize = 14.sp, lineHeight = 21.sp,
                        )
                    }
                }

                if (r.related.isNotEmpty()) {
                    item {
                        Text("同词族 / 相关词", fontWeight = FontWeight.SemiBold, color = Accent)
                        Text("现在可以直接点开，不用退出查词卡。", color = Color.Gray, fontSize = 11.sp)
                    }
                    items(r.related.take(12)) { related ->
                        OutlinedButton(
                            onClick = { activeWord = related },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(related, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(relatedMeanings[related] ?: "正在补充中文…", fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                            }
                            Text("›")
                        }
                    }
                }

            } ?: item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("正在查本地法中词典…")
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
        }
    }
}


@Composable
private fun DictionaryCenterScreen(
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    settings: AppSettings,
    store: DictionaryCenterStore,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var activeWord by rememberSaveable { mutableStateOf("") }
    var lookup by remember { mutableStateOf<DictionaryLookup?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var favoriteRefresh by remember { mutableStateOf(0) }
    var historyRefresh by remember { mutableStateOf(0) }
    var vocabRefresh by remember { mutableStateOf(0) }
    var extensionRefresh by remember { mutableStateOf(0) }
    var extensionBusy by remember { mutableStateOf(false) }
    var extensionProgress by remember { mutableStateOf(0) }
    var extensionMessage by remember { mutableStateOf("") }
    var phraseMeanings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val history = remember(historyRefresh) { store.history(20) }
    val favorites = remember(favoriteRefresh) { store.favorites().sorted() }

    LaunchedEffect(lookup?.result?.phrases, settings.safeMode) {
        val phrases = lookup?.result?.phrases.orEmpty().take(10)
        val offline = withContext(Dispatchers.IO) {
            phrases.associateWith { phrase ->
                runCatching { dictionary.phraseMeaningOffline(phrase) }.getOrDefault("").trim()
            }
        }
        phraseMeanings = offline.mapValues { (_, value) -> value.ifBlank { "中文释义暂缺" } }

        if (!settings.safeMode) {
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

    fun lookupWord(raw: String, speak: Boolean = true) {
        val word = raw.trim()
        if (word.isBlank()) return

        query = word
        activeWord = word
        suggestions = emptyList()
        loading = true

        if (speak) tts.speak(word)

        scope.launch {
            val local = withContext(Dispatchers.IO) { dictionary.lookupSafe(word) }
            val isMultiword = word.any { it.isWhitespace() }

            if (local.result.definitions.isNotEmpty() || !isMultiword) {
                lookup = local
                loading = false
            } else {
                // Multi-word mode is intentionally local-first. Show lexical help
                // immediately, then enrich it with normal sentence translation when
                // available. A blocked Google connection therefore never makes the
                // dictionary look frozen.
                val offline = withContext(Dispatchers.IO) { offlineSentenceGloss(dictionary, word, "") }
                val base = DictionaryLookup(
                    result = DictionaryResult(
                        query = word,
                        lemma = word,
                        pos = if (READER_FRENCH_TOKEN.findAll(word).count() >= 5) "句子" else "短语",
                        ipa = "",
                        definitions = if (offline.isBlank()) emptyList() else listOf("离线词典辅助：$offline"),
                        examples = emptyList(),
                        related = emptyList(),
                        phrases = emptyList(),
                    ),
                    source = if (offline.isBlank()) "离线词典" else "离线词典辅助",
                    error = local.error,
                )
                lookup = base
                loading = false

                if (!settings.safeMode) {
                    val translated = withContext(Dispatchers.IO) {
                        SentenceTranslationManager.translate(
                            context = context,
                            sentence = word,
                            allowNetwork = true,
                        )
                    }
                    val natural = translated.text.trim().takeIf { text ->
                        text.isNotBlank() && text.any { ch -> ch in '\u3400'..'\u9FFF' }
                    }
                    if (natural != null) {
                        lookup = base.copy(
                            result = base.result.copy(
                                definitions = buildList {
                                    add(natural)
                                    if (offline.isNotBlank()) add("离线词典辅助：$offline")
                                }
                            ),
                            source = "${translated.source.ifBlank { "整句翻译" }} + 离线词典辅助",
                            error = translated.error.takeIf { it.isNotBlank() },
                        )
                    }
                }
            }

            store.recordLookup(word)
            historyRefresh++
            vocabRefresh++
        }
    }

    LaunchedEffect(query, activeWord) {
        val clean = query.trim()
        if (
            clean.isBlank() ||
            clean.any { it.isWhitespace() } ||
            clean.equals(activeWord, ignoreCase = true)
        ) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(100)
        suggestions = withContext(Dispatchers.IO) {
            dictionary.suggest(clean, 12)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "离线法语词典",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    remember(extensionRefresh) { dictionary.stats() },
                    fontSize = 11.sp,
                    color = Color(0xFF557A55),
                )
            }
            Text(
                "本地优先",
                fontSize = 11.sp,
                color = Color(0xFF557A55),
                modifier = Modifier
                    .background(Color(0xFFEAF2E6), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("输入法语单词 / 短语 / 句子") },
                placeholder = { Text("on / emploi du temps / Je comprends…") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { lookupWord(query) },
                enabled = query.isNotBlank() && !loading,
            ) {
                Text("查询")
            }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(8.dp)) {
                    suggestions.forEach { suggestion ->
                        TextButton(
                            onClick = { lookupWord(suggestion) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                suggestion,
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                            )
                            Text("›")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        if (activeWord.isBlank() && lookup == null) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("发音", fontWeight = FontWeight.SemiBold)
                            Text(
                                tts.offlineFrenchEngineStatus(),
                                fontSize = 11.sp,
                                color = Color(0xFF557A55),
                                lineHeight = 17.sp,
                            )
                            Text(
                                "当前路线：${tts.routeLabel()}。成功播放过的网络音频会按设置保存到本地缓存。",
                                fontSize = 10.sp,
                                color = Color.Gray,
                            )
                        }
                    }
                }

                item {
                    val installed = remember(extensionRefresh) { dictionary.extensionInstalled() }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2EA)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("WikDict 法中词典", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        remember(extensionRefresh) { dictionary.extensionDescription() },
                                        fontSize = 10.sp,
                                        color = Color(0xFF746B63),
                                        lineHeight = 15.sp,
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (!extensionBusy) {
                                            extensionBusy = true
                                            extensionProgress = 0
                                            extensionMessage = "正在下载 WikDict 官方法中 SQLite（约 3 MB）…"
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    dictionary.installOrUpdateExtension { count ->
                                                        scope.launch { extensionProgress = count }
                                                    }
                                                }
                                                extensionBusy = false
                                                result.onSuccess { count ->
                                                    extensionMessage = "WikDict 已安装 · $count 个法语词头"
                                                    extensionRefresh++
                                                }.onFailure { error ->
                                                    extensionMessage = error.message ?: "扩展词典安装失败"
                                                }
                                            }
                                        }
                                    },
                                    enabled = !extensionBusy,
                                ) {
                                    Text(if (installed) "更新" else "安装")
                                }
                            }
                            if (extensionBusy) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    if (extensionProgress > 0) "已校验 $extensionProgress 个词头" else extensionMessage,
                                    fontSize = 10.sp,
                                    color = Accent,
                                )
                            } else if (extensionMessage.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(extensionMessage, fontSize = 10.sp, color = Color(0xFF557A55))
                            }
                            Text(
                                "采用混合查询：Lirelia 主词典负责词性、IPA、词形、例句与短语；WikDict 的 Wiktionary/DBnary 中文义项会自动合并进同一张词卡，主词典漏词时也可单独兜底。",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }

                if (favorites.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("★ 收藏", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${favorites.size}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    items(favorites.take(20), key = { "fav_$it" }) { word ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lookupWord(word) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    word,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 17.sp,
                                )
                                Text("★", color = Accent)
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("最近查询", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    store.clearHistory()
                                    historyRefresh++
                                }
                            ) { Text("清空", fontSize = 11.sp) }
                        }
                    }
                    items(history, key = { "history_$it" }) { word ->
                        OutlinedButton(
                            onClick = { lookupWord(word) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                word,
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Serif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("›")
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        } else {
            val currentLookup = lookup
            if (loading || currentLookup == null) {
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(9.dp))
                        Text("正在查本地词典…", color = Color.Gray)
                    }
                }
            } else {
                val r = currentLookup.result
                val isFavorite = store.isFavorite(activeWord)
                val vocabStatus = remember(activeWord, vocabRefresh) {
                    learningStore.vocabularyStatus(activeWord)
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            activeWord,
                                            fontSize = if (activeWord.any { it.isWhitespace() }) 21.sp else 31.sp,
                                            lineHeight = if (activeWord.any { it.isWhitespace() }) 28.sp else 36.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Serif,
                                            maxLines = if (activeWord.any { it.isWhitespace() }) 4 else 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                        if (
                                            r.lemma.isNotBlank() &&
                                            !r.lemma.equals(activeWord, ignoreCase = true)
                                        ) {
                                            Text(
                                                "原形：${r.lemma}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF557A55),
                                            )
                                        }

                                        val pos = friendlyDictionaryPos(r)
                                        val gender = nounGenderLabel(r, "", activeWord)
                                        val posWithGender = if (pos.split(" · ").contains("名词")) "名词（${gender ?: "性别未收录"}）" else pos
                                        val meta = listOf(
                                            posWithGender.takeIf { it.isNotBlank() },
                                            r.ipa.takeIf { it.isNotBlank() },
                                        ).filterNotNull().joinToString("  ·  ")

                                        if (meta.isNotBlank()) {
                                            Text(meta, fontSize = 13.sp, color = Color.Gray)
                                        }

                                        Text(
                                            "📚 ${currentLookup.source}",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                        )
                                    }

                                    OutlinedButton(onClick = { tts.speak(activeWord) }) {
                                        Text("🔊")
                                    }
                                    Spacer(Modifier.width(5.dp))
                                    OutlinedButton(
                                        onClick = {
                                            store.toggleFavorite(activeWord)
                                            favoriteRefresh++
                                        }
                                    ) {
                                        Text(if (isFavorite) "★" else "☆")
                                    }
                                }
                            }
                        }
                    }

                    if (r.definitions.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D9)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "离线词典暂未找到这个词或表达。单词可尝试原形；句子仍可使用离线词义辅助。",
                                    Modifier.padding(12.dp),
                                    color = Color(0xFF6B625C),
                                )
                            }
                        }
                    } else {
                        item {
                            Text("中文释义", fontWeight = FontWeight.SemiBold, color = Accent)
                        }
                        items(r.definitions) { definition ->
                            Text(
                                "• $definition",
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                            )
                        }
                    }

                    FrenchConjugator.conjugate(r.lemma, r.pos)?.let { card ->
                        item {
                            HorizontalDivider()
                            Text("动词变位", fontWeight = FontWeight.SemiBold, color = Accent)
                            Text(card.note, fontSize = 10.sp, color = Color.Gray)
                        }
                        items(card.sections) { section ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(11.dp)) {
                                    Text(section.title, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(5.dp))
                                    section.forms.forEach { (subject, form) ->
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                subject,
                                                modifier = Modifier.width(90.dp),
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                            )
                                            Text(
                                                form,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (r.examples.isNotEmpty()) {
                        item {
                            HorizontalDivider()
                            Text("例句", fontWeight = FontWeight.SemiBold, color = Accent)
                        }
                        items(r.examples.take(5)) { example ->
                            val display = DictionaryDisplayFormatter.example(example)
                            if (display.french.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        display.french,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                    )
                                    if (display.chinese.isNotBlank()) {
                                        Text(
                                            display.chinese,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = Color.Gray,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (r.phrases.isNotEmpty()) {
                        item {
                            HorizontalDivider()
                            Text("常用表达", fontWeight = FontWeight.SemiBold, color = Accent)
                            Text(
                                "点表达继续用同一套离线词典查询。",
                                fontSize = 10.sp,
                                color = Color.Gray,
                            )
                        }
                        items(r.phrases.take(10)) { phrase ->
                            OutlinedButton(
                                onClick = { lookupWord(phrase) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        phrase,
                                        fontFamily = FontFamily.Serif,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        phraseMeanings[phrase] ?: "正在补充中文…",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text("›")
                            }
                        }
                    }

                    if (r.related.isNotEmpty()) {
                        item {
                            Text("同词族 / 相关词", fontWeight = FontWeight.SemiBold, color = Accent)
                        }
                        items(r.related.take(14)) { related ->
                            TextButton(
                                onClick = { lookupWord(related) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    related,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = FontFamily.Serif,
                                )
                                Text("›")
                            }
                        }
                    }

                    item {
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Button(
                                onClick = {
                                    learningStore.addVocabulary(
                                        r,
                                        contextText = "",
                                        bookId = "__dictionary__",
                                        bookTitle = "词典中心",
                                    )
                                    learningStore.markLearning(activeWord)
                                    vocabRefresh++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (vocabStatus == "learning") Color(0xFF557A55)
                                        else Accent
                                ),
                            ) {
                                Text(if (vocabStatus == "learning") "✓ 学习中" else "＋ 生词")
                            }

                            OutlinedButton(
                                onClick = {
                                    learningStore.addVocabulary(
                                        r,
                                        contextText = "",
                                        bookId = "__dictionary__",
                                        bookTitle = "词典中心",
                                    )
                                    learningStore.markKnown(activeWord)
                                    vocabRefresh++
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (vocabStatus == "known") "✓ 已掌握" else "认识")
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                activeWord = ""
                                lookup = null
                                query = ""
                                suggestions = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("返回词典首页")
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun VocabularyScreen(
    learningStore: LearningStore,
    tts: TtsManager,
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adaptiveStore = remember { com.cy.languagereader.mobile.data.AdaptiveTrainingStore(context) }
    var allItems by remember { mutableStateOf(learningStore.vocabulary()) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(VocabFilter.DUE) }
    var selectedBookId by rememberSaveable { mutableStateOf("") }
    var quizItems by remember { mutableStateOf<List<VocabularyItem>>(emptyList()) }
    var quizIndex by remember { mutableStateOf(0) }
    var quizReveal by remember { mutableStateOf(false) }
    var quizStartedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var quizAutoPronounce by remember { mutableStateOf(settings.vocabReviewAutoPronounce) }

    LaunchedEffect(Unit) {
        allItems = withContext(Dispatchers.IO) { learningStore.vocabulary() }
    }

    val bookSources = remember(allItems) {
        allItems
            .filter { it.bookId.isNotBlank() && it.bookTitle.isNotBlank() }
            .distinctBy { it.bookId }
            .sortedBy { it.bookTitle.lowercase() }
    }

    val shown = remember(allItems, query, filter, selectedBookId) {
        val now = System.currentTimeMillis()
        allItems.filter { item ->
            val statusOk = when (filter) {
                VocabFilter.DUE -> item.status != "known" && (item.nextReviewAt == 0L || item.nextReviewAt <= now)
                VocabFilter.LEARNING -> item.status != "known"
                VocabFilter.KNOWN -> item.status == "known"
                VocabFilter.ALL -> true
            }
            val bookOk = selectedBookId.isBlank() || item.bookId == selectedBookId
            val queryOk = query.isBlank() ||
                item.word.contains(query, true) ||
                item.lemma.contains(query, true) ||
                item.definition.contains(query, true) ||
                item.context.contains(query, true) ||
                item.bookTitle.contains(query, true)
            statusOk && bookOk && queryOk
        }
    }

    val dueCount = allItems.count {
        it.status != "known" && (it.nextReviewAt == 0L || it.nextReviewAt <= System.currentTimeMillis())
    }
    val learningCount = allItems.count { it.status != "known" }
    val knownCount = allItems.count { it.status == "known" }

    Column(modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("词汇学习中心", fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${allItems.size} 个追踪词 · $learningCount 学习中 · $knownCount 已掌握",
                    color = Color.Gray, fontSize = 12.sp,
                )
            }
            OutlinedButton(
                onClick = {
                    val text = shown.joinToString("\n") { item ->
                        buildString {
                            append(item.word)
                            if (item.lemma.isNotBlank() && !item.lemma.equals(item.word, true)) append(" [${item.lemma}]")
                            if (item.definition.isNotBlank()) append(" — ${item.definition}")
                            if (item.bookTitle.isNotBlank()) append("  📖${item.bookTitle}")
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Lirelia 词表", text))
                    Toast.makeText(context, "已复制当前词表（${shown.size} 条）", Toast.LENGTH_SHORT).show()
                },
                enabled = shown.isNotEmpty(),
            ) { Text("复制") }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = {
                    quizItems = allItems
                        .filter {
                            it.status != "known" &&
                                (it.nextReviewAt == 0L || it.nextReviewAt <= System.currentTimeMillis())
                        }
                        .take(30)
                    quizIndex = 0
                    quizReveal = false
                    quizStartedAt = System.currentTimeMillis()
                },
                enabled = dueCount > 0,
            ) { Text("复习 $dueCount") }
        }

        Spacer(Modifier.height(10.dp))

        val reviewedToday = learningStore.reviewedTodayCount()
        val reviewGoal = settings.dailyVocabReviewGoal.coerceAtLeast(1)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(11.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("今日复习", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        "$reviewedToday / $reviewGoal",
                        color = if (reviewedToday >= reviewGoal) Color(0xFF557A55) else Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { (reviewedToday.toFloat() / reviewGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val masteryNew = allItems.count { it.status != "known" && it.reviewCount == 0 }
        val masteryEarly = allItems.count { it.status != "known" && it.reviewCount in 1..2 }
        val masteryStrong = allItems.count { it.status != "known" && it.reviewCount >= 3 }
        val masteryKnown = allItems.count { it.status == "known" }
        val masteryTotal = allItems.size.coerceAtLeast(1)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(11.dp)) {
                Text("词汇掌握结构", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                listOf(
                    "新加入" to masteryNew,
                    "巩固中" to masteryEarly,
                    "较熟悉" to masteryStrong,
                    "已掌握" to masteryKnown,
                ).forEach { (label, count) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(56.dp), fontSize = 11.sp)
                        LinearProgressIndicator(
                            progress = { count.toFloat() / masteryTotal },
                            modifier = Modifier.weight(1f).height(6.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text("$count", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val vocabTrend = remember(allItems) { vocabularyDailySeries(allItems, 14) }
        val maxAdded = vocabTrend.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(11.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("近 14 天新增词汇", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        "${vocabTrend.sumOf { it.second }} 个",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    vocabTrend.forEach { (_, count) ->
                        val fraction = count.toFloat() / maxAdded
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight((0.08f + 0.92f * fraction).coerceIn(0.08f, 1f))
                                .background(
                                    if (count > 0) Color(0xFF7DA07A) else Color(0xFFE6E0D9),
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item {
                if (selectedBookId.isBlank()) Button(onClick = { selectedBookId = "" }) { Text("全部书籍") }
                else OutlinedButton(onClick = { selectedBookId = "" }) { Text("全部书籍") }
            }
            items(bookSources, key = { it.bookId }) { source ->
                if (selectedBookId == source.bookId) {
                    Button(onClick = { selectedBookId = source.bookId }) {
                        Text(source.bookTitle.take(16), maxLines = 1)
                    }
                } else {
                    OutlinedButton(onClick = { selectedBookId = source.bookId }) {
                        Text(source.bookTitle.take(16), maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { FilterButton("待复习 $dueCount", filter == VocabFilter.DUE) { filter = VocabFilter.DUE } }
            item { FilterButton("学习中 $learningCount", filter == VocabFilter.LEARNING) { filter = VocabFilter.LEARNING } }
            item { FilterButton("已掌握 $knownCount", filter == VocabFilter.KNOWN) { filter = VocabFilter.KNOWN } }
            item { FilterButton("全部", filter == VocabFilter.ALL) { filter = VocabFilter.ALL } }
        }

        Spacer(Modifier.height(9.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索单词 / 中文 / 原句 / 书名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Text("当前显示 ${shown.size} 个", color = Color.Gray, fontSize = 11.sp)

        if (shown.isEmpty()) {
            Text("当前筛选条件下没有词。", color = Color.Gray, modifier = Modifier.padding(top = 26.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(shown, key = { "${it.word.lowercase()}_${it.bookId}" }) { item ->
                    VocabCard(
                        item = item,
                        tts = tts,
                        onReviewed = {
                            learningStore.markReviewed(item.word)
                            allItems = learningStore.vocabulary()
                        },
                        onToggleKnown = {
                            if (item.status == "known") learningStore.markLearning(item.word)
                            else learningStore.markKnown(item.word)
                            allItems = learningStore.vocabulary()
                        },
                        onDelete = {
                            learningStore.removeVocabulary(item.word)
                            allItems = learningStore.vocabulary()
                        },
                    )
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }

    if (quizItems.isNotEmpty() && quizIndex < quizItems.size) {
        val quizItem = quizItems[quizIndex]
        val quizId = "vocab:" + quizItem.word.trim().replace('’', '\'').lowercase(Locale.FRENCH)
        val quizState = adaptiveStore.state(quizId)
        val mastery = (quizState?.mastery ?: 0f).coerceIn(0f, 1f)
        val progress = (quizIndex + 1).toFloat() / quizItems.size.coerceAtLeast(1)

        LaunchedEffect(quizItem.word, quizAutoPronounce) {
            if (quizAutoPronounce) {
                delay(120)
                tts.speak(quizItem.word)
            }
        }

        fun advanceQuiz() {
            quizIndex += 1
            quizReveal = false
            quizStartedAt = System.currentTimeMillis()
            if (quizIndex >= quizItems.size) {
                quizItems = emptyList()
                quizIndex = 0
            }
        }

        fun rateQuiz(correct: Boolean, confidence: Float, known: Boolean = false) {
            val state = adaptiveStore.record(
                itemId = quizId,
                kind = "personal_vocab",
                correct = correct,
                responseMs = (System.currentTimeMillis() - quizStartedAt).coerceIn(250L, 120_000L),
                source = "personal",
                confidence = confidence,
            )
            if (known) {
                learningStore.markKnown(quizItem.word)
            } else {
                learningStore.markLearning(quizItem.word)
                learningStore.syncVocabularyReview(quizItem.word, state.nextDueAt)
            }
            allItems = learningStore.vocabulary()
            advanceQuiz()
        }

        Dialog(
            onDismissRequest = {
                quizItems = emptyList()
                quizIndex = 0
                quizReveal = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFFFFCF7),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("单词复习", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text(
                                "${quizIndex + 1} / ${quizItems.size} · 掌握度 ${(mastery * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }
                        TextButton(
                            onClick = {
                                quizItems = emptyList()
                                quizIndex = 0
                                quizReveal = false
                            }
                        ) { Text("关闭") }
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("自动发音", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("进入下一个单词时自动朗读；关闭后仍可手动点发音。", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = quizAutoPronounce,
                            onCheckedChange = { enabled ->
                                quizAutoPronounce = enabled
                                settings.vocabReviewAutoPronounce = enabled
                            },
                        )
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 18.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F1E9)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 38.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    quizItem.word,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Serif,
                                    textAlign = TextAlign.Center,
                                )
                                if (quizItem.lemma.isNotBlank() && !quizItem.lemma.equals(quizItem.word, true)) {
                                    Text("原形 ${quizItem.lemma}", fontSize = 13.sp, color = Color.Gray)
                                }
                                OutlinedButton(onClick = { tts.speak(quizItem.word) }) { Text("🔊 发音") }
                            }
                        }

                        if (!quizReveal) {
                            Text(
                                "先在脑中想一下它的中文意思，再显示答案。",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = Color.Gray,
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("中文释义", fontSize = 12.sp, color = Color(0xFF557A55), fontWeight = FontWeight.Bold)
                                    Text(
                                        quizItem.definition.ifBlank { "暂无中文释义" },
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 29.sp,
                                    )
                                    if (quizItem.context.isNotBlank()) {
                                        HorizontalDivider()
                                        Text("原句", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            quizItem.context,
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 17.sp,
                                            lineHeight = 26.sp,
                                            color = Color(0xFF5F5953),
                                        )
                                    }
                                    if (quizItem.bookTitle.isNotBlank()) {
                                        Text("📖 ${quizItem.bookTitle}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    if (!quizReveal) {
                        Button(
                            onClick = { quizReveal = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text("显示答案", fontSize = 16.sp) }
                    } else {
                        Text(
                            "你对这个词的感觉？",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { rateQuiz(correct = false, confidence = 1f) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            ) { Text("忘了", fontSize = 13.sp) }
                            OutlinedButton(
                                onClick = { rateQuiz(correct = true, confidence = 0.55f) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            ) { Text("有印象", fontSize = 13.sp) }
                            Button(
                                onClick = { rateQuiz(correct = true, confidence = 1f, known = true) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            ) { Text("掌握", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label, fontSize = 12.sp) }
    else OutlinedButton(onClick = onClick) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun VocabCard(
    item: VocabularyItem,
    tts: TtsManager,
    onReviewed: () -> Unit,
    onToggleKnown: () -> Unit,
    onDelete: () -> Unit,
) {
    val known = item.status == "known"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (known) Color(0xFFF1F6ED) else Color(0xFFFFFCF7)
        ),
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.word, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    if (known) "已掌握" else "学习中",
                    fontSize = 11.sp,
                    color = if (known) Color(0xFF557A55) else Accent,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { tts.speak(item.word) }) { Text("🔊") }
            }
            if (item.lemma.isNotBlank() && !item.lemma.equals(item.word, true)) {
                Text("原形：${item.lemma}", fontSize = 12.sp, color = Color.Gray)
            }
            if (item.definition.isNotBlank()) Text(item.definition, fontSize = 15.sp)
            if (item.context.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.context,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = Color(0xFF6B625C),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.bookTitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text("📖 ${item.bookTitle}", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (known) "不进入复习队列" else reviewStatus(item),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                )
                if (!known) TextButton(onClick = onReviewed) { Text("复习过") }
                TextButton(onClick = onToggleKnown) { Text(if (known) "↩ 学习中" else "✓ 掌握") }
                TextButton(onClick = onDelete) { Text("删除", color = Color(0xFF9A5555)) }
            }
        }
    }
}

@Composable
private fun AnnotationManagerScreen(
    learningStore: LearningStore,
    books: List<BookItem>,
    modifier: Modifier = Modifier,
    onOpenAnnotation: (String, Int) -> Unit = { _, _ -> },
) {
    var items by remember { mutableStateOf(learningStore.allAnnotations()) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedBookId by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf("") }
    var notesOnly by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Pair<String, TextAnnotation>?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val bookNames = remember(books) { books.associate { it.id to it.title } }

    val shown = remember(items, query, selectedBookId, selectedColor, notesOnly) {
        items.filter { (bookId, mark) ->
            (selectedBookId.isBlank() || bookId == selectedBookId) &&
                (selectedColor.isBlank() || mark.color == selectedColor) &&
                (!notesOnly || mark.note.isNotBlank()) &&
                (
                    query.isBlank() ||
                        mark.text.contains(query, true) ||
                        mark.note.contains(query, true) ||
                        bookNames[bookId].orEmpty().contains(query, true)
                )
        }
    }

    val noteCount = shown.count { it.second.note.isNotBlank() }
    val selectedTitle = bookNames[selectedBookId].orEmpty()

    Column(modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("标注与笔记", fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${shown.size} 条 · 其中 $noteCount 条有笔记",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }

            OutlinedButton(
                onClick = {
                    val text = learningStore.exportAnnotationsMarkdown(
                        bookNames = bookNames,
                        onlyBookId = selectedBookId,
                        notesOnly = notesOnly,
                    )
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            if (selectedBookId.isBlank()) "Lirelia 标注.md"
                            else "${selectedTitle.ifBlank { "本书" }}_笔记.md",
                            text,
                        )
                    )
                    Toast.makeText(context, "Markdown 已复制", Toast.LENGTH_SHORT).show()
                }
            ) { Text(if (selectedBookId.isBlank()) "MD" else "本书笔记") }

            Spacer(Modifier.width(5.dp))

            OutlinedButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "Lirelia 标注.csv",
                            learningStore.exportAnnotationsCsv(bookNames, selectedBookId),
                        )
                    )
                    Toast.makeText(context, "CSV 已复制", Toast.LENGTH_SHORT).show()
                }
            ) { Text("CSV") }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item {
                if (selectedBookId.isBlank()) {
                    Button(onClick = { selectedBookId = "" }) { Text("全部书籍") }
                } else {
                    OutlinedButton(onClick = { selectedBookId = "" }) { Text("全部书籍") }
                }
            }
            items(books, key = { it.id }) { book ->
                if (selectedBookId == book.id) {
                    Button(onClick = { selectedBookId = book.id }) {
                        Text(book.title.take(14), maxLines = 1)
                    }
                } else {
                    OutlinedButton(onClick = { selectedBookId = book.id }) {
                        Text(book.title.take(14), maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                if (selectedColor.isBlank()) {
                    Button(onClick = { selectedColor = "" }) { Text("全部颜色", fontSize = 11.sp) }
                } else {
                    OutlinedButton(onClick = { selectedColor = "" }) { Text("全部颜色", fontSize = 11.sp) }
                }
            }

            items(
                listOf(
                    "yellow" to "黄",
                    "green" to "绿",
                    "blue" to "蓝",
                    "pink" to "粉",
                    "underline" to "U下划线",
                )
            ) { entry ->
                val (code, label) = entry
                if (selectedColor == code) {
                    Button(onClick = { selectedColor = "" }) { Text(label, fontSize = 11.sp) }
                } else {
                    OutlinedButton(onClick = { selectedColor = code }) { Text(label, fontSize = 11.sp) }
                }
            }

            item {
                if (notesOnly) {
                    Button(onClick = { notesOnly = false }) { Text("只看笔记", fontSize = 11.sp) }
                } else {
                    OutlinedButton(onClick = { notesOnly = true }) { Text("只看笔记", fontSize = 11.sp) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索原文 / 笔记 / 书名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        if (selectedBookId.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        selectedTitle.ifBlank { selectedBookId },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "当前筛选 ${shown.size} 条标注 · $noteCount 条笔记",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (shown.isEmpty()) {
            Text(
                "没有符合条件的标注。",
                color = Color.Gray,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(
                    shown,
                    key = { (bookId, mark) ->
                        if (mark.source == "readium" && mark.locatorJson.isNotBlank()) {
                            "${bookId}_readium_${mark.locatorJson.hashCode()}"
                        } else {
                            "${bookId}_${mark.paragraphIndex}_${mark.startOffset}_${mark.endOffset}"
                        }
                    }
                ) { (bookId, mark) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (mark.color == "underline") {
                                    Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, color = Accent)
                                } else {
                                    Box(
                                        Modifier
                                            .size(16.dp)
                                            .background(
                                                annotationComposeColor(
                                                    mark.color,
                                                    readerPalette(ReaderTheme.PAPER),
                                                ),
                                                RoundedCornerShape(4.dp),
                                            )
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    mark.text,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            if (mark.note.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    mark.note,
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B625C),
                                    lineHeight = 19.sp,
                                )
                            }

                            Spacer(Modifier.height(5.dp))
                            Text(
                                if (mark.source == "readium") {
                                    "📖 ${bookNames[bookId] ?: bookId} · EPUB 精确定位"
                                } else {
                                    "📖 ${bookNames[bookId] ?: bookId} · 第 ${mark.paragraphIndex + 1} 段"
                                },
                                fontSize = 11.sp,
                                color = Color.Gray,
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = {
                                        editTarget = bookId to mark
                                        noteDraft = mark.note
                                    }
                                ) { Text(if (mark.note.isBlank()) "加笔记" else "编辑笔记") }

                                if (mark.source != "readium") {
                                    TextButton(
                                        onClick = { onOpenAnnotation(bookId, mark.paragraphIndex) }
                                    ) { Text("打开原文") }
                                }

                                TextButton(
                                    onClick = {
                                        if (mark.source == "readium" && mark.locatorJson.isNotBlank()) {
                                            learningStore.removeReadiumAnnotation(bookId, mark.locatorJson)
                                        } else {
                                            learningStore.removeAnnotationExact(
                                                bookId = bookId,
                                                paragraphIndex = mark.paragraphIndex,
                                                startOffset = mark.startOffset,
                                                endOffset = mark.endOffset,
                                            )
                                        }
                                        items = learningStore.allAnnotations()
                                    }
                                ) { Text("删除", color = Color(0xFF9A5555)) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }

    editTarget?.let { (bookId, mark) ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("阅读笔记") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mark.text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = { Text("写下你的理解、疑问或提醒") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (mark.source == "readium" && mark.locatorJson.isNotBlank()) {
                            learningStore.updateReadiumAnnotationNote(
                                bookId = bookId,
                                locatorJson = mark.locatorJson,
                                note = noteDraft.trim(),
                            )
                        } else {
                            learningStore.updateAnnotationNote(
                                bookId = bookId,
                                paragraphIndex = mark.paragraphIndex,
                                startOffset = mark.startOffset,
                                endOffset = mark.endOffset,
                                note = noteDraft.trim(),
                            )
                        }
                        items = learningStore.allAnnotations()
                        editTarget = null
                    }
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ProfileMetric(modifier: Modifier = Modifier, value: String, label: String) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(label, fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    dictionary: DictionaryRepository,
    readingStats: ReadingStatsStore,
    learningStore: LearningStore,
    tts: TtsManager,
    books: List<BookItem>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val readingProfileStore = remember { ReadingProfileStore(context) }
    val adaptiveStore = remember { com.cy.languagereader.mobile.data.AdaptiveTrainingStore(context) }
    val learningSnapshot = remember { learningStore.vocabulary() }
    val learningCount = learningSnapshot.count { it.status != "known" }
    val knownCount = learningSnapshot.count { it.status == "known" }
    val annotationCount = remember { learningStore.allAnnotations().size }
    val backupManager = remember { DataBackupManager(context, learningStore) }
    val backupScope = rememberCoroutineScope()
    // Backup JSON can become large; never put it in the Activity saved-state Bundle.
    var backupText by remember { mutableStateOf("") }
    var backupStatus by remember { mutableStateOf("") }
    var backupBusy by remember { mutableStateOf(false) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            backupScope.launch {
                backupBusy = true
                backupStatus = "正在生成备份…"
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val raw = backupManager.exportBackup()
                        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)
                            ?.use { it.write(raw) }
                            ?: error("无法写入目标文件")
                    }
                }
                backupStatus = result.fold(
                    onSuccess = { "✓ 备份文件已保存" },
                    onFailure = { "保存失败：${it.message}" },
                )
                backupBusy = false
            }
        }
    }

    val loadBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            backupScope.launch {
                backupBusy = true
                backupStatus = "正在恢复备份…"
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val raw = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            ?: error("无法读取备份文件")
                        raw to backupManager.restoreBackup(raw)
                    }
                }
                result.onSuccess { (raw, summary) ->
                    backupText = raw
                    backupStatus =
                        "✓ 已恢复：${summary.vocabCount} 个词汇、" +
                        "${summary.highlightCount} 条标注、" +
                        "${summary.preferenceCount} 项设置，" +
                        "${summary.trainingStateCount} 个训练/语法进度，" +
                        "${summary.bookAliasCount} 本书已建立重新导入映射。建议重新打开应用以加载全部恢复设置。"
                }.onFailure {
                    backupStatus = "恢复失败：${it.message ?: "备份格式错误"}"
                }
                backupBusy = false
            }
        }
    }

    var safeMode by remember { mutableStateOf(settings.safeMode) }
    var pronunciationCache by remember { mutableStateOf(settings.pronunciationCacheEnabled) }
    var diagnosticReport by rememberSaveable { mutableStateOf("") }
    var diagnosticRunning by remember { mutableStateOf(false) }
    val diagnosticScope = rememberCoroutineScope()

    val stats by produceState(initialValue = "正在准备本地法中词典…") {
        value = runCatching { withContext(Dispatchers.IO) { dictionary.stats() } }.getOrElse { "词典初始化失败：${it.message}" }
    }
    val readingProfile by produceState(
        initialValue = ReadingProfileSnapshot(0L, 0L, 0, 0, emptyList(), emptyList(), 0f, 0f),
        key1 = readingStats.totalSeconds(),
    ) {
        value = withContext(Dispatchers.IO) { readingProfileStore.snapshot(30) }
    }
    val trainingProfile by produceState(initialValue = emptyMap<String, com.cy.languagereader.mobile.data.AdaptiveTrainingStats>()) {
        value = withContext(Dispatchers.IO) {
            mapOf(
                "阴阳性" to adaptiveStore.statsForKinds(listOf("gender")),
                "à / de" to adaptiveStore.statsForKinds(listOf("preposition")),
                "动词变位" to adaptiveStore.statsForKinds(listOf("conjugation")),
                "整句输出" to adaptiveStore.statsForKinds(listOf("sentence_output")),
                "个人生词" to adaptiveStore.statsForKinds(listOf("personal_vocab")),
            )
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text("内置离线法中词典", fontWeight = FontWeight.SemiBold)
        Text(stats, color = Color.Gray, fontSize = 13.sp)
        Text("点单词时只查手机本机词典，不使用 AI、不需要网络。", color = Color(0xFF557A55), fontSize = 12.sp)

        Spacer(Modifier.height(20.dp))
        Text("稳定性与发音", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("安全模式", fontWeight = FontWeight.Medium)
                        Text(
                            "关闭联网整句翻译、联网备用发音和翻页动画；离线查词与标注不受影响。",
                            fontSize = 11.sp,
                            color = Color.Gray,
                        )
                    }
                    Switch(
                        checked = safeMode,
                        onCheckedChange = {
                            safeMode = it
                            settings.safeMode = it
                            tts.configure(
                                networkFallbackEnabled = !it && settings.pronunciationRoute != "OFFLINE",
                                persistentCacheEnabled = pronunciationCache,
                                pronunciationRoute = settings.pronunciationRoute,
                                fallbackEnabled = settings.pronunciationFallbackEnabled,
                            )
                        },
                    )
                }

                HorizontalDivider()

                Text(
                    "发音网络模式已移到阅读界面的「阅读控制中心」，避免两处设置互相冲突。",
                    fontSize = 11.sp,
                    color = Color(0xFF6B625C),
                )
                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("保存成功发音", fontWeight = FontWeight.Medium)
                        Text(
                            "某个词成功下载一次后，下次可直接从本机缓存播放。",
                            fontSize = 11.sp,
                            color = Color.Gray,
                        )
                    }
                    Switch(
                        checked = pronunciationCache,
                        onCheckedChange = {
                            pronunciationCache = it
                            settings.pronunciationCacheEnabled = it
                            tts.configure(
                                networkFallbackEnabled = !safeMode && settings.pronunciationRoute != "OFFLINE",
                                persistentCacheEnabled = it,
                                pronunciationRoute = settings.pronunciationRoute,
                                fallbackEnabled = settings.pronunciationFallbackEnabled,
                            )
                        },
                    )
                }

                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    OutlinedButton(
                        onClick = { tts.speak("bonjour") },
                        modifier = Modifier.weight(1f),
                    ) { Text("🔊 测试 bonjour", fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = {
                            val deleted = tts.clearPronunciationCache()
                            Toast.makeText(context, "已清理 $deleted 个发音缓存", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("清理发音缓存", fontSize = 11.sp) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
            Column(Modifier.padding(12.dp)) {
                Text("🔊 当前发音路线", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(tts.routeLabel(), fontSize = 11.sp, color = Color(0xFF557A55))
                Text("切换路线请进入任意书籍 → 阅读控制中心。", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("系统诊断中心", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "以后遇到“没声音 / 查不到词 / 标注异常”，先运行这里，再把报告发给我。",
                    fontSize = 11.sp,
                    color = Color(0xFF6B625C),
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        diagnosticRunning = true
                        diagnosticScope.launch {
                            val dictionaryPart = withContext(Dispatchers.IO) {
                                runCatching { dictionary.diagnosticsReport() }
                                    .getOrElse { "词典诊断异常：${it.message}" }
                            }
                            val versionName = runCatching {
                                context.packageManager
                                    .getPackageInfo(context.packageName, 0)
                                    .versionName
                            }.getOrNull() ?: "?"

                            val vocabSize = withContext(Dispatchers.IO) {
                                runCatching { learningStore.vocabulary().size }.getOrDefault(-1)
                            }
                            val annotationSize = withContext(Dispatchers.IO) {
                                runCatching { learningStore.allAnnotations().size }.getOrDefault(-1)
                            }

                            diagnosticReport = buildString {
                                appendLine("=== Lirelia 诊断报告 ===")
                                appendLine("App：$versionName")
                                appendLine("Android：${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
                                appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL}")
                                appendLine("书籍：${books.size}")
                                appendLine("生词：$vocabSize")
                                appendLine("标注：$annotationSize")
                                appendLine("安全模式：${if (safeMode) "开启" else "关闭"}")
                                appendLine()
                                appendLine("--- 离线词典 ---")
                                appendLine(dictionaryPart)
                                appendLine()
                                appendLine("--- 法语发音 ---")
                                appendLine(tts.diagnosticsReport())
                            }.trim()
                            diagnosticRunning = false
                        }
                    },
                    enabled = !diagnosticRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (diagnosticRunning) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在诊断…")
                    } else {
                        Text("运行完整诊断")
                    }
                }

                if (diagnosticReport.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = diagnosticReport,
                        onValueChange = {},
                        readOnly = true,
                        minLines = 8,
                        maxLines = 16,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("诊断报告") },
                    )
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Lirelia 诊断报告", diagnosticReport)
                            )
                            Toast.makeText(context, "诊断报告已复制", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("复制诊断报告") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("每日目标", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))) {
            Column(Modifier.padding(12.dp)) {
                var readingGoal by remember { mutableStateOf(settings.dailyReadingGoalMinutes) }
                var vocabGoal by remember { mutableStateOf(settings.dailyVocabReviewGoal) }

                SettingStepper(
                    "每天阅读",
                    "$readingGoal 分钟",
                    {
                        readingGoal = (readingGoal - 5).coerceAtLeast(5)
                        settings.dailyReadingGoalMinutes = readingGoal
                    },
                    {
                        readingGoal = (readingGoal + 5).coerceAtMost(240)
                        settings.dailyReadingGoalMinutes = readingGoal
                    },
                )

                SettingStepper(
                    "每天复习",
                    "$vocabGoal 词",
                    {
                        vocabGoal = (vocabGoal - 5).coerceAtLeast(5)
                        settings.dailyVocabReviewGoal = vocabGoal
                    },
                    {
                        vocabGoal = (vocabGoal + 5).coerceAtMost(200)
                        settings.dailyVocabReviewGoal = vocabGoal
                    },
                )
                Text("目标只用于提醒和统计，不会限制阅读。", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("🧠 我的阅读法语画像", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
            Column(Modifier.padding(12.dp)) {
                Text("基于你真实的阅读与查词行为，不虚构词汇量；数据从 V3.18 开始积累。", fontSize = 11.sp, color = Color(0xFF6B625C))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    ProfileMetric(Modifier.weight(1f), String.format(Locale.US, "%.1f", readingProfile.lookupRate30), "每100词查词")
                    ProfileMetric(Modifier.weight(1f), formatCount(readingProfile.exposedWords30.toInt()), "30天读过词")
                    ProfileMetric(Modifier.weight(1f), readingProfile.uniqueLookupWords30.toString(), "30天查过词")
                }
                Spacer(Modifier.height(10.dp))
                val delta = readingProfile.recent7Rate - readingProfile.previous7Rate
                Text(
                    when {
                        readingProfile.exposedWords30 == 0L -> "开始阅读并点词后，这里会自动形成你的个人画像。"
                        readingProfile.previous7Rate <= 0f -> "最近7天查词密度：${String.format(Locale.US, "%.1f", readingProfile.recent7Rate)} / 100词"
                        delta < -0.3f -> "📉 最近7天查词密度下降 ${String.format(Locale.US, "%.1f", -delta)}：阅读正在变顺。"
                        delta > 0.3f -> "📈 最近7天查词密度上升 ${String.format(Locale.US, "%.1f", delta)}：最近材料可能更有挑战。"
                        else -> "➡️ 最近7天查词密度基本稳定。"
                    },
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF557A55),
                )
                if (readingProfile.lookups30 > 0) {
                    Text(
                        "重复查询约 ${String.format(Locale.US, "%.0f%%", readingProfile.repeatRate30)} · 重复出现的词会优先列为顽固词",
                        fontSize = 10.sp, color = Color.Gray,
                    )
                }
            }
        }

        if (readingProfile.recent14Days.any { it.exposedWords > 0L }) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))) {
                Column(Modifier.padding(12.dp)) {
                    Text("14天查词密度", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("数字越低，表示同样阅读量下需要查的词越少。", fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.height(7.dp))
                    readingProfile.recent14Days.filter { it.exposedWords > 0L }.takeLast(7).forEach { day ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(day.date.takeLast(5), modifier = Modifier.width(48.dp), fontSize = 10.sp, color = Color.Gray)
                            LinearProgressIndicator(
                                progress = { (day.lookupsPer100 / 20f).coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f).height(6.dp),
                            )
                            Text(String.format(Locale.US, " %.1f", day.lookupsPer100), fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        if (readingProfile.stubbornWords.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5E9))) {
                Column(Modifier.padding(12.dp)) {
                    Text("🔁 顽固词", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("这些词你查过不止一次，值得优先复习。", fontSize = 10.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    readingProfile.stubbornWords.take(8).forEach { item ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(item.word, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            Text("${item.count}×", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F1E8))) {
            Column(Modifier.padding(12.dp)) {
                Text("🎯 训练能力画像", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("来自你真正做过的训练；没有作答的数据不会硬算分。", fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(7.dp))
                trainingProfile.forEach { (label, stat) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.width(70.dp), fontSize = 11.sp)
                        LinearProgressIndicator(
                            progress = { stat.averageMastery.coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(6.dp),
                        )
                        Text(
                            if (stat.tracked == 0) "  —" else "  ${(stat.averageMastery * 100).toInt()}%",
                            modifier = Modifier.width(42.dp), fontSize = 10.sp, textAlign = TextAlign.End,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("阅读统计", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4))) {
            Column(Modifier.padding(12.dp)) {
                Text("今天：${formatDuration(readingStats.todaySeconds())}")
                Text("最近 7 天：${formatDuration(readingStats.secondsInLastDays(7))}")
                Text("最近 30 天：${formatDuration(readingStats.secondsInLastDays(30))}")
                Text("累计：${formatDuration(readingStats.totalSeconds())}")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("🔥 ${readingStats.currentStreakDays()} 天", fontWeight = FontWeight.SemiBold)
                        Text("连续阅读", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("📅 ${readingStats.activeDayCount()} 天", fontWeight = FontWeight.SemiBold)
                        Text("累计活跃", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(Modifier.weight(1f)) {
                        val mostRead = books.maxByOrNull { readingStats.bookSeconds(it.id) }
                        Text(
                            if (mostRead == null) "—" else formatDuration(readingStats.bookSeconds(mostRead.id)),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("最多单书", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val recent = readingStats.recentDays(7)
                if (recent.isEmpty()) {
                    Text("还没有阅读记录。", fontSize = 12.sp, color = Color.Gray)
                } else {
                    recent.forEach { (date, seconds) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(date, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray)
                            Text(formatDuration(seconds), fontSize = 12.sp)
                        }
                    }
                }

                val topBooks = books
                    .map { it to readingStats.bookSeconds(it.id) }
                    .filter { it.second > 0L }
                    .sortedByDescending { it.second }
                    .take(5)

                if (topBooks.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("阅读时长最多", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    topBooks.forEachIndexed { index, pair ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${index + 1}.",
                                modifier = Modifier.width(24.dp),
                                fontSize = 11.sp,
                                color = Color.Gray,
                            )
                            Text(
                                pair.first.title,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(formatDuration(pair.second), fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("28 天阅读日历", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))) {
            Column(Modifier.padding(12.dp)) {
                val calendar28 = readingStats.calendarDays(28)
                val maxDay = calendar28.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                Text(
                    "颜色越深，表示当天阅读时间越长。",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.height(8.dp))
                calendar28.chunked(7).forEach { week ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        week.forEach { day ->
                            val date = day.first
                            val seconds = day.second
                            val intensity = (seconds.toFloat() / maxDay).coerceIn(0f, 1f)
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .background(
                                            if (seconds <= 0L) Color(0xFFE7E0D8)
                                            else Accent.copy(alpha = 0.22f + 0.68f * intensity),
                                            RoundedCornerShape(7.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        date.takeLast(2).trimStart('0'),
                                        fontSize = 9.sp,
                                        color = if (intensity > 0.65f) Color.White else Ink,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "近 28 天共 ${formatDuration(readingStats.secondsInLastDays(28))}",
                    fontSize = 11.sp,
                    color = Color(0xFF6B625C),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("学习数据摘要", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6ED))) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("${books.size}", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("书籍", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("$learningCount", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("学习中", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("$knownCount", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("已掌握", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("$annotationCount", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("标注", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val report = buildString {
                            appendLine("Lirelia 学习摘要")
                            appendLine("书籍：${books.size}")
                            appendLine("学习中词汇：$learningCount")
                            appendLine("已掌握词汇：$knownCount")
                            appendLine("标注：$annotationCount")
                            appendLine("今日阅读：${formatDuration(readingStats.todaySeconds())}")
                            appendLine("最近7天：${formatDuration(readingStats.secondsInLastDays(7))}")
                            appendLine("最近30天：${formatDuration(readingStats.secondsInLastDays(30))}")
                            appendLine("累计阅读：${formatDuration(readingStats.totalSeconds())}")
                            appendLine("连续阅读：${readingStats.currentStreakDays()} 天")
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lirelia 学习摘要", report))
                        Toast.makeText(context, "学习摘要已复制", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("复制学习摘要")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("数据备份与恢复", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "备份包含：设置、阅读统计、进度/书签/收藏、标签/书架/系列、生词与标注、PDF答题文本框，以及训练/52语法的自适应进度和暂停中的训练。",
                    fontSize = 11.sp,
                    color = Color(0xFF6B625C),
                )
                Text(
                    "不包含 EPUB/PDF/TXT 原文件和封面缓存。换手机后需要重新导入书籍。",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
                Spacer(Modifier.height(9.dp))

                Button(
                    onClick = {
                        val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        saveBackupLauncher.launch("LanguageReader_Backup_$date.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupBusy,
                ) {
                    Text("保存备份文件（推荐）")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        loadBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupBusy,
                ) {
                    Text("从备份文件恢复")
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        backupScope.launch {
                            backupBusy = true
                            backupStatus = "正在生成备份…"
                            val result = runCatching { withContext(Dispatchers.IO) { backupManager.exportBackup() } }
                            result.onSuccess { raw ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Lirelia Backup", raw))
                                backupText = raw
                                backupStatus = "✓ 备份已复制到剪贴板"
                            }.onFailure { backupStatus = "生成失败：${it.message}" }
                            backupBusy = false
                        }
                    },
                    enabled = !backupBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("复制完整学习备份")
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        backupText = clip
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            .orEmpty()
                        backupStatus = if (backupText.isBlank()) "剪贴板里没有文本" else "已载入剪贴板备份"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("从剪贴板载入")
                }

                if (backupText.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupText,
                        onValueChange = { backupText = it; backupStatus = "" },
                        label = { Text("备份 JSON") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            backupScope.launch {
                                backupBusy = true
                                backupStatus = "正在恢复备份…"
                                val result = runCatching { withContext(Dispatchers.IO) { backupManager.restoreBackup(backupText) } }
                                result.onSuccess { summary ->
                                    backupStatus =
                                        "✓ 已恢复：${summary.vocabCount} 个词汇、" +
                                        "${summary.highlightCount} 条标注、" +
                                        "${summary.preferenceCount} 项设置/统计，" +
                                        "${summary.trainingStateCount} 个训练/语法进度，" +
                                        "${summary.bookAliasCount} 本书重连映射。建议重新打开应用以加载全部恢复设置"
                                }.onFailure {
                                    backupStatus = "恢复失败：${it.message ?: "备份格式错误"}"
                                }
                                backupBusy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !backupBusy,
                    ) {
                        Text("恢复这份备份")
                    }
                }

                if (backupStatus.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        backupStatus,
                        fontSize = 11.sp,
                        color = if (backupStatus.startsWith("✓")) Color(0xFF557A55) else Color(0xFF9A5D4D),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("翻译策略", fontWeight = FontWeight.SemiBold)
        Text("单词优先使用离线法中词典；整句先显示离线词典辅助，再由联网翻译在原位置更新。无需本地 AI 服务。", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(26.dp))
        Text("V2.3 FULLSCREEN & EPUB IMAGES", fontWeight = FontWeight.SemiBold)
        Text("稳定优先：点词离线查词 · 256项查词缓存 · 启动预热 · 句内短语识别 · 发音离线复用缓存 · 发音故障记录 · 系统诊断中心 · 安全模式 · 可滚动设置页 · V2.0全部书库/学习/备份功能", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

private fun frenchWordRange(text: String, rawOffset: Int): IntRange? {
    if (text.isEmpty()) return null
    var offset = rawOffset.coerceIn(0, text.lastIndex)
    fun isWordChar(c: Char) = c.isLetter() || c == '’' || c == '\'' || c == '-'
    if (!isWordChar(text[offset])) {
        if (offset > 0 && isWordChar(text[offset - 1])) offset-- else return null
    }
    var start = offset
    var end = offset
    while (start > 0 && isWordChar(text[start - 1])) start--
    while (end < text.lastIndex && isWordChar(text[end + 1])) end++
    while (start <= end && (text[start] == '-' || text[start] == '\'' || text[start] == '’')) start++
    while (end >= start && (text[end] == '-' || text[end] == '\'' || text[end] == '’')) end--
    return if (start <= end) start..end else null
}

private fun nearestFrenchWordRange(text: String, rawOffset: Int): IntRange? {
    if (text.isBlank()) return null
    val safe = rawOffset.coerceIn(0, text.lastIndex)
    for (distance in 1..14) {
        val left = safe - distance
        if (left >= 0) frenchWordRange(text, left)?.let { return it }
        val right = safe + distance
        if (right <= text.lastIndex) frenchWordRange(text, right)?.let { return it }
    }
    return null
}

private fun sentenceRange(text: String, offset: Int): IntRange {
    if (text.isBlank()) return 0..0
    return FrenchSentenceSegmenter.range(text, offset) ?: 0..text.lastIndex.coerceAtLeast(0)
}

private fun extractSentence(text: String, offset: Int): String =
    FrenchSentenceSegmenter.sentence(text, offset, maxChars = 1000)

private fun semanticChunks(sentence: String): List<String> {
    val normalized = sentence.trim()
    if (normalized.isBlank()) return emptyList()
    val rough = normalized.split(Regex("(?<=[,;:!?…])\\s+"))
        .map { it.trim() }.filter { it.isNotBlank() }
    return if (rough.size <= 1 && normalized.length > 90)
        normalized.split(Regex("(?<=,)\\s*"), limit = 4).map { it.trim() }.filter { it.isNotBlank() }
    else rough
}


private fun searchPreview(paragraph: String, query: String): String {
    val index = paragraph.indexOf(query, ignoreCase = true)
    if (index < 0) return paragraph.take(220)
    val start = (index - 90).coerceAtLeast(0)
    val end = (index + query.length + 130).coerceAtMost(paragraph.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < paragraph.length) "…" else ""
    return prefix + paragraph.substring(start, end).replace(Regex("\\s+"), " ").trim() + suffix
}




private fun daysUntilDate(dateText: String): Int? {
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(dateText.trim())) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val target = runCatching { format.parse(dateText.trim()) }.getOrNull() ?: return null

    val nowCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val targetCalendar = java.util.Calendar.getInstance().apply {
        time = target
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return ((targetCalendar.timeInMillis - nowCalendar.timeInMillis) / 86_400_000L).toInt()
}

private fun readingPlanLine(
    book: BookItem,
    currentParagraph: Int,
    targetDate: String,
): String {
    val remaining = (book.totalParagraphs - currentParagraph - 1).coerceAtLeast(0)
    if (remaining <= 0) return "已完成"

    val days = daysUntilDate(targetDate)
        ?: return "$targetDate · 剩 $remaining 段"

    if (days < 0) return "已逾期 ${-days} 天 · 剩 $remaining 段"
    if (days == 0) return "今天到期 · 剩 $remaining 段"

    val perDay = ((remaining + days - 1) / days).coerceAtLeast(1)
    return "$targetDate · 还剩 $days 天 · 每天约 $perDay 段"
}

private fun shelfLabel(code: String): String = when (code) {
    "reading" -> "在读"
    "finished" -> "读完"
    "paused" -> "搁置"
    else -> "待读"
}

private fun shelfColor(code: String): Color = when (code) {
    "reading" -> Color(0xFF3F7D57)
    "finished" -> Color(0xFF476FA8)
    "paused" -> Color(0xFF8C6A4A)
    else -> Color(0xFF8A8178)
}


private fun vocabularyDailySeries(
    items: List<VocabularyItem>,
    days: Int,
): List<Pair<String, Int>> {
    if (days <= 0) return emptyList()

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val counts = items.groupingBy { format.format(Date(it.createdAt)) }.eachCount()
    val calendar = java.util.Calendar.getInstance()
    val reversed = buildList {
        repeat(days) {
            val key = format.format(calendar.time)
            add(key to (counts[key] ?: 0))
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
    }
    return reversed.reversed()
}

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

private fun reviewStatus(item: VocabularyItem): String {
    if (item.reviewCount == 0) return "新词 · 今天复习"
    if (item.nextReviewAt <= 0L) return "已复习 ${item.reviewCount} 次"
    val now = System.currentTimeMillis()
    if (item.nextReviewAt <= now) return "已复习 ${item.reviewCount} 次 · 到期"
    val days = ((item.nextReviewAt - now + 86_399_999L) / 86_400_000L).coerceAtLeast(1L)
    return "已复习 ${item.reviewCount} 次 · $days 天后"
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fk", n / 1_000.0)
    else -> n.toString()
}

private fun formatChars(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fk", n / 1_000.0)
    else -> n.toString()
}
