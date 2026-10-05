package com.cy.languagereader.mobile.data

import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.util.Locale
import kotlin.math.ln

internal data class LexicalResolution(
    val lemma: String,
    val pos: String,
    val ipa: String,
    val definitions: List<String>,
    val examples: List<String>,
    val related: List<String>,
    val morphology: List<String>,
    val alternatives: List<DictionaryAlternative>,
    val source: String,
)

/**
 * Lirelia Dictionary Engine 2.0 resolver.
 *
 * The important rule is architectural: lexical meanings and inflection analyses are
 * different data. A surface form may be both a lexeme (`tâche` noun) and an inflected
 * form (`tâcher` verb). We collect every candidate, score them with lightweight French
 * context rules, and only then choose what the reading card should lead with.
 */
internal object LexicalResolver {
    private data class Candidate(
        val lemma: String,
        val lemmaNorm: String,
        val pos: String,
        var ipa: String = "",
        val definitions: LinkedHashSet<String> = linkedSetOf(),
        val examples: LinkedHashSet<String> = linkedSetOf(),
        val related: LinkedHashSet<String> = linkedSetOf(),
        val morphology: LinkedHashSet<String> = linkedSetOf(),
        var score: Double = 0.0,
        var source: String = "",
        var exactSemantic: Boolean = false,
    )

