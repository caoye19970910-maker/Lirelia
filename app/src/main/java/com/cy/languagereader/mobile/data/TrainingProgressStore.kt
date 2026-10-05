package com.cy.languagereader.mobile.data

import android.content.Context

data class TrainingProgress(
    val answered: Int = 0,
    val correct: Int = 0,
    val bestStreak: Int = 0,
)

class TrainingProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("french_training_progress", Context.MODE_PRIVATE)

    fun load(mode: String): TrainingProgress = TrainingProgress(
        answered = prefs.getInt("${mode}_answered", 0),
        correct = prefs.getInt("${mode}_correct", 0),
        bestStreak = prefs.getInt("${mode}_best_streak", 0),
    )

    fun record(mode: String, correct: Boolean, streak: Int): TrainingProgress {
        val old = load(mode)
        val updated = TrainingProgress(
            answered = old.answered + 1,
            correct = old.correct + if (correct) 1 else 0,
            bestStreak = maxOf(old.bestStreak, streak),
        )
        prefs.edit()
            .putInt("${mode}_answered", updated.answered)
            .putInt("${mode}_correct", updated.correct)
            .putInt("${mode}_best_streak", updated.bestStreak)
            .apply()
        return updated
    }
}
