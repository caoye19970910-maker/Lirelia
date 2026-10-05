package com.cy.languagereader.mobile.data

import java.util.Locale

/**
 * Tiny always-available fallback dictionary.
 *
 * The full 156k-entry dictionary remains the primary source. This map exists so
 * a failed first-time SQLite install/open never turns a tap into an app crash.
 */
object CoreFrenchChineseDictionary {
    private data class CoreEntry(
        val lemma: String,
        val pos: String,
        val definitions: List<String>,
        val related: List<String> = emptyList(),
    )

    private val entries = mapOf(
        "recueil" to CoreEntry("recueil", "nom", listOf("文集；选集；合集", "汇编；收集物"), listOf("recueillir", "collection")),
        "fiction" to CoreEntry("fiction", "nom", listOf("虚构作品；小说", "虚构；想象出来的事")),
        "histoire" to CoreEntry("histoire", "nom", listOf("故事", "历史", "事情；经历"), listOf("historique", "récit")),
        "soir" to CoreEntry("soir", "nom", listOf("晚上；傍晚")),
        "livre" to CoreEntry("livre", "nom", listOf("书；书籍")),
        "collection" to CoreEntry("collection", "nom", listOf("合集；系列", "收藏；收集"), listOf("collectionner", "recueil")),
        "court" to CoreEntry("court", "adjectif", listOf("短的；简短的")),
        "courte" to CoreEntry("court", "adjectif", listOf("短的；简短的")),
        "drôle" to CoreEntry("drôle", "adjectif", listOf("好笑的；有趣的", "奇怪的")),
        "enfant" to CoreEntry("enfant", "nom", listOf("孩子；儿童")),
        "vouloir" to CoreEntry("vouloir", "verbe", listOf("想要；希望"), listOf("volonté")),
        "veulent" to CoreEntry("vouloir", "verbe", listOf("想要；希望（vouloir 的第三人称复数）")),
        "détendre" to CoreEntry("détendre", "verbe", listOf("使放松；使舒缓"), listOf("détente")),
        "affirmation" to CoreEntry("affirmation", "nom", listOf("肯定；断言", "肯定句；积极陈述")),
        "positif" to CoreEntry("positif", "adjectif", listOf("积极的；正面的", "肯定的")),
        "positive" to CoreEntry("positif", "adjectif", listOf("积极的；正面的", "肯定的")),
        "inconnu" to CoreEntry("inconnu", "adjectif / nom", listOf("未知的；不认识的", "陌生人")),
        "inconnue" to CoreEntry("inconnu", "adjectif / nom", listOf("未知的；不认识的", "陌生人")),
        "personnage" to CoreEntry("personnage", "nom", listOf("人物；角色")),
        "lieu" to CoreEntry("lieu", "nom", listOf("地方；场所")),
        "événement" to CoreEntry("événement", "nom", listOf("事件；事情；活动")),
        "nom" to CoreEntry("nom", "nom", listOf("名字；姓名", "名词")),
        "lire" to CoreEntry("lire", "verbe", listOf("读；阅读"), listOf("lecture", "lecteur")),
        "lecture" to CoreEntry("lecture", "nom", listOf("阅读；读物"), listOf("lire", "lecteur")),
        "écrire" to CoreEntry("écrire", "verbe", listOf("写；书写"), listOf("écriture", "écrivain")),
        "être" to CoreEntry("être", "verbe", listOf("是；存在")),
        "avoir" to CoreEntry("avoir", "verbe", listOf("有；拥有")),
        "faire" to CoreEntry("faire", "verbe", listOf("做；进行；制造")),
        "aller" to CoreEntry("aller", "verbe", listOf("去；前往")),
        "venir" to CoreEntry("venir", "verbe", listOf("来；来到")),
        "dire" to CoreEntry("dire", "verbe", listOf("说；告诉")),
        "voir" to CoreEntry("voir", "verbe", listOf("看见；看到")),
        "savoir" to CoreEntry("savoir", "verbe", listOf("知道；会")),
        "pouvoir" to CoreEntry("pouvoir", "verbe", listOf("能够；可以")),
        "devoir" to CoreEntry("devoir", "verbe", listOf("必须；应该", "作业；职责")),
        "prendre" to CoreEntry("prendre", "verbe", listOf("拿；取；乘坐；花费")),
        "manger" to CoreEntry("manger", "verbe", listOf("吃；进食"), listOf("mangeur", "alimentation")),
        "regarder" to CoreEntry("regarder", "verbe", listOf("看；注视；观看"), listOf("regard")),
        "parler" to CoreEntry("parler", "verbe", listOf("说话；交谈")),
        "travailler" to CoreEntry("travailler", "verbe", listOf("工作；学习")),
        "apprendre" to CoreEntry("apprendre", "verbe", listOf("学习；学会", "得知；获悉")),
        "comprendre" to CoreEntry("comprendre", "verbe", listOf("理解；明白", "包括；包含")),
        "français" to CoreEntry("français", "adjectif / nom", listOf("法国的；法语的", "法语；法国人")),
        "française" to CoreEntry("français", "adjectif", listOf("法国的；法语的")),
        "mot" to CoreEntry("mot", "nom", listOf("单词；词语；话")),
        "phrase" to CoreEntry("phrase", "nom", listOf("句子")),
        "temps" to CoreEntry("temps", "nom", listOf("时间", "天气", "时态")),
        "jour" to CoreEntry("jour", "nom", listOf("天；白天")),
        "maison" to CoreEntry("maison", "nom", listOf("房子；家")),
        "école" to CoreEntry("école", "nom", listOf("学校")),
        "travail" to CoreEntry("travail", "nom", listOf("工作；劳动")),
        "famille" to CoreEntry("famille", "nom", listOf("家庭；家人")),
        "ami" to CoreEntry("ami", "nom", listOf("朋友")),
        "amie" to CoreEntry("ami", "nom", listOf("女性朋友；朋友")),
        "beaucoup" to CoreEntry("beaucoup", "adverbe", listOf("很多；非常")),
        "toujours" to CoreEntry("toujours", "adverbe", listOf("总是；一直；仍然")),
        "jamais" to CoreEntry("jamais", "adverbe", listOf("从不；永不", "曾经（特定结构中）")),
        "encore" to CoreEntry("encore", "adverbe", listOf("还；仍然；再一次")),
        "déjà" to CoreEntry("déjà", "adverbe", listOf("已经；早已")),
        "parce" to CoreEntry("parce", "expression", listOf("用于 parce que：因为")),
        "pourquoi" to CoreEntry("pourquoi", "adverbe", listOf("为什么")),
        "comment" to CoreEntry("comment", "adverbe", listOf("怎么；如何")),
        "personne" to CoreEntry("personne", "nom", listOf("人；个人", "没有人（ne...personne）")),
        "vivant" to CoreEntry("vivant", "adjectif / nom", listOf("活着的；有生命的", "生动的；活跃的")),
        "mort" to CoreEntry("mort", "adjectif / nom", listOf("死的；死亡的", "死者；死亡"), listOf("mourir")),
        "entreprise" to CoreEntry("entreprise", "nom", listOf("企业；公司", "事业；行动"), listOf("entreprendre")),
        "endroit" to CoreEntry("endroit", "nom", listOf("地方；地点；场所")),
        "cas" to CoreEntry("cas", "nom", listOf("情况；案例；情形", "语法格")),
        "fortuit" to CoreEntry("fortuit", "adjectif", listOf("偶然的；意外的")),
        "ressemblance" to CoreEntry("ressemblance", "nom", listOf("相似；相像；相似之处"), listOf("ressembler")),
        "ressembler" to CoreEntry("ressembler", "verbe", listOf("像；与……相似"), listOf("ressemblance")),
        "tout" to CoreEntry("tout", "déterminant / pronom / adverbe", listOf("全部的；整个的", "一切；所有", "完全地")),
        "toute" to CoreEntry("tout", "déterminant", listOf("全部的；整个的（阴性）")),
        "avec" to CoreEntry("avec", "préposition", listOf("和；与；带着；用")),
        "sans" to CoreEntry("sans", "préposition", listOf("没有；不带；无")),
        "pour" to CoreEntry("pour", "préposition", listOf("为了；给；对于；持续（时间）")),
        "dans" to CoreEntry("dans", "préposition", listOf("在……里面；在……之中；在……后")),
        "sur" to CoreEntry("sur", "préposition", listOf("在……上；关于；在……之中")),
        "sous" to CoreEntry("sous", "préposition", listOf("在……下面；在……之下")),
        "chez" to CoreEntry("chez", "préposition", listOf("在……家；在……那里；在……中")),
        "entre" to CoreEntry("entre", "préposition", listOf("在……之间；在……中间")),
        "avant" to CoreEntry("avant", "préposition / adverbe", listOf("在……之前；以前")),
        "après" to CoreEntry("après", "préposition / adverbe", listOf("在……之后；以后")),
        "depuis" to CoreEntry("depuis", "préposition / adverbe", listOf("自从；从……以来；已经（持续时间）")),
        "pendant" to CoreEntry("pendant", "préposition", listOf("在……期间；持续……时间")),
        "par" to CoreEntry("par", "préposition", listOf("通过；由；每；经过")),
        "de" to CoreEntry("de", "préposition", listOf("的；从；关于；由")),
        "à" to CoreEntry("à", "préposition", listOf("在；向；到；给；以")),
        "ce" to CoreEntry("ce", "déterminant / pronom", listOf("这个；这；那")),
        "cette" to CoreEntry("ce", "déterminant", listOf("这个；这个（阴性）")),
        "ces" to CoreEntry("ce", "déterminant", listOf("这些；那些")),
        "un" to CoreEntry("un", "article / nombre", listOf("一个；一；某个")),
        "une" to CoreEntry("un", "article", listOf("一个；某个（阴性）")),
        "des" to CoreEntry("des", "article / préposition", listOf("一些；……的；从这些")),
        "le" to CoreEntry("le", "article / pronom", listOf("这个；该；它（宾语）")),
        "la" to CoreEntry("la", "article / pronom", listOf("这个；该；她/它（宾语）")),
        "les" to CoreEntry("les", "article / pronom", listOf("这些；他们/它们（宾语）")),
        "qui" to CoreEntry("qui", "pronom", listOf("谁；……的人/事物（关系代词主语）")),
        "que" to CoreEntry("que", "conjonction / pronom", listOf("……；什么；……的对象")),
        "où" to CoreEntry("où", "adverbe / pronom", listOf("哪里；在那里；……的地方/时间")),
        "on" to CoreEntry("on", "pronom", listOf("人们；大家；有人", "我们（口语中常代替 nous）")),
        "je" to CoreEntry("je", "pronom", listOf("我（主语）")),
        "tu" to CoreEntry("tu", "pronom", listOf("你（单数、熟人间）")),
        "il" to CoreEntry("il", "pronom", listOf("他；它（阳性主语）")),
        "elle" to CoreEntry("elle", "pronom", listOf("她；它（阴性主语）")),
        "nous" to CoreEntry("nous", "pronom", listOf("我们")),
        "vous" to CoreEntry("vous", "pronom", listOf("您；你们")),
        "ils" to CoreEntry("ils", "pronom", listOf("他们；它们（阳性或混合复数）")),
        "elles" to CoreEntry("elles", "pronom", listOf("她们；它们（阴性复数）")),
        "en" to CoreEntry("en", "pronom / préposition", listOf("其中；从那里；关于它/它们（代词）", "在……；乘……；用……（介词，依语境）")),
        "y" to CoreEntry("y", "pronom", listOf("在那里；到那里", "对它/对此（代替 à + 事物）")),
        "dont" to CoreEntry("dont", "pronom relatif", listOf("其；其中；……的（与 de 连用）")),
        "mais" to CoreEntry("mais", "conjonction", listOf("但是；可是")),
        "ou" to CoreEntry("ou", "conjonction", listOf("或者；还是")),
        "et" to CoreEntry("et", "conjonction", listOf("和；而且")),
        "donc" to CoreEntry("donc", "conjonction / adverbe", listOf("所以；因此；那么")),
        "car" to CoreEntry("car", "conjonction", listOf("因为；由于")),
        "si" to CoreEntry("si", "conjonction / adverbe", listOf("如果；是否；如此；这么")),
        "très" to CoreEntry("très", "adverbe", listOf("很；非常")),
        "bien" to CoreEntry("bien", "adverbe / nom", listOf("好；很好；充分地", "财产；好处")),
        "mal" to CoreEntry("mal", "adverbe / nom", listOf("不好；糟糕地", "疼痛；坏处")),
        "plus" to CoreEntry("plus", "adverbe", listOf("更多；更加；不再（ne...plus）")),
        "moins" to CoreEntry("moins", "adverbe", listOf("更少；较少")),
        "aussi" to CoreEntry("aussi", "adverbe", listOf("也；同样；如此")),
        "même" to CoreEntry("même", "adjectif / pronom / adverbe", listOf("相同的；本人；甚至")),
        "autre" to CoreEntry("autre", "adjectif / pronom", listOf("其他的；另一个")),
        "petit" to CoreEntry("petit", "adjectif / nom", listOf("小的；年幼的；小孩")),
        "grand" to CoreEntry("grand", "adjectif", listOf("大的；高的；伟大的")),
        "nouveau" to CoreEntry("nouveau", "adjectif", listOf("新的；新来的")),
        "bon" to CoreEntry("bon", "adjectif", listOf("好的；合适的；美味的")),
        "mauvais" to CoreEntry("mauvais", "adjectif", listOf("坏的；不好的；错误的")),
        "premier" to CoreEntry("premier", "adjectif / nom", listOf("第一的；最初的；第一位")),
        "dernier" to CoreEntry("dernier", "adjectif / nom", listOf("最后的；上一个的；最后一个")),
        "chose" to CoreEntry("chose", "nom", listOf("东西；事情；事物")),
        "fois" to CoreEntry("fois", "nom", listOf("次；回；倍")),
        "monde" to CoreEntry("monde", "nom", listOf("世界；人们；社会")),
        "vie" to CoreEntry("vie", "nom", listOf("生活；生命；一生")),
        "homme" to CoreEntry("homme", "nom", listOf("男人；人；人类")),
        "femme" to CoreEntry("femme", "nom", listOf("女人；女性；妻子")),
        "pays" to CoreEntry("pays", "nom", listOf("国家；乡村；地区")),
        "ville" to CoreEntry("ville", "nom", listOf("城市")),
        "place" to CoreEntry("place", "nom", listOf("地方；位置；广场；座位")),
        "question" to CoreEntry("question", "nom", listOf("问题；疑问")),
        "réponse" to CoreEntry("réponse", "nom", listOf("回答；答案")),

        // V3.15 reading supplement: frequent A2–B2 words that are surprisingly
        // absent from the compact 156k mobile source dictionary. These entries keep
        // first lookup useful even with no network connection.
        "triste" to CoreEntry("triste", "adjectif", listOf("悲伤的；难过的；令人伤感的"), listOf("tristesse")),
        "important" to CoreEntry("important", "adjectif", listOf("重要的；重大的；数量可观的")),
        "possible" to CoreEntry("possible", "adjectif / nom", listOf("可能的；可行的", "可能性；可能做到的事")),
        "impossible" to CoreEntry("impossible", "adjectif / nom", listOf("不可能的；办不到的", "不可能的事情")),
        "simple" to CoreEntry("simple", "adjectif", listOf("简单的；单纯的；普通的")),
        "joli" to CoreEntry("joli", "adjectif", listOf("漂亮的；好看的；可爱的")),
        "libre" to CoreEntry("libre", "adjectif", listOf("自由的；空闲的；未被占用的")),
        "malade" to CoreEntry("malade", "adjectif / nom", listOf("生病的；不舒服的", "病人")),
        "rapide" to CoreEntry("rapide", "adjectif", listOf("快速的；迅速的")),
        "lent" to CoreEntry("lent", "adjectif", listOf("缓慢的；慢的")),
        "inutile" to CoreEntry("inutile", "adjectif", listOf("无用的；没必要的；徒劳的")),
        "principal" to CoreEntry("principal", "adjectif / nom", listOf("主要的；首要的", "负责人；校长（依语境）")),
        "public" to CoreEntry("public", "adjectif / nom", listOf("公共的；公开的", "公众；观众")),
        "social" to CoreEntry("social", "adjectif", listOf("社会的；社交的；社会保障相关的")),
        "culturel" to CoreEntry("culturel", "adjectif", listOf("文化的；与文化有关的")),
        "sombre" to CoreEntry("sombre", "adjectif", listOf("昏暗的；深色的；阴沉的")),
        "inflexible" to CoreEntry("inflexible", "adjectif", listOf("不屈的；不妥协的；坚定到近乎强硬的", "不可弯曲的"), listOf("flexible")),
        "intraitable" to CoreEntry("intraitable", "adjectif", listOf("不妥协的；难以对付的；强硬的"), listOf("traiter")),
        "baissé" to CoreEntry("baisser", "participe passé / adjectif", listOf("放低的；垂下的；降低的"), listOf("baisser")),
        "profondément" to CoreEntry("profondément", "adverbe", listOf("深深地；强烈地；深刻地"), listOf("profond")),
        "désormais" to CoreEntry("désormais", "adverbe", listOf("从今以后；此后；现在起")),
        "forestier" to CoreEntry("forestier", "adjectif / nom", listOf("森林的；林业的", "林业人员")),
        "aile" to CoreEntry("aile", "nom", listOf("翅膀；翼；侧翼")),
        "pleurer" to CoreEntry("pleurer", "verbe", listOf("哭；哭泣；为……悲伤")),
        "jurer" to CoreEntry("jurer", "verbe", listOf("发誓；宣誓", "咒骂（依语境）")),
        "virulent" to CoreEntry("virulent", "adjectif", listOf("激烈的；尖锐的；恶毒的", "毒性强的；强烈的")),
        "faire-valoir" to CoreEntry("faire-valoir", "nom", listOf("陪衬；衬托他人的人或事物")),
        "rigide" to CoreEntry("rigide", "adjectif", listOf("僵硬的；严格的；死板的")),
        "souple" to CoreEntry("souple", "adjectif", listOf("柔软的；灵活的；有弹性的")),
        "précieux" to CoreEntry("précieux", "adjectif", listOf("珍贵的；宝贵的；贵重的")),
        "curieux" to CoreEntry("curieux", "adjectif / nom", listOf("好奇的；奇怪的", "好奇的人")),
        "sensible" to CoreEntry("sensible", "adjectif", listOf("敏感的；可感知的；明显的")),
        "agréable" to CoreEntry("agréable", "adjectif", listOf("令人愉快的；舒适的；讨人喜欢的")),
        "désagréable" to CoreEntry("désagréable", "adjectif", listOf("令人不快的；讨厌的；不舒服的")),
        "certainement" to CoreEntry("certainement", "adverbe", listOf("当然；肯定地；很可能"), listOf("certain")),
        "probablement" to CoreEntry("probablement", "adverbe", listOf("大概；很可能")),
        "soudain" to CoreEntry("soudain", "adjectif / adverbe", listOf("突然的；忽然地")),
        "malgré" to CoreEntry("malgré", "préposition", listOf("尽管；不顾")),
        "pourtant" to CoreEntry("pourtant", "adverbe", listOf("然而；可是；尽管如此")),
        "cependant" to CoreEntry("cependant", "adverbe", listOf("然而；不过；与此同时")),
        "ainsi" to CoreEntry("ainsi", "adverbe", listOf("这样；如此；因此")),
        "environ" to CoreEntry("environ", "adverbe / préposition", listOf("大约；左右")),
        "davantage" to CoreEntry("davantage", "adverbe", listOf("更多；更加")),
        "ailleurs" to CoreEntry("ailleurs", "adverbe", listOf("在别处；到别处；此外")),
        "autrement" to CoreEntry("autrement", "adverbe", listOf("以别的方式；否则；不同地")),
        "ensemble" to CoreEntry("ensemble", "adverbe / nom", listOf("一起；共同", "整体；集合")),
    )