    fun resolve(db: SQLiteDatabase, input: String, context: String = "", contextOffset: Int = -1): LexicalResolution? {
        val query = clean(input)
        if (query.isBlank()) return null
        val key = normalize(query)
        val candidates = linkedMapOf<String, Candidate>()

        fun candidate(lemma: String, pos: String): Candidate {
            val lemmaNorm = normalize(lemma)
            val id = "$lemmaNorm|${pos.lowercase(Locale.FRENCH)}"
            return candidates.getOrPut(id) { Candidate(lemma, lemmaNorm, pos) }
        }

        // 1) Exact lexical readings. These are genuine dictionary senses, never
        // morphology prose such as "tâcher 的第三人称…".
        db.rawQuery(
            """
            SELECT lemma, lemma_norm, pos, ipa, definitions, examples, related,
                   quality, frequency, source
            FROM lexeme
            WHERE surface_norm=?
            ORDER BY quality DESC, frequency DESC, id
            """.trimIndent(),
            arrayOf(key),
        ).use { c ->
            while (c.moveToNext()) {
                val lemma = c.getString(0).orEmpty().ifBlank { query }
                val pos = c.getString(2).orEmpty()
                val item = candidate(lemma, pos)
                item.exactSemantic = true
                item.ipa = item.ipa.ifBlank { c.getString(3).orEmpty() }
                parseJsonArray(c.getString(4)).forEach(item.definitions::add)
                parseJsonArray(c.getString(5)).forEach(item.examples::add)
                parseJsonArray(c.getString(6)).forEach(item.related::add)
                val quality = c.getInt(7)
                val frequency = c.getDouble(8)
                item.score = maxOf(item.score, 100.0 + quality * 0.40 + frequencyBonus(frequency))
                item.source = c.getString(9).orEmpty()
            }
        }

        // 2) Morphological analyses normalized out of the old dictionary. Group all
        // grammatical readings of one (surface, lemma, POS) rather than presenting
        // each tense/person as a fake Chinese definition.
        db.rawQuery(
            """
            SELECT lemma, lemma_norm, pos, morphology, priority, frequency, source
            FROM form_analysis
            WHERE form_norm=?
            ORDER BY priority DESC, id
            """.trimIndent(),
            arrayOf(key),
        ).use { c ->
            while (c.moveToNext()) {
                val lemma = c.getString(0).orEmpty()
                if (lemma.isBlank()) continue
                val pos = c.getString(2).orEmpty()
                val item = candidate(lemma, pos)
                c.getString(3)?.trim()?.takeIf { it.isNotBlank() }?.let(item.morphology::add)
                val priority = c.getInt(4)
                val frequency = c.getDouble(5)
                var base = 122.0 + priority * 0.18 + frequencyBonus(frequency)
                if (item.morphology.any { it.contains("分詞") || it.contains("分词") }) base += 7.0
                val source = c.getString(6).orEmpty()
                if (source == "legacy_form_index" && item.morphology.any { it.contains("alternative", ignoreCase = true) }) {
                    base -= 45.0
                }
                item.score = maxOf(item.score, base)
                if (item.source.isBlank()) item.source = source
            }
        }

        // 3) Explicit common irregular conjugations. This closes holes such as
        // `dit -> dire` even when the source dictionary lacks a form row.
        FrenchConjugator.analysesForForm(query).forEach { analysis ->
            if (hasLexeme(db, analysis.lemma)) {
                val item = candidate(analysis.lemma, "verb")
                analysis.details.forEach(item.morphology::add)
                item.score = maxOf(item.score, 150.0)
                if (item.source.isBlank()) item.source = "irregular_conjugation"
            }
        }

        // 4) Conservative regular -er reverse rules. They are only accepted when
        // the proposed lemma actually exists in our lexical data, so `livre` can
        // resolve to `livrer` in "il livre…" without inventing arbitrary verbs.
        FrenchConjugator.regularLemmaCandidatesForForm(query).forEach { analysis ->
            if (hasLexeme(db, analysis.lemma)) {
                val item = candidate(analysis.lemma, "verb")
                analysis.details.forEach(item.morphology::add)
                item.score = maxOf(item.score, 125.0)
                if (item.source.isBlank()) item.source = "regular_conjugation"
            }
        }

        if (candidates.isEmpty()) return null

        // Hydrate morphology-only candidates from their lemma's lexical senses.
        candidates.values.forEach { hydrateLemma(db, it) }

        val contextWindow = ContextWindow(query, context, contextOffset)
        candidates.values.forEach { it.score += contextScore(it, contextWindow) }

        val ranked = candidates.values
            .sortedWith(compareByDescending<Candidate> { it.score }
                .thenByDescending { it.definitions.isNotEmpty() }
                .thenByDescending { it.exactSemantic })

        val best = ranked.first()
        val alternatives = ranked.drop(1)
            .filter { alt -> alt.lemmaNorm != best.lemmaNorm || posCategories(alt.pos) != posCategories(best.pos) }
            .take(4)
            .map { alt ->
                DictionaryAlternative(
                    lemma = alt.lemma,
                    pos = alt.pos,
                    morphology = orderedMorphology(alt.morphology).firstOrNull().orEmpty(),
                    preview = alt.definitions.firstOrNull().orEmpty(),
                )
            }

        return LexicalResolution(
            lemma = best.lemma,
            pos = best.pos,
            ipa = best.ipa,
            definitions = best.definitions.toList(),
            examples = best.examples.toList(),
            related = best.related.toList(),
            morphology = orderedMorphology(best.morphology),
            alternatives = alternatives,
            source = best.source,
        )
    }

    private data class LemmaSense(
        val pos: String,
        val ipa: String,
        val definitions: String?,
        val examples: String?,
        val related: String?,
        val quality: Int,
        val frequency: Double,
        val source: String,
    )

