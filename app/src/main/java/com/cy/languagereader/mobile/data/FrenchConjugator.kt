package com.cy.languagereader.mobile.data

data class ConjugationSection(
    val title: String,
    val forms: List<Pair<String, String>>,
)

data class ConjugationCard(
    val lemma: String,
    val sections: List<ConjugationSection>,
    val note: String = "",
)

data class ConjugationAnalysis(
    val lemma: String,
    val details: List<String>,
)

/**
 * Offline French conjugation helper used by both dictionary cards and the
 * French training center. Common irregular verbs are stored explicitly so the
 * training screen never invents a form from a regular ending rule.
 */
object FrenchConjugator {
    private val subjects = listOf("je", "tu", "il/elle", "nous", "vous", "ils/elles")

    fun conjugate(lemmaRaw: String, pos: String): ConjugationCard? {
        if (!pos.contains("verb", ignoreCase = true) &&
            !pos.contains("verbe", ignoreCase = true)) return null

        val lemma = lemmaRaw.trim().lowercase()
        irregular[lemma]?.let { spec ->
            return ConjugationCard(
                lemma = lemmaRaw,
                sections = listOf(
                    section("Présent", spec.present),
                    section("Imparfait", spec.imparfait),
                    section("Futur simple", spec.futur),
                    section("Conditionnel présent", spec.conditional),
                    ConjugationSection("Passé composé", listOf("participe passé" to spec.participle)),
                ),
                note = "常用不规则动词 · 离线专表",
            )
        }

        return when {
            lemma.endsWith("er") && lemma.length > 2 -> regularEr(lemmaRaw, lemma)
            lemma.endsWith("ir") && lemma.length > 2 -> regularIr(lemmaRaw, lemma)
            lemma.endsWith("re") && lemma.length > 2 -> regularRe(lemmaRaw, lemma)
            else -> null
        }
    }

    /**
     * Reverse lookup for common irregular forms encountered while reading.
     * This lets the dictionary resolve forms such as `devrait` → `devoir` even
     * when the bundled form_index is incomplete.
     */
    fun lemmaForForm(formRaw: String): String? = analysesForForm(formRaw).firstOrNull()?.lemma

    /**
     * Reverse analyses for common irregular verbs. Unlike the old one-lemma helper,
     * this keeps every matching grammatical reading so the dictionary can rank the
     * candidate with sentence context instead of silently discarding ambiguity.
     */
    fun analysesForForm(formRaw: String): List<ConjugationAnalysis> {
        val form = formRaw.trim().lowercase()
        if (form.isBlank()) return emptyList()
        val labels = listOf(
            "第一人称单数", "第二人称单数", "第三人称单数",
            "第一人称复数", "第二人称复数", "第三人称复数",
        )
        return buildList {
            irregular.forEach { (lemma, spec) ->
                val details = linkedSetOf<String>()
                spec.present.forEachIndexed { index, value ->
                    if (value.lowercase() == form) details += "直陈式现在时 · ${labels[index]}"
                }
                spec.imparfait.forEachIndexed { index, value ->
                    if (value.lowercase() == form) details += "直陈式未完成过去时 · ${labels[index]}"
                }
                spec.futur.forEachIndexed { index, value ->
                    if (value.lowercase() == form) details += "简单将来时 · ${labels[index]}"
                }
                spec.conditional.forEachIndexed { index, value ->
                    if (value.lowercase() == form) details += "条件式现在时 · ${labels[index]}"
                }
                if (spec.participle.lowercase() == form) details += "过去分词"
                if (details.isNotEmpty()) add(ConjugationAnalysis(lemma, details.toList()))
            }
        }
    }