    fun lookup(input: String): DictionaryResult? {
        val cleaned = normalize(input)
        val direct = morphologyCandidates(cleaned).firstNotNullOfOrNull { entries[it] } ?: return null
        return DictionaryResult(
            query = input.trim(),
            lemma = direct.lemma,
            pos = direct.pos,
            ipa = "",
            definitions = direct.definitions,
            examples = emptyList(),
            related = direct.related,
            phrases = emptyList(),
        )
    }

    private fun morphologyCandidates(word: String): List<String> = buildList {
        add(word)
        if (word.length > 4 && word.endsWith("s")) add(word.dropLast(1))
        if (word.length > 4 && word.endsWith("x")) add(word.dropLast(1))
        if (word.length > 5 && word.endsWith("es")) add(word.dropLast(2))
        if (word.length > 4 && word.endsWith("e")) add(word.dropLast(1))
        if (word.length > 5 && word.endsWith("aux")) add(word.dropLast(3) + "al")
        if (word.length > 5 && word.endsWith("eaux")) add(word.dropLast(1))

        if (word.length > 5 && word.endsWith("ive")) add(word.dropLast(3) + "if")
        if (word.length > 6 && word.endsWith("ives")) add(word.dropLast(4) + "if")
        if (word.length > 6 && word.endsWith("euse")) add(word.dropLast(4) + "eux")
        if (word.length > 7 && word.endsWith("euses")) add(word.dropLast(5) + "eux")
        if (word.length > 5 && word.endsWith("ère")) add(word.dropLast(3) + "er")
        if (word.length > 6 && word.endsWith("ères")) add(word.dropLast(4) + "er")
        if (word.length > 6 && word.endsWith("enne")) add(word.dropLast(4) + "en")
        if (word.length > 7 && word.endsWith("ennes")) add(word.dropLast(5) + "en")

        when {
            word.length > 5 && word.endsWith("ées") -> {
                add(word.dropLast(2))
                add(word.dropLast(3) + "er")
            }
            word.length > 4 && word.endsWith("ée") -> {
                add(word.dropLast(1))
                add(word.dropLast(2) + "er")
            }
            word.length > 4 && word.endsWith("és") -> {
                add(word.dropLast(1))
                add(word.dropLast(2) + "er")
            }
            word.length > 3 && word.endsWith("é") -> add(word.dropLast(1) + "er")
        }
    }.distinct()

    private fun normalize(value: String): String =
        value.trim()
            .trim(' ', '.', ',', '!', '?', ';', ':', '«', '»', '"', '“', '”', '(', ')', '[', ']', '{', '}')
            .replace('’', '\'')
            .lowercase(Locale.FRENCH)
}
