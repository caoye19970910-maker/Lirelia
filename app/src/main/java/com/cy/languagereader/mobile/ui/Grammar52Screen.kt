@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.cy.languagereader.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.languagereader.mobile.data.AdaptiveTrainingStore
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.DictionaryLookup
import com.cy.languagereader.mobile.data.FrenchAnswerJudge
import com.cy.languagereader.mobile.data.Grammar52Data
import com.cy.languagereader.mobile.data.GrammarExercise
import com.cy.languagereader.mobile.data.GrammarLesson
import com.cy.languagereader.mobile.data.GrammarMemoryCard
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.service.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class GrammarStudyMode { OVERVIEW, MEMORY, PRACTICE }

private data class GrammarLookupTarget(
    val term: String,
    val fullFrench: String,
    val chinese: String,
    val lessonNumber: Int,
)

@Composable
fun Grammar52Screen(
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLesson by rememberSaveable { mutableIntStateOf(0) }
    var refreshToken by remember { mutableIntStateOf(0) }

    if (selectedLesson == 0) {
        Grammar52Hub(
            modifier = modifier,
            adaptiveStore = adaptiveStore,
            refreshToken = refreshToken,
            onBack = onBack,
            onOpenLesson = { selectedLesson = it },
        )
    } else {
        GrammarLessonScreen(
            modifier = modifier,
            lesson = Grammar52Data.lesson(selectedLesson) ?: Grammar52Data.lessons.first(),
            dictionary = dictionary,
            learningStore = learningStore,
            tts = tts,
            adaptiveStore = adaptiveStore,
            onBack = {
                selectedLesson = 0
                refreshToken++
            },
            onProgressChanged = { refreshToken++ },
        )
    }
}