    private fun hydrateLemma(db: SQLiteDatabase, item: Candidate) {
        if (item.definitions.isNotEmpty() && item.ipa.isNotBlank()) return
        val rows = mutableListOf<LemmaSense>()
        db.rawQuery(
            """
            SELECT pos, ipa, definitions, examples, related, quality, frequency, source
            FROM lexeme
            WHERE lemma_norm=?
            ORDER BY quality DESC, frequency DESC, id
            """.trimIndent(),
            arrayOf(item.lemmaNorm),
        ).use { c ->
            while (c.moveToNext()) {
                rows += LemmaSense(
                    pos = c.getString(0).orEmpty(),
                    ipa = c.getString(1).orEmpty(),
                    definitions = c.getString(2),
                    examples = c.getString(3),
                    related = c.getString(4),
                    quality = c.getInt(5),
                    frequency = c.getDouble(6),
                    source = c.getString(7).orEmpty(),
                )
            }
        }
        if (rows.isEmpty()) return

        // Prefer a genuinely compatible lexical sense. The database contains a
        // small number of Chinese/composite POS labels (e.g. “名词（阴性）”,
        // “形容词 / 名词”), so SQL string equality is not enough here.
        val row = rows.firstOrNull { posCompatible(item.pos, it.pos) } ?: rows.first()
        item.ipa = item.ipa.ifBlank { row.ipa }
        parseJsonArray(row.definitions).forEach(item.definitions::add)
        parseJsonArray(row.examples).forEach(item.examples::add)
        parseJsonArray(row.related).forEach(item.related::add)
        item.score += row.quality * 0.03 + frequencyBonus(row.frequency) * 0.25
        if (item.source.isBlank()) item.source = row.source
    }

    private fun hasLexeme(db: SQLiteDatabase, lemma: String): Boolean =
        db.rawQuery("SELECT 1 FROM lexeme WHERE lemma_norm=? LIMIT 1", arrayOf(normalize(lemma))).use { it.moveToFirst() }

