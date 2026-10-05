package com.cy.languagereader.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class TrainingKnowledgeState(
    val itemId: String,
    val kind: String,
    val attempts: Int,
    val correct: Int,
    val streak: Int,
    val avgResponseMs: Long,
    val mastery: Float,
    val lastSeenAt: Long,
    val nextDueAt: Long,
    val source: String,
)

data class AdaptiveTrainingStats(
    val tracked: Int,
    val mastered: Int,
    val due: Int,
    val averageMastery: Float,
)

/**
 * Per-knowledge-point scheduler for deliberate practice.
 *
 * The knowledge table is small enough to keep a process-local snapshot. This avoids
 * re-querying hundreds/thousands of conjugation IDs on every answer while the DB
 * remains the durable source of truth.
 */
class AdaptiveTrainingStore(context: Context) : SQLiteOpenHelper(context, "adaptive_training.db", null, 2) {
    private val cacheLock = Any()
    private val cache = linkedMapOf<String, TrainingKnowledgeState>()
    @Volatile private var cacheLoaded = false

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE knowledge(
                item_id TEXT PRIMARY KEY,
                kind TEXT NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                correct INTEGER NOT NULL DEFAULT 0,
                streak INTEGER NOT NULL DEFAULT 0,
                avg_response_ms INTEGER NOT NULL DEFAULT 0,
                mastery REAL NOT NULL DEFAULT 0,
                last_seen_at INTEGER NOT NULL DEFAULT 0,
                next_due_at INTEGER NOT NULL DEFAULT 0,
                source TEXT NOT NULL DEFAULT 'built_in'
            )""".trimIndent()
        )
        createIndexes(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createIndexes(db)
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_knowledge_due ON knowledge(next_due_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_knowledge_kind ON knowledge(kind)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_knowledge_kind_due ON knowledge(kind,next_due_at)")
    }

    private fun row(c: android.database.Cursor) = TrainingKnowledgeState(
        itemId = c.getString(0),
        kind = c.getString(1),
        attempts = c.getInt(2),
        correct = c.getInt(3),
        streak = c.getInt(4),
        avgResponseMs = c.getLong(5),
        mastery = c.getFloat(6),
        lastSeenAt = c.getLong(7),
        nextDueAt = c.getLong(8),
        source = c.getString(9),
    )

    private fun ensureCache() {
        if (cacheLoaded) return
        synchronized(cacheLock) {
            if (cacheLoaded) return
            cache.clear()
            readableDatabase.rawQuery(
                """SELECT item_id,kind,attempts,correct,streak,avg_response_ms,mastery,last_seen_at,next_due_at,source
                   FROM knowledge""",
                null,
            ).use { c ->
                while (c.moveToNext()) {
                    val state = row(c)
                    cache[state.itemId] = state
                }
            }
            cacheLoaded = true
        }
    }

    fun state(itemId: String): TrainingKnowledgeState? {
        ensureCache()
        return synchronized(cacheLock) { cache[itemId] }
    }

    fun states(itemIds: List<String>): Map<String, TrainingKnowledgeState> {
        if (itemIds.isEmpty()) return emptyMap()
        ensureCache()
        return synchronized(cacheLock) {
            buildMap {
                itemIds.distinct().forEach { id -> cache[id]?.let { put(id, it) } }
            }
        }
    }

    fun record(
        itemId: String,
        kind: String,
        correct: Boolean,
        responseMs: Long,
        source: String = "built_in",
        now: Long = System.currentTimeMillis(),
        confidence: Float = 1f,
    ): TrainingKnowledgeState {
        val old = state(itemId)
        val attempts = (old?.attempts ?: 0) + 1
        val correctCount = (old?.correct ?: 0) + if (correct) 1 else 0
        val streak = if (correct) (old?.streak ?: 0) + 1 else 0
        val safeMs = responseMs.coerceIn(250L, 120_000L)
        val avgMs = if (old == null || old.attempts == 0) safeMs
        else ((old.avgResponseMs * old.attempts) + safeMs) / attempts

        val speed = when {
            safeMs <= 4_000L -> 1.0f
            safeMs <= 8_000L -> 0.9f
            safeMs <= 15_000L -> 0.78f
            safeMs <= 30_000L -> 0.62f
            else -> 0.48f
        }
        val safeConfidence = confidence.coerceIn(0.25f, 1f)
        val mastery = if (correct) {
            val gain = (0.10f + speed * 0.08f + min(streak, 5) * 0.008f) * safeConfidence
            min(1f, (old?.mastery ?: 0f) + gain)
        } else {
            max(0f, (old?.mastery ?: 0f) - 0.22f)
        }

        val nextDue = if (!correct) {
            now + 10L * 60L * 1000L
        } else {
            val base = when {
                streak <= 1 -> 6L * 60L * 60L * 1000L
                streak == 2 -> 24L * 60L * 60L * 1000L
                streak == 3 -> 3L * 24L * 60L * 60L * 1000L
                streak == 4 -> 7L * 24L * 60L * 60L * 1000L
                streak == 5 -> 14L * 24L * 60L * 60L * 1000L
                else -> 30L * 24L * 60L * 60L * 1000L
            }
            val latencyFactor = when {
                speed >= 0.9f -> 1.15
                speed >= 0.75f -> 1.0
                else -> 0.65
            }
            val confidenceFactor = 0.35 + 0.65 * safeConfidence
            now + (base * latencyFactor * confidenceFactor).toLong()
        }

        val state = TrainingKnowledgeState(
            itemId = itemId,
            kind = kind,
            attempts = attempts,
            correct = correctCount,
            streak = streak,
            avgResponseMs = avgMs,
            mastery = mastery,
            lastSeenAt = now,
            nextDueAt = nextDue,
            source = source,
        )
        val values = ContentValues().apply {
            put("item_id", state.itemId)
            put("kind", state.kind)
            put("attempts", state.attempts)
            put("correct", state.correct)
            put("streak", state.streak)
            put("avg_response_ms", state.avgResponseMs)
            put("mastery", state.mastery)
            put("last_seen_at", state.lastSeenAt)
            put("next_due_at", state.nextDueAt)
            put("source", state.source)
        }
        writableDatabase.insertWithOnConflict("knowledge", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        synchronized(cacheLock) { cache[itemId] = state }
        return state
    }

    fun pickAdaptive(candidates: List<String>, exclude: String? = null, now: Long = System.currentTimeMillis()): String? {
        val usable = candidates.filter { it != exclude }
        if (usable.isEmpty()) return candidates.firstOrNull()
        val stateMap = states(usable)
        return usable
            .asSequence()
            .map { id -> id to (stateMap[id]?.let { selectionScore(it, now) } ?: 350.0) }
            .sortedByDescending { it.second }
            .take(12)
            .toList()
            .random()
            .first
    }

    fun scoreForSelection(itemId: String, now: Long = System.currentTimeMillis()): Double =
        state(itemId)?.let { selectionScore(it, now) } ?: 350.0

    fun bestKindScore(kind: String, totalCandidates: Int, now: Long = System.currentTimeMillis()): Double {
        ensureCache()
        val matching = synchronized(cacheLock) { cache.values.filter { it.kind == kind } }
        val tracked = matching.size
        var best = matching.maxOfOrNull { selectionScore(it, now) } ?: Double.NEGATIVE_INFINITY
        if (tracked < totalCandidates) best = maxOf(best, 350.0)
        return if (best == Double.NEGATIVE_INFINITY) 350.0 else best
    }

    fun weakCount(kinds: List<String>? = null): Int {
        ensureCache()
        val allowed = kinds?.toSet()
        return synchronized(cacheLock) {
            cache.values.count { it.attempts >= 2 && it.mastery < 0.55f && (allowed == null || it.kind in allowed) }
        }
    }

    fun bestSelectionScore(candidates: List<String>, now: Long = System.currentTimeMillis()): Double {
        if (candidates.isEmpty()) return Double.NEGATIVE_INFINITY
        val stateMap = states(candidates)
        return candidates.maxOf { id -> stateMap[id]?.let { selectionScore(it, now) } ?: 350.0 }
    }

    private fun selectionScore(s: TrainingKnowledgeState, now: Long): Double {
        val overdueHours = ((now - s.nextDueAt).coerceAtLeast(0L) / 3_600_000.0)
        val weakBonus = (1.0 - s.mastery) * 100.0
        val errorRate = if (s.attempts == 0) 1.0 else 1.0 - s.correct.toDouble() / s.attempts
        val dueBonus = if (s.nextDueAt <= now) 500.0
        else -((s.nextDueAt - now) / 3_600_000.0).coerceAtMost(200.0)
        return dueBonus + overdueHours.coerceAtMost(300.0) + weakBonus + errorRate * 100.0
    }

    /** Remove scheduler rows for personal words that are no longer in the active vocab bank. */
    fun prunePersonalVocabulary(activeItemIds: Set<String>) {
        ensureCache()
        val stale = synchronized(cacheLock) {
            cache.values.filter { it.kind == "personal_vocab" && it.itemId !in activeItemIds }.map { it.itemId }
        }
        if (stale.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            stale.forEach { id -> db.delete("knowledge", "item_id=?", arrayOf(id)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        synchronized(cacheLock) { stale.forEach(cache::remove) }
    }

    fun prunePersonalPhrases(activeItemIds: Set<String>) {
        ensureCache()
        val stale = synchronized(cacheLock) {
            cache.values.filter { it.kind == "personal_phrase" && it.itemId !in activeItemIds }.map { it.itemId }
        }
        if (stale.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            stale.forEach { id -> db.delete("knowledge", "item_id=?", arrayOf(id)) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        synchronized(cacheLock) { stale.forEach(cache::remove) }
    }

    fun exportBackupJson(): JSONArray {
        ensureCache()
        val snapshot = synchronized(cacheLock) { cache.values.sortedBy { it.itemId } }
        val out = JSONArray()
        snapshot.forEach { s ->
            out.put(
                JSONObject()
                    .put("item_id", s.itemId)
                    .put("kind", s.kind)
                    .put("attempts", s.attempts)
                    .put("correct", s.correct)
                    .put("streak", s.streak)
                    .put("avg_response_ms", s.avgResponseMs)
                    .put("mastery", s.mastery.toDouble())
                    .put("last_seen_at", s.lastSeenAt)
                    .put("next_due_at", s.nextDueAt)
                    .put("source", s.source)
            )
        }
        return out
    }

    fun restoreBackupJson(items: JSONArray): Int {
        val db = writableDatabase
        val restoredStates = linkedMapOf<String, TrainingKnowledgeState>()
        db.beginTransaction()
        try {
            db.delete("knowledge", null, null)
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val itemId = item.optString("item_id")
                val kind = item.optString("kind")
                if (itemId.isBlank() || kind.isBlank()) continue
                val state = TrainingKnowledgeState(
                    itemId = itemId,
                    kind = kind,
                    attempts = item.optInt("attempts", 0).coerceAtLeast(0),
                    correct = item.optInt("correct", 0).coerceAtLeast(0),
                    streak = item.optInt("streak", 0).coerceAtLeast(0),
                    avgResponseMs = item.optLong("avg_response_ms", 0L).coerceAtLeast(0L),
                    mastery = item.optDouble("mastery", 0.0).coerceIn(0.0, 1.0).toFloat(),
                    lastSeenAt = item.optLong("last_seen_at", 0L).coerceAtLeast(0L),
                    nextDueAt = item.optLong("next_due_at", 0L).coerceAtLeast(0L),
                    source = item.optString("source", "built_in"),
                )
                val values = ContentValues().apply {
                    put("item_id", state.itemId)
                    put("kind", state.kind)
                    put("attempts", state.attempts)
                    put("correct", state.correct)
                    put("streak", state.streak)
                    put("avg_response_ms", state.avgResponseMs)
                    put("mastery", state.mastery)
                    put("last_seen_at", state.lastSeenAt)
                    put("next_due_at", state.nextDueAt)
                    put("source", state.source)
                }
                db.insertWithOnConflict("knowledge", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                restoredStates[state.itemId] = state
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        synchronized(cacheLock) {
            cache.clear()
            cache.putAll(restoredStates)
            cacheLoaded = true
        }
        return restoredStates.size
    }

    fun dueCount(kind: String? = null, now: Long = System.currentTimeMillis()): Int {
        ensureCache()
        return synchronized(cacheLock) {
            cache.values.count { it.nextDueAt <= now && (kind == null || it.kind == kind) }
        }
    }

    fun statsForKinds(kinds: List<String>, now: Long = System.currentTimeMillis()): AdaptiveTrainingStats {
        if (kinds.isEmpty()) return AdaptiveTrainingStats(0, 0, 0, 0f)
        ensureCache()
        val allowed = kinds.toSet()
        val values = synchronized(cacheLock) { cache.values.filter { it.kind in allowed } }
        if (values.isEmpty()) return AdaptiveTrainingStats(0, 0, 0, 0f)
        return AdaptiveTrainingStats(
            tracked = values.size,
            mastered = values.count { it.mastery >= 0.85f && it.streak >= 4 },
            due = values.count { it.nextDueAt <= now },
            averageMastery = values.map { it.mastery }.average().toFloat(),
        )
    }

    fun stats(now: Long = System.currentTimeMillis()): AdaptiveTrainingStats {
        ensureCache()
        val values = synchronized(cacheLock) { cache.values.toList() }
        if (values.isEmpty()) return AdaptiveTrainingStats(0, 0, 0, 0f)
        return AdaptiveTrainingStats(
            tracked = values.size,
            mastered = values.count { it.mastery >= 0.85f && it.streak >= 4 },
            due = values.count { it.nextDueAt <= now },
            averageMastery = values.map { it.mastery }.average().toFloat(),
        )
    }
}
