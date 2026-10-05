package com.cy.languagereader.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TrainingWrongItem(
    val kind: String,
    val itemId: String,
)

data class TrainingSessionSnapshot(
    val modeName: String,
    val answered: Int,
    val correct: Int,
    val streak: Int,
    val target: Int,
    val startedAt: Long,
    val updatedAt: Long,
    val currentKind: String = "",
    val currentItemId: String = "",
    val currentWrong: Boolean = false,
    val wrongItems: List<TrainingWrongItem> = emptyList(),
)

/** Durable checkpoint including the exact unanswered question and this session's mistakes. */
class TrainingSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("french_training_session", Context.MODE_PRIVATE)

    fun load(): TrainingSessionSnapshot? {
        if (!prefs.getBoolean("active", false)) return null
        val mode = prefs.getString("mode", null).orEmpty()
        if (mode.isBlank()) return null
        return TrainingSessionSnapshot(
            modeName = mode,
            answered = prefs.getInt("answered", 0).coerceAtLeast(0),
            correct = prefs.getInt("correct", 0).coerceAtLeast(0),
            streak = prefs.getInt("streak", 0).coerceAtLeast(0),
            target = prefs.getInt("target", 0).coerceAtLeast(0),
            startedAt = prefs.getLong("started_at", 0L),
            updatedAt = prefs.getLong("updated_at", 0L),
            currentKind = prefs.getString("current_kind", "").orEmpty(),
            currentItemId = prefs.getString("current_item_id", "").orEmpty(),
            currentWrong = prefs.getBoolean("current_wrong", false),
            wrongItems = parseWrongItems(prefs.getString("wrong_items", "[]").orEmpty()),
        )
    }

    fun start(modeName: String, target: Int): TrainingSessionSnapshot {
        val now = System.currentTimeMillis()
        val snapshot = TrainingSessionSnapshot(modeName, 0, 0, 0, target.coerceAtLeast(0), now, now)
        write(snapshot)
        return snapshot
    }

    fun update(modeName: String, answered: Int, correct: Int, streak: Int, target: Int) {
        val old = load()
        val now = System.currentTimeMillis()
        write(
            TrainingSessionSnapshot(
                modeName = modeName,
                answered = answered.coerceAtLeast(0),
                correct = correct.coerceIn(0, answered.coerceAtLeast(0)),
                streak = streak.coerceAtLeast(0),
                target = target.coerceAtLeast(0),
                startedAt = old?.takeIf { it.modeName == modeName }?.startedAt ?: now,
                updatedAt = now,
                currentKind = old?.takeIf { it.modeName == modeName }?.currentKind.orEmpty(),
                currentItemId = old?.takeIf { it.modeName == modeName }?.currentItemId.orEmpty(),
                currentWrong = old?.takeIf { it.modeName == modeName }?.currentWrong ?: false,
                wrongItems = old?.takeIf { it.modeName == modeName }?.wrongItems.orEmpty(),
            )
        )
    }

    fun updateQuestion(modeName: String, kind: String, itemId: String) {
        val old = load()?.takeIf { it.modeName == modeName } ?: return
        val sameQuestion = old.currentKind == kind && old.currentItemId == itemId
        write(
            old.copy(
                currentKind = kind,
                currentItemId = itemId,
                currentWrong = if (sameQuestion) old.currentWrong else false,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    fun setCurrentWrong(modeName: String, wrong: Boolean) {
        val old = load()?.takeIf { it.modeName == modeName } ?: return
        write(old.copy(currentWrong = wrong, updatedAt = System.currentTimeMillis()))
    }

    fun recordWrong(modeName: String, kind: String, itemId: String) {
        if (kind.isBlank() || itemId.isBlank()) return
        val old = load()?.takeIf { it.modeName == modeName } ?: return
        val item = TrainingWrongItem(kind, itemId)
        val next = if (old.wrongItems.any { it.kind == kind && it.itemId == itemId }) {
            old.wrongItems
        } else {
            old.wrongItems + item
        }
        write(old.copy(wrongItems = next, currentWrong = true, updatedAt = System.currentTimeMillis()))
    }

    fun clearQuestion(modeName: String) {
        val old = load()?.takeIf { it.modeName == modeName } ?: return
        write(old.copy(currentKind = "", currentItemId = "", currentWrong = false, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Completes an active session while keeping its wrong-question list available on the hub.
     */
    fun finish(modeName: String) {
        val old = load()?.takeIf { it.modeName == modeName }
        val edit = prefs.edit()
        if (old != null) {
            edit.putString("last_completed_mode", old.modeName)
            edit.putLong("last_completed_at", System.currentTimeMillis())
            edit.putString("last_completed_wrong_items", encodeWrongItems(old.wrongItems))
        }
        removeActiveKeys(edit).apply()
    }

    fun lastCompletedWrongItems(): List<TrainingWrongItem> =
        parseWrongItems(prefs.getString("last_completed_wrong_items", "[]").orEmpty())

    fun lastCompletedMode(): String = prefs.getString("last_completed_mode", "").orEmpty()

    /** Discards only the active/resumable session; the previous completed mistake list is preserved. */
    fun clear() {
        removeActiveKeys(prefs.edit()).apply()
    }

    private fun write(snapshot: TrainingSessionSnapshot) {
        prefs.edit()
            .putBoolean("active", true)
            .putString("mode", snapshot.modeName)
            .putInt("answered", snapshot.answered)
            .putInt("correct", snapshot.correct)
            .putInt("streak", snapshot.streak)
            .putInt("target", snapshot.target)
            .putLong("started_at", snapshot.startedAt)
            .putLong("updated_at", snapshot.updatedAt)
            .putString("current_kind", snapshot.currentKind)
            .putString("current_item_id", snapshot.currentItemId)
            .putBoolean("current_wrong", snapshot.currentWrong)
            .putString("wrong_items", encodeWrongItems(snapshot.wrongItems))
            .apply()
    }

    private fun removeActiveKeys(edit: android.content.SharedPreferences.Editor): android.content.SharedPreferences.Editor = edit
        .remove("active")
        .remove("mode")
        .remove("answered")
        .remove("correct")
        .remove("streak")
        .remove("target")
        .remove("started_at")
        .remove("updated_at")
        .remove("current_kind")
        .remove("current_item_id")
        .remove("current_wrong")
        .remove("wrong_items")

    private fun encodeWrongItems(items: List<TrainingWrongItem>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().put("kind", item.kind).put("item_id", item.itemId))
        }
    }.toString()

    private fun parseWrongItems(raw: String): List<TrainingWrongItem> = runCatching {
        val arr = JSONArray(raw.ifBlank { "[]" })
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val kind = obj.optString("kind").trim()
                val itemId = obj.optString("item_id").trim()
                if (kind.isNotBlank() && itemId.isNotBlank()) add(TrainingWrongItem(kind, itemId))
            }
        }
    }.getOrDefault(emptyList())
}
