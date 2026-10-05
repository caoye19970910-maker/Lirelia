package com.cy.languagereader.mobile.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.BookItem
import com.cy.languagereader.mobile.data.BookRepository
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.data.PdfPageAnalysis
import com.cy.languagereader.mobile.data.PdfPageAnalyzer
import com.cy.languagereader.mobile.data.PdfTextBoxNote
import com.cy.languagereader.mobile.data.PdfTextBoxStore
import com.cy.languagereader.mobile.data.PdfWordBox
import com.cy.languagereader.mobile.data.ReadingStatsStore
import com.cy.languagereader.mobile.data.FrenchSentenceSegmenter
import com.cy.languagereader.mobile.data.ReadingProfileStore
import com.cy.languagereader.mobile.data.TextAnnotation
import com.cy.languagereader.mobile.service.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

private val PdfBackground = Color(0xFFCBC8C2)
private val PdfChrome = Color(0xFFF4EEE5)
private val PdfInk = Color(0xFF2E2A26)
private val PdfAccent = Color(0xFF755A43)

@Composable
internal fun PdfReaderScreen(
    book: BookItem,
    bookRepository: BookRepository,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    settings: AppSettings,
    readingStats: ReadingStatsStore,
    initialPage: Int? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val sourceFile = remember(book.sourceFile) { File(book.sourceFile) }
    val textBoxStore = remember { PdfTextBoxStore(context) }
    val readingProfile = remember { ReadingProfileStore(context) }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val pageCount by produceState<Int?>(
        initialValue = null,
        key1 = book.id,
        key2 = book.sourceFile,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                PdfPageAnalyzer.pageCount(context, sourceFile)
            }.getOrDefault(0)
        }
    }

    val count = pageCount
    if (count == null) {
        Box(
            Modifier.fillMaxSize().background(PdfBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (count <= 0 || !sourceFile.exists()) {
        Box(
            Modifier.fillMaxSize().background(PdfBackground),
            contentAlignment = Alignment.Center,
        ) {
            Card {
                Column(
                    Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("PDF 无法打开", fontWeight = FontWeight.SemiBold)
                    Text(
                        "原始 PDF 文件不存在或页面读取失败。",
                        color = Color.Gray,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("返回书库") }
                }
            }
        }
        return
    }

    val startPage = (
        initialPage ?: bookRepository.loadProgress(book.id)
    ).coerceIn(0, count - 1)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startPage,
    )

    val analysisCache = remember(book.id) {
        mutableStateMapOf<Int, PdfPageAnalysis>()
    }

    var annotations by remember(book.id) {
        mutableStateOf(learningStore.textAnnotations(book.id))
    }

    var textBoxes by remember(book.id) {
        mutableStateOf(textBoxStore.list(book.id))
    }

    var selection by remember {
        mutableStateOf<WordSelection?>(null)
    }

    var sentenceSelection by remember {
        mutableStateOf<SentenceSelection?>(null)
    }

    var chromeVisible by remember { mutableStateOf(true) }
    var answerMode by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1.0f) }
    var pageMode by remember { mutableStateOf(false) }
    var pageSwipeOffset by remember { mutableStateOf(0f) }
    var jumpDialog by remember { mutableStateOf(false) }
    var jumpDraft by remember { mutableStateOf((startPage + 1).toString()) }

    var pendingTextBoxPosition by remember {
        mutableStateOf<Triple<Int, Float, Float>?>(null)
    }
    var editingTextBox by remember {
        mutableStateOf<PdfTextBoxNote?>(null)
    }
    var textBoxDraft by remember { mutableStateOf("") }

    val currentPage = listState.firstVisibleItemIndex
        .coerceIn(0, count - 1)

    val currentAnalysis = analysisCache[currentPage]
    val hasTextLayer = currentAnalysis?.hasTextLayer

    LaunchedEffect(book.id, currentPage, currentAnalysis?.text) {
        val text = currentAnalysis?.text.orEmpty()
        if (text.isNotBlank() && text != PdfPageAnalyzer.EMPTY_PAGE_SENTINEL) {
            withContext(Dispatchers.IO) {
                readingProfile.recordExposure(book.id, "pdf:$currentPage", text)
            }
        }
    }

    val screenWidthDp = configuration.screenWidthDp.dp
    val basePageWidth = (
        configuration.screenWidthDp - if (
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) 26 else 14
    ).coerceAtLeast(260).dp

    val pageWidth = basePageWidth * zoom

    val sessionSeconds = rememberReadingSessionSeconds(
        bookId = book.id,
        settings = settings,
        readingStats = readingStats,
    )

    DisposableEffect(activity) {
        onDispose {
            activity?.setReaderImmersive(false)
        }
    }

    LaunchedEffect(chromeVisible, activity) {
        activity?.setReaderImmersive(!chromeVisible)
    }

    // Hide controls while scrolling down; reveal them on upward movement.
    LaunchedEffect(listState) {
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
                val movingDown =
                    index > previousIndex ||
                        (
                            index == previousIndex &&
                                offset > previousOffset + 10
                            )

                val movingUp =
                    index < previousIndex ||
                        (
                            index == previousIndex &&
                                offset < previousOffset - 10
                            )

                if (movingDown) chromeVisible = false
                if (movingUp) chromeVisible = true
            }

            previousIndex = index
            previousOffset = offset
        }
    }

    LaunchedEffect(listState, book.id) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                bookRepository.saveProgress(book.id, page)
            }
    }

    // Bottom sheets can momentarily reveal system bars on Huawei/HarmonyOS.
    LaunchedEffect(
        selection,
        sentenceSelection,
        chromeVisible,
        activity,
    ) {
        if (
            !chromeVisible &&
            (
                selection != null ||
                    sentenceSelection != null
                )
        ) {
            repeat(4) {
                activity?.setReaderImmersive(true)
                delay(70)
            }
        }
    }

    fun openTextBox(
        pageIndex: Int,
        x: Float,
        y: Float,
    ) {
        editingTextBox = null
        pendingTextBoxPosition = Triple(pageIndex, x, y)
        textBoxDraft = ""
    }

    fun editTextBox(item: PdfTextBoxNote) {
        pendingTextBoxPosition = null
        editingTextBox = item
        textBoxDraft = item.text
    }

    fun saveTextBox() {
        val clean = textBoxDraft.trim()

        val editing = editingTextBox
        if (editing != null) {
            if (clean.isBlank()) {
                textBoxStore.remove(book.id, editing.id)
            } else {
                textBoxStore.update(
                    book.id,
                    editing.copy(text = clean),
                )
            }

            textBoxes = textBoxStore.list(book.id)
            editingTextBox = null
            textBoxDraft = ""
            return
        }

        val pending = pendingTextBoxPosition ?: return
        if (clean.isBlank()) {
            pendingTextBoxPosition = null
            return
        }

        textBoxStore.add(
            bookId = book.id,
            pageIndex = pending.first,
            x = pending.second,
            y = pending.third,
            text = clean,
        )
        textBoxes = textBoxStore.list(book.id)
        pendingTextBoxPosition = null
        textBoxDraft = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(PdfBackground)
    ) {

if (pageMode) {
    // Fixed one-page-at-a-time mode. Pinch zoom works directly on the
    // page; horizontal swipes move exactly one PDF page.
    Box(
        Modifier
            .fillMaxSize()
            .padding(
                top = if (chromeVisible) 70.dp else 4.dp,
                bottom = if (chromeVisible) 68.dp else 4.dp,
            )
            .pointerInput(pageMode, currentPage, zoom) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    if (abs(gestureZoom - 1f) > 0.001f) {
                        zoom = (
                            zoom * gestureZoom
                        ).coerceIn(0.65f, 3.5f)
                    }

                    if (abs(pan.x) > abs(pan.y) && zoom <= 1.02f) {
                        pageSwipeOffset += pan.x
                    }
                }
            }
            .pointerInput(pageMode, currentPage, zoom) {
                detectDragGestures(
                    onDragEnd = {
                        if (zoom <= 1.02f) {
                            val threshold = 90f
                            when {
                                pageSwipeOffset < -threshold &&
                                    currentPage < count - 1 -> {
                                    scope.launch {
                                        listState.scrollToItem(
                                            currentPage + 1
                                        )
                                    }
                                }

                                pageSwipeOffset > threshold &&
                                    currentPage > 0 -> {
                                    scope.launch {
                                        listState.scrollToItem(
                                            currentPage - 1
                                        )
                                    }
                                }
                            }
                        }
                        pageSwipeOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (
                            zoom <= 1.02f &&
                            abs(dragAmount.x) >
                            abs(dragAmount.y)
                        ) {
                            pageSwipeOffset += dragAmount.x
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val pageIndex = currentPage
        val analysis = analysisCache[pageIndex]

        LaunchedEffect(book.id, pageIndex, analysis) {
            if (analysis == null) {
                val loaded = withContext(Dispatchers.IO) {
                    PdfPageAnalyzer.analyzePage(
                        context = context,
                        file = sourceFile,
                        pageIndex = pageIndex,
                    )
                }
                analysisCache[pageIndex] = loaded
            }
        }

        PdfPageCard(
            file = sourceFile,
            pageIndex = pageIndex,
            pageCount = count,
            pageWidth = (
                configuration.screenWidthDp -
                    if (
                        configuration.orientation ==
                            Configuration.ORIENTATION_LANDSCAPE
                    ) 34 else 18
                ).coerceAtLeast(260).dp * zoom,
            zoom = zoom,
            analysis = analysis,
            annotations = annotations[pageIndex].orEmpty(),
            temporarySelection = sentenceSelection
                ?.takeIf { it.paragraphIndex == pageIndex }
                ?.let { it.wordStart to it.wordEnd },
            textBoxes = textBoxes.filter {
                it.pageIndex == pageIndex
            },
            answerMode = answerMode,
            onWordTap = { word ->
                val page = analysisCache[pageIndex]
                    ?: return@PdfPageCard

                scope.launch(Dispatchers.IO) {
                    readingProfile.recordLookup(word.word, book.id)
                }
                selection = WordSelection(
                    word = word.word,
                    sentence = pdfExtractSentence(
                        page.text,
                        word.startOffset,
                    ),
                    paragraphIndex = pageIndex,
                    startOffset = word.startOffset,
                    endOffset = word.endOffset,
                )
            },
            onWordLongPress = { word ->
                val page = analysisCache[pageIndex]
                    ?: return@PdfPageCard

                val range = pdfSentenceRange(
                    page.text,
                    word.startOffset,
                )

                if (range != null) {
                    sentenceSelection = SentenceSelection(
                        sentence = page.text.substring(
                            range.first,
                            range.last + 1,
                        ),
                        paragraphIndex = pageIndex,
                        sentenceStart = range.first,
                        sentenceEnd = range.last + 1,
                        word = word.word,
                        wordStart = word.startOffset,
                        wordEnd = word.endOffset,
                    )
                }
            },
            onAnswerTap = { x, y ->
                openTextBox(pageIndex, x, y)
            },
            onEditTextBox = ::editTextBox,
        )
    }
} else {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = if (chromeVisible) 74.dp else 8.dp,
            bottom = if (chromeVisible) 82.dp else 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = (0 until count).toList(),
            key = { it },
        ) { pageIndex ->
            val analysis = analysisCache[pageIndex]

            LaunchedEffect(
                book.id,
                pageIndex,
                analysis,
            ) {
                if (analysis == null) {
                    val loaded = withContext(Dispatchers.IO) {
                        PdfPageAnalyzer.analyzePage(
                            context = context,
                            file = sourceFile,
                            pageIndex = pageIndex,
                        )
                    }
                    analysisCache[pageIndex] = loaded
                }
            }

            PdfPageCard(
                file = sourceFile,
                pageIndex = pageIndex,
                pageCount = count,
                pageWidth = pageWidth,
                zoom = zoom,
                analysis = analysis,
                annotations = annotations[pageIndex].orEmpty(),
                temporarySelection = sentenceSelection
                    ?.takeIf { it.paragraphIndex == pageIndex }
                    ?.let { it.wordStart to it.wordEnd },
                textBoxes = textBoxes.filter {
                    it.pageIndex == pageIndex
                },
                answerMode = answerMode,
                onWordTap = { word ->
                    val page = analysisCache[pageIndex]
                        ?: return@PdfPageCard

                    scope.launch(Dispatchers.IO) {
                        readingProfile.recordLookup(word.word, book.id)
                    }
                    selection = WordSelection(
                        word = word.word,
                        sentence = pdfExtractSentence(
                            page.text,
                            word.startOffset,
                        ),
                        paragraphIndex = pageIndex,
                        startOffset = word.startOffset,
                        endOffset = word.endOffset,
                    )
                },
                onWordLongPress = { word ->
                    val page = analysisCache[pageIndex]
                        ?: return@PdfPageCard

                    val range = pdfSentenceRange(
                        page.text,
                        word.startOffset,
                    )

                    if (range != null) {
                        sentenceSelection = SentenceSelection(
                            sentence = page.text.substring(
                                range.first,
                                range.last + 1,
                            ),
                            paragraphIndex = pageIndex,
                            sentenceStart = range.first,
                            sentenceEnd = range.last + 1,
                            word = word.word,
                            wordStart = word.startOffset,
                            wordEnd = word.endOffset,
                        )
                    }
                },
                onAnswerTap = { x, y ->
                    openTextBox(pageIndex, x, y)
                },
                onEditTextBox = ::editTextBox,
            )
        }
    }
}
        if (chromeVisible) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PdfChrome.copy(alpha = 0.97f),
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TextButton(onClick = onBack) {
                        Text("‹ 书库")
                    }

                    Column(
                        Modifier.weight(1f),
                    ) {
                        Text(
                            book.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            buildString {
                                append("第 ${currentPage + 1} / $count 页")
                                when (hasTextLayer) {
                                    true -> append(" · 可点词")
                                    false -> append(" · 本页无文字层")
                                    null -> append(" · 分析中")
                                }
                            },
                            fontSize = 10.sp,
                            color = if (hasTextLayer == false) {
                                Color(0xFFB06A48)
                            } else {
                                Color.Gray
                            },
                        )
                    }

                    TextButton(
                        onClick = {
                            jumpDraft = (currentPage + 1).toString()
                            jumpDialog = true
                        }
                    ) {
                        Text("页码")
                    }

                    TextButton(
                        onClick = {
                            val next = bookRepository.toggleBookmark(
                                book.id,
                                currentPage,
                            )
                            // Keep repository state only; PDF page bookmarks use
                            // the same page-index convention as progress.
                            next
                        }
                    ) {
                        Text("🔖")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PdfChrome.copy(alpha = 0.97f),
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
            ) {
                Row(
                    Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 5.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            zoom = (zoom - 0.15f)
                                .coerceAtLeast(0.75f)
                        }
                    ) {
                        Text("－")
                    }

                    Text(
                        "${(zoom * 100).toInt()}%",
                        fontSize = 11.sp,
                        modifier = Modifier.width(42.dp),
                    )

                    OutlinedButton(
                        onClick = {
                            zoom = (zoom + 0.15f)
                                .coerceAtMost(2.25f)
                        }
                    ) {
                        Text("＋")
                    }

                    TextButton(
                        onClick = { zoom = 1f }
                    ) {
                        Text("适宽", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            pageMode = !pageMode
                            chromeVisible = true
                            pageSwipeOffset = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pageMode) {
                                Color(0xFF557A55)
                            } else {
                                PdfAccent
                            },
                        ),
                    ) {
                        Text(
                            if (pageMode) "单页 ✓" else "单页",
                            fontSize = 11.sp,
                        )
                    }

                    Button(
                        onClick = {
                            answerMode = !answerMode
                            chromeVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (answerMode) {
                                Color(0xFF557A55)
                            } else {
                                PdfAccent
                            },
                        ),
                    ) {
                        Text(
                            if (answerMode) {
                                "答题 ✓"
                            } else {
                                "答题"
                            },
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    chromeVisible = true
                    activity?.setReaderImmersive(false)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(46.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text("☰", fontSize = 17.sp)
            }
        }

        if (answerMode && chromeVisible) {
            Text(
                "答题模式：轻点页面空白处插入文本框；点击已有文本框可修改。",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 66.dp)
                    .background(
                        Color(0xCC557A55),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 5.dp,
                    ),
            )
        }
    }

    selection?.let { sel ->
        val page = analysisCache[sel.paragraphIndex]

        DictionarySheet(
            selection = sel,
            sourceBook = book,
            dictionary = dictionary,
            learningStore = learningStore,
            tts = tts,
            settings = settings,
            initialMarkColor =
                annotations[sel.paragraphIndex]
                    .orEmpty()
                    .firstOrNull {
                        it.startOffset == sel.startOffset &&
                            it.endOffset == sel.endOffset
                    }?.color,
            onDismiss = {
                selection = null
            },
            onSaveVocab = { result ->
                learningStore.addVocabulary(
                    result,
                    sel.sentence,
                    book.id,
                    book.title,
                )
            },
            onWordMark = { color ->
                val fullPageText = page?.text.orEmpty()

                if (
                    fullPageText.isNotBlank() &&
                    sel.endOffset <= fullPageText.length
                ) {
                    if (color != null) {
                        learningStore.addTextAnnotation(
                            bookId = book.id,
                            paragraphIndex = sel.paragraphIndex,
                            fullParagraph = fullPageText,
                            startOffset = sel.startOffset,
                            endOffset = sel.endOffset,
                            color = color,
                        )
                    } else {
                        learningStore.removeTextAnnotation(
                            book.id,
                            sel.paragraphIndex,
                            sel.startOffset,
                            sel.endOffset,
                        )
                    }

                    annotations =
                        learningStore.textAnnotations(book.id)
                }
            },
        )
    }

    sentenceSelection?.let { sel ->
        val page = analysisCache[sel.paragraphIndex]
        val paragraph = page?.text.orEmpty()

        if (paragraph.isNotBlank()) {
            SentenceSheet(
                selection = sel,
                paragraph = paragraph,
                existingAnnotations =
                    annotations[sel.paragraphIndex].orEmpty(),
                settings = settings,
                tts = tts,
                onDictionary = {
                    selection = WordSelection(
                        word = sel.word,
                        sentence = sel.sentence,
                        paragraphIndex = sel.paragraphIndex,
                        startOffset = sel.wordStart,
                        endOffset = sel.wordEnd,
                    )
                    sentenceSelection = null
                },
                onAnnotation = {
                    start,
                    end,
                    color,
                    note,
                    ->
                    if (color == null) {
                        learningStore.removeTextAnnotation(
                            book.id,
                            sel.paragraphIndex,
                            start,
                            end,
                        )
                    } else {
                        learningStore.addTextAnnotation(
                            book.id,
                            sel.paragraphIndex,
                            paragraph,
                            start,
                            end,
                            color,
                            note,
                        )
                    }

                    annotations =
                        learningStore.textAnnotations(book.id)
                },
                onDismiss = {
                    sentenceSelection = null
                },
            )
        }
    }

    if (
        pendingTextBoxPosition != null ||
        editingTextBox != null
    ) {
        val editing = editingTextBox

        AlertDialog(
            onDismissRequest = {
                pendingTextBoxPosition = null
                editingTextBox = null
                textBoxDraft = ""
            },
            title = {
                Text(
                    if (editing == null) {
                        "插入答题文本框"
                    } else {
                        "编辑文本框"
                    }
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = textBoxDraft,
                        onValueChange = {
                            textBoxDraft = it
                        },
                        label = {
                            Text("答案 / 笔记")
                        },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        "文本框会固定在当前 PDF 页面位置，不改变原文件。",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = ::saveTextBox,
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row {
                    if (editing != null) {
                        TextButton(
                            onClick = {
                                textBoxStore.remove(
                                    book.id,
                                    editing.id,
                                )
                                textBoxes =
                                    textBoxStore.list(book.id)
                                editingTextBox = null
                                textBoxDraft = ""
                            }
                        ) {
                            Text(
                                "删除",
                                color = Color(0xFFB44A4A),
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            pendingTextBoxPosition = null
                            editingTextBox = null
                            textBoxDraft = ""
                        }
                    ) {
                        Text("取消")
                    }
                }
            },
        )
    }

    if (jumpDialog) {
        AlertDialog(
            onDismissRequest = {
                jumpDialog = false
            },
            title = {
                Text("跳转到页面")
            },
            text = {
                OutlinedTextField(
                    value = jumpDraft,
                    onValueChange = {
                        jumpDraft = it.filter(Char::isDigit)
                    },
                    singleLine = true,
                    label = {
                        Text("1 – $count")
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpDraft
                            .toIntOrNull()
                            ?.coerceIn(1, count)
                            ?: (currentPage + 1)

                        jumpDialog = false

                        scope.launch {
                            listState.scrollToItem(target - 1)
                        }
                    }
                ) {
                    Text("跳转")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        jumpDialog = false
                    }
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun PdfPageCard(
    file: File,
    pageIndex: Int,
    pageCount: Int,
    pageWidth: Dp,
    zoom: Float,
    analysis: PdfPageAnalysis?,
    annotations: List<TextAnnotation>,
    temporarySelection: Pair<Int, Int>? = null,
    textBoxes: List<PdfTextBoxNote>,
    answerMode: Boolean,
    onWordTap: (PdfWordBox) -> Unit,
    onWordLongPress: (PdfWordBox) -> Unit,
    onAnswerTap: (Float, Float) -> Unit,
    onEditTextBox: (PdfTextBoxNote) -> Unit,
) {
    val density = LocalDensity.current

    val targetWidthPx = with(density) {
        pageWidth.roundToPx().coerceIn(620, 2200)
    }

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = file.absolutePath,
        key2 = pageIndex,
        key3 = targetWidthPx,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                PdfPageAnalyzer.renderPage(
                    file = file,
                    pageIndex = pageIndex,
                    targetWidthPx = targetWidthPx,
                )
            }.getOrNull()
        }
    }

    val rendered = bitmap

    DisposableEffect(rendered) {
        onDispose {
            if (
                rendered != null &&
                !rendered.isRecycled
            ) {
                rendered.recycle()
            }
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${pageIndex + 1} / $pageCount",
            fontSize = 9.sp,
            color = Color(0xFF6F6A64),
            modifier = Modifier.padding(bottom = 3.dp),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (rendered == null) {
                Box(
                    Modifier
                        .width(pageWidth)
                        .height(pageWidth * 1.414f)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val ratio = rendered.width.toFloat() /
                    rendered.height.coerceAtLeast(1)
                val pageHeight = pageWidth / ratio

                Box(
                    Modifier
                        .width(pageWidth)
                        .height(pageHeight)
                        .background(Color.White)
                        .pointerInput(
                            analysis,
                            answerMode,
                            zoom,
                        ) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val nx = (
                                        offset.x /
                                            size.width.coerceAtLeast(1)
                                        ).coerceIn(0f, 1f)

                                    val ny = (
                                        offset.y /
                                            size.height.coerceAtLeast(1)
                                        ).coerceIn(0f, 1f)

                                    if (answerMode) {
                                        onAnswerTap(nx, ny)
                                    } else {
                                        findPdfWord(
                                            analysis = analysis,
                                            x = nx,
                                            y = ny,
                                        )?.let(onWordTap)
                                    }
                                },
                                onLongPress = { offset ->
                                    if (!answerMode) {
                                        val nx = (
                                            offset.x /
                                                size.width.coerceAtLeast(1)
                                            ).coerceIn(0f, 1f)

                                        val ny = (
                                            offset.y /
                                                size.height.coerceAtLeast(1)
                                            ).coerceIn(0f, 1f)

                                        findPdfWord(
                                            analysis = analysis,
                                            x = nx,
                                            y = ny,
                                        )?.let(onWordLongPress)
                                    }
                                },
                            )
                        }
                ) {
                    Image(
                        bitmap = rendered.asImageBitmap(),
                        contentDescription = "PDF 第 ${pageIndex + 1} 页",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )

                    val pageAnalysis = analysis

                    if (
                        pageAnalysis != null &&
                        pageAnalysis.hasTextLayer &&
                        temporarySelection != null
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val (selectionStart, selectionEnd) = temporarySelection
                            pageAnalysis.words.forEach { word ->
                                if (word.startOffset < selectionEnd && word.endOffset > selectionStart) {
                                    drawRect(
                                        color = Color(0xFF2196D3).copy(alpha = 0.66f),
                                        topLeft = Offset(word.left * size.width, word.top * size.height),
                                        size = Size(
                                            (word.right - word.left).coerceAtLeast(0.002f) * size.width,
                                            (word.bottom - word.top).coerceAtLeast(0.004f) * size.height,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    if (
                        pageAnalysis != null &&
                        pageAnalysis.hasTextLayer &&
                        annotations.isNotEmpty()
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            pageAnalysis.words.forEach { word ->
                                val mark = annotations
                                    .firstOrNull {
                                        word.startOffset < it.endOffset &&
                                            word.endOffset > it.startOffset
                                    }
                                    ?: return@forEach

                                val color = pdfAnnotationColor(mark.color)

                                if (mark.color == "underline") {
                                    val y = word.bottom * size.height
                                    drawLine(
                                        color = Color(0xFF7A5E45).copy(alpha = 0.78f),
                                        start = Offset(word.left * size.width, y),
                                        end = Offset(word.right * size.width, y),
                                        strokeWidth = 1.6f,
                                    )
                                } else {
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(
                                            word.left * size.width,
                                            word.top * size.height,
                                        ),
                                        size = Size(
                                            (
                                                word.right -
                                                    word.left
                                                ).coerceAtLeast(0.002f) *
                                                size.width,
                                            (
                                                word.bottom -
                                                    word.top
                                                ).coerceAtLeast(0.004f) *
                                                size.height,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    textBoxes.forEach { item ->
                        val left = pageWidth * item.x
                        val top = pageHeight * item.y
                        val width = pageWidth * item.width
                        val height = pageHeight * item.height

                        Box(
                            Modifier
                                .offset(
                                    x = left,
                                    y = top,
                                )
                                .width(width)
                                .height(height)
                                .background(
                                    Color.White.copy(alpha = 0.84f),
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable(
                                    enabled = answerMode,
                                ) {
                                    onEditTextBox(item)
                                }
                                .padding(4.dp),
                        ) {
                            Text(
                                item.text,
                                fontSize = (
                                    10f * zoom.coerceIn(0.8f, 1.45f)
                                    ).sp,
                                color = Color(0xFF1F1F1F),
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (
                        pageAnalysis != null &&
                        !pageAnalysis.hasTextLayer
                    ) {
                        Text(
                            "扫描页 / 无文字层：可阅读和答题，暂不能点词",
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(6.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.55f),
                                    RoundedCornerShape(5.dp),
                                )
                                .padding(
                                    horizontal = 7.dp,
                                    vertical = 4.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun findPdfWord(
    analysis: PdfPageAnalysis?,
    x: Float,
    y: Float,
): PdfWordBox? {
    val words = analysis?.words.orEmpty()
    if (words.isEmpty()) return null

    val exact = words.firstOrNull { word ->
        x >= word.left - 0.004f &&
            x <= word.right + 0.004f &&
            y >= word.top - 0.006f &&
            y <= word.bottom + 0.006f
    }

    if (exact != null) return exact

    val nearest = words.minByOrNull { word ->
        val cx = (word.left + word.right) / 2f
        val cy = (word.top + word.bottom) / 2f
        val dx = cx - x
        val dy = cy - y
        dx * dx + dy * dy
    } ?: return null

    val cx = (nearest.left + nearest.right) / 2f
    val cy = (nearest.top + nearest.bottom) / 2f

    val distance = sqrt(
        (cx - x) * (cx - x) +
            (cy - y) * (cy - y)
    )

    return nearest.takeIf { distance <= 0.026f }
}

private fun pdfAnnotationColor(name: String): Color =
    when (name) {
        "green" -> Color(0xFF43A047).copy(alpha = 0.64f)
        "blue" -> Color(0xFF3B82D0).copy(alpha = 0.62f)
        "pink" -> Color(0xFFE75480).copy(alpha = 0.64f)
        "gray" -> Color(0xFF8A8A8A).copy(alpha = 0.54f)
        else -> Color(0xFFF0B429).copy(alpha = 0.68f)
    }

private fun pdfSentenceRange(
    text: String,
    offset: Int,
): IntRange? = FrenchSentenceSegmenter.range(text, offset)

private fun pdfExtractSentence(
    text: String,
    offset: Int,
): String = FrenchSentenceSegmenter.sentence(text, offset, maxChars = 520)
