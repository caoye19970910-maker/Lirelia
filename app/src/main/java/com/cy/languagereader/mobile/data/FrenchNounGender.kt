package com.cy.languagereader.mobile.data

import java.util.Locale

/**
 * V5.0: one gender resolver shared by the normal reader and Readium.
 *
 * Priority: explicit training lexicon -> nearby article in the real sentence ->
 * explicit dictionary markers -> curated high-frequency nouns -> high-confidence
 * suffix hint. The suffix path is labelled as inferred so the UI never presents a
 * heuristic as dictionary fact.
 */
object FrenchNounGender {
    private val token = Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+(?:[-'’][A-Za-zÀ-ÖØ-öø-ÿŒœÆæ]+)*")

    private val common = mapOf(
        "nid" to "阳性",
        "foule" to "阴性",
        "baguette" to "阴性",
        "forêt" to "阴性",
        "oiseau" to "阳性",
        "sommeil" to "阳性",
        "quartier" to "阳性",
        "classe" to "阴性",
        "directeur" to "阳性",
        "retenue" to "阴性",
        "bruit" to "阳性",
        "pouvoir" to "阳性",
        "paix" to "阴性",
        "branche" to "阴性",
        "montagne" to "阴性",
        "sorcière" to "阴性",
        "fée" to "阴性",
        "elfe" to "阳性",
        "troll" to "阳性",
    )

    fun label(result: DictionaryResult, sentence: String = "", selectedWord: String = result.query): String? {
        val pos = result.pos.lowercase(Locale.ROOT)
        if (!("noun" in pos || "nom" in pos || "名词" in pos || "名詞" in pos)) return null

        val lemma = normalize(result.lemma.ifBlank { selectedWord })
        val word = normalize(selectedWord)

        FrenchTrainingData.nounGender.firstOrNull {
            normalize(it.noun) == lemma || normalize(it.noun) == word
        }?.let { return if (it.gender == "f") "阴性" else "阳性" }

        val tokens = token.findAll(sentence).toList()
        val selected = tokens.indexOfFirst { normalize(it.value) == word }
        if (selected > 0) {
            when (normalize(tokens[selected - 1].value)) {
                "une", "la", "ma", "ta", "sa", "cette" -> return "阴性"
                "un", "le", "du", "au", "ce", "cet" -> return "阳性"
            }
        }

        val raw = result.definitions.joinToString(" ").lowercase(Locale.FRENCH)
        if (Regex("(?:^|\\s)n\\.?\\s*f\\.?(?:\\s|$)").containsMatchIn(raw)) return "阴性"
        if (Regex("(?:^|\\s)n\\.?\\s*m\\.?(?:\\s|$)").containsMatchIn(raw)) return "阳性"

        common[lemma]?.let { return it }
        common[word]?.let { return it }

        // Do not promote suffix heuristics to grammatical facts. French has too
        // many exceptions for that to be trustworthy in a learning dictionary.
        return null
    }

    private fun normalize(value: String): String = value
        .trim()
        .replace('’', '\'')
        .lowercase(Locale.FRENCH)
}