@Composable
private fun Grammar52Hub(
    modifier: Modifier,
    adaptiveStore: AdaptiveTrainingStore,
    refreshToken: Int,
    onBack: () -> Unit,
    onOpenLesson: (Int) -> Unit,
) {
    val allIds = remember { Grammar52Data.allCardIds() + Grammar52Data.allExerciseIds() }
    val states = remember(refreshToken) { adaptiveStore.states(allIds) }
    val now = System.currentTimeMillis()
    val average = if (allIds.isEmpty()) 0f else allIds.map { states[it]?.mastery ?: 0f }.average().toFloat()
    val mastered = allIds.count { (states[it]?.mastery ?: 0f) >= 0.85f }
    val dueIds = allIds.filter { id -> states[id]?.let { it.nextDueAt <= now } == true }.toSet()
    val weakIds = allIds.filter { id -> states[id]?.let { it.attempts >= 2 && it.mastery < 0.55f } == true }.toSet()
    val startedLessons = Grammar52Data.lessons.count { lesson ->
        lesson.cards.indices.any { idx -> states["grammar:memory:${lesson.number}:$idx"] != null }
    }
    fun lessonIds(lesson: GrammarLesson): List<String> =
        lesson.cards.indices.map { "grammar:memory:${lesson.number}:$it" } +
            Grammar52Data.practiceExercises(lesson).map { "grammar:practice:${lesson.number}:${it.id}" }
    val dueLesson = Grammar52Data.lessons.firstOrNull { lesson -> lessonIds(lesson).any { it in dueIds } }
    val recommended = dueLesson ?: Grammar52Data.lessons.firstOrNull { lesson ->
        lessonIds(lesson).any { id -> (states[id]?.mastery ?: 0f) < 0.70f }
    } ?: Grammar52Data.lessons.first()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text("← 训练") }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("🧠 52语法", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("每课先理解→记忆→至少6个训练点；长按法语即可查词/翻译", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("整体进度", fontWeight = FontWeight.Bold)
                    Text("${(average * 100).toInt()}%")
                }
                LinearProgressIndicator(progress = { average.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("已开始 $startedLessons / 52 课 · 已掌握 $mastered / ${allIds.size} · 到期 ${dueIds.size} · 薄弱 ${weakIds.size}", fontSize = 12.sp, color = Color.Gray)
                Button(onClick = { onOpenLesson(recommended.number) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (dueLesson != null) "复习到期语法 · 第${recommended.number}课 ${recommended.titleChinese}" else "继续学习 · 第${recommended.number}课 ${recommended.titleChinese}")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Grammar52Data.lessons.groupBy { it.category }.forEach { (category, lessons) ->
                item {
                    Text(category, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                }
                items(lessons, key = { it.number }) { lesson ->
                    val ids = lesson.cards.indices.map { "grammar:memory:${lesson.number}:$it" } +
                        Grammar52Data.practiceExercises(lesson).map { "grammar:practice:${lesson.number}:${it.id}" }
                    val progress = if (ids.isEmpty()) 0f else ids.map { states[it]?.mastery ?: 0f }.average().toFloat()
                    val dueCount = ids.count { it in dueIds }
                    GrammarLessonRow(lesson, progress, dueCount) { onOpenLesson(lesson.number) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GrammarLessonRow(lesson: GrammarLesson, progress: Float, dueCount: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(42.dp).height(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(lesson.number.toString(), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.titleChinese, fontWeight = FontWeight.SemiBold)
                Text("${lesson.titleFrench} · ${lesson.cards.size}张记忆卡 · ${Grammar52Data.practiceExercises(lesson).size}个训练点", fontSize = 12.sp, color = Color.Gray)
                Text("教材 p.${lesson.bookPage}", fontSize = 10.sp, color = Color.Gray)
                if (!lesson.bodyAvailableInUploadedPdf) {
                    Text("当前PDF仅有目录标题", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (dueCount > 0) Text("到期 $dueCount", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.width(66.dp))
            }
        }
    }
}

@Composable
private fun GrammarLessonScreen(
    modifier: Modifier,
    lesson: GrammarLesson,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onBack: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    var modeName by rememberSaveable(lesson.number) { mutableStateOf(GrammarStudyMode.MEMORY.name) }
    val mode = GrammarStudyMode.valueOf(modeName)
    var lookup by remember { mutableStateOf<GrammarLookupTarget?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text("← 52语法") }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${lesson.number}. ${lesson.titleChinese}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${lesson.titleFrench} · 教材 p.${lesson.bookPage}", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (mode == GrammarStudyMode.OVERVIEW) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("⚡ 总览") }
            else OutlinedButton(onClick = { modeName = GrammarStudyMode.OVERVIEW.name }, modifier = Modifier.weight(1f)) { Text("⚡ 总览") }
            if (mode == GrammarStudyMode.MEMORY) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("🧠 记忆") }
            else OutlinedButton(onClick = { modeName = GrammarStudyMode.MEMORY.name }, modifier = Modifier.weight(1f)) { Text("🧠 记忆") }
            if (mode == GrammarStudyMode.PRACTICE) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("✍ 练习") }
            else OutlinedButton(onClick = { modeName = GrammarStudyMode.PRACTICE.name }, modifier = Modifier.weight(1f)) { Text("✍ 练习") }
        }

        if (!lesson.bodyAvailableInUploadedPdf) {
            Text(
                "提示：你上传的PDF正文在第32课后中断；这一课先按教材目录主题制作标准 A2–B1 记忆与练习。",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
            when (mode) {
                GrammarStudyMode.OVERVIEW -> GrammarQuickOverview(
                    lesson = lesson,
                    onLookup = { term, fr, zh -> lookup = GrammarLookupTarget(term, fr, zh, lesson.number) },
                )
                GrammarStudyMode.MEMORY -> GrammarMemorySession(
                    lesson = lesson,
                    adaptiveStore = adaptiveStore,
                    onLookup = { term, fr, zh -> lookup = GrammarLookupTarget(term, fr, zh, lesson.number) },
                    onFinished = { modeName = GrammarStudyMode.PRACTICE.name },
                    onProgressChanged = onProgressChanged,
                )
                GrammarStudyMode.PRACTICE -> GrammarPracticeSession(
                    lesson = lesson,
                    adaptiveStore = adaptiveStore,
                    onLookup = { term, fr, zh -> lookup = GrammarLookupTarget(term, fr, zh, lesson.number) },
                    onProgressChanged = onProgressChanged,
                )
            }
        }
    }

    lookup?.let { target ->
        GrammarLookupDialog(
            target = target,
            dictionary = dictionary,
            learningStore = learningStore,
            tts = tts,
            onDismiss = { lookup = null },
        )
    }
}

@Composable
private fun GrammarQuickOverview(
    lesson: GrammarLesson,
    onLookup: (String, String, String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("⚡ 30秒速览", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("只看公式、关键规则、典型例句和易错提醒。长按法语仍可查词/翻译。", fontSize = 11.sp, color = Color.Gray)
            }
        }
        if (lesson.cards.size >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("↔ 一眼区分", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    lesson.cards.forEach { card ->
                        Text(card.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        LongPressFrenchText(
                            text = card.formula,
                            chinese = card.rule,
                            fontSize = 13,
                            onLookup = onLookup,
                        )
                    }
                }
            }
        }
        lesson.cards.forEachIndexed { index, card ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("${index + 1}. ${card.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    LongPressFrenchText(
                        text = card.formula,
                        chinese = card.rule,
                        fontSize = 14,
                        fontWeight = FontWeight.SemiBold,
                        onLookup = onLookup,
                    )
                    Text(card.rule, fontSize = 12.sp, lineHeight = 18.sp, color = Color.DarkGray)
                    card.examples.take(2).forEach { ex ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            LongPressFrenchText(
                                text = ex.french,
                                chinese = ex.chinese,
                                fontSize = 14,
                                fontWeight = FontWeight.Medium,
                                onLookup = onLookup,
                            )
                            Text(ex.chinese, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Text("⚠ ${card.tip}", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GrammarMemorySession(
    lesson: GrammarLesson,
    adaptiveStore: AdaptiveTrainingStore,
    onLookup: (String, String, String) -> Unit,
    onFinished: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    var index by rememberSaveable(lesson.number) { mutableIntStateOf(0) }
    val card = lesson.cards[index.coerceIn(0, lesson.cards.lastIndex)]
    val itemId = "grammar:memory:${lesson.number}:$index"
    val state = remember(index) { adaptiveStore.state(itemId) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("记忆 ${index + 1}/${lesson.cards.size}", fontWeight = FontWeight.Bold)
                Text("掌握 ${(state?.mastery?.times(100) ?: 0f).toInt()}%", color = Color.Gray, fontSize = 12.sp)
            }
            LinearProgressIndicator(progress = { (index + 1f) / lesson.cards.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            GrammarMemoryCardView(card, onLookup)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("你现在记住了吗？", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        adaptiveStore.record(itemId, "grammar_memory", false, 30_000L, if (lesson.bodyAvailableInUploadedPdf) "grammar_book" else "grammar_toc_standard")
                        onProgressChanged()
                        if (index < lesson.cards.lastIndex) index++ else onFinished()
                    }, modifier = Modifier.weight(1f),
                ) { Text("😵 不会") }
                OutlinedButton(
                    onClick = {
                        adaptiveStore.record(
                            itemId, "grammar_memory", true, 15_000L,
                            if (lesson.bodyAvailableInUploadedPdf) "grammar_book" else "grammar_toc_standard",
                            confidence = 0.55f,
                        )
                        onProgressChanged()
                        if (index < lesson.cards.lastIndex) index++ else onFinished()
                    }, modifier = Modifier.weight(1f),
                ) { Text("🤔 模糊") }
                Button(
                    onClick = {
                        adaptiveStore.record(itemId, "grammar_memory", true, 2_500L, if (lesson.bodyAvailableInUploadedPdf) "grammar_book" else "grammar_toc_standard")
                        onProgressChanged()
                        if (index < lesson.cards.lastIndex) index++ else onFinished()
                    }, modifier = Modifier.weight(1f),
                ) { Text("✅ 记住") }
            }
        }
    }
}

@Composable
private fun GrammarMemoryCardView(card: GrammarMemoryCard, onLookup: (String, String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(card.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(card.rule, fontSize = 15.sp, lineHeight = 22.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                LongPressFrenchText(
                    text = card.formula,
                    chinese = card.rule,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    fontSize = 15,
                    fontWeight = FontWeight.SemiBold,
                    onLookup = onLookup,
                )
            }
            card.examples.forEach { ex ->
                Column {
                    LongPressFrenchText(
                        text = ex.french,
                        chinese = ex.chinese,
                        fontSize = 16,
                        fontWeight = FontWeight.Medium,
                        onLookup = onLookup,
                    )
                    Text(ex.chinese, color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider()
            Text("💡 ${card.tip}", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.secondary)
            Text("长按上面的法语词/表达即可查词、看中文并加入生词本。", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun GrammarPracticeSession(
    lesson: GrammarLesson,
    adaptiveStore: AdaptiveTrainingStore,
    onLookup: (String, String, String) -> Unit,
    onProgressChanged: () -> Unit,
) {
    var index by rememberSaveable(lesson.number) { mutableIntStateOf(0) }
    var input by remember(lesson.number, index) { mutableStateOf("") }
    var wrong by remember(lesson.number, index) { mutableStateOf(false) }
    var feedback by remember(lesson.number, index) { mutableStateOf("") }
    var startedAt by remember(lesson.number, index) { mutableLongStateOf(System.currentTimeMillis()) }
    var correctFlash by remember { mutableStateOf(false) }
    val focusRequester = remember(lesson.number, index) { FocusRequester() }

    val exercises = remember(lesson.number) { Grammar52Data.practiceExercises(lesson) }
    val exercise = exercises[index.coerceIn(0, exercises.lastIndex)]
    LaunchedEffect(exercise.id, wrong, correctFlash) {
        if (exercise.options.isEmpty() && !wrong && !correctFlash) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }
    fun nextQuestion() {
        index = if (index >= exercises.lastIndex) 0 else index + 1
        input = ""
        wrong = false
        feedback = ""
        startedAt = System.currentTimeMillis()
    }
    fun submit(answer: String) {
        if (wrong) return
        val judgement = FrenchAnswerJudge.short(answer, exercise.answers)
        val correct = judgement.correct
        val responseMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(250L)
        val id = "grammar:practice:${lesson.number}:${exercise.id}"
        adaptiveStore.record(id, "grammar_practice", correct, responseMs, if (lesson.bodyAvailableInUploadedPdf) "grammar_book" else "grammar_toc_standard")
        onProgressChanged()
        if (correct) {
            correctFlash = true
        } else {
            wrong = true
            feedback = buildString {
                append("正确答案：${exercise.answers.first()}\n")
                if (judgement.differenceHint.isNotBlank()) append(judgement.differenceHint).append('\n')
                append(exercise.explanation)
            }
        }
    }

    fun revealAnswer() {
        if (wrong || correctFlash) return
        val responseMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(250L)
        val id = "grammar:practice:${lesson.number}:${exercise.id}"
        adaptiveStore.record(id, "grammar_practice", false, responseMs, if (lesson.bodyAvailableInUploadedPdf) "grammar_book" else "grammar_toc_standard")
        onProgressChanged()
        wrong = true
        feedback = "正确答案：${exercise.answers.first()}\n${exercise.explanation}"
    }

    LaunchedEffect(correctFlash) {
        if (correctFlash) {
            delay(320)
            correctFlash = false
            nextQuestion()
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("练习 ${index + 1}/${exercises.size}", fontWeight = FontWeight.Bold)
                    Text(if (exercise.id.startsWith("auto_")) "理解检查" else "应用练习", fontSize = 10.sp, color = Color.Gray)
                }
                AnimatedVisibility(
                    visible = correctFlash,
                    enter = fadeIn() + scaleIn(initialScale = 0.75f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                ) {
                    Text("✨ Bien !", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(progress = { (index + 1f) / exercises.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LongPressFrenchText(
                        text = exercise.prompt,
                        chinese = exercise.chineseHint,
                        fontSize = 21,
                        fontWeight = FontWeight.Bold,
                        onLookup = onLookup,
                    )
                    if (exercise.chineseHint.isNotBlank()) Text(exercise.chineseHint, color = Color.Gray, fontSize = 12.sp)

                    if (exercise.options.isNotEmpty()) {
                        exercise.options.forEach { option ->
                            GrammarOption(
                                option = option,
                                enabled = !wrong && !correctFlash,
                                onClick = { submit(option) },
                                onLongPress = { onLookup(option, option, "") },
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { if (!wrong) input = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            label = { Text("输入答案") },
                            enabled = !wrong && !correctFlash,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank() && !wrong && !correctFlash) submit(input) }),
                        )
                        Button(onClick = { submit(input) }, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank() && !wrong && !correctFlash) {
                            Text("提交")
                        }
                    }

                    if (!wrong && !correctFlash) {
                        TextButton(onClick = ::revealAnswer, modifier = Modifier.fillMaxWidth()) {
                            Text("想不起来 · 直接看答案")
                        }
                    }

                    if (wrong) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("这题先停一下", fontWeight = FontWeight.Bold)
                                Text(feedback, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        if (wrong) {
            Button(onClick = ::nextQuestion, modifier = Modifier.fillMaxWidth()) { Text("下一题") }
        } else {
            Text(
                "答对会自动进入下一题；答错才停下来查看解释。",
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 11.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GrammarOption(
    option: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    ) {
        Text(option, modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), fontSize = 16.sp)
    }
}

@Composable
private fun LongPressFrenchText(
    text: String,
    chinese: String,
    onLookup: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
    fontWeight: FontWeight? = null,
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        modifier = modifier.pointerInput(text) {
            detectTapGestures(onLongPress = { pos: Offset ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(pos).coerceIn(0, text.length.coerceAtLeast(1) - 1)
                val token = frenchLookupAt(text, offset)
                if (token.isNotBlank()) onLookup(dictionaryFriendlyToken(token), text, chinese)
            })
        },
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        onTextLayout = { layout = it },
    )
}

private val grammarLookupPhrases = listOf(
    "être sur le point de", "être en train de", "à condition de", "à condition que",
    "avoir besoin de", "avoir envie de", "avoir peur de", "être capable de",
    "venir de", "il faut que", "bien que", "pour que", "afin que", "afin de",
    "grâce à", "à cause de", "en raison de", "parce que", "tandis que", "alors que",
    "avant de", "après avoir", "après être", "de plus en plus", "de moins en moins",
    "plus que", "moins que", "aussi que", "autant que", "au lieu de", "près de",
    "loin de", "à côté de", "au-dessus de", "au-dessous de", "avoir l'habitude de",
    "avoir l'intention de", "permettre de", "permettre à", "essayer de", "réussir à",
).sortedByDescending { it.length }

private val grammarPhraseMeanings = mapOf(
    "être sur le point de" to "正要……；即将……",
    "être en train de" to "正在……",
    "à condition de" to "条件是……；只要……（后接不定式）",
    "à condition que" to "条件是……；只要……（后接虚拟式）",
    "avoir besoin de" to "需要……",
    "avoir envie de" to "想要……",
    "avoir peur de" to "害怕……",
    "être capable de" to "能够……",
    "venir de" to "刚刚……",
    "il faut que" to "必须……；需要……（后接虚拟式）",
    "bien que" to "虽然；尽管（后接虚拟式）",
    "pour que" to "为了使……（后接虚拟式）",
    "afin que" to "为了使……（后接虚拟式）",
    "afin de" to "为了……（后接不定式）",
    "grâce à" to "多亏；由于（通常表示积极原因）",
    "à cause de" to "因为；由于（常表示消极原因）",
    "en raison de" to "由于；鉴于",
    "parce que" to "因为",
    "tandis que" to "而；然而；当……时",
    "alors que" to "而；然而；当……时",
    "avant de" to "在……之前",
    "après avoir" to "在做完……之后（avoir 作助动词）",
    "après être" to "在……之后（être 作助动词）",
    "au lieu de" to "而不是；代替",
    "près de" to "在……附近",
    "loin de" to "远离……",
    "à côté de" to "在……旁边",
    "au-dessus de" to "在……上方",
    "au-dessous de" to "在……下方",
    "avoir l'habitude de" to "习惯于……",
    "avoir l'intention de" to "打算……",
    "permettre de" to "使……成为可能；允许做……",
    "permettre à" to "允许某人……",
    "essayer de" to "尝试……",
    "réussir à" to "成功做到……",
)

private fun frenchLookupAt(text: String, offset: Int): String {
    if (text.isBlank()) return ""
    val normalized = text.lowercase(Locale.ROOT).replace('’', '\'')
    val safeOffset = offset.coerceIn(0, text.lastIndex)
    grammarLookupPhrases.forEach { phrase ->
        val needle = phrase.lowercase(Locale.ROOT).replace('’', '\'')
        var from = 0
        while (from < normalized.length) {
            val start = normalized.indexOf(needle, from)
            if (start < 0) break
            val endExclusive = start + needle.length
            if (safeOffset in start until endExclusive) return text.substring(start, endExclusive)
            from = start + 1
        }
    }
    return frenchTokenAt(text, safeOffset)
}

private fun grammarPhraseTranslation(term: String): String? {
    val normalized = term.trim().lowercase(Locale.ROOT).replace('’', '\'')
    return grammarPhraseMeanings.entries.firstOrNull {
        it.key.lowercase(Locale.ROOT).replace('’', '\'') == normalized
    }?.value
}

private fun frenchTokenAt(text: String, offset: Int): String {
    if (text.isBlank()) return ""
    fun allowed(c: Char) = c.isLetter() || c == '\'' || c == '’' || c == '-' || c == '‑'
    var start = offset.coerceIn(0, text.lastIndex)
    var end = start
    if (!allowed(text[start])) {
        var left = start - 1
        while (left >= 0 && !allowed(text[left])) left--
        if (left < 0) return ""
        start = left
        end = left
    }
    while (start > 0 && allowed(text[start - 1])) start--
    while (end < text.lastIndex && allowed(text[end + 1])) end++
    return text.substring(start, end + 1).trim('-', '‑', '\'', '’')
}

private fun dictionaryFriendlyToken(token: String): String {
    val clean = token.trim()
    val apostropheIndex = clean.indexOfAny(charArrayOf('\'', '’'))
    if (apostropheIndex in 1..3) {
        val prefix = clean.substring(0, apostropheIndex).lowercase(Locale.ROOT)
        if (prefix in setOf("j", "m", "t", "s", "n", "l", "d", "c", "qu")) {
            return clean.substring(apostropheIndex + 1)
        }
    }
    return clean
}

@Composable
private fun GrammarLookupDialog(
    target: GrammarLookupTarget,
    dictionary: DictionaryRepository,
    learningStore: LearningStore,
    tts: TtsManager,
    onDismiss: () -> Unit,
) {
    var lookup by remember(target.term) { mutableStateOf<DictionaryLookup?>(null) }
    var added by remember(target.term) { mutableStateOf(learningStore.containsVocabulary(target.term)) }

    LaunchedEffect(target.term) {
        lookup = withContext(Dispatchers.IO) { dictionary.lookupSafe(target.term) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.term, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val loaded = lookup
                if (loaded == null) {
                    Text("正在查询离线词典…", color = Color.Gray)
                } else {
                    val result = loaded.result
                    if (result.lemma.isNotBlank() && !result.lemma.equals(target.term, ignoreCase = true)) {
                        Text("原形：${result.lemma}", fontWeight = FontWeight.SemiBold)
                    }
                    grammarPhraseTranslation(target.term)?.let { meaning ->
                        Text("短语：$meaning", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (result.definitions.isNotEmpty()) {
                        result.definitions.take(4).forEach { Text("• $it") }
                    } else {
                        Text("离线词典暂未找到这个词。", color = Color.Gray)
                    }
                    if (target.fullFrench.isNotBlank()) {
                        HorizontalDivider()
                        Text(target.fullFrench, fontWeight = FontWeight.Medium)
                        if (target.chinese.isNotBlank()) Text(target.chinese, color = Color.Gray)
                    }
                    Text(loaded.source, fontSize = 10.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { tts.speak(target.term) }) { Text("🔊 发音") }
                TextButton(
                    enabled = lookup != null,
                    onClick = {
                        val result = lookup?.result ?: return@TextButton
                        learningStore.addVocabulary(
                            result = result,
                            contextText = target.fullFrench,
                            bookId = "grammar52_${target.lessonNumber}",
                            bookTitle = "52语法 · 第${target.lessonNumber}课",
                        )
                        added = true
                    },
                ) { Text(if (added) "✓ 已加入" else "+ 生词") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

private fun normalizeGrammarAnswer(value: String): String = FrenchAnswerJudge.normalizeShort(value)
