package com.cy.languagereader.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cy.languagereader.mobile.data.AdaptiveTrainingStats
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.AdaptiveTrainingStore
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.FrenchAnswerJudge
import com.cy.languagereader.mobile.data.FrenchConjugator
import com.cy.languagereader.mobile.data.FrenchTrainingData
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.data.NounGenderQuestion
import com.cy.languagereader.mobile.data.PhraseItem
import com.cy.languagereader.mobile.data.ReadingStatsStore
import com.cy.languagereader.mobile.data.SentenceOutputData
import com.cy.languagereader.mobile.data.SentenceOutputQuestion
import com.cy.languagereader.mobile.data.TrainingProgress
import com.cy.languagereader.mobile.data.TrainingProgressStore
import com.cy.languagereader.mobile.data.TrainingSessionSnapshot
import com.cy.languagereader.mobile.data.TrainingSessionStore
import com.cy.languagereader.mobile.data.TrainingWrongItem
import com.cy.languagereader.mobile.data.VerbPrepositionQuestion
import com.cy.languagereader.mobile.data.VocabularyContext
import com.cy.languagereader.mobile.data.VocabularyItem
import com.cy.languagereader.mobile.service.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class TrainingMode(val key: String, val label: String, val emoji: String) {
    DAILY("daily", "今日混合", "📅"),
    GENDER("gender", "阴阳性", "⚥"),
    PREPOSITION("preposition", "à/de", "🔗"),
    CONJUGATION("conjugation", "变位", "⚡"),
    SENTENCE("sentence", "整句输出", "✍️"),
    PERSONAL("personal", "我的生词", "📖"),
    PHRASE("phrase", "我的表达", "💬"),
}

private enum class MixKind { GENDER, PREPOSITION, CONJUGATION, SENTENCE, PERSONAL, PHRASE }

private val CORE_TRAINING_KINDS = listOf("gender", "preposition", "conjugation", "sentence_output", "personal_vocab", "personal_phrase")

private data class ConjugationQuiz(
    val lemma: String,
    val tense: String,
    val subjectIndex: Int,
    val answer: String,
    val allForms: List<String>,
) {
    val id: String get() = "conj:${lemma.lowercase()}:$tense:$subjectIndex"
}

private typealias AdaptiveAnswer = (itemId: String, kind: String, correct: Boolean, responseMs: Long, source: String) -> Unit

@Composable
fun FrenchTrainingScreen(
    tts: TtsManager,
    learningStore: LearningStore,
    dictionary: DictionaryRepository,
    settings: AppSettings,
    readingStats: ReadingStatsStore,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val aggregateStore = remember { TrainingProgressStore(context) }
    val adaptiveStore = remember { AdaptiveTrainingStore(context) }
    val sessionStore = remember { TrainingSessionStore(context) }
    var resumableSession by remember {
        val saved = sessionStore.load()
        if (saved != null && saved.modeName == TrainingMode.DAILY.name && saved.target > 0 && saved.answered >= saved.target) {
            sessionStore.finish(saved.modeName)
            mutableStateOf<TrainingSessionSnapshot?>(null)
        } else {
            mutableStateOf(saved)
        }
    }
    var sessionWrongItems by remember { mutableStateOf(resumableSession?.wrongItems.orEmpty()) }
    var lastCompletedWrongItems by remember { mutableStateOf(sessionStore.lastCompletedWrongItems()) }
    var sessionModeName by rememberSaveable { mutableStateOf<String?>(null) }
    var resumeKindName by rememberSaveable { mutableStateOf("") }
    var resumeItemId by rememberSaveable { mutableStateOf("") }
    var resumeWrong by rememberSaveable { mutableStateOf(false) }
    var grammarOpen by rememberSaveable { mutableStateOf(false) }
    val sessionMode = sessionModeName?.let { TrainingMode.valueOf(it) }

    var sessionAnswered by rememberSaveable { mutableIntStateOf(0) }
    var sessionCorrect by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    var sessionTarget by rememberSaveable { mutableIntStateOf(0) }
    var successToken by rememberSaveable { mutableIntStateOf(0) }
    var persisted by remember(sessionModeName) {
        mutableStateOf(aggregateStore.load(sessionMode?.key ?: TrainingMode.DAILY.key))
    }
    var adaptiveStats by remember { mutableStateOf(AdaptiveTrainingStats(0, 0, 0, 0f)) }
    var weakCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(adaptiveStore) {
        val loaded = withContext(Dispatchers.IO) {
            adaptiveStore.statsForKinds(CORE_TRAINING_KINDS) to adaptiveStore.weakCount(CORE_TRAINING_KINDS)
        }
        adaptiveStats = loaded.first
        weakCount = loaded.second
    }

    fun startSession(mode: TrainingMode) {
        val target = if (mode == TrainingMode.DAILY) recommendedDailyQuestionCount(adaptiveStats.due, weakCount) else 0
        sessionStore.start(mode.name, target)
        resumableSession = null
        sessionWrongItems = emptyList()
        resumeKindName = ""
        resumeItemId = ""
        resumeWrong = false
        sessionModeName = mode.name
        sessionAnswered = 0
        sessionCorrect = 0
        streak = 0
        sessionTarget = target
        persisted = aggregateStore.load(mode.key)
        adaptiveStats = adaptiveStore.statsForKinds(CORE_TRAINING_KINDS)
        weakCount = adaptiveStore.weakCount(CORE_TRAINING_KINDS)
    }

    fun resumeSession(snapshot: TrainingSessionSnapshot) {
        val mode = runCatching { TrainingMode.valueOf(snapshot.modeName) }.getOrNull() ?: return
        resumableSession = null
        sessionWrongItems = snapshot.wrongItems
        resumeKindName = snapshot.currentKind
        resumeItemId = snapshot.currentItemId
        resumeWrong = snapshot.currentWrong
        sessionModeName = mode.name
        sessionAnswered = snapshot.answered
        sessionCorrect = snapshot.correct
        streak = snapshot.streak
        sessionTarget = snapshot.target
        persisted = aggregateStore.load(mode.key)
    }

    fun leaveSession() {
        val activeMode = sessionMode
        if (activeMode != null) {
            val completedDaily = activeMode == TrainingMode.DAILY && sessionTarget > 0 && sessionAnswered >= sessionTarget
            if (completedDaily) {
                sessionStore.finish(activeMode.name)
                lastCompletedWrongItems = sessionWrongItems
            } else {
                sessionStore.update(activeMode.name, sessionAnswered, sessionCorrect, streak, sessionTarget)
            }
        }
        sessionModeName = null
        resumeKindName = ""
        resumeItemId = ""
        resumeWrong = false
        sessionAnswered = 0
        sessionCorrect = 0
        streak = 0
        sessionTarget = 0
        resumableSession = sessionStore.load()?.takeUnless {
            it.modeName == TrainingMode.DAILY.name && it.target > 0 && it.answered >= it.target
        }
        sessionWrongItems = resumableSession?.wrongItems.orEmpty()
        adaptiveStats = adaptiveStore.statsForKinds(CORE_TRAINING_KINDS)
        weakCount = adaptiveStore.weakCount(CORE_TRAINING_KINDS)
    }

    fun record(itemId: String, kind: String, correct: Boolean, responseMs: Long, source: String) {
        val activeMode = sessionMode ?: return
        sessionAnswered += 1
        if (correct) {
            sessionCorrect += 1
            streak += 1
            successToken += 1
        } else {
            streak = 0
        }
        adaptiveStore.record(itemId, kind, correct, responseMs, source)
        persisted = aggregateStore.record(activeMode.key, correct, streak)
        sessionStore.update(activeMode.name, sessionAnswered, sessionCorrect, streak, sessionTarget)
        // If the answer was correct, the old question is already consumed. Clear it
        // before the composable advances so a process kill in this tiny window cannot
        // make the answered question reappear after resume. Wrong answers stay pinned.
        if (correct) {
            sessionStore.clearQuestion(activeMode.name)
        } else {
            sessionStore.recordWrong(activeMode.name, kind, itemId)
            sessionWrongItems = sessionStore.load()?.wrongItems ?: (sessionWrongItems + TrainingWrongItem(kind, itemId)).distinctBy { it.kind to it.itemId }
        }
        adaptiveStats = adaptiveStore.statsForKinds(CORE_TRAINING_KINDS)
        weakCount = adaptiveStore.weakCount(CORE_TRAINING_KINDS)
    }

    val personalWords = remember(sessionModeName, sessionAnswered) {
        learningStore.vocabulary().filter { it.status != "known" && it.word.isNotBlank() }
    }
    val personalPhrases = remember(sessionModeName, sessionAnswered) {
        learningStore.expressions().filter { it.status != "known" && it.expression.isNotBlank() }
    }
    LaunchedEffect(personalWords.map { it.word to it.status }, personalPhrases.map { it.expression to it.status }) {
        val activeWordIds = personalWords.map(::personalId).toSet()
        val activePhraseIds = personalPhrases.map(::phraseId).toSet()
        withContext(Dispatchers.IO) {
            adaptiveStore.prunePersonalVocabulary(activeWordIds)
            adaptiveStore.prunePersonalPhrases(activePhraseIds)
        }
    }

    if (grammarOpen) {
        Grammar52Screen(
            dictionary = dictionary,
            learningStore = learningStore,
            tts = tts,
            adaptiveStore = adaptiveStore,
            onBack = {
                grammarOpen = false
                adaptiveStats = adaptiveStore.statsForKinds(CORE_TRAINING_KINDS)
                weakCount = adaptiveStore.weakCount(CORE_TRAINING_KINDS)
            },
            modifier = modifier,
        )
    } else if (sessionMode == null) {
        TrainingHub(
            modifier = modifier,
            adaptiveStats = adaptiveStats,
            learningStore = learningStore,
            settings = settings,
            readingStats = readingStats,
            weakCount = weakCount,
            personalCount = personalWords.size,
            phraseCount = personalPhrases.size,
            personalWords = personalWords,
            personalPhrases = personalPhrases,
            builtInCount = FrenchTrainingData.nounGender.size + FrenchTrainingData.verbPrepositions.size +
                FrenchConjugator.irregularTrainingVerbs().size * 4 * 6 + SentenceOutputData.questions.size,
            resume = resumableSession,
            lastCompletedWrongItems = lastCompletedWrongItems,
            onResume = ::resumeSession,
            onDiscardResume = {
                resumableSession?.let { snapshot ->
                    sessionStore.finish(snapshot.modeName)
                    lastCompletedWrongItems = snapshot.wrongItems
                }
                resumableSession = null
            },
            onStart = ::startSession,
            onGrammar = { grammarOpen = true },
        )
    } else {
        TrainingSession(
            modifier = modifier,
            mode = sessionMode,
            tts = tts,
            learningStore = learningStore,
            adaptiveStore = adaptiveStore,
            personalWords = personalWords,
            personalPhrases = personalPhrases,
            sessionAnswered = sessionAnswered,
            sessionCorrect = sessionCorrect,
            streak = streak,
            sessionTarget = sessionTarget,
            persisted = persisted,
            successToken = successToken,
            resumeKindName = resumeKindName,
            resumeItemId = resumeItemId,
            resumeWrong = resumeWrong,
            wrongItems = sessionWrongItems,
            onQuestionVisible = { kind, itemId ->
                sessionMode?.let { sessionStore.updateQuestion(it.name, kind, itemId) }
                resumeKindName = ""
                resumeItemId = ""
                resumeWrong = false
            },
            onBack = ::leaveSession,
            onAnswered = ::record,
        )
    }
}