    /**
     * Conservative regular -er reverse candidates. The dictionary resolver only
     * accepts a returned lemma when that lemma actually exists in its lexical data,
     * which prevents these spelling rules from inventing definitions.
     */
    fun regularLemmaCandidatesForForm(formRaw: String): List<ConjugationAnalysis> {
        val form = formRaw.trim().lowercase()
        if (form.length < 3) return emptyList()
        val out = linkedMapOf<String, MutableSet<String>>()
        fun add(lemma: String, detail: String) {
            if (lemma.length >= 4) out.getOrPut(lemma) { linkedSetOf() }.add(detail)
        }
        when {
            form.endsWith("ées") && form.length > 4 -> add(form.dropLast(3) + "er", "过去分词 · 阴性复数")
            form.endsWith("ée") && form.length > 3 -> add(form.dropLast(2) + "er", "过去分词 · 阴性单数")
            form.endsWith("és") && form.length > 3 -> add(form.dropLast(2) + "er", "过去分词 · 阳性复数")
            form.endsWith("é") && form.length > 2 -> add(form.dropLast(1) + "er", "过去分词 · 阳性单数")
        }
        if (form.endsWith("ent") && form.length > 4) add(form.dropLast(3) + "er", "直陈式现在时 · 第三人称复数")
        if (form.endsWith("ons") && form.length > 4) add(form.dropLast(3) + "er", "直陈式现在时 · 第一人称复数")
        if (form.endsWith("ez") && form.length > 3) add(form.dropLast(2) + "er", "直陈式现在时 · 第二人称复数")
        if (form.endsWith("es") && form.length > 3) add(form.dropLast(2) + "er", "直陈式现在时 · 第二人称单数")
        if (form.endsWith("e") && form.length > 2) add(form.dropLast(1) + "er", "直陈式现在时 · 第一/第三人称单数")
        return out.map { (lemma, details) -> ConjugationAnalysis(lemma, details.toList()) }
    }

    /** Data exposed specifically for the quiz UI. */
    fun irregularTrainingVerbs(): List<String> = irregular.keys.toList()

    fun trainingSubjects(): List<String> = subjects

    fun irregularTrainingForms(lemmaRaw: String): Map<String, List<String>>? {
        val spec = irregular[lemmaRaw.trim().lowercase()] ?: return null
        return linkedMapOf(
            "Présent" to spec.present,
            "Imparfait" to spec.imparfait,
            "Futur simple" to spec.futur,
            "Conditionnel présent" to spec.conditional,
        )
    }

    private fun regularEr(display: String, lemma: String): ConjugationCard {
        val stem = lemma.dropLast(2)
        val present = listOf(
            stem + "e", stem + "es", stem + "e",
            stem + "ons", stem + "ez", stem + "ent"
        )
        val imperfect = listOf(
            stem + "ais", stem + "ais", stem + "ait",
            stem + "ions", stem + "iez", stem + "aient"
        )
        val future = futureForms(lemma)
        return ConjugationCard(
            lemma = display,
            sections = listOf(
                section("Présent", present),
                section("Imparfait", imperfect),
                section("Futur simple", future),
                section("Conditionnel présent", conditionalForms(lemma)),
                ConjugationSection("Passé composé", listOf("participe passé" to (stem + "é"))),
            ),
            note = "规则 -er 动词 · manger/commencer 等拼写变化词需按专门规则检查",
        )
    }

    private fun regularIr(display: String, lemma: String): ConjugationCard {
        val stem = lemma.dropLast(2)
        val present = listOf(
            stem + "is", stem + "is", stem + "it",
            stem + "issons", stem + "issez", stem + "issent"
        )
        val imperfect = listOf(
            stem + "issais", stem + "issais", stem + "issait",
            stem + "issions", stem + "issiez", stem + "issaient"
        )
        return ConjugationCard(
            lemma = display,
            sections = listOf(
                section("Présent", present),
                section("Imparfait", imperfect),
                section("Futur simple", futureForms(lemma)),
                section("Conditionnel présent", conditionalForms(lemma)),
                ConjugationSection("Passé composé", listOf("participe passé" to (stem + "i"))),
            ),
            note = "规则第二组 -ir 动词 · 常见不规则 -ir 动词使用内置专表",
        )
    }