    private fun contextScore(candidate: Candidate, ctx: ContextWindow): Double {
        if (ctx.tokens.isEmpty() || ctx.index < 0) return 0.0
        val pos = posCategories(candidate.pos)
        val prev = ctx.prev(1)
        val prev2 = ctx.prev(2)
        val next = ctx.next(1)
        var score = 0.0

        val subjectPronouns = setOf("je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "qui", "ce", "ça", "cela")
        val determiners = setOf(
            "un", "une", "des", "le", "la", "les", "du", "de", "ce", "cet", "cette", "ces",
            "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses", "notre", "nos", "votre", "vos",
            "leur", "leurs", "chaque", "quel", "quelle", "quels", "quelles", "aucun", "aucune", "plusieurs",
        )
        val prepositions = setOf("à", "de", "dans", "sur", "sous", "avec", "pour", "par", "sans", "vers", "chez", "entre", "en")
        val avoirForms = setOf("ai", "as", "a", "avons", "avez", "ont", "avais", "avait", "avions", "aviez", "avaient", "aurai", "aura", "aurait")
        val etreForms = setOf("suis", "es", "est", "sommes", "êtes", "sont", "étais", "était", "étions", "étiez", "étaient", "sera", "seront", "serait")

        if (prev in subjectPronouns && "verb" in pos) score += 55.0
        if (prev in subjectPronouns && "noun" in pos) score -= 20.0

        if (prev in determiners) {
            if ("noun" in pos) score += 50.0
            if ("adj" in pos) score += 18.0
            if ("verb" in pos) score -= 25.0
        }

        if (prev in prepositions && "noun" in pos) score += 20.0
        if (prev2 in prepositions && prev in setOf("l", "le", "la", "les", "un", "une") && "noun" in pos) score += 45.0

        if (prev in avoirForms && "verb" in pos) {
            if (candidate.morphology.any { it.contains("分詞") || it.contains("分词") }) score += 65.0
            else score += 20.0
        }
        if (prev in etreForms && "verb" in pos) {
            // With être a past participle may be passive, but an exact adjective
            // reading is often the better dictionary lead (`le siège est pris`).
            if (candidate.morphology.any { it.contains("分詞") || it.contains("分词") }) score += 30.0
            else score += 20.0
        }

        // Possessive determiners normally stand directly before a noun/adjective.
        if ("det" in pos && next.isNotBlank()) score += 25.0

        if (prev in etreForms && "adj" in pos) score += 50.0

        // A lexical adjective after a content-looking token is a common pattern
        // (`siège pris`, `porte fermée`). Keep this deliberately modest.
        if ("adj" in pos && prev.isNotBlank() && prev !in subjectPronouns && prev !in determiners && prev !in prepositions) score += 12.0

        return score
    }

    private class ContextWindow(query: String, context: String, focusOffset: Int = -1) {
        val tokens: List<String>
        val index: Int

        init {
            // Keep character positions stable so the exact tapped occurrence can be
            // identified. Curly apostrophe -> ASCII apostrophe is length preserving.
            val normalized = context.replace('’', '\'').lowercase(Locale.FRENCH)
            val matches = Regex("[a-zà-öø-ÿœæ-]+").findAll(normalized).toList()
            tokens = matches.map { match ->
                when (match.value) {
                    "qu" -> "que"
                    else -> match.value
                }
            }
            val q = normalize(query)

            val focused = if (focusOffset >= 0) {
                val direct = matches.indexOfFirst { match -> focusOffset in match.range }
                if (direct >= 0 && tokens.getOrNull(direct) == q) {
                    direct
                } else {
                    matches.indices
                        .filter { tokens.getOrNull(it) == q }
                        .minByOrNull { kotlin.math.abs(matches[it].range.first - focusOffset) }
                        ?: -1
                }
            } else {
                -1
            }

            index = if (focused >= 0) focused else tokens.indexOf(q)
        }

        fun prev(distance: Int): String = tokens.getOrNull(index - distance).orEmpty()
        fun next(distance: Int): String = tokens.getOrNull(index + distance).orEmpty()
    }

    private fun orderedMorphology(values: Collection<String>): List<String> = values
        .filter { it.isNotBlank() }
        .distinct()
        .sortedByDescending { detail ->
            when {
                detail.contains("分词") || detail.contains("分詞") -> 100
                detail.contains("现在时") || detail.contains("現在時") -> 90
                detail.contains("未完成过去") || detail.contains("未完成過去") -> 80
                detail.contains("将来") || detail.contains("將來") -> 70
                detail.contains("条件") || detail.contains("條件") -> 60
                detail.contains("虚拟") || detail.contains("虛擬") -> 50
                detail.contains("命令") -> 40
                detail.contains("先过去") || detail.contains("先過去") -> 20
                else -> 30
            }
        }

    private fun posCompatible(a: String, b: String): Boolean {
        val left = posCategories(a)
        val right = posCategories(b)
        return left.isNotEmpty() && right.isNotEmpty() && left.any { it in right }
    }

    private fun posCategories(raw: String): Set<String> {
        val p = raw.lowercase(Locale.FRENCH)
        val out = linkedSetOf<String>()
        if ("verb" in p || "verbe" in p || "动词" in p || "動詞" in p) out += "verb"
        if ("noun" in p || "nom" in p || "名词" in p || "名詞" in p) out += "noun"
        if ("adj" in p || "形容词" in p || "形容詞" in p) out += "adj"
        if ("det" in p || "détermin" in p || "限定词" in p || "限定詞" in p || "冠词" in p || "冠詞" in p) out += "det"
        if ("pron" in p || "代词" in p || "代詞" in p) out += "pron"
        if ("adv" in p || "副词" in p || "副詞" in p) out += "adv"
        if ("prep" in p || "préposition" in p || "介词" in p || "介詞" in p) out += "prep"
        if ("conj" in p || "conjonction" in p || "连词" in p || "連詞" in p) out += "conj"
        if ("intj" in p || "interjection" in p || "感叹词" in p || "感嘆詞" in p) out += "intj"
        if (out.isEmpty()) p.trim().takeIf { it.isNotBlank() }?.let(out::add)
        return out
    }

    private fun frequencyBonus(value: Double): Double = if (value > 0.0) ln(1.0 + value) * 3.0 else 0.0

    private fun parseJsonArray(raw: String?): List<String> = runCatching {
        val arr = JSONArray(raw ?: "[]")
        buildList {
            for (i in 0 until arr.length()) {
                arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun clean(value: String): String =
        value.trim().trim(' ', '.', ',', '!', '?', ';', ':', '«', '»', '"', '“', '”', '(', ')', '[', ']', '{', '}')

    private fun normalize(value: String): String = clean(value).replace('’', '\'').lowercase(Locale.FRENCH)
}