@Composable
private fun TrainingHub(
    modifier: Modifier,
    adaptiveStats: AdaptiveTrainingStats,
    learningStore: LearningStore,
    settings: AppSettings,
    readingStats: ReadingStatsStore,
    weakCount: Int,
    personalCount: Int,
    phraseCount: Int,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
    builtInCount: Int,
    resume: TrainingSessionSnapshot?,
    lastCompletedWrongItems: List<TrainingWrongItem>,
    onResume: (TrainingSessionSnapshot) -> Unit,
    onDiscardResume: () -> Unit,
    onStart: (TrainingMode) -> Unit,
    onGrammar: () -> Unit,
) {
    val dailyTarget = recommendedDailyQuestionCount(adaptiveStats.due, weakCount)
    val estimatedMinutes = ((dailyTarget * 25 + 59) / 60).coerceAtLeast(3)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("法语训练", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "每天先清到期和薄弱项，再加入少量新内容。进入答题页后保持一屏一题：答对自动下一题，答错才停下来。",
            color = Color.Gray,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        TodayLearningCard(
            readingStats = readingStats,
            learningStore = learningStore,
            settings = settings,
        )
        resume?.let { snapshot ->
            val resumeMode = runCatching { TrainingMode.valueOf(snapshot.modeName) }.getOrNull()
            if (resumeMode != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E6)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⏸ 上次训练还没结束", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "${resumeMode.emoji} ${resumeMode.label} · 已答 ${snapshot.answered} 题 · 正确 ${snapshot.correct} · 错题 ${snapshot.wrongItems.size} · 连对 ${snapshot.streak}",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onResume(snapshot) }, modifier = Modifier.weight(1f)) { Text("继续上次训练") }
                            TextButton(onClick = onDiscardResume, modifier = Modifier.weight(0.55f)) { Text("结束") }
                        }
                    }
                }
                if (snapshot.wrongItems.isNotEmpty()) {
                    WrongQuestionsSummaryCard(
                        title = "本次练习错题",
                        wrongItems = snapshot.wrongItems,
                        personalWords = personalWords,
                        personalPhrases = personalPhrases,
                    )
                }
            }
        }
        if (resume == null && lastCompletedWrongItems.isNotEmpty()) {
            WrongQuestionsSummaryCard(
                title = "上次训练错题",
                wrongItems = lastCompletedWrongItems,
                personalWords = personalWords,
                personalPhrases = personalPhrases,
            )
        }
        DailyOverview(adaptiveStats, weakCount, personalCount, phraseCount, builtInCount)
        Button(
            onClick = { onStart(TrainingMode.DAILY) },
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("📅 开始今日训练 · $dailyTarget 题 · 约 $estimatedMinutes 分钟", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onGrammar,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("🧠 52语法 · 记忆 + 练习 + 间隔复习", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text("专项训练", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        TrainingMode.entries.filterNot { it == TrainingMode.DAILY }.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    OutlinedButton(
                        onClick = { onStart(item) },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text("${item.emoji} ${item.label}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5F0)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                "训练原则：到期优先 → 薄弱优先 → 新知识；熟练后从选择题升级为主动输入和语境回忆。想不起来可以直接点“不会”，比乱猜更有利于系统安排复习。",
                modifier = Modifier.padding(14.dp),
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun TrainingSession(
    modifier: Modifier,
    mode: TrainingMode,
    tts: TtsManager,
    learningStore: LearningStore,
    adaptiveStore: AdaptiveTrainingStore,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
    sessionAnswered: Int,
    sessionCorrect: Int,
    streak: Int,
    sessionTarget: Int,
    persisted: TrainingProgress,
    successToken: Int,
    resumeKindName: String,
    resumeItemId: String,
    resumeWrong: Boolean,
    wrongItems: List<TrainingWrongItem>,
    onQuestionVisible: (String, String) -> Unit,
    onBack: () -> Unit,
    onAnswered: AdaptiveAnswer,
) {
    val dailyTarget = remember(mode, sessionTarget) {
        if (mode == TrainingMode.DAILY && sessionTarget > 0) sessionTarget
        else recommendedDailyQuestionCount(adaptiveStore.statsForKinds(CORE_TRAINING_KINDS).due, adaptiveStore.weakCount(CORE_TRAINING_KINDS))
    }
    var wrongDialogOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text("← 暂停")
            }
            Column(Modifier.weight(1f)) {
                Text("${mode.emoji} ${mode.label}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("答对自动下一题 · 答错才停留 · 返回会保存", color = Color.Gray, fontSize = 11.sp)
            }
            if (wrongItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = { wrongDialogOpen = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) { Text("错题 ${wrongItems.size}", fontSize = 11.sp) }
                if (wrongDialogOpen) {
                    WrongQuestionsDialog(
                        wrongItems = wrongItems,
                        personalWords = personalWords,
                        personalPhrases = personalPhrases,
                        onDismiss = { wrongDialogOpen = false },
                    )
                }
            }
            Text("🔥 $streak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        CompactSessionStats(sessionAnswered, sessionCorrect, persisted)

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                when (mode) {
                    TrainingMode.DAILY -> DailyMixedTrainer(
                        tts = tts,
                        learningStore = learningStore,
                        adaptiveStore = adaptiveStore,
                        personalWords = personalWords,
                        personalPhrases = personalPhrases,
                        target = dailyTarget,
                        sessionAnswered = sessionAnswered,
                        sessionCorrect = sessionCorrect,
                        onAnswered = onAnswered,
                        onFinished = onBack,
                        resumeKindName = resumeKindName,
                        resumeItemId = resumeItemId,
                        resumeWrong = resumeWrong,
                        onQuestionVisible = onQuestionVisible,
                    )
                    TrainingMode.GENDER -> GenderTrainer(tts, adaptiveStore, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                    TrainingMode.PREPOSITION -> PrepositionTrainer(tts, adaptiveStore, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                    TrainingMode.CONJUGATION -> ConjugationTrainer(tts, adaptiveStore, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                    TrainingMode.SENTENCE -> SentenceOutputTrainer(tts, adaptiveStore, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                    TrainingMode.PERSONAL -> PersonalVocabTrainer(tts, learningStore, adaptiveStore, personalWords, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                    TrainingMode.PHRASE -> PersonalPhraseTrainer(tts, learningStore, adaptiveStore, personalPhrases, onAnswered, initialItemId = resumeItemId, initialWrong = resumeWrong, onQuestionVisible = onQuestionVisible)
                }
                SuccessBurst(trigger = successToken, streak = streak)
            }
        }
    }
}

@Composable
private fun CompactSessionStats(sessionAnswered: Int, sessionCorrect: Int, persisted: TrainingProgress) {
    val rate = if (sessionAnswered == 0) 0 else sessionCorrect * 100 / sessionAnswered
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF3EEE7), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("本轮 $sessionCorrect/$sessionAnswered · $rate%", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("最佳连对 ${persisted.bestStreak}", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun WrongQuestionsSummaryCard(
    title: String,
    wrongItems: List<TrainingWrongItem>,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
) {
    var open by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E6)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${wrongItems.size} 道 · 可随时回来查看", color = Color.Gray, fontSize = 11.sp)
            }
            OutlinedButton(onClick = { open = true }) { Text("查看错题") }
        }
    }
    if (open) {
        WrongQuestionsDialog(
            wrongItems = wrongItems,
            personalWords = personalWords,
            personalPhrases = personalPhrases,
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun WrongQuestionsDialog(
    wrongItems: List<TrainingWrongItem>,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本次练习错题 · ${wrongItems.size}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (wrongItems.isEmpty()) {
                    Text("这一轮暂时没有错题。", color = Color.Gray)
                } else {
                    wrongItems.forEachIndexed { index, item ->
                        val summary = wrongQuestionSummary(item, personalWords, personalPhrases)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F5F0), RoundedCornerShape(12.dp))
                                .padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text("${index + 1}. ${summary.first}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (summary.second.isNotBlank()) {
                                Text(summary.second, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun wrongQuestionSummary(
    item: TrainingWrongItem,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
): Pair<String, String> = when (item.kind) {
    "gender" -> {
        val q = FrenchTrainingData.nounGender.firstOrNull { genderId(it) == item.itemId }
        if (q != null) q.noun to "${q.meaning} · 正确：${if (q.gender == "m") "阳性" else "阴性"}（${q.articleForm}）"
        else item.itemId.removePrefix("gender:") to "名词阴阳性"
    }
    "preposition" -> {
        val q = FrenchTrainingData.verbPrepositions.firstOrNull { prepId(it) == item.itemId }
        if (q != null) q.prompt to "${q.meaning} · 正确：${q.answer} · ${q.pattern}"
        else item.itemId.removePrefix("prep:") to "à / de"
    }
    "conjugation" -> {
        val q = CONJUGATION_BANK.firstOrNull { it.id == item.itemId }
        if (q != null) "${spokenSubject(q.subjectIndex)} ${q.lemma} · ${q.tense}" to "正确：${q.answer}"
        else item.itemId.removePrefix("conj:") to "动词变位"
    }
    "sentence_output" -> {
        val q = SentenceOutputData.questions.firstOrNull { sentenceId(it) == item.itemId }
        if (q != null) q.chinese to "参考：${q.answers.firstOrNull().orEmpty()}"
        else item.itemId.removePrefix("sentence:") to "整句输出"
    }
    "personal_vocab" -> {
        val word = item.itemId.removePrefix("vocab:")
        val q = personalWords.firstOrNull { personalId(it) == item.itemId }
        (q?.word ?: word) to (q?.definition.orEmpty().ifBlank { "我的生词" })
    }
    "personal_phrase" -> {
        val phrase = item.itemId.removePrefix("phrase:")
        val q = personalPhrases.firstOrNull { phraseId(it) == item.itemId }
        (q?.expression ?: phrase) to (q?.definition.orEmpty().ifBlank { "我的表达" })
    }
    else -> item.itemId to item.kind
}

@Composable
private fun TodayLearningCard(
    readingStats: ReadingStatsStore,
    learningStore: LearningStore,
    settings: AppSettings,
) {
    val todaySeconds = readingStats.todaySeconds()
    val readingGoalMinutes = settings.dailyReadingGoalMinutes.coerceAtLeast(1)
    val readingProgress = (todaySeconds.toFloat() / (readingGoalMinutes * 60f)).coerceIn(0f, 1f)
    val reviewed = learningStore.reviewedTodayCount()
    val vocabGoal = settings.dailyVocabReviewGoal.coerceAtLeast(1)
    val vocabProgress = (reviewed.toFloat() / vocabGoal).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEE4)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日学习", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("🔥 连续 ${readingStats.currentStreakDays()} 天", fontSize = 11.sp, color = Color(0xFF7B6C5D))
                }
                Text(
                    if (readingProgress >= 1f && vocabProgress >= 1f) "✓ 今日完成" else "继续加油",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (readingProgress >= 1f && vocabProgress >= 1f) Color(0xFF557A55) else MaterialTheme.colorScheme.primary,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("阅读 ${formatTrainingDuration(todaySeconds)} / ${readingGoalMinutes}m", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { readingProgress }, modifier = Modifier.fillMaxWidth().height(7.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("复习 $reviewed / $vocabGoal 词", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = { vocabProgress }, modifier = Modifier.fillMaxWidth().height(7.dp))
                }
            }
        }
    }
}

private fun formatTrainingDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun DailyOverview(adaptiveStats: AdaptiveTrainingStats, weakCount: Int, personalCount: Int, phraseCount: Int, builtInCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF5EA)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🧠 今日训练系统", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("到期 ${adaptiveStats.due} · 薄弱 $weakCount", fontWeight = FontWeight.SemiBold, color = Color(0xFF557A55))
            }
            Text(
                "内置知识点约 $builtInCount · 生词 $personalCount · 表达 $phraseCount · 已追踪 ${adaptiveStats.tracked} · 已掌握 ${adaptiveStats.mastered}",
                fontSize = 12.sp,
                color = Color.DarkGray,
            )
            LinearProgressIndicator(
                progress = { adaptiveStats.averageMastery.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
            )
            Text("综合掌握度 ${(adaptiveStats.averageMastery * 100).toInt()}% · 系统会优先安排到期和薄弱知识点", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun DailyMixedTrainer(
    tts: TtsManager,
    learningStore: LearningStore,
    adaptiveStore: AdaptiveTrainingStore,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
    target: Int,
    sessionAnswered: Int,
    sessionCorrect: Int,
    onAnswered: AdaptiveAnswer,
    onFinished: () -> Unit,
    resumeKindName: String = "",
    resumeItemId: String = "",
    resumeWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    val initialRound = if (resumeWrong && resumeItemId.isNotBlank()) {
        (sessionAnswered - 1).coerceAtLeast(0)
    } else {
        sessionAnswered
    }
    var round by rememberSaveable { mutableIntStateOf(initialRound.coerceAtMost(target)) }
    var lastKindName by rememberSaveable { mutableStateOf<String?>(null) }
    if (round >= target) {
        val rate = if (sessionAnswered == 0) 0 else sessionCorrect * 100 / sessionAnswered
        QuestionCard {
            QuizEyebrow("今日混合训练")
            Text("🎉 今日建议量完成", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("$sessionCorrect/$sessionAnswered · 正确率 $rate%", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "今天到这里就已经够了。系统会把答错、反应慢和到期的知识点重新排进后续复习；想继续可以再开一组。",
                color = Color.Gray,
                lineHeight = 21.sp,
            )
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                Text("完成并返回训练首页")
            }
        }
        return
    }

    val current = remember(round) {
        val forced = resumeKindName.takeIf { resumeItemId.isNotBlank() }
            ?.let { runCatching { MixKind.valueOf(it) }.getOrNull() }
        forced ?: pickMixKind(
            store = adaptiveStore,
            personalWords = personalWords,
            personalPhrases = personalPhrases,
            lastKind = lastKindName?.let { runCatching { MixKind.valueOf(it) }.getOrNull() },
        )
    }
    fun advance() {
        lastKindName = current.name
        round++
    }

    key(round) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("今日训练 · 第 ${round + 1}/$target 题", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("按薄弱度自动选题", color = Color.Gray, fontSize = 11.sp)
            }
            val forcedId = if (resumeItemId.isNotBlank() && resumeKindName == current.name) resumeItemId else ""
            when (current) {
                MixKind.GENDER -> GenderTrainer(tts, adaptiveStore, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
                MixKind.PREPOSITION -> PrepositionTrainer(tts, adaptiveStore, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
                MixKind.CONJUGATION -> ConjugationTrainer(tts, adaptiveStore, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
                MixKind.SENTENCE -> SentenceOutputTrainer(tts, adaptiveStore, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
                MixKind.PERSONAL -> PersonalVocabTrainer(tts, learningStore, adaptiveStore, personalWords, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
                MixKind.PHRASE -> PersonalPhraseTrainer(tts, learningStore, adaptiveStore, personalPhrases, onAnswered, ::advance, forcedId, resumeWrong && forcedId.isNotBlank(), onQuestionVisible)
            }
        }
    }
}

@Composable
private fun GenderTrainer(
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    val bank = FrenchTrainingData.nounGender
    var index by remember {
        val forced = GENDER_IDS.indexOf(initialItemId)
        mutableIntStateOf(if (forced >= 0) forced else pickGenderIndex(bank, adaptiveStore, null))
    }
    var wrongAnswer by remember { mutableStateOf<String?>(if (initialWrong) "__revealed__" else null) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val q = bank[index]
    val id = genderId(q)
    LaunchedEffect(id) { onQuestionVisible(MixKind.GENDER.name, id) }

    fun next() {
        if (onNextExternal != null) {
            onNextExternal()
        } else {
            index = pickGenderIndex(bank, adaptiveStore, id)
            wrongAnswer = null
            startedAt = System.currentTimeMillis()
        }
    }

    fun submitChoice(value: String) {
        val correct = value == q.gender
        onAnswered(id, "gender", correct, System.currentTimeMillis() - startedAt, "built_in")
        if (correct) next() else wrongAnswer = value
    }

    QuestionCard {
        QuizEyebrow("名词阴阳性 · 直接选择 · ${bank.size} 题")
        Text(q.noun, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(q.meaning, color = Color.Gray)
        OutlinedButton(onClick = { tts.speak(q.noun) }) { Text("🔊 发音") }

        if (wrongAnswer == null) {
            Text("选择这个名词的性别", fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceButton("♂ 阳性", false, true, Modifier.weight(1f)) { submitChoice("m") }
                ChoiceButton("♀ 阴性", false, true, Modifier.weight(1f)) { submitChoice("f") }
            }
        } else {
            AnswerFeedback(
                false,
                "${if (q.gender == "m") "阳性" else "阴性"}：${q.articleForm}",
                genderFeedbackDetail(q),
            ) { tts.speak(q.example) }
            NextButton(::next)
        }
    }
}

@Composable
private fun PrepositionTrainer(
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    val bank = FrenchTrainingData.verbPrepositions
    var index by remember {
        val forced = PREPOSITION_IDS.indexOf(initialItemId)
        mutableIntStateOf(if (forced >= 0) forced else pickPrepIndex(bank, adaptiveStore, null))
    }
    var wrongAnswer by remember { mutableStateOf<String?>(if (initialWrong) "__revealed__" else null) }
    var input by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val q = bank[index]
    val id = prepId(q)
    LaunchedEffect(id) { onQuestionVisible(MixKind.PREPOSITION.name, id) }
    val state = remember(id) { adaptiveStore.state(id) }
    val activeRecall = (state?.mastery ?: 0f) >= 0.38f || (state?.attempts ?: 0) >= 3
    val contextRecall = (state?.mastery ?: 0f) >= 0.72f && (state?.attempts ?: 0) >= 5
    val promptText = if (contextRecall) prepExampleCloze(q) else q.prompt

    fun next() {
        if (onNextExternal != null) {
            onNextExternal()
        } else {
            index = pickPrepIndex(bank, adaptiveStore, id)
            wrongAnswer = null
            input = ""
            startedAt = System.currentTimeMillis()
        }
    }

    fun submit(value: String) {
        val normalized = normalizePrep(value)
        val correct = normalized == normalizePrep(q.answer)
        onAnswered(id, "preposition", correct, System.currentTimeMillis() - startedAt, "built_in")
        if (correct) next() else wrongAnswer = normalized
    }

    fun revealAnswer() {
        onAnswered(id, "preposition", false, System.currentTimeMillis() - startedAt, "built_in")
        wrongAnswer = "__revealed__"
    }

    QuestionCard {
        QuizEyebrow("动词介词 · 自适应复习 · ${bank.size} 个结构")
        Text(promptText, fontSize = if (contextRecall) 21.sp else 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
        Text(if (contextRecall) "语境回忆 · ${q.meaning}" else q.meaning, color = Color.Gray)

        if (wrongAnswer == null) {
            if (!activeRecall) {
                Text("空格应该填？", fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceButton("à", false, true, Modifier.weight(1f)) { submit("à") }
                    ChoiceButton("de", false, true, Modifier.weight(1f)) { submit("de") }
                }
            } else {
                Text(if (contextRecall) "语境输出：自己填入 à 或 de" else "主动回忆：自己输入 à 或 de", fontWeight = FontWeight.Medium)
                TrainingInputField(
                    value = input,
                    onValueChange = { input = it },
                    focusKey = id,
                    placeholder = "à / de",
                    onDone = { if (input.isNotBlank()) submit(input) },
                )
                Button(onClick = { submit(input) }, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("检查答案") }
                TextButton(onClick = ::revealAnswer, modifier = Modifier.fillMaxWidth()) { Text("想不起来 · 直接看答案") }
            }
        } else {
            AnswerFeedback(false, q.pattern, "${q.meaning}\n${q.example}") { tts.speak(q.example) }
            NextButton(::next)
        }
    }
}

@Composable
private fun ConjugationTrainer(
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    var quiz by remember {
        val forced = CONJUGATION_IDS.indexOf(initialItemId)
        mutableStateOf(if (forced >= 0) CONJUGATION_BANK[forced] else newConjugationQuiz(adaptiveStore))
    }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(initialWrong) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(quiz.id) { onQuestionVisible(MixKind.CONJUGATION.name, quiz.id) }

    fun next() {
        if (onNextExternal != null) {
            onNextExternal()
        } else {
            quiz = newConjugationQuiz(adaptiveStore, exclude = quiz)
            answer = ""
            wrong = false
            startedAt = System.currentTimeMillis()
        }
    }

    fun submit() {
        val correct = FrenchAnswerJudge.short(answer, listOf(quiz.answer)).correct
        onAnswered(quiz.id, "conjugation", correct, System.currentTimeMillis() - startedAt, "built_in")
        if (correct) next() else wrong = true
    }

    fun revealAnswer() {
        onAnswered(quiz.id, "conjugation", false, System.currentTimeMillis() - startedAt, "built_in")
        wrong = true
    }

    QuestionCard {
        QuizEyebrow("不规则动词 · 主动输出 · ${FrenchConjugator.irregularTrainingVerbs().size} 个核心动词")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(quiz.lemma, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { tts.speak(quiz.lemma) }) { Text("🔊") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TrainingPill(quiz.tense)
            TrainingPill(FrenchConjugator.trainingSubjects()[quiz.subjectIndex])
        }

        if (!wrong) {
            Text("请自己输入正确变位", fontWeight = FontWeight.Medium)
            TrainingInputField(
                value = answer,
                onValueChange = { answer = it },
                focusKey = quiz.id,
                placeholder = "例如：viendrons",
                onDone = { if (answer.isNotBlank()) submit() },
            )
            Button(
                onClick = ::submit,
                enabled = answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("检查答案") }
            TextButton(onClick = ::revealAnswer, modifier = Modifier.fillMaxWidth()) { Text("想不起来 · 直接看完整变位") }
        } else {
            AnswerFeedback(
                false,
                "${FrenchConjugator.trainingSubjects()[quiz.subjectIndex]} ${quiz.answer}",
                "注意词干和词尾变化。下面对照完整六人称，再进入下一题。",
            ) { tts.speak("${spokenSubject(quiz.subjectIndex)} ${quiz.answer}") }
            CompactConjugationTable(quiz)
            NextButton(::next)
        }
    }
}

@Composable
private fun SentenceOutputTrainer(
    tts: TtsManager,
    adaptiveStore: AdaptiveTrainingStore,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    val bank = SentenceOutputData.questions
    var index by remember {
        val ids = bank.map(::sentenceId)
        val forced = ids.indexOf(initialItemId)
        mutableIntStateOf(if (forced >= 0) forced else pickSentenceIndex(bank, adaptiveStore, null))
    }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(initialWrong) }
    var differenceHint by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val q = bank[index]
    val id = sentenceId(q)
    LaunchedEffect(id) { onQuestionVisible(MixKind.SENTENCE.name, id) }

    fun next() {
        if (onNextExternal != null) {
            onNextExternal()
        } else {
            index = pickSentenceIndex(bank, adaptiveStore, id)
            answer = ""
            wrong = false
            differenceHint = ""
            startedAt = System.currentTimeMillis()
        }
    }

    fun submit() {
        val judgement = FrenchAnswerJudge.sentence(answer, q.answers)
        val correct = judgement.correct
        differenceHint = judgement.differenceHint
        onAnswered(id, "sentence_output", correct, System.currentTimeMillis() - startedAt, "built_in")
        if (correct) next() else wrong = true
    }

    fun revealAnswer() {
        onAnswered(id, "sentence_output", false, System.currentTimeMillis() - startedAt, "built_in")
        wrong = true
    }

    QuestionCard {
        QuizEyebrow("整句输出 · 主动表达 · ${bank.size} 句")
        Text(q.chinese, fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainingPill(q.level)
            TrainingPill("整句主动回忆")
        }
        Text("训练重点：${q.focus}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("提示：${q.hint}", color = Color.Gray, fontSize = 12.sp)

        if (!wrong) {
            TrainingInputField(
                value = answer,
                onValueChange = { answer = it },
                focusKey = id,
                placeholder = "用法语写出完整句子",
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                onDone = { if (answer.isNotBlank()) submit() },
            )
            Button(onClick = ::submit, enabled = answer.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("检查整句") }
            TextButton(onClick = ::revealAnswer, modifier = Modifier.fillMaxWidth()) { Text("想不起来 · 看参考表达") }
        } else {
            val model = q.answers.first()
            AnswerFeedback(
                false,
                model,
                listOf(differenceHint, "训练重点：${q.focus}", "记忆提示：${q.hint}").filter { it.isNotBlank() }.joinToString("\n"),
            ) { tts.speak(model) }
            if (q.answers.size > 1) {
                Text("也接受：${q.answers.drop(1).joinToString(" / ")}", color = Color.Gray, fontSize = 12.sp, lineHeight = 17.sp)
            }
            NextButton(::next)
        }
    }
}

@Composable
private fun PersonalVocabTrainer(
    tts: TtsManager,
    learningStore: LearningStore,
    adaptiveStore: AdaptiveTrainingStore,
    personalWords: List<VocabularyItem>,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    if (personalWords.isEmpty()) {
        QuestionCard {
            QuizEyebrow("我的生词")
            Text("这里还没有待学习的个人词。", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("在阅读文章或 PDF 时，把词加入生词本，或者直接给词做高亮标记，它就会自动进入这里。", color = Color.Gray, lineHeight = 21.sp)
        }
        return
    }

    var item by remember(personalWords) {
        val ids = personalWords.map(::personalId)
        val forced = ids.indexOf(initialItemId)
        mutableStateOf(if (forced >= 0) personalWords[forced] else pickPersonalWord(personalWords, adaptiveStore, null))
    }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(initialWrong) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sourceContexts by remember(item.word) { mutableStateOf<List<VocabularyContext>>(emptyList()) }
    val id = personalId(item)
    LaunchedEffect(id) {
        onQuestionVisible(MixKind.PERSONAL.name, id)
        sourceContexts = withContext(Dispatchers.IO) { learningStore.vocabularyContexts(item.word, 8) }
    }
    val selectedContext = remember(item, sourceContexts) {
        val clozeContexts = sourceContexts.filter { makeCloze(item, it.text) != null }
        when {
            clozeContexts.isNotEmpty() -> clozeContexts[item.reviewCount % clozeContexts.size]
            sourceContexts.isNotEmpty() -> sourceContexts[item.reviewCount % sourceContexts.size]
            else -> VocabularyContext(item.context, item.bookId, item.bookTitle, item.createdAt)
        }
    }
    val cloze = remember(item, selectedContext) { makeCloze(item, selectedContext.text) }

    fun next() {
        if (onNextExternal != null) {
            onNextExternal()
        } else {
            item = pickPersonalWord(personalWords, adaptiveStore, id)
            answer = ""
            wrong = false
            startedAt = System.currentTimeMillis()
        }
    }

    fun submit() {
        val accepted = listOf(item.word, item.lemma).filter { it.isNotBlank() }.distinct()
        val correct = FrenchAnswerJudge.short(answer, accepted).correct
        onAnswered(id, "personal_vocab", correct, System.currentTimeMillis() - startedAt, "personal")
        val adaptive = adaptiveStore.state(id)
        if (correct) {
            learningStore.syncVocabularyReview(item.word, adaptive?.nextDueAt ?: 0L)
            next()
        } else {
            learningStore.markLearning(item.word)
            wrong = true
        }
    }

    fun revealAnswer() {
        onAnswered(id, "personal_vocab", false, System.currentTimeMillis() - startedAt, "personal")
        learningStore.markLearning(item.word)
        wrong = true
    }

    QuestionCard {
        QuizEyebrow("我的阅读生词 · ${personalWords.size} 个待学习")
        if (cloze != null) {
            Text("从原文回忆这个词", fontWeight = FontWeight.Medium)
            Text(cloze, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text("根据中文/释义写出法语词", fontWeight = FontWeight.Medium)
            Text(item.definition.ifBlank { "回忆这个生词" }, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        if (selectedContext.bookTitle.isNotBlank()) {
            Text("原文来源：${selectedContext.bookTitle} · 已保存 ${sourceContexts.size.coerceAtLeast(1)} 条语境", color = Color.Gray, fontSize = 11.sp)
        } else if (item.bookTitle.isNotBlank()) {
            Text("来源：${item.bookTitle}", color = Color.Gray, fontSize = 11.sp)
        }

        if (!wrong) {
            TrainingInputField(
                value = answer,
                onValueChange = { answer = it },
                focusKey = id,
                placeholder = "输入法语词",
                onDone = { if (answer.isNotBlank()) submit() },
            )
            Button(
                onClick = ::submit,
                enabled = answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("检查答案") }
            TextButton(onClick = ::revealAnswer, modifier = Modifier.fillMaxWidth()) { Text("想不起来 · 直接看答案") }
        } else {
            AnswerFeedback(
                false,
                item.word + if (item.lemma.isNotBlank() && item.lemma != item.word) "　(${item.lemma})" else "",
                listOf(item.definition, selectedContext.text).filter { it.isNotBlank() }.joinToString("\n").ifBlank { "继续在真实语境中巩固这个词。" },
            ) { tts.speak(item.word) }
            NextButton(::next)
        }
    }
}

@Composable
private fun TrainingInputField(
    value: String,
    onValueChange: (String) -> Unit,
    focusKey: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    enabled: Boolean = true,
    onDone: () -> Unit,
) {
    val requester = remember(focusKey) { FocusRequester() }
    LaunchedEffect(focusKey, enabled) {
        if (enabled) {
            delay(120)
            runCatching { requester.requestFocus() }
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().focusRequester(requester),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun QuestionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        // Normally the whole question fits on one screen. On small screens, landscape,
        // large font scales or long correction feedback, scrolling becomes a fallback
        // instead of letting the bottom action button disappear behind the viewport/IME.
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun QuizEyebrow(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick, modifier, enabled = enabled) { Text(text, fontSize = 18.sp) }
    else OutlinedButton(onClick, modifier, enabled = enabled) { Text(text, fontSize = 18.sp) }
}

@Composable
private fun AnswerFeedback(correct: Boolean, answer: String, detail: String, onSpeak: () -> Unit) {
    val background = if (correct) Color(0xFFEAF7EC) else Color(0xFFFFECE8)
    val heading = if (correct) "✓ 正确" else "✗ 再记一下"
    Column(
        Modifier.fillMaxWidth().background(background, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(heading, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(answer, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(detail, fontSize = 14.sp, lineHeight = 20.sp)
        OutlinedButton(onClick = onSpeak) { Text("🔊 听例句/答案") }
    }
}

@Composable
private fun NextButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("下一题 →") }
}

@Composable
private fun TrainingPill(text: String) {
    Text(
        text,
        Modifier.background(Color(0xFFF1EADF), RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    )
}

@Composable
private fun CompactConjugationTable(quiz: ConjugationQuiz) {
    val subjects = FrenchConjugator.trainingSubjects()
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF8F5F0), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        quiz.allForms.forEachIndexed { i, form ->
            Row(Modifier.fillMaxWidth()) {
                Text(subjects[i], Modifier.weight(0.38f), color = Color.Gray, fontSize = 12.sp)
                Text(form, Modifier.weight(0.62f), fontSize = 13.sp, fontWeight = if (i == quiz.subjectIndex) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun SuccessBurst(trigger: Int, streak: Int) {
    if (trigger <= 0) return
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(850, easing = FastOutSlowInEasing))
        delay(80)
    }
    val p = progress.value
    if (p >= 1f) return
    Box(Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            val count = if (streak > 0 && streak % 5 == 0) 24 else 14
            repeat(count) { i ->
                val angle = (2.0 * PI * i / count).toFloat()
                val distance = 24f + p * if (count > 14) 150f else 105f
                val x = center.x + cos(angle) * distance
                val y = center.y + sin(angle) * distance
                val alpha = (1f - p).coerceIn(0f, 1f)
                val radius = (9f - p * 5f).coerceAtLeast(2f)
                val color = if (i % 2 == 0) Color(0xFFFFC107) else Color(0xFF66BB6A)
                drawCircle(color.copy(alpha = alpha), radius, Offset(x, y))
            }
        }
        val pop = 1f + sin((p * PI).toFloat()) * 0.22f
        Text(
            if (streak > 0 && streak % 10 == 0) "🏆 $streak 连对！" else if (streak > 0 && streak % 5 == 0) "🔥 $streak 连对！" else "✨ Parfait !",
            Modifier.graphicsLayer { alpha = (1f - p).coerceIn(0f, 1f); scaleX = pop; scaleY = pop },
            fontSize = if (streak > 0 && streak % 5 == 0) 28.sp else 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PersonalPhraseTrainer(
    tts: TtsManager,
    learningStore: LearningStore,
    adaptiveStore: AdaptiveTrainingStore,
    phrases: List<PhraseItem>,
    onAnswered: AdaptiveAnswer,
    onNextExternal: (() -> Unit)? = null,
    initialItemId: String = "",
    initialWrong: Boolean = false,
    onQuestionVisible: (String, String) -> Unit = { _, _ -> },
) {
    if (phrases.isEmpty()) {
        QuestionCard {
            QuizEyebrow("我的表达")
            Text("这里还没有从阅读中收藏的表达。", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("阅读时在“自动识别短语”里点“+ 表达”，原句会一起保存并进入这里。", color = Color.Gray, lineHeight = 21.sp)
        }
        return
    }
    var item by remember(phrases) {
        val ids = phrases.map(::phraseId)
        val forced = ids.indexOf(initialItemId)
        mutableStateOf(if (forced >= 0) phrases[forced] else pickPersonalPhrase(phrases, adaptiveStore, null))
    }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(initialWrong) }
    var startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var contexts by remember(item.expression) { mutableStateOf<List<VocabularyContext>>(emptyList()) }
    val id = phraseId(item)
    LaunchedEffect(id) {
        onQuestionVisible(MixKind.PHRASE.name, id)
        contexts = withContext(Dispatchers.IO) { learningStore.expressionContexts(item.expression, 8) }
    }
    val context = contexts.firstOrNull()?.text?.ifBlank { item.context } ?: item.context
    val cloze = remember(item, context) { makePhraseCloze(item.expression, context) }

    fun next() {
        if (onNextExternal != null) onNextExternal() else {
            item = pickPersonalPhrase(phrases, adaptiveStore, id)
            answer = ""; wrong = false; startedAt = System.currentTimeMillis()
        }
    }
    fun submit() {
        val correct = FrenchAnswerJudge.short(answer, listOf(item.expression)).correct
        onAnswered(id, "personal_phrase", correct, System.currentTimeMillis() - startedAt, "reading")
        if (correct) next() else wrong = true
    }
    fun reveal() {
        onAnswered(id, "personal_phrase", false, System.currentTimeMillis() - startedAt, "reading")
        wrong = true
    }

    QuestionCard {
        QuizEyebrow("阅读表达 · ${phrases.size} 个")
        if (cloze != null) {
            Text("把原文中的完整表达写回来", fontWeight = FontWeight.Medium)
            Text(cloze, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text("根据中文/释义写出完整表达", fontWeight = FontWeight.Medium)
            Text(item.definition.ifBlank { "回忆这个固定表达" }, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        if (item.bookTitle.isNotBlank()) Text("来源：${item.bookTitle}", color = Color.Gray, fontSize = 11.sp)
        if (!wrong) {
            TrainingInputField(
                value = answer, onValueChange = { answer = it }, focusKey = id,
                placeholder = "输入完整法语表达", onDone = { if (answer.isNotBlank()) submit() },
            )
            Button(onClick = ::submit, enabled = answer.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("确认") }
            TextButton(onClick = ::reveal, modifier = Modifier.fillMaxWidth()) { Text("不会 / 看答案") }
        } else {
            AnswerFeedback(false, item.expression, item.definition.ifBlank { "把它作为一个整体记忆。" }) { tts.speak(item.expression) }
            if (context.isNotBlank()) Text("原句：$context", color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
            NextButton(::next)
        }
    }
}

private fun makePhraseCloze(expression: String, context: String): String? {
    val cleanContext = context.trim()
    val cleanExpression = expression.trim()
    if (cleanContext.isBlank() || cleanExpression.isBlank()) return null
    val regex = Regex(Regex.escape(cleanExpression), RegexOption.IGNORE_CASE)
    return if (regex.containsMatchIn(cleanContext)) cleanContext.replaceFirst(regex, "________") else null
}

private val CONJUGATION_BANK: List<ConjugationQuiz> by lazy {
    buildList {
        FrenchConjugator.irregularTrainingVerbs().forEach { lemma ->
            val byTense = FrenchConjugator.irregularTrainingForms(lemma) ?: return@forEach
            byTense.forEach { (tense, forms) ->
                forms.forEachIndexed { subjectIndex, answer ->
                    add(ConjugationQuiz(lemma, tense, subjectIndex, answer, forms))
                }
            }
        }
    }
}

private val CONJUGATION_IDS: List<String> by lazy { CONJUGATION_BANK.map { it.id } }
private val GENDER_IDS: List<String> by lazy { FrenchTrainingData.nounGender.map(::genderId) }
private val PREPOSITION_IDS: List<String> by lazy { FrenchTrainingData.verbPrepositions.map(::prepId) }

private fun recommendedDailyQuestionCount(due: Int, weak: Int): Int {
    val load = due + weak / 2
    return when {
        load <= 4 -> 12
        load <= 12 -> 16
        load <= 28 -> 20
        else -> 24
    }
}

private fun pickMixKind(
    store: AdaptiveTrainingStore,
    personalWords: List<VocabularyItem>,
    personalPhrases: List<PhraseItem>,
    lastKind: MixKind?,
): MixKind {
    val ranked = buildList<Pair<MixKind, Double>> {
        add(MixKind.GENDER to store.bestKindScore("gender", FrenchTrainingData.nounGender.size))
        add(MixKind.PREPOSITION to store.bestKindScore("preposition", FrenchTrainingData.verbPrepositions.size))
        add(MixKind.CONJUGATION to store.bestKindScore("conjugation", CONJUGATION_BANK.size))
        add(MixKind.SENTENCE to store.bestKindScore("sentence_output", SentenceOutputData.questions.size))
        if (personalWords.isNotEmpty()) add(MixKind.PERSONAL to store.bestKindScore("personal_vocab", personalWords.size))
        if (personalPhrases.isNotEmpty()) add(MixKind.PHRASE to store.bestKindScore("personal_phrase", personalPhrases.size))
    }.sortedByDescending { it.second }
    if (ranked.isEmpty()) return MixKind.GENDER
    val topScore = ranked.first().second
    val nearTop = ranked.filter { topScore - it.second <= 80.0 }
    val withoutRepeat = nearTop.filterNot { it.first == lastKind }
    return (withoutRepeat.ifEmpty { nearTop }).random().first
}

private fun genderFeedbackDetail(q: NounGenderQuestion): String {
    val n = q.noun.lowercase()
    val hint = when {
        q.gender == "f" && (n.endsWith("tion") || n.endsWith("sion")) -> "记忆钩子：-tion / -sion 结尾通常是阴性。"
        q.gender == "f" && (n.endsWith("té") || n.endsWith("ité")) -> "记忆钩子：-té / -ité 结尾通常是阴性。"
        q.gender == "f" && (n.endsWith("ance") || n.endsWith("ence")) -> "记忆钩子：-ance / -ence 结尾通常是阴性。"
        q.gender == "f" && n.endsWith("ure") -> "记忆钩子：很多 -ure 结尾名词是阴性。"
        q.gender == "m" && n.endsWith("ment") -> "记忆钩子：-ment 结尾通常是阳性。"
        q.gender == "m" && n.endsWith("isme") -> "记忆钩子：-isme 结尾通常是阳性。"
        q.gender == "m" && n.endsWith("age") -> "记忆钩子：很多 -age 结尾名词是阳性，但有例外。"
        else -> "记忆时把冠词和名词一起背：${q.articleForm}。"
    }
    return "${q.example}\n$hint"
}

private fun prepExampleCloze(q: VerbPrepositionQuestion): String {
    val example = q.example
    return if (normalizePrep(q.answer) == "à") {
        val regex = Regex("(?i)\\bà\\b")
        if (regex.containsMatchIn(example)) example.replaceFirst(regex, "___") else q.prompt
    } else {
        val de = Regex("(?i)\\bde\\b")
        when {
            de.containsMatchIn(example) -> example.replaceFirst(de, "___")
            Regex("(?i)d['’]").containsMatchIn(example) -> example.replaceFirst(Regex("(?i)d['’]"), "___ ")
            else -> q.prompt
        }
    }
}

private fun genderId(q: NounGenderQuestion) = "gender:${normalizeFrench(q.noun)}"
private fun prepId(q: VerbPrepositionQuestion) = "prep:${normalizeFrench(q.pattern)}"
private fun personalId(v: VocabularyItem) = "vocab:${normalizeFrench(v.word)}"
private fun phraseId(v: PhraseItem) = "phrase:${normalizeFrench(v.expression)}"
private fun sentenceId(q: SentenceOutputQuestion) = "sentence:${q.id}"

private fun pickGenderIndex(bank: List<NounGenderQuestion>, store: AdaptiveTrainingStore, excludeId: String?): Int {
    val ids = bank.map(::genderId)
    val chosen = store.pickAdaptive(ids, excludeId) ?: ids.first()
    return ids.indexOf(chosen).coerceAtLeast(0)
}

private fun pickPrepIndex(bank: List<VerbPrepositionQuestion>, store: AdaptiveTrainingStore, excludeId: String?): Int {
    val ids = bank.map(::prepId)
    val chosen = store.pickAdaptive(ids, excludeId) ?: ids.first()
    return ids.indexOf(chosen).coerceAtLeast(0)
}

private fun pickSentenceIndex(bank: List<SentenceOutputQuestion>, store: AdaptiveTrainingStore, excludeId: String?): Int {
    val ids = bank.map(::sentenceId)
    val chosen = store.pickAdaptive(ids, excludeId) ?: ids.first()
    return ids.indexOf(chosen).coerceAtLeast(0)
}

private fun pickPersonalWord(words: List<VocabularyItem>, store: AdaptiveTrainingStore, excludeId: String?): VocabularyItem {
    val ids = words.map(::personalId)
    val chosen = store.pickAdaptive(ids, excludeId) ?: ids.first()
    return words[ids.indexOf(chosen).coerceAtLeast(0)]
}

private fun pickPersonalPhrase(items: List<PhraseItem>, store: AdaptiveTrainingStore, excludeId: String?): PhraseItem {
    val ids = items.map(::phraseId)
    val chosen = store.pickAdaptive(ids, excludeId) ?: ids.first()
    return items[ids.indexOf(chosen).coerceAtLeast(0)]
}

private fun newConjugationQuiz(store: AdaptiveTrainingStore, exclude: ConjugationQuiz? = null): ConjugationQuiz {
    val chosen = store.pickAdaptive(CONJUGATION_IDS, exclude?.id) ?: CONJUGATION_IDS.first()
    return CONJUGATION_BANK[CONJUGATION_IDS.indexOf(chosen).coerceAtLeast(0)]
}

private fun makeCloze(item: VocabularyItem, contextText: String = item.context): String? {
    val context = contextText.trim()
    if (context.isBlank()) return null
    val candidates = listOf(item.word, item.lemma).filter { it.isNotBlank() }.distinctBy { it.lowercase() }
    candidates.forEach { word ->
        val regex = Regex("(?i)(?<![\\p{L}’-])" + Regex.escape(word) + "(?![\\p{L}’-])")
        if (regex.containsMatchIn(context)) return context.replaceFirst(regex, "____")
    }
    return null
}

private fun normalizeFrench(value: String): String = value.trim().lowercase().replace('’', '\'').replace(Regex("\\s+"), " ")
private fun normalizeSentence(value: String): String = normalizeFrench(value)
    .replace(Regex("[.!?;:,]+$"), "")
    .replace(Regex("\\s+"), " ")
    .trim()
private fun normalizePrep(value: String): String {
    val normalized = normalizeFrench(value)
    return when {
        normalized == "d" || normalized == "d'" -> "de"
        normalized.startsWith("d'") -> "de"
        else -> normalized
    }
}
private fun spokenSubject(index: Int): String = when (index) { 0 -> "je"; 1 -> "tu"; 2 -> "il"; 3 -> "nous"; 4 -> "vous"; else -> "ils" }