    private fun regularRe(display: String, lemma: String): ConjugationCard {
        val stem = lemma.dropLast(2)
        val futureStem = lemma.dropLast(1)
        val present = listOf(
            stem + "s", stem + "s", stem,
            stem + "ons", stem + "ez", stem + "ent"
        )
        val imperfect = listOf(
            stem + "ais", stem + "ais", stem + "ait",
            stem + "ions", stem + "iez", stem + "aient"
        )
        return ConjugationCard(
            lemma = display,
            sections = listOf(
                section("Présent", present),
                section("Imparfait", imperfect),
                section("Futur simple", futureForms(futureStem)),
                section("Conditionnel présent", conditionalForms(futureStem)),
                ConjugationSection("Passé composé", listOf("participe passé" to (stem + "u"))),
            ),
            note = "规则 -re 近似表 · 常见不规则 -re 动词使用内置专表",
        )
    }

    private fun futureForms(stem: String): List<String> =
        listOf(stem + "ai", stem + "as", stem + "a", stem + "ons", stem + "ez", stem + "ont")

    private fun conditionalForms(stem: String): List<String> =
        listOf(stem + "ais", stem + "ais", stem + "ait", stem + "ions", stem + "iez", stem + "aient")

    private fun section(title: String, values: List<String>) =
        ConjugationSection(title, subjects.zip(values))

    private data class Irregular(
        val present: List<String>,
        val imparfait: List<String>,
        val futur: List<String>,
        val conditional: List<String>,
        val participle: String,
    )

    private fun irr(
        present: String,
        imperfect: String,
        future: String,
        conditional: String,
        participle: String,
    ) = Irregular(
        present.split('|'),
        imperfect.split('|'),
        future.split('|'),
        conditional.split('|'),
        participle,
    )

