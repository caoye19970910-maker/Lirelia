package com.cy.languagereader.mobile.data

import java.text.Normalizer
import java.util.Locale

data class AnswerJudgement(
    val correct: Boolean,
    val normalizedUser: String,
    val normalizedExpected: String,
    val differenceHint: String = "",
)

/**
 * Conservative French-answer normalisation.
 *
 * We deliberately ignore only presentation differences that should not turn a
 * correct answer into a wrong one: case, typographic apostrophes, non-breaking
 * spaces, spacing around apostrophes/hyphens/punctuation, and terminal
 * punctuation. Accents, articles, prepositions, conjugations and word order are
 * kept intact, so a/à, de/à, est/était, etc. remain real errors.
 */
object FrenchAnswerJudge {
    fun sentence(user: String, accepted: List<String>): AnswerJudgement {
        val userNorm = normalizeSentence(user)
        val expectedNorms = accepted.map(::normalizeSentence)
        val matchingIndex = expectedNorms.indexOf(userNorm)
        if (matchingIndex >= 0) {
            return AnswerJudgement(true, userNorm, expectedNorms[matchingIndex])
        }

        val closestIndex = accepted.indices.minByOrNull { i -> tokenDistance(userNorm, expectedNorms[i]) } ?: 0
        val expected = accepted.getOrElse(closestIndex) { accepted.firstOrNull().orEmpty() }
        val expectedNorm = expectedNorms.getOrElse(closestIndex) { normalizeSentence(expected) }
        return AnswerJudgement(
            correct = false,
            normalizedUser = userNorm,
            normalizedExpected = expectedNorm,
            differenceHint = firstDifferenceHint(userNorm, expectedNorm),
        )
    }

    fun short(user: String, accepted: List<String>): AnswerJudgement {
        val userNorm = normalizeShort(user)
        val expectedNorms = accepted.map(::normalizeShort)
        val matchingIndex = expectedNorms.indexOf(userNorm)
        if (matchingIndex >= 0) {
            return AnswerJudgement(true, userNorm, expectedNorms[matchingIndex])
        }
        val closestIndex = accepted.indices.minByOrNull { i -> tokenDistance(userNorm, expectedNorms[i]) } ?: 0
        val expectedNorm = expectedNorms.getOrElse(closestIndex) { accepted.firstOrNull()?.let(::normalizeShort).orEmpty() }
        return AnswerJudgement(false, userNorm, expectedNorm, firstDifferenceHint(userNorm, expectedNorm))
    }

    fun normalizeSentence(value: String): String = normalizeBase(value)
        .replace(Regex("[.!?;:,…]+$"), "")
        .trim()

    fun normalizeShort(value: String): String = normalizeBase(value)
        .replace(Regex("[.!?;:,…]+$"), "")
        .trim()

    private fun normalizeBase(value: String): String {
        var s = Normalizer.normalize(value, Normalizer.Form.NFC)
            .lowercase(Locale.FRANCE)
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('ʼ', '\'')
            .replace('‐', '-')
            .replace('‑', '-')
            .replace('–', '-')
            .trim()

        // Formatting around French elisions and hyphenated inversions should not
        // decide correctness (j ' ai == j'ai; a - t - il == a-t-il).
        s = s.replace(Regex("\\s*'\\s*"), "'")
        s = s.replace(Regex("\\s*-\\s*"), "-")
        // Normalise spacing around punctuation but keep the punctuation itself;
        // internal punctuation can still distinguish genuinely different text.
        s = s.replace(Regex("\\s*([,;:!?])\\s*"), "$1 ")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    private fun tokenDistance(a: String, b: String): Int {
        val aa = answerTokens(a)
        val bb = answerTokens(b)
        val common = minOf(aa.size, bb.size)
        var distance = kotlin.math.abs(aa.size - bb.size)
        for (i in 0 until common) if (aa[i] != bb[i]) distance++
        return distance
    }

    private fun firstDifferenceHint(userNorm: String, expectedNorm: String): String {
        if (userNorm.isBlank()) return "你还没有输入答案。"
        val user = answerTokens(userNorm)
        val expected = answerTokens(expectedNorm)
        val common = minOf(user.size, expected.size)
        for (i in 0 until common) {
            if (user[i] != expected[i]) {
                return "第一个不同点：你写了「${user[i]}」，参考表达是「${expected[i]}」。"
            }
        }
        return when {
            user.size < expected.size -> "你的答案少了：${expected.drop(user.size).joinToString(" ")}。"
            user.size > expected.size -> "你的答案多了：${user.drop(expected.size).joinToString(" ")}。"
            else -> "请对照参考表达检查变位、冠词、介词或拼写。"
        }
    }

    private fun answerTokens(value: String): List<String> = value
        .replace(Regex("([,;:!?])"), " $1 ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
}