    private val irregular = linkedMapOf(
        "être" to irr(
            "suis|es|est|sommes|êtes|sont",
            "étais|étais|était|étions|étiez|étaient",
            "serai|seras|sera|serons|serez|seront",
            "serais|serais|serait|serions|seriez|seraient", "été"
        ),
        "avoir" to irr(
            "ai|as|a|avons|avez|ont",
            "avais|avais|avait|avions|aviez|avaient",
            "aurai|auras|aura|aurons|aurez|auront",
            "aurais|aurais|aurait|aurions|auriez|auraient", "eu"
        ),
        "aller" to irr(
            "vais|vas|va|allons|allez|vont",
            "allais|allais|allait|allions|alliez|allaient",
            "irai|iras|ira|irons|irez|iront",
            "irais|irais|irait|irions|iriez|iraient", "allé"
        ),
        "faire" to irr(
            "fais|fais|fait|faisons|faites|font",
            "faisais|faisais|faisait|faisions|faisiez|faisaient",
            "ferai|feras|fera|ferons|ferez|feront",
            "ferais|ferais|ferait|ferions|feriez|feraient", "fait"
        ),
        "venir" to irr(
            "viens|viens|vient|venons|venez|viennent",
            "venais|venais|venait|venions|veniez|venaient",
            "viendrai|viendras|viendra|viendrons|viendrez|viendront",
            "viendrais|viendrais|viendrait|viendrions|viendriez|viendraient", "venu"
        ),
        "tenir" to irr(
            "tiens|tiens|tient|tenons|tenez|tiennent",
            "tenais|tenais|tenait|tenions|teniez|tenaient",
            "tiendrai|tiendras|tiendra|tiendrons|tiendrez|tiendront",
            "tiendrais|tiendrais|tiendrait|tiendrions|tiendriez|tiendraient", "tenu"
        ),
        "prendre" to irr(
            "prends|prends|prend|prenons|prenez|prennent",
            "prenais|prenais|prenait|prenions|preniez|prenaient",
            "prendrai|prendras|prendra|prendrons|prendrez|prendront",
            "prendrais|prendrais|prendrait|prendrions|prendriez|prendraient", "pris"
        ),
        "apprendre" to irr(
            "apprends|apprends|apprend|apprenons|apprenez|apprennent",
            "apprenais|apprenais|apprenait|apprenions|appreniez|apprenaient",
            "apprendrai|apprendras|apprendra|apprendrons|apprendrez|apprendront",
            "apprendrais|apprendrais|apprendrait|apprendrions|apprendriez|apprendraient", "appris"
        ),
        "comprendre" to irr(
            "comprends|comprends|comprend|comprenons|comprenez|comprennent",
            "comprenais|comprenais|comprenait|comprenions|compreniez|comprenaient",
            "comprendrai|comprendras|comprendra|comprendrons|comprendrez|comprendront",
            "comprendrais|comprendrais|comprendrait|comprendrions|comprendriez|comprendraient", "compris"
        ),
        "mettre" to irr(
            "mets|mets|met|mettons|mettez|mettent",
            "mettais|mettais|mettait|mettions|mettiez|mettaient",
            "mettrai|mettras|mettra|mettrons|mettrez|mettront",
            "mettrais|mettrais|mettrait|mettrions|mettriez|mettraient", "mis"
        ),
        "pouvoir" to irr(
            "peux|peux|peut|pouvons|pouvez|peuvent",
            "pouvais|pouvais|pouvait|pouvions|pouviez|pouvaient",
            "pourrai|pourras|pourra|pourrons|pourrez|pourront",
            "pourrais|pourrais|pourrait|pourrions|pourriez|pourraient", "pu"
        ),
        "vouloir" to irr(
            "veux|veux|veut|voulons|voulez|veulent",
            "voulais|voulais|voulait|voulions|vouliez|voulaient",
            "voudrai|voudras|voudra|voudrons|voudrez|voudront",
            "voudrais|voudrais|voudrait|voudrions|voudriez|voudraient", "voulu"
        ),
        "devoir" to irr(
            "dois|dois|doit|devons|devez|doivent",
            "devais|devais|devait|devions|deviez|devaient",
            "devrai|devras|devra|devrons|devrez|devront",
            "devrais|devrais|devrait|devrions|devriez|devraient", "dû"
        ),
        "savoir" to irr(
            "sais|sais|sait|savons|savez|savent",
            "savais|savais|savait|savions|saviez|savaient",
            "saurai|sauras|saura|saurons|saurez|sauront",
            "saurais|saurais|saurait|saurions|sauriez|sauraient", "su"
        ),
        "voir" to irr(
            "vois|vois|voit|voyons|voyez|voient",
            "voyais|voyais|voyait|voyions|voyiez|voyaient",
            "verrai|verras|verra|verrons|verrez|verront",
            "verrais|verrais|verrait|verrions|verriez|verraient", "vu"
        ),
        "recevoir" to irr(
            "reçois|reçois|reçoit|recevons|recevez|reçoivent",
            "recevais|recevais|recevait|recevions|receviez|recevaient",
            "recevrai|recevras|recevra|recevrons|recevrez|recevront",
            "recevrais|recevrais|recevrait|recevrions|recevriez|recevraient", "reçu"
        ),
        "dire" to irr(
            "dis|dis|dit|disons|dites|disent",
            "disais|disais|disait|disions|disiez|disaient",
            "dirai|diras|dira|dirons|direz|diront",
            "dirais|dirais|dirait|dirions|diriez|diraient", "dit"
        ),
        "lire" to irr(
            "lis|lis|lit|lisons|lisez|lisent",
            "lisais|lisais|lisait|lisions|lisiez|lisaient",
            "lirai|liras|lira|lirons|lirez|liront",
            "lirais|lirais|lirait|lirions|liriez|liraient", "lu"
        ),
        "écrire" to irr(
            "écris|écris|écrit|écrivons|écrivez|écrivent",
            "écrivais|écrivais|écrivait|écrivions|écriviez|écrivaient",
            "écrirai|écriras|écrira|écrirons|écrirez|écriront",
            "écrirais|écrirais|écrirait|écririons|écririez|écriraient", "écrit"
        ),
        "boire" to irr(
            "bois|bois|boit|buvons|buvez|boivent",
            "buvais|buvais|buvait|buvions|buviez|buvaient",
            "boirai|boiras|boira|boirons|boirez|boiront",
            "boirais|boirais|boirait|boirions|boiriez|boiraient", "bu"
        ),
        "croire" to irr(
            "crois|crois|croit|croyons|croyez|croient",
            "croyais|croyais|croyait|croyions|croyiez|croyaient",
            "croirai|croiras|croira|croirons|croirez|croiront",
            "croirais|croirais|croirait|croirions|croiriez|croiraient", "cru"
        ),
        "connaître" to irr(
            "connais|connais|connaît|connaissons|connaissez|connaissent",
            "connaissais|connaissais|connaissait|connaissions|connaissiez|connaissaient",
            "connaîtrai|connaîtras|connaîtra|connaîtrons|connaîtrez|connaîtront",
            "connaîtrais|connaîtrais|connaîtrait|connaîtrions|connaîtriez|connaîtraient", "connu"
        ),
        "vivre" to irr(
            "vis|vis|vit|vivons|vivez|vivent",
            "vivais|vivais|vivait|vivions|viviez|vivaient",
            "vivrai|vivras|vivra|vivrons|vivrez|vivront",
            "vivrais|vivrais|vivrait|vivrions|vivriez|vivraient", "vécu"
        ),
        "suivre" to irr(
            "suis|suis|suit|suivons|suivez|suivent",
            "suivais|suivais|suivait|suivions|suiviez|suivaient",
            "suivrai|suivras|suivra|suivrons|suivrez|suivront",
            "suivrais|suivrais|suivrait|suivrions|suivriez|suivraient", "suivi"
        ),
        "rire" to irr(
            "ris|ris|rit|rions|riez|rient",
            "riais|riais|riait|riions|riiez|riaient",
            "rirai|riras|rira|rirons|rirez|riront",
            "rirais|rirais|rirait|ririons|ririez|riraient", "ri"
        ),
        "courir" to irr(
            "cours|cours|court|courons|courez|courent",
            "courais|courais|courait|courions|couriez|couraient",
            "courrai|courras|courra|courrons|courrez|courront",
            "courrais|courrais|courrait|courrions|courriez|courraient", "couru"
        ),
        "mourir" to irr(
            "meurs|meurs|meurt|mourons|mourez|meurent",
            "mourais|mourais|mourait|mourions|mouriez|mouraient",
            "mourrai|mourras|mourra|mourrons|mourrez|mourront",
            "mourrais|mourrais|mourrait|mourrions|mourriez|mourraient", "mort"
        ),
        "naître" to irr(
            "nais|nais|naît|naissons|naissez|naissent",
            "naissais|naissais|naissait|naissions|naissiez|naissaient",
            "naîtrai|naîtras|naîtra|naîtrons|naîtrez|naîtront",
            "naîtrais|naîtrais|naîtrait|naîtrions|naîtriez|naîtraient", "né"
        ),
        "valoir" to irr(
            "vaux|vaux|vaut|valons|valez|valent",
            "valais|valais|valait|valions|valiez|valaient",
            "vaudrai|vaudras|vaudra|vaudrons|vaudrez|vaudront",
            "vaudrais|vaudrais|vaudrait|vaudrions|vaudriez|vaudraient", "valu"
        ),
        "ouvrir" to irr(
            "ouvre|ouvres|ouvre|ouvrons|ouvrez|ouvrent",
            "ouvrais|ouvrais|ouvrait|ouvrions|ouvriez|ouvraient",
            "ouvrirai|ouvriras|ouvrira|ouvrirons|ouvrirez|ouvriront",
            "ouvrirais|ouvrirais|ouvrirait|ouvririons|ouvririez|ouvriraient", "ouvert"
        ),
        "dormir" to irr(
            "dors|dors|dort|dormons|dormez|dorment",
            "dormais|dormais|dormait|dormions|dormiez|dormaient",
            "dormirai|dormiras|dormira|dormirons|dormirez|dormiront",
            "dormirais|dormirais|dormirait|dormirions|dormiriez|dormiraient", "dormi"
        ),
        "partir" to irr(
            "pars|pars|part|partons|partez|partent",
            "partais|partais|partait|partions|partiez|partaient",
            "partirai|partiras|partira|partirons|partirez|partiront",
            "partirais|partirais|partirait|partirions|partiriez|partiraient", "parti"
        ),
        "sortir" to irr(
            "sors|sors|sort|sortons|sortez|sortent",
            "sortais|sortais|sortait|sortions|sortiez|sortaient",
            "sortirai|sortiras|sortira|sortirons|sortirez|sortiront",
            "sortirais|sortirais|sortirait|sortirions|sortiriez|sortiraient", "sorti"
        ),
        "conduire" to irr(
            "conduis|conduis|conduit|conduisons|conduisez|conduisent",
            "conduisais|conduisais|conduisait|conduisions|conduisiez|conduisaient",
            "conduirai|conduiras|conduira|conduirons|conduirez|conduiront",
            "conduirais|conduirais|conduirait|conduirions|conduiriez|conduiraient", "conduit"
        ),
        "envoyer" to irr(
            "envoie|envoies|envoie|envoyons|envoyez|envoient",
            "envoyais|envoyais|envoyait|envoyions|envoyiez|envoyaient",
            "enverrai|enverras|enverra|enverrons|enverrez|enverront",
            "enverrais|enverrais|enverrait|enverrions|enverriez|enverraient", "envoyé"
        ),

        "offrir" to irr(
            "offre|offres|offre|offrons|offrez|offrent",
            "offrais|offrais|offrait|offrions|offriez|offraient",
            "offrirai|offriras|offrira|offrirons|offrirez|offriront",
            "offrirais|offrirais|offrirait|offririons|offririez|offriraient", "offert"
        ),
        "servir" to irr(
            "sers|sers|sert|servons|servez|servent",
            "servais|servais|servait|servions|serviez|servaient",
            "servirai|serviras|servira|servirons|servirez|serviront",
            "servirais|servirais|servirait|servirions|serviriez|serviraient", "servi"
        ),
        "sentir" to irr(
            "sens|sens|sent|sentons|sentez|sentent",
            "sentais|sentais|sentait|sentions|sentiez|sentaient",
            "sentirai|sentiras|sentira|sentirons|sentirez|sentiront",
            "sentirais|sentirais|sentirait|sentirions|sentiriez|sentiraient", "senti"
        ),
        "revenir" to irr(
            "reviens|reviens|revient|revenons|revenez|reviennent",
            "revenais|revenais|revenait|revenions|reveniez|revenaient",
            "reviendrai|reviendras|reviendra|reviendrons|reviendrez|reviendront",
            "reviendrais|reviendrais|reviendrait|reviendrions|reviendriez|reviendraient", "revenu"
        ),
        "devenir" to irr(
            "deviens|deviens|devient|devenons|devenez|deviennent",
            "devenais|devenais|devenait|devenions|deveniez|devenaient",
            "deviendrai|deviendras|deviendra|deviendrons|deviendrez|deviendront",
            "deviendrais|deviendrais|deviendrait|deviendrions|deviendriez|deviendraient", "devenu"
        ),
        "obtenir" to irr(
            "obtiens|obtiens|obtient|obtenons|obtenez|obtiennent",
            "obtenais|obtenais|obtenait|obtenions|obteniez|obtenaient",
            "obtiendrai|obtiendras|obtiendra|obtiendrons|obtiendrez|obtiendront",
            "obtiendrais|obtiendrais|obtiendrait|obtiendrions|obtiendriez|obtiendraient", "obtenu"
        ),
        "retenir" to irr(
            "retiens|retiens|retient|retenons|retenez|retiennent",
            "retenais|retenais|retenait|retenions|reteniez|retenaient",
            "retiendrai|retiendras|retiendra|retiendrons|retiendrez|retiendront",
            "retiendrais|retiendrais|retiendrait|retiendrions|retiendriez|retiendraient", "retenu"
        ),
        "perdre" to irr(
            "perds|perds|perd|perdons|perdez|perdent",
            "perdais|perdais|perdait|perdions|perdiez|perdaient",
            "perdrai|perdras|perdra|perdrons|perdrez|perdront",
            "perdrais|perdrais|perdrait|perdrions|perdriez|perdraient", "perdu"
        ),
        "attendre" to irr(
            "attends|attends|attend|attendons|attendez|attendent",
            "attendais|attendais|attendait|attendions|attendiez|attendaient",
            "attendrai|attendras|attendra|attendrons|attendrez|attendront",
            "attendrais|attendrais|attendrait|attendrions|attendriez|attendraient", "attendu"
        ),
        "cueillir" to irr(
            "cueille|cueilles|cueille|cueillons|cueillez|cueillent",
            "cueillais|cueillais|cueillait|cueillions|cueilliez|cueillaient",
            "cueillerai|cueilleras|cueillera|cueillerons|cueillerez|cueilleront",
            "cueillerais|cueillerais|cueillerait|cueillerions|cueilleriez|cueilleraient", "cueilli"
        ),
        "couvrir" to irr(
            "couvre|couvres|couvre|couvrons|couvrez|couvrent",
            "couvrais|couvrais|couvrait|couvrions|couvriez|couvraient",
            "couvrirai|couvriras|couvrira|couvrirons|couvrirez|couvriront",
            "couvrirais|couvrirais|couvrirait|couvririons|couvririez|couvriraient", "couvert"
        ),
        "découvrir" to irr(
            "découvre|découvres|découvre|découvrons|découvrez|découvrent",
            "découvrais|découvrais|découvrait|découvrions|découvriez|découvraient",
            "découvrirai|découvriras|découvrira|découvrirons|découvrirez|découvriront",
            "découvrirais|découvrirais|découvrirait|découvririons|découvririez|découvriraient", "découvert"
        ),
        "souffrir" to irr(
            "souffre|souffres|souffre|souffrons|souffrez|souffrent",
            "souffrais|souffrais|souffrait|souffrions|souffriez|souffraient",
            "souffrirai|souffriras|souffrira|souffrirons|souffrirez|souffriront",
            "souffrirais|souffrirais|souffrirait|souffririons|souffririez|souffriraient", "souffert"
        ),
        "fuir" to irr(
            "fuis|fuis|fuit|fuyons|fuyez|fuient",
            "fuyais|fuyais|fuyait|fuyions|fuyiez|fuyaient",
            "fuirai|fuiras|fuira|fuirons|fuirez|fuiront",
            "fuirais|fuirais|fuirait|fuirions|fuiriez|fuiraient", "fui"
        ),
        "battre" to irr(
            "bats|bats|bat|battons|battez|battent",
            "battais|battais|battait|battions|battiez|battaient",
            "battrai|battras|battra|battrons|battrez|battront",
            "battrais|battrais|battrait|battrions|battriez|battraient", "battu"
        ),
        "joindre" to irr(
            "joins|joins|joint|joignons|joignez|joignent",
            "joignais|joignais|joignait|joignions|joigniez|joignaient",
            "joindrai|joindras|joindra|joindrons|joindrez|joindront",
            "joindrais|joindrais|joindrait|joindrions|joindriez|joindraient", "joint"
        ),
    )
}
