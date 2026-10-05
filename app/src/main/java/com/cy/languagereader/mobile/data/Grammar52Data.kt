package com.cy.languagereader.mobile.data

data class GrammarExample(val french: String, val chinese: String)

data class GrammarMemoryCard(
    val title: String,
    val rule: String,
    val formula: String,
    val examples: List<GrammarExample>,
    val tip: String,
)

data class GrammarExercise(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val answers: List<String>,
    val explanation: String,
    val chineseHint: String,
)

data class GrammarLesson(
    val number: Int,
    val titleFrench: String,
    val titleChinese: String,
    val category: String,
    val bookPage: Int,
    val bodyAvailableInUploadedPdf: Boolean,
    val cards: List<GrammarMemoryCard>,
    val exercises: List<GrammarExercise>,
) {
    val id: String get() = "grammar_${number}"
}

object Grammar52Data {
    val lessons: List<GrammarLesson> = listOf(
        GrammarLesson(
            number = 1, titleFrench = "Le verbe « être »", titleChinese = "动词 être", category = "基础结构", bookPage = 8, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "先记住现在时", rule = "être 的 présent：je suis, tu es, il/elle/on est, nous sommes, vous êtes, ils/elles sont。", formula = "suis · es · est · sommes · êtes · sont",
                    examples = listOf(GrammarExample("Je suis professeur.", "我是老师。"), GrammarExample("Nous sommes à Paris.", "我们在巴黎。")),
                    tip = "把 suis / sommes / êtes / sont 当成四个视觉锚点。",
                ),
                GrammarMemoryCard(
                    title = "地点与身份", rule = "être 常和身份、国籍、地点连用。城市前常用 à，来源城市用 de，人在某人家/某机构常用 chez。", formula = "être + nom/adjectif · être à/de/chez",
                    examples = listOf(GrammarExample("Elle est chinoise.", "她是中国人。"), GrammarExample("Vous êtes chez Paul.", "你们在保罗家。")),
                    tip = "职业作表语时常不用冠词：Il est médecin。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "1_1", prompt = "Nous ___ en vacances.", options = listOf("sommes", "êtes", "sont"),
                    answers = listOf("sommes"), explanation = "nous 对应 sommes。", chineseHint = "我们在度假。",
                ),
                GrammarExercise(
                    id = "1_2", prompt = "Elle est ___ Madrid.", options = listOf("à", "de", "chez"),
                    answers = listOf("à"), explanation = "城市地点通常用 à。", chineseHint = "她在马德里。",
                ),
            ),
        ),
        GrammarLesson(
            number = 2, titleFrench = "L’adjectif (1)", titleChinese = "形容词：阴阳性与配合", category = "基础结构", bookPage = 12, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "阴性怎么变", rule = "多数形容词阴性在阳性后加 -e；部分会双写辅音或发生词尾变化。", formula = "grand → grande · italien → italienne",
                    examples = listOf(GrammarExample("Il est grand. Elle est grande.", "他高。她高。"), GrammarExample("Il est sportif. Elle est sportive.", "他爱运动。她爱运动。")),
                    tip = "先记规则，再单独记 beau/belle、nouveau/nouvelle 等高频不规则。",
                ),
                GrammarMemoryCard(
                    title = "复数与一致", rule = "形容词要和所修饰名词在性、数上一致；多数复数加 -s。", formula = "masc/fém + sing/plur",
                    examples = listOf(GrammarExample("Ils sont français.", "他们是法国人。"), GrammarExample("Elles sont françaises.", "她们是法国人。")),
                    tip = "看到主语先判断：男/女？单/复？再变形。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "2_1", prompt = "Anne est ___ . (grand)", options = listOf(),
                    answers = listOf("grande"), explanation = "Anne 是阴性单数，所以 grand → grande。", chineseHint = "安娜很高。",
                ),
                GrammarExercise(
                    id = "2_2", prompt = "Paul et Marc sont ___ . (blond)", options = listOf(),
                    answers = listOf("blonds"), explanation = "阳性复数通常加 -s。", chineseHint = "保罗和马克是金发。",
                ),
            ),
        ),
        GrammarLesson(
            number = 3, titleFrench = "La négation et l’interrogation (1)", titleChinese = "否定与基础疑问", category = "基础结构", bookPage = 18, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "ne ... pas", rule = "简单否定把 ne 和 pas 放在变位动词两边；元音前 ne → n’。", formula = "ne/n’ + verbe + pas",
                    examples = listOf(GrammarExample("Je ne suis pas fatigué.", "我不累。"), GrammarExample("Il n’est pas ici.", "他不在这里。")),
                    tip = "口语常省 ne，但写作和考试里保留更稳妥。",
                ),
                GrammarMemoryCard(
                    title = "三种基础问法", rule = "可以用升调、est-ce que，或倒装。对否定问题表示“恰恰相反”用 si。", formula = "Tu viens ? · Est-ce que tu viens ? · Viens-tu ?",
                    examples = listOf(GrammarExample("Est-ce que vous êtes prêt ?", "您准备好了吗？"), GrammarExample("Vous n’êtes pas français ? — Si.", "你不是法国人吗？——不，我是。")),
                    tip = "看到否定问句，先想 oui / non / si 哪个逻辑正确。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "3_1", prompt = "Je ___ suis ___ marié.", options = listOf("ne / pas", "n’ / pas", "pas / ne"),
                    answers = listOf("ne / pas"), explanation = "suis 前用 ne，pas 放在动词后。", chineseHint = "我没有结婚。",
                ),
                GrammarExercise(
                    id = "3_2", prompt = "Tu n’aimes pas le café ? — ___, j’adore ça !", options = listOf("Oui", "Non", "Si"),
                    answers = listOf("Si"), explanation = "否定问句中，用 si 反驳否定。", chineseHint = "你不喜欢咖啡吗？——不，我很喜欢！",
                ),
            ),
        ),
        GrammarLesson(
            number = 4, titleFrench = "Le nom et l’article", titleChinese = "名词与冠词", category = "基础结构", bookPage = 22, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "性与数", rule = "法语名词有阴阳性；多数复数加 -s，但有若干常见特殊词尾。", formula = "un problème · une solution · des problèmes",
                    examples = listOf(GrammarExample("un voyage", "一次旅行"), GrammarExample("une décision", "一个决定")),
                    tip = "学名词时尽量连冠词一起记，而不是只记裸词。",
                ),
                GrammarMemoryCard(
                    title = "三组常用冠词", rule = "不定冠词 un/une/des 表示一个或若干；定冠词 le/la/les 表示特指或泛指；à/de 与 le/les 会缩合。", formula = "à + le = au · de + le = du · à + les = aux · de + les = des",
                    examples = listOf(GrammarExample("Je vais au cinéma.", "我去电影院。"), GrammarExample("Je parle des voisins.", "我谈到邻居们。")),
                    tip = "à la / de la 不缩合。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "4_1", prompt = "___ problème est difficile.", options = listOf("Le", "La", "Une"),
                    answers = listOf("Le"), explanation = "problème 是阳性。", chineseHint = "这个问题很难。",
                ),
                GrammarExercise(
                    id = "4_2", prompt = "Je vais ___ bureau.", options = listOf("au", "à le", "du"),
                    answers = listOf("au"), explanation = "à + le 必须缩合成 au。", chineseHint = "我去办公室。",
                ),
            ),
        ),
        GrammarLesson(
            number = 5, titleFrench = "« C’est » et « Il est »", titleChinese = "C’est 与 Il est", category = "基础结构", bookPage = 32, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "识别用 c’est", rule = "c’est / ce sont 常用于指出、介绍、识别某人或某物，后面经常接限定词 + 名词。", formula = "C’est + déterminant + nom",
                    examples = listOf(GrammarExample("C’est mon voisin.", "这是我的邻居。"), GrammarExample("Ce sont des amis.", "这些是朋友。")),
                    tip = "问 Qu’est-ce que c’est ? / Qui est-ce ? 时，答案常从 c’est 开始。",
                ),
                GrammarMemoryCard(
                    title = "描述用 il est", rule = "描述已经明确的人或物，常用 il/elle est + adjectif；职业、国籍等无修饰时也常直接接名词。", formula = "Il est + adjectif / profession",
                    examples = listOf(GrammarExample("Il est sympathique.", "他很友好。"), GrammarExample("Elle est médecin.", "她是医生。")),
                    tip = "有修饰的职业名词往往回到 c’est：C’est un excellent médecin。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "5_1", prompt = "Paul ? ___ mon voisin.", options = listOf("C’est", "Il est", "Ce sont"),
                    answers = listOf("C’est"), explanation = "介绍身份，用 C’est + 名词短语。", chineseHint = "保罗？这是我的邻居。",
                ),
                GrammarExercise(
                    id = "5_2", prompt = "Marie est architecte. ___ très créative.", options = listOf("C’est", "Elle est", "Ce sont"),
                    answers = listOf("Elle est"), explanation = "描述已知 Marie，用 elle est + adjectif。", chineseHint = "玛丽是建筑师。她很有创造力。",
                ),
            ),
        ),
        GrammarLesson(
            number = 6, titleFrench = "Les possessifs", titleChinese = "物主词", category = "基础结构", bookPage = 36, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "物主形容词看“被拥有物”", rule = "mon/ma/mes, ton/ta/tes, son/sa/ses 等要和后面的名词在性、数上一致，而不是和拥有者性别一致。", formula = "mon frère · ma sœur · mes parents",
                    examples = listOf(GrammarExample("Paul aime sa voiture.", "保罗喜欢他的车。"), GrammarExample("Marie aime son travail.", "玛丽喜欢她的工作。")),
                    tip = "son travail 不是因为 Marie 是女性，而是 travail 是阳性。",
                ),
                GrammarMemoryCard(
                    title = "元音前用 mon/ton/son", rule = "阴性单数名词若以元音或哑音 h 开头，为了发音通常用 mon/ton/son。", formula = "mon amie · ton école · son idée",
                    examples = listOf(GrammarExample("C’est mon amie.", "这是我的女性朋友。"), GrammarExample("J’aime son idée.", "我喜欢他的/她的想法。")),
                    tip = "不要写 ma amie。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "6_1", prompt = "C’est ___ amie. (je)", options = listOf("mon", "ma", "mes"),
                    answers = listOf("mon"), explanation = "amie 是阴性，但元音开头，用 mon。", chineseHint = "这是我的女性朋友。",
                ),
                GrammarExercise(
                    id = "6_2", prompt = "Nous aimons ___ professeur.", options = listOf("notre", "nos", "votre"),
                    answers = listOf("notre"), explanation = "单数 professeur 搭配 notre。", chineseHint = "我们喜欢我们的老师。",
                ),
            ),
        ),
        GrammarLesson(
            number = 7, titleFrench = "Les noms de parenté et de groupe", titleChinese = "亲属与群体名词", category = "基础结构", bookPage = 38, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "亲属词成组记", rule = "父母、祖父母、孙辈、叔姨舅姑等最好按关系网络记忆，而不是孤立背词。", formula = "père/mère · frère/sœur · grand-père/grand-mère",
                    examples = listOf(GrammarExample("Mes parents habitent à Quanzhou.", "我的父母住在泉州。"), GrammarExample("Ma sœur a deux enfants.", "我姐姐/妹妹有两个孩子。")),
                    tip = "用你自己的家庭关系造句最容易记住。",
                ),
                GrammarMemoryCard(
                    title = "les gens 与 tout le monde", rule = "les gens 是复数，动词和形容词按复数；tout le monde 语义是“大家”，但语法上用单数动词。", formula = "les gens sont... · tout le monde est...",
                    examples = listOf(GrammarExample("Les gens sont pressés.", "人们很匆忙。"), GrammarExample("Tout le monde est prêt.", "大家都准备好了。")),
                    tip = "这组很适合做“语义复数 vs 语法单数”对比记忆。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "7_1", prompt = "Tout le monde ___ prêt.", options = listOf("est", "sont", "êtes"),
                    answers = listOf("est"), explanation = "tout le monde 用单数动词。", chineseHint = "大家都准备好了。",
                ),
                GrammarExercise(
                    id = "7_2", prompt = "Les gens ___ souvent pressés.", options = listOf("est", "sont", "sommes"),
                    answers = listOf("sont"), explanation = "les gens 是复数。", chineseHint = "人们经常很匆忙。",
                ),
            ),
        ),
        GrammarLesson(
            number = 8, titleFrench = "Les démonstratifs", titleChinese = "指示词", category = "基础结构", bookPage = 40, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "ce/cet/cette/ces", rule = "指示形容词直接放在名词前：ce 阳性、cet 阳性元音前、cette 阴性、ces 复数。", formula = "ce livre · cet homme · cette idée · ces livres",
                    examples = listOf(GrammarExample("Regarde cet homme.", "看这个男人。"), GrammarExample("J’aime cette ville.", "我喜欢这座城市。")),
                    tip = "cet 只解决阳性单数元音前的发音问题。",
                ),
                GrammarMemoryCard(
                    title = "celui/celle/ceux/celles", rule = "指示代词用来替代已经出现的名词，常接 -ci/-là、de... 或关系从句。", formula = "celui · celle · ceux · celles",
                    examples = listOf(GrammarExample("Je prends celui-ci.", "我拿这个。"), GrammarExample("Celle de Paul est rouge.", "保罗的那个是红色的。")),
                    tip = "先看被替代名词的性和数，再选形式。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "8_1", prompt = "___ étudiant est chinois.", options = listOf("Ce", "Cet", "Cette"),
                    answers = listOf("Cet"), explanation = "étudiant 阳性单数且元音开头，用 cet。", chineseHint = "这个学生是中国人。",
                ),
                GrammarExercise(
                    id = "8_2", prompt = "Je préfère ___ de gauche. (la robe)", options = listOf("celui", "celle", "ceux"),
                    answers = listOf("celle"), explanation = "robe 是阴性单数，用 celle。", chineseHint = "我更喜欢左边那件（裙子）。",
                ),
            ),
        ),
        GrammarLesson(
            number = 9, titleFrench = "« Il y a » et « C’est »", titleChinese = "Il y a 与 C’est", category = "基础结构", bookPage = 42, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "il y a = 存在", rule = "il y a 用来说明某处“有/存在”某人某物，形式固定，不随名词单复数变化。", formula = "Dans... il y a + nom",
                    examples = listOf(GrammarExample("Dans la rue, il y a une voiture.", "街上有一辆车。"), GrammarExample("Il y a des étudiants ici.", "这里有学生。")),
                    tip = "先“引入存在”，再用 c’est / ce sont 去识别。",
                ),
                GrammarMemoryCard(
                    title = "c’est = 识别", rule = "当对象已经出现，需要说明“这是什么/是谁”时用 c’est 或 ce sont。", formula = "Il y a X. C’est / Ce sont X.",
                    examples = listOf(GrammarExample("Il y a un musée. C’est le Louvre.", "有一座博物馆。那是卢浮宫。"), GrammarExample("Il y a deux filles. Ce sont mes sœurs.", "有两个女孩。她们是我的姐妹。")),
                    tip = "把两句当成固定叙述链来练。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "9_1", prompt = "Dans mon sac, ___ un livre.", options = listOf("il y a", "c’est", "ce sont"),
                    answers = listOf("il y a"), explanation = "表达存在，用 il y a。", chineseHint = "我的包里有一本书。",
                ),
                GrammarExercise(
                    id = "9_2", prompt = "Il y a un homme devant la porte. ___ mon voisin.", options = listOf("Il y a", "C’est", "Ce sont"),
                    answers = listOf("C’est"), explanation = "第二句是在识别这个人。", chineseHint = "门前有一个男人。他是我的邻居。",
                ),
            ),
        ),
        GrammarLesson(
            number = 10, titleFrench = "La situation dans l’espace (1)", titleChinese = "空间位置（一）", category = "空间·数量·时间", bookPage = 44, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "城市与国家介词", rule = "城市通常 à；阴性国家或元音开头国家用 en；阳性国家用 au；复数国家用 aux。", formula = "à Paris · en France · au Japon · aux États-Unis",
                    examples = listOf(GrammarExample("Je suis à Paris.", "我在巴黎。"), GrammarExample("Elle vit au Canada.", "她住在加拿大。")),
                    tip = "国家前介词要和国家的性、数一起记。",
                ),
                GrammarMemoryCard(
                    title = "基本空间表达", rule = "dans 表示内部，sur 表示表面，sous 表示下方；près de / loin de 表示距离。", formula = "dans · sur · sous · près de · loin de",
                    examples = listOf(GrammarExample("Le livre est sur la table.", "书在桌上。"), GrammarExample("La banque est près de la gare.", "银行在车站附近。")),
                    tip = "用房间里的真实物品做定位练习最直观。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "10_1", prompt = "Nous allons ___ Japon.", options = listOf("en", "au", "aux"),
                    answers = listOf("au"), explanation = "Japon 是阳性单数国家，用 au。", chineseHint = "我们去日本。",
                ),
                GrammarExercise(
                    id = "10_2", prompt = "Le chat est ___ la table. (在桌子下面)", options = listOf("sur", "sous", "dans"),
                    answers = listOf("sous"), explanation = "“下面”用 sous。", chineseHint = "猫在桌子下面。",
                ),
            ),
        ),
        GrammarLesson(
            number = 11, titleFrench = "Le verbe « avoir »", titleChinese = "动词 avoir", category = "空间·数量·时间", bookPage = 48, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "avoir 现在时", rule = "j’ai, tu as, il/elle/on a, nous avons, vous avez, ils/elles ont。", formula = "ai · as · a · avons · avez · ont",
                    examples = listOf(GrammarExample("J’ai vingt-huit ans.", "我28岁。"), GrammarExample("Nous avons un problème.", "我们有一个问题。")),
                    tip = "年龄用 avoir，不用 être。",
                ),
                GrammarMemoryCard(
                    title = "高频 avoir 表达", rule = "avoir faim/soif/froid/chaud/sommeil/peur/besoin de 等是整块表达。", formula = "avoir + nom",
                    examples = listOf(GrammarExample("J’ai faim.", "我饿了。"), GrammarExample("J’ai besoin de repos.", "我需要休息。")),
                    tip = "把它们当成固定搭配，不要逐词翻译。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "11_1", prompt = "Vous ___ deux enfants.", options = listOf("avez", "êtes", "ont"),
                    answers = listOf("avez"), explanation = "vous 对应 avez。", chineseHint = "您有两个孩子。",
                ),
                GrammarExercise(
                    id = "11_2", prompt = "J’___ besoin d’un visa.", options = listOf("ai", "suis", "est"),
                    answers = listOf("ai"), explanation = "固定搭配 avoir besoin de。", chineseHint = "我需要签证。",
                ),
            ),
        ),
        GrammarLesson(
            number = 12, titleFrench = "L’adjectif (2)", titleChinese = "形容词位置与特殊意义", category = "空间·数量·时间", bookPage = 54, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "多数放名词后", rule = "颜色、国籍和大多数描述性形容词通常放在名词后；一小组高频短形容词常放前面。", formula = "une voiture rouge · un bon livre",
                    examples = listOf(GrammarExample("une chemise blanche", "一件白衬衫"), GrammarExample("un petit appartement", "一套小公寓")),
                    tip = "不要死背“前/后”，先记最常见搭配。",
                ),
                GrammarMemoryCard(
                    title = "位置可能改变意义", rule = "某些形容词前置/后置意义不同，如 grand、ancien、cher；neuf 与 nouveau 也不是同一概念。", formula = "un grand homme ≠ un homme grand",
                    examples = listOf(GrammarExample("un ancien professeur", "一位以前的老师"), GrammarExample("un professeur ancien", "一位年长/古老意义上的老师")),
                    tip = "遇到特殊形容词时连完整名词短语记。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "12_1", prompt = "une voiture ___ (rouge)", options = listOf(),
                    answers = listOf("rouge"), explanation = "颜色通常放名词后。", chineseHint = "一辆红色汽车。",
                ),
                GrammarExercise(
                    id = "12_2", prompt = "___ ami (ancien = 以前的)", options = listOf("un ancien", "un ami ancien", "ancien un"),
                    answers = listOf("un ancien"), explanation = "“以前的朋友”用 ancien 前置：un ancien ami。", chineseHint = "一个以前的朋友。",
                ),
            ),
        ),
        GrammarLesson(
            number = 13, titleFrench = "Les nombres", titleChinese = "数字", category = "空间·数量·时间", bookPage = 58, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "基数词与书写", rule = "数字表达要特别熟悉 70、80、90 系列；vingt 和 cent 只有在正好构成整数倍且后面无其他数字时才可能加 -s。", formula = "quatre-vingts · quatre-vingt-un",
                    examples = listOf(GrammarExample("deux cents", "两百"), GrammarExample("deux cent cinq", "两百零五")),
                    tip = "TCF 听力里数字反应速度很重要，建议配合发音练。",
                ),
                GrammarMemoryCard(
                    title = "序数与时间单位", rule = "序数多用基数 + -ième，premier/première 特殊；an/année 用法也要区分。", formula = "premier · deuxième · troisième",
                    examples = listOf(GrammarExample("le premier jour", "第一天"), GrammarExample("une année difficile", "艰难的一年")),
                    tip = "数字不要只会认，要能听写和口头说出。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "13_1", prompt = "80 用法正确的是？", options = listOf("quatre-vingts", "quatre-vingt", "quatres-vingts"),
                    answers = listOf("quatre-vingts"), explanation = "单独的 80 写 quatre-vingts。", chineseHint = "八十。",
                ),
                GrammarExercise(
                    id = "13_2", prompt = "81 用法正确的是？", options = listOf("quatre-vingt-un", "quatre-vingts-un", "quatre vingt et un"),
                    answers = listOf("quatre-vingt-un"), explanation = "后面还有数字时 vingt 不加 s。", chineseHint = "八十一。",
                ),
            ),
        ),
        GrammarLesson(
            number = 14, titleFrench = "Le temps (1)", titleChinese = "日期、时间与天气", category = "空间·数量·时间", bookPage = 64, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "日期与季节", rule = "日期通常用 le + 数字 + 月；星期和月份一般不用大写。季节常见 au printemps, en été, en automne, en hiver。", formula = "Nous sommes le 3 février.",
                    examples = listOf(GrammarExample("Nous sommes lundi.", "今天是星期一。"), GrammarExample("Je pars en été.", "我夏天出发。")),
                    tip = "日期里的 1 日常用 premier。",
                ),
                GrammarMemoryCard(
                    title = "几点与天气", rule = "报时常用 Il est + heure；天气常用 Il fait + adjectif / Il y a + nom / il + verbe。", formula = "Il est huit heures. · Il fait beau.",
                    examples = listOf(GrammarExample("Il est midi.", "现在是中午。"), GrammarExample("Il pleut.", "在下雨。")),
                    tip = "时间和天气都大量使用无人称 il。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "14_1", prompt = "Nous sommes ___ 12 mars.", options = listOf("le", "à", "en"),
                    answers = listOf("le"), explanation = "日期前用 le。", chineseHint = "今天是3月12日。",
                ),
                GrammarExercise(
                    id = "14_2", prompt = "___ beau aujourd’hui.", options = listOf("Il fait", "Il est", "C’est"),
                    answers = listOf("Il fait"), explanation = "天气形容词常用 Il fait。", chineseHint = "今天天气很好。",
                ),
            ),
        ),
        GrammarLesson(
            number = 15, titleFrench = "Les indéfinis", titleChinese = "不定词", category = "空间·数量·时间", bookPage = 68, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "限定词：数量不精确", rule = "chaque 表示“每一个”，quelques 表示“几个”，plusieurs 表示“多个”，certains 表示“某些”，aucun 表示“没有任何”。", formula = "chaque · quelques · plusieurs · certains · aucun",
                    examples = listOf(GrammarExample("Chaque étudiant répond.", "每个学生都回答。"), GrammarExample("J’ai quelques questions.", "我有几个问题。")),
                    tip = "chaque 后接单数；plusieurs 后接复数。",
                ),
                GrammarMemoryCard(
                    title = "tout 的配合", rule = "tout 作限定词时随名词变化：tout/toute/tous/toutes；tout le monde 虽表示所有人，却用单数动词。", formula = "tout · toute · tous · toutes",
                    examples = listOf(GrammarExample("Toute la journée", "一整天"), GrammarExample("Tous les étudiants", "所有学生")),
                    tip = "把 tout 的四种形式和名词性数一起判断。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "15_1", prompt = "___ étudiant doit répondre.", options = listOf("Chaque", "Plusieurs", "Quelques"),
                    answers = listOf("Chaque"), explanation = "后面是单数 étudiant，且表示每一个，用 chaque。", chineseHint = "每个学生都必须回答。",
                ),
                GrammarExercise(
                    id = "15_2", prompt = "___ les filles sont prêtes.", options = listOf("Toutes", "Tout", "Toute"),
                    answers = listOf("Toutes"), explanation = "filles 是阴性复数，用 toutes。", chineseHint = "所有女孩都准备好了。",
                ),
            ),
        ),
        GrammarLesson(
            number = 16, titleFrench = "Les verbes en « -er » au présent", titleChinese = "-ER 动词现在时", category = "现在时·代词", bookPage = 74, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "六个人称词尾", rule = "规则 -er 动词去掉 -er 后加 -e, -es, -e, -ons, -ez, -ent。", formula = "e · es · e · ons · ez · ent",
                    examples = listOf(GrammarExample("Je parle français.", "我说法语。"), GrammarExample("Nous travaillons beaucoup.", "我们工作很多。")),
                    tip = "-ent 在第三人称复数通常不发音。",
                ),
                GrammarMemoryCard(
                    title = "拼写变化为了保留发音", rule = "manger → nous mangeons，commencer → nous commençons；部分动词还会发生重音/辅音变化。", formula = "manger → mangeons · commencer → commençons",
                    examples = listOf(GrammarExample("Nous mangeons ici.", "我们在这里吃饭。"), GrammarExample("Nous commençons à huit heures.", "我们八点开始。")),
                    tip = "先掌握高频例外，不必一次背完所有拼写变化。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "16_1", prompt = "Nous ___ français. (parler)", options = listOf(),
                    answers = listOf("parlons"), explanation = "nous 的规则词尾是 -ons。", chineseHint = "我们说法语。",
                ),
                GrammarExercise(
                    id = "16_2", prompt = "Ils ___ à Paris. (habiter)", options = listOf(),
                    answers = listOf("habitent"), explanation = "ils 的书写词尾是 -ent。", chineseHint = "他们住在巴黎。",
                ),
            ),
        ),
        GrammarLesson(
            number = 17, titleFrench = "Le temps (2)", titleChinese = "持续时间表达", category = "现在时·代词", bookPage = 80, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "depuis + 仍在持续", rule = "从过去开始并持续到现在，常用 présent + depuis。", formula = "présent + depuis",
                    examples = listOf(GrammarExample("J’habite ici depuis trois ans.", "我住在这里三年了，而且现在仍住这里。"), GrammarExample("Depuis quand apprenez-vous le français ?", "你学法语多久了？")),
                    tip = "中文“已经……多久”不等于一定用过去时。",
                ),
                GrammarMemoryCard(
                    title = "pendant / pour / en", rule = "pendant 表示一段实际经历的时长；pour 常表示计划时长；en 可表示完成某事所花的时间。", formula = "pendant · pour · en",
                    examples = listOf(GrammarExample("J’ai travaillé pendant deux heures.", "我工作了两个小时。"), GrammarExample("Je pars pour trois jours.", "我要离开三天。")),
                    tip = "先判断：持续到现在？已完成？计划？完成所需时间？",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "17_1", prompt = "J’habite ici ___ 2023.", options = listOf("depuis", "pendant", "pour"),
                    answers = listOf("depuis"), explanation = "从2023到现在仍持续，用 depuis。", chineseHint = "我从2023年起住在这里。",
                ),
                GrammarExercise(
                    id = "17_2", prompt = "J’ai dormi ___ huit heures.", options = listOf("pendant", "depuis", "pour"),
                    answers = listOf("pendant"), explanation = "已完成的一段时长常用 pendant。", chineseHint = "我睡了八个小时。",
                ),
            ),
        ),
        GrammarLesson(
            number = 18, titleFrench = "L’adverbe", titleChinese = "副词", category = "现在时·代词", bookPage = 82, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "副词修饰动作或程度", rule = "形容词修饰名词，副词常修饰动词、形容词或另一个副词。bon 是形容词，bien 是副词。", formula = "bon ≠ bien",
                    examples = listOf(GrammarExample("Il parle bien français.", "他法语说得好。"), GrammarExample("C’est un bon professeur.", "这是一位好老师。")),
                    tip = "先问自己：我是在描述“东西/人”，还是描述“怎么做”？",
                ),
                GrammarMemoryCard(
                    title = "-ment 构成", rule = "很多副词由形容词变为阴性形式后加 -ment；也有 fréquent → fréquemment 等特殊拼写。", formula = "rapide → rapidement",
                    examples = listOf(GrammarExample("Elle répond rapidement.", "她回答得很快。"), GrammarExample("Il travaille sérieusement.", "他工作很认真。")),
                    tip = "常用副词建议直接按整词记。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "18_1", prompt = "Il parle très ___ français.", options = listOf("bien", "bon", "bonne"),
                    answers = listOf("bien"), explanation = "修饰 parle 用副词 bien。", chineseHint = "他法语说得很好。",
                ),
                GrammarExercise(
                    id = "18_2", prompt = "C’est un ___ restaurant.", options = listOf("bon", "bien", "beaucoup"),
                    answers = listOf("bon"), explanation = "修饰名词 restaurant 用形容词 bon。", chineseHint = "这是一家好餐厅。",
                ),
            ),
        ),
        GrammarLesson(
            number = 19, titleFrench = "L’expression de la quantité", titleChinese = "数量表达", category = "现在时·代词", bookPage = 86, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "部分冠词", rule = "不可数或不确定数量常用 du/de la/de l’/des。", formula = "du pain · de la salade · de l’eau · des légumes",
                    examples = listOf(GrammarExample("Je bois de l’eau.", "我喝水。"), GrammarExample("Il mange du pain.", "他吃面包。")),
                    tip = "不要把所有中文“吃/喝某物”都翻成定冠词。",
                ),
                GrammarMemoryCard(
                    title = "数量词后用 de", rule = "beaucoup de, peu de, assez de, trop de, un kilo de 等后面直接接 de；否定时部分冠词/不定冠词通常也变 de。", formula = "beaucoup de · pas de",
                    examples = listOf(GrammarExample("J’ai beaucoup de travail.", "我有很多工作。"), GrammarExample("Je n’ai pas de voiture.", "我没有车。")),
                    tip = "数量词本身已经表达数量，因此后面不再用 du/de la/des。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "19_1", prompt = "Je bois ___ café.", options = listOf("du", "de", "le"),
                    answers = listOf("du"), explanation = "不确定数量的阳性不可数名词用 du。", chineseHint = "我喝咖啡。",
                ),
                GrammarExercise(
                    id = "19_2", prompt = "Nous avons beaucoup ___ travail.", options = listOf("de", "du", "des"),
                    answers = listOf("de"), explanation = "beaucoup 后固定用 de。", chineseHint = "我们有很多工作。",
                ),
            ),
        ),
        GrammarLesson(
            number = 20, titleFrench = "Le pronom « en »", titleChinese = "代词 en", category = "现在时·代词", bookPage = 90, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "en 替代 de + 内容", rule = "en 常替代 de + 名词，也替代部分冠词引出的东西。位置通常在变位动词前。", formula = "de + nom → en",
                    examples = listOf(GrammarExample("Je parle de ce livre. → J’en parle.", "我谈这本书。→ 我谈到它。"), GrammarExample("Tu veux du café ? → Oui, j’en veux.", "你要咖啡吗？→ 要，我要一些。")),
                    tip = "看到 de / du / de la / des，先考虑能不能用 en。",
                ),
                GrammarMemoryCard(
                    title = "数量要保留", rule = "en 替代名词，但具体数量通常仍留在句中。", formula = "J’en ai deux.",
                    examples = listOf(GrammarExample("J’ai trois frères. → J’en ai trois.", "我有三个兄弟。→ 我有三个。"), GrammarExample("Il boit beaucoup de café. → Il en boit beaucoup.", "他喝很多咖啡。→ 他喝很多。")),
                    tip = "en 不是把整个数量短语都删掉。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "20_1", prompt = "Tu parles de ton travail ? — Oui, j’___ parle.", options = listOf("en", "y", "le"),
                    answers = listOf("en"), explanation = "de ton travail → en。", chineseHint = "你谈你的工作吗？——是的，我谈。",
                ),
                GrammarExercise(
                    id = "20_2", prompt = "Vous avez deux enfants ? — Oui, j’___ ai deux.", options = listOf("en", "y", "les"),
                    answers = listOf("en"), explanation = "名词被 en 替代，数量 deux 保留。", chineseHint = "您有两个孩子吗？——是的，有两个。",
                ),
            ),
        ),
        GrammarLesson(
            number = 21, titleFrench = "La situation dans l’espace (2)", titleChinese = "空间位置（二）", category = "现在时·代词", bookPage = 94, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "地区与行政区", rule = "地区、州、省等地点介词取决于名称的性、数和是否带冠词；常见模式有 en, dans le, dans les。", formula = "en Bretagne · dans le Jura · dans les Alpes",
                    examples = listOf(GrammarExample("Il vit en Bretagne.", "他住在布列塔尼。"), GrammarExample("Nous sommes dans les Alpes.", "我们在阿尔卑斯山区。")),
                    tip = "地点名称最好和介词一起建立词组记忆。",
                ),
                GrammarMemoryCard(
                    title = "来源用 de/du/des", rule = "表示来自某地区时，常与其冠词形式对应：de, du, des。", formula = "venir de / du / des",
                    examples = listOf(GrammarExample("Je viens de Bretagne.", "我来自布列塔尼。"), GrammarExample("Il revient des Alpes.", "他从阿尔卑斯回来。")),
                    tip = "去哪里和从哪里是两套介词，要成对练。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "21_1", prompt = "Il habite ___ Bretagne.", options = listOf("en", "au", "aux"),
                    answers = listOf("en"), explanation = "Bretagne 常用 en。", chineseHint = "他住在布列塔尼。",
                ),
                GrammarExercise(
                    id = "21_2", prompt = "Elle revient ___ Alpes.", options = listOf("des", "du", "de la"),
                    answers = listOf("des"), explanation = "复数地区来源用 des。", chineseHint = "她从阿尔卑斯回来。",
                ),
            ),
        ),
        GrammarLesson(
            number = 22, titleFrench = "Le comparatif et le superlatif", titleChinese = "比较级与最高级", category = "现在时·代词", bookPage = 96, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "比较级三方向", rule = "形容词/副词用 plus/aussi/moins ... que；名词数量用 plus/autant/moins de ... que。", formula = "plus · aussi · moins ... que",
                    examples = listOf(GrammarExample("Paul est plus grand que Marc.", "保罗比马克高。"), GrammarExample("J’ai autant de travail que toi.", "我的工作和你一样多。")),
                    tip = "比较对象前通常有 que。",
                ),
                GrammarMemoryCard(
                    title = "不规则 meilleur / mieux", rule = "bon 的比较级常是 meilleur，bien 的比较级是 mieux；最高级加 le/la/les。", formula = "bon → meilleur · bien → mieux",
                    examples = listOf(GrammarExample("Ce livre est meilleur.", "这本书更好。"), GrammarExample("Elle parle mieux français.", "她法语说得更好。")),
                    tip = "meilleur 是形容词，mieux 是副词。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "22_1", prompt = "Elle est ___ grande que moi. (更)", options = listOf("plus", "mieux", "meilleure"),
                    answers = listOf("plus"), explanation = "普通形容词比较用 plus + adjectif + que。", chineseHint = "她比我高。",
                ),
                GrammarExercise(
                    id = "22_2", prompt = "Il parle ___ que moi.", options = listOf("mieux", "meilleur", "plus bon"),
                    answers = listOf("mieux"), explanation = "修饰 parler，bien 的比较级是 mieux。", chineseHint = "他说得比我好。",
                ),
            ),
        ),
        GrammarLesson(
            number = 23, titleFrench = "Le verbe « aller »", titleChinese = "动词 aller", category = "现在时·代词", bookPage = 102, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "aller 现在时", rule = "je vais, tu vas, il/elle/on va, nous allons, vous allez, ils/elles vont。", formula = "vais · vas · va · allons · allez · vont",
                    examples = listOf(GrammarExample("Je vais à Paris.", "我去巴黎。"), GrammarExample("Nous allons au cinéma.", "我们去电影院。")),
                    tip = "vais/va/vont 词形差异大，要整体记。",
                ),
                GrammarMemoryCard(
                    title = "aller + 地点/交通", rule = "去地点用 à/au/aux/chez 等；交通方式常见 en + véhicule 或 à pied/à vélo。", formula = "aller à/au/aux/chez · en voiture · à pied",
                    examples = listOf(GrammarExample("Je vais chez le médecin.", "我去医生那里。"), GrammarExample("Nous allons en train.", "我们坐火车去。")),
                    tip = "aller 也是 futur proche 的核心助动结构。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "23_1", prompt = "Nous ___ à l’université.", options = listOf("allons", "allez", "vont"),
                    answers = listOf("allons"), explanation = "nous 对应 allons。", chineseHint = "我们去大学。",
                ),
                GrammarExercise(
                    id = "23_2", prompt = "Je vais ___ pied.", options = listOf("à", "en", "au"),
                    answers = listOf("à"), explanation = "固定说 à pied。", chineseHint = "我步行去。",
                ),
            ),
        ),
        GrammarLesson(
            number = 24, titleFrench = "Le pronom « y »", titleChinese = "代词 y", category = "现在时·代词", bookPage = 104, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "y 替代地点", rule = "y 可以替代大多数地点补语，位置通常在变位动词前。", formula = "à/dans/sur... + lieu → y",
                    examples = listOf(GrammarExample("Je vais à Paris. → J’y vais.", "我去巴黎。→ 我去那里。"), GrammarExample("Il habite en France. → Il y habite.", "他住在法国。→ 他住在那里。")),
                    tip = "地点不是只能由 à 引出，en/dans/sur 等也常可用 y。",
                ),
                GrammarMemoryCard(
                    title = "y 替代 à + 事物", rule = "当 à 后面是事物/概念而不是人时，常可用 y。", formula = "penser à quelque chose → y penser",
                    examples = listOf(GrammarExample("Je pense à mon avenir. → J’y pense.", "我考虑未来。→ 我在考虑。"), GrammarExample("Elle répond à la question. → Elle y répond.", "她回答问题。→ 她回答它。")),
                    tip = "à + 人通常不用 y，而用重读代词或间接宾语形式。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "24_1", prompt = "Tu vas à la bibliothèque ? — Oui, j’___ vais.", options = listOf("y", "en", "la"),
                    answers = listOf("y"), explanation = "地点 à la bibliothèque → y。", chineseHint = "你去图书馆吗？——是的，我去。",
                ),
                GrammarExercise(
                    id = "24_2", prompt = "Vous pensez à ce projet ? — Oui, nous ___ pensons.", options = listOf("y", "en", "le"),
                    answers = listOf("y"), explanation = "à + 事物 → y。", chineseHint = "你们考虑这个项目吗？——是的。",
                ),
            ),
        ),
        GrammarLesson(
            number = 25, titleFrench = "La situation dans l’espace et le temps (3)", titleChinese = "空间与时间（三）", category = "现在时·代词", bookPage = 106, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "起点、终点和界限", rule = "de... à... 表示从……到……；jusqu’à 表示一直到；à partir de 表示从某点开始。", formula = "de... à... · jusqu’à · à partir de",
                    examples = listOf(GrammarExample("Je travaille de neuf heures à midi.", "我从九点工作到中午。"), GrammarExample("Le magasin est ouvert jusqu’à 20 h.", "商店营业到20点。")),
                    tip = "把空间和时间都想成一条线，会更好理解。",
                ),
                GrammarMemoryCard(
                    title = "大约与区间", rule = "vers/environ 表示大约；entre... et... 表示两者之间；parmi 表示在一群/多个之中。", formula = "vers · environ · entre...et · parmi",
                    examples = listOf(GrammarExample("J’arrive vers huit heures.", "我大约八点到。"), GrammarExample("Choisis parmi ces livres.", "从这些书中选。")),
                    tip = "vers 后直接接时间，environ 常直接放数量前。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "25_1", prompt = "Le cours dure ___ 8 h ___ 10 h.", options = listOf("de / à", "à / de", "depuis / pendant"),
                    answers = listOf("de / à"), explanation = "范围起点终点用 de... à...。", chineseHint = "课程从8点到10点。",
                ),
                GrammarExercise(
                    id = "25_2", prompt = "J’arrive ___ 18 h. (大约)", options = listOf("vers", "jusqu’à", "depuis"),
                    answers = listOf("vers"), explanation = "“大约某个时间点”常用 vers。", chineseHint = "我大约18点到。",
                ),
            ),
        ),
        GrammarLesson(
            number = 26, titleFrench = "Les verbes en « -ir », « -re » et « -oir »", titleChinese = "-IR/-RE/-OIR 动词", category = "现在时·代词", bookPage = 108, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "-ir 有不同家族", rule = "finir 型常出现 -iss-：nous finissons；但 partir/dormir 等不是同一套规则，必须按动词家族记。", formula = "finir → finissons · partir → partons",
                    examples = listOf(GrammarExample("Nous finissons à cinq heures.", "我们五点结束。"), GrammarExample("Ils partent demain.", "他们明天出发。")),
                    tip = "看到 -ir 不能自动套同一套词尾。",
                ),
                GrammarMemoryCard(
                    title = "第三组靠高频家族", rule = "prendre, mettre, venir, pouvoir, vouloir, devoir, savoir 等最好按词干变化和“家族”记。", formula = "prendre → prends/prenons/prennent",
                    examples = listOf(GrammarExample("Je prends le métro.", "我坐地铁。"), GrammarExample("Ils peuvent venir.", "他们可以来。")),
                    tip = "这正适合和你已经做好的不规则变位训练联动。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "26_1", prompt = "Nous ___ à six heures. (finir)", options = listOf(),
                    answers = listOf("finissons"), explanation = "finir 属于常见第二组，nous finissons。", chineseHint = "我们六点结束。",
                ),
                GrammarExercise(
                    id = "26_2", prompt = "Ils ___ venir. (pouvoir)", options = listOf(),
                    answers = listOf("peuvent"), explanation = "pouvoir 第三人称复数是 peuvent。", chineseHint = "他们可以来。",
                ),
            ),
        ),
        GrammarLesson(
            number = 27, titleFrench = "Les verbes pronominaux", titleChinese = "代词式动词", category = "现在时·代词", bookPage = 122, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "反身代词随主语变化", rule = "me/te/se/nous/vous/se 放在变位动词前。", formula = "je me · tu te · il se · nous nous · vous vous · ils se",
                    examples = listOf(GrammarExample("Je me lève à sept heures.", "我七点起床。"), GrammarExample("Nous nous reposons.", "我们休息。")),
                    tip = "否定时 ne 包住“代词 + 动词”：Je ne me lève pas。",
                ),
                GrammarMemoryCard(
                    title = "反身与相互", rule = "同一形式既可表示对自己做动作，也可表示彼此做动作，要靠语境判断。", formula = "se laver · se parler",
                    examples = listOf(GrammarExample("Elle se lave.", "她洗自己。"), GrammarExample("Ils se parlent.", "他们互相交谈。")),
                    tip = "有些动词代词式和非代词式意义不同，需要整词记。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "27_1", prompt = "Nous ___ levons tôt.", options = listOf("nous", "se", "vous"),
                    answers = listOf("nous"), explanation = "nous 主语搭配 nous nous levons。", chineseHint = "我们起得很早。",
                ),
                GrammarExercise(
                    id = "27_2", prompt = "Je ne ___ couche pas tard.", options = listOf("me", "se", "te"),
                    answers = listOf("me"), explanation = "je 对应 me。", chineseHint = "我睡得不晚。",
                ),
            ),
        ),
        GrammarLesson(
            number = 28, titleFrench = "Les pronoms compléments", titleChinese = "宾语代词", category = "现在时·代词", bookPage = 124, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "直接宾语 le/la/les", rule = "无介词直接跟在动词后的对象可用 le/la/les；代词放在变位动词前。", formula = "voir Paul → le voir",
                    examples = listOf(GrammarExample("Je connais Marie. → Je la connais.", "我认识玛丽。→ 我认识她。"), GrammarExample("Il achète les livres. → Il les achète.", "他买这些书。→ 他买它们。")),
                    tip = "先判断原句里有没有介词。",
                ),
                GrammarMemoryCard(
                    title = "间接宾语 lui/leur", rule = "à + 人在很多动词后可变为 lui/leur。me/te/nous/vous 形式与直接宾语相同。", formula = "parler à Paul → lui parler",
                    examples = listOf(GrammarExample("Je parle à Paul. → Je lui parle.", "我和保罗说话。→ 我跟他说话。"), GrammarExample("Elle écrit à ses parents. → Elle leur écrit.", "她给父母写信。→ 她给他们写信。")),
                    tip = "lui 不区分阴阳性；leur 是复数。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "28_1", prompt = "Je vois Marie. → Je ___ vois.", options = listOf("la", "lui", "y"),
                    answers = listOf("la"), explanation = "Marie 是直接宾语，阴性单数用 la。", chineseHint = "我看见玛丽。",
                ),
                GrammarExercise(
                    id = "28_2", prompt = "Nous parlons à nos parents. → Nous ___ parlons.", options = listOf("leur", "les", "en"),
                    answers = listOf("leur"), explanation = "à + 人复数，用 leur。", chineseHint = "我们和父母说话。",
                ),
            ),
        ),
        GrammarLesson(
            number = 29, titleFrench = "Les pronoms toniques", titleChinese = "重读人称代词", category = "现在时·代词", bookPage = 132, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "形式与位置", rule = "moi, toi, lui, elle, nous, vous, eux, elles 常用于介词后、强调、比较等。", formula = "avec moi · pour lui · chez eux",
                    examples = listOf(GrammarExample("Tu viens avec moi ?", "你和我一起来吗？"), GrammarExample("Je travaille chez eux.", "我在他们那里工作。")),
                    tip = "介词后不能用 je/tu/il。",
                ),
                GrammarMemoryCard(
                    title = "强调和 c’est... qui", rule = "需要强调某个人时可以把重读代词独立放出，或用 C’est moi qui...。", formula = "Moi, je... · C’est lui qui...",
                    examples = listOf(GrammarExample("Moi, je préfère le thé.", "我呢，我更喜欢茶。"), GrammarExample("C’est elle qui décide.", "是她决定。")),
                    tip = "qui 后动词的人称通常跟被强调的人保持语义一致。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "29_1", prompt = "Tu viens avec ___ ? (我)", options = listOf("moi", "je", "me"),
                    answers = listOf("moi"), explanation = "介词 avec 后用重读代词 moi。", chineseHint = "你和我一起来吗？",
                ),
                GrammarExercise(
                    id = "29_2", prompt = "C’est ___ qui parle. (他)", options = listOf("lui", "il", "le"),
                    answers = listOf("lui"), explanation = "C’est ... qui 里强调人用重读代词。", chineseHint = "是他在说话。",
                ),
            ),
        ),
        GrammarLesson(
            number = 30, titleFrench = "L’impératif", titleChinese = "命令式", category = "现在时·代词", bookPage = 134, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "只有 tu/nous/vous", rule = "肯定命令式通常取 présent 的 tu/nous/vous 形式但省主语；规则 -er 动词 tu 形式通常去掉 -s。", formula = "Parle ! · Parlons ! · Parlez !",
                    examples = listOf(GrammarExample("Écoute !", "听！"), GrammarExample("Prenez votre temps.", "慢慢来。")),
                    tip = "aller 的 tu 命令式是 va。",
                ),
                GrammarMemoryCard(
                    title = "宾语代词位置变化", rule = "肯定命令中代词放动词后并用连字符；否定命令中代词回到动词前。", formula = "Donne-le-moi. · Ne me le donne pas.",
                    examples = listOf(GrammarExample("Vas-y !", "去吧！"), GrammarExample("N’en parle pas !", "别谈这个！")),
                    tip = "-er 动词在 y/en 前常恢复 -s：Vas-y, Manges-en。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "30_1", prompt = "___ ici ! (venir, vous)", options = listOf(),
                    answers = listOf("venez"), explanation = "vous 命令式与 présent vous 形式相同。", chineseHint = "请到这里来！",
                ),
                GrammarExercise(
                    id = "30_2", prompt = "正确的否定命令是？", options = listOf("Ne le fais pas !", "Fais ne le pas !", "Ne fais-le pas !"),
                    answers = listOf("Ne le fais pas !"), explanation = "否定命令中宾语代词放在动词前。", chineseHint = "别做它！",
                ),
            ),
        ),
        GrammarLesson(
            number = 31, titleFrench = "Le conditionnel (1)", titleChinese = "条件式（一）：礼貌与建议", category = "现在时·代词", bookPage = 136, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "构成：未来词干 + 未完成过去时词尾", rule = "大多数动词用 futur simple 的词干，加 -ais, -ais, -ait, -ions, -iez, -aient。", formula = "infinitif/stem + ais/ais/ait/ions/iez/aient",
                    examples = listOf(GrammarExample("Je voudrais un café.", "我想要一杯咖啡。"), GrammarExample("Nous pourrions partir demain.", "我们可以明天出发。")),
                    tip = "vouloir → voudr-, pouvoir → pourr-, devoir → devr- 等词干和未来时共用。",
                ),
                GrammarMemoryCard(
                    title = "礼貌、建议、愿望", rule = "conditionnel présent 常让请求更柔和，也可表达建议、愿望和假设性判断。", formula = "Je voudrais... · Vous pourriez... · Il faudrait...",
                    examples = listOf(GrammarExample("Vous pourriez répéter ?", "您可以重复一遍吗？"), GrammarExample("Tu devrais dormir plus.", "你应该多睡一点。")),
                    tip = "TCF 口语里这是非常实用的礼貌工具。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "31_1", prompt = "Je ___ réserver une chambre. (vouloir)", options = listOf(),
                    answers = listOf("voudrais"), explanation = "vouloir 条件式第一人称是 voudrais。", chineseHint = "我想订一间房。",
                ),
                GrammarExercise(
                    id = "31_2", prompt = "Tu ___ faire plus de sport. (devoir)", options = listOf(),
                    answers = listOf("devrais"), explanation = "建议常用 tu devrais。", chineseHint = "你应该多运动。",
                ),
            ),
        ),
        GrammarLesson(
            number = 32, titleFrench = "Les relatifs", titleChinese = "关系代词", category = "现在时·代词", bookPage = 140, bodyAvailableInUploadedPdf = true,
            cards = listOf(
                GrammarMemoryCard(
                    title = "qui / que / où / dont", rule = "qui 在从句中作主语；que 作直接宾语；où 指地点或时间；dont 对应 de。", formula = "qui=S · que=COD · où=lieu/temps · dont=de",
                    examples = listOf(GrammarExample("La femme qui parle est ma sœur.", "正在说话的女人是我姐姐/妹妹。"), GrammarExample("Le livre que je lis est utile.", "我读的这本书很有用。")),
                    tip = "判断 qui/que 最快的方法：看空格后面有没有另一个主语。",
                ),
                GrammarMemoryCard(
                    title = "ce qui / ce que / ce dont", rule = "先行词不是具体名词而是“……的事情/东西”时，常用 ce qui, ce que, ce dont。", formula = "ce qui · ce que · ce dont",
                    examples = listOf(GrammarExample("Je comprends ce que tu dis.", "我明白你说的内容。"), GrammarExample("Ce qui m’intéresse, c’est la langue.", "让我感兴趣的是语言。")),
                    tip = "仍然用同一逻辑：主语→ce qui，宾语→ce que，de→ce dont。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "32_1", prompt = "Le garçon ___ parle est mon frère.", options = listOf("qui", "que", "dont"),
                    answers = listOf("qui"), explanation = "空格本身是 parle 的主语，用 qui。", chineseHint = "正在说话的男孩是我弟弟/哥哥。",
                ),
                GrammarExercise(
                    id = "32_2", prompt = "Le film ___ je regarde est français.", options = listOf("que", "qui", "où"),
                    answers = listOf("que"), explanation = "从句已有主语 je，缺直接宾语，用 que。", chineseHint = "我正在看的电影是法国电影。",
                ),
            ),
        ),
        GrammarLesson(
            number = 33, titleFrench = "L’interrogation (2)", titleChinese = "疑问句（二）", category = "句法·表达", bookPage = 148, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "疑问词各管什么", rule = "qui 问人，que/quoi 问事物，où 问地点，quand 问时间，comment 问方式，pourquoi 问原因，combien 问数量。", formula = "qui · que/quoi · où · quand · comment · pourquoi · combien",
                    examples = listOf(GrammarExample("Pourquoi apprenez-vous le français ?", "您为什么学法语？"), GrammarExample("Avec qui partez-vous ?", "您和谁一起出发？")),
                    tip = "疑问词和介词可以组合：avec qui, de quoi, à qui。",
                ),
                GrammarMemoryCard(
                    title = "quel 要配合", rule = "quel/quelle/quels/quelles 是疑问限定词，要和后面的名词性数一致。", formula = "quel · quelle · quels · quelles",
                    examples = listOf(GrammarExample("Quelle heure est-il ?", "几点了？"), GrammarExample("Quels livres préférez-vous ?", "您喜欢哪些书？")),
                    tip = "先看后面的名词，再决定 quel 的形式。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "33_1", prompt = "___ partez-vous ? — Demain.", options = listOf("Quand", "Où", "Pourquoi"),
                    answers = listOf("Quand"), explanation = "答案是时间，用 quand。", chineseHint = "您什么时候出发？——明天。",
                ),
                GrammarExercise(
                    id = "33_2", prompt = "___ langue apprenez-vous ?", options = listOf("Quelle", "Quel", "Quels"),
                    answers = listOf("Quelle"), explanation = "langue 阴性单数，用 quelle。", chineseHint = "您在学哪种语言？",
                ),
            ),
        ),
        GrammarLesson(
            number = 34, titleFrench = "La négation (2)", titleChinese = "否定（二）", category = "句法·表达", bookPage = 152, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "不只 ne...pas", rule = "常见否定还有 ne...plus（不再）、ne...jamais（从不）、ne...rien（什么也不）、ne...personne（谁也不）。", formula = "ne...plus/jamais/rien/personne",
                    examples = listOf(GrammarExample("Je ne fume plus.", "我不再抽烟。"), GrammarExample("Je ne vois personne.", "我谁也没看见。")),
                    tip = "rien/personne 在句中位置取决于语法功能，但都要先掌握核心框架。",
                ),
                GrammarMemoryCard(
                    title = "ne...que 不是完全否定", rule = "ne...que 表示“只、仅仅”，语义上是限制。ni...ni 表示“既不……也不……”。", formula = "ne...que · ni...ni",
                    examples = listOf(GrammarExample("Je n’ai que dix euros.", "我只有十欧元。"), GrammarExample("Je ne bois ni café ni thé.", "我既不喝咖啡也不喝茶。")),
                    tip = "看到 ne 不要自动判定整句为否定。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "34_1", prompt = "Je ne fume ___ . (不再)", options = listOf("plus", "jamais", "que"),
                    answers = listOf("plus"), explanation = "ne...plus = 不再。", chineseHint = "我不再抽烟。",
                ),
                GrammarExercise(
                    id = "34_2", prompt = "Je n’ai ___ deux minutes.", options = listOf("que", "rien", "personne"),
                    answers = listOf("que"), explanation = "ne...que 表示“只有”。", chineseHint = "我只有两分钟。",
                ),
            ),
        ),
        GrammarLesson(
            number = 35, titleFrench = "Le discours indirect au présent", titleChinese = "现在时中的间接引语", category = "句法·表达", bookPage = 156, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "陈述用 que，是非问句用 si", rule = "转述陈述通常用 dire que...；转述 oui/non 问题常用 demander si...。", formula = "dire que · demander si",
                    examples = listOf(GrammarExample("Il dit : « Je suis prêt. » → Il dit qu’il est prêt.", "他说“我准备好了。”→ 他说他准备好了。"), GrammarExample("Elle demande : « Tu viens ? » → Elle demande si tu viens.", "她问“你来吗？”→ 她问你是否来。")),
                    tip = "主句在现在时时态通常不后移，但人称要按新说话视角调整。",
                ),
                GrammarMemoryCard(
                    title = "疑问词保留，命令变 de + infinitif", rule = "特殊疑问句保留 où/quand/pourquoi 等；命令常变 demander/ordonner de + infinitif。", formula = "demander où... · dire de + infinitif",
                    examples = listOf(GrammarExample("Il demande où tu habites.", "他问你住哪里。"), GrammarExample("Elle me dit de venir.", "她叫我来。")),
                    tip = "先判断原话属于陈述、是非问句、特殊问句还是命令。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "35_1", prompt = "Il demande : « Tu es prêt ? » → Il demande ___ tu es prêt.", options = listOf("si", "que", "de"),
                    answers = listOf("si"), explanation = "是非问句间接化用 si。", chineseHint = "他问你是否准备好了。",
                ),
                GrammarExercise(
                    id = "35_2", prompt = "Elle dit : « Ferme la porte. » → Elle me dit ___ fermer la porte.", options = listOf("de", "que", "si"),
                    answers = listOf("de"), explanation = "命令常用 de + infinitif。", chineseHint = "她叫我关门。",
                ),
            ),
        ),
        GrammarLesson(
            number = 36, titleFrench = "Le gérondif", titleChinese = "副动词 Gérondif", category = "句法·表达", bookPage = 158, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "en + participe présent", rule = "gérondif 通常由 en + 现在分词组成；现在分词常取 nous 词干去 -ons 加 -ant。", formula = "en + radical de nous + ant",
                    examples = listOf(GrammarExample("Nous parlons → en parlant", "我们说 → 一边说/通过说"), GrammarExample("Nous faisons → en faisant", "我们做 → 一边做")),
                    tip = "être → étant, avoir → ayant, savoir → sachant 是高频特殊形式。",
                ),
                GrammarMemoryCard(
                    title = "同时、方式、条件", rule = "gérondif 可表达两个动作同时发生、做事方式或条件，通常两个动作主语相同。", formula = "en travaillant...",
                    examples = listOf(GrammarExample("J’écoute de la musique en travaillant.", "我工作时听音乐。"), GrammarExample("On apprend en pratiquant.", "通过练习来学习。")),
                    tip = "如果两个动作主语不同，就不要机械使用 gérondif。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "36_1", prompt = "Nous parlons → en ___", options = listOf(),
                    answers = listOf("parlant"), explanation = "parlons 去 -ons + -ant。", chineseHint = "一边说。",
                ),
                GrammarExercise(
                    id = "36_2", prompt = "J’apprends beaucoup ___ lisant.", options = listOf("en", "à", "de"),
                    answers = listOf("en"), explanation = "gérondif 固定以 en 引出。", chineseHint = "我通过阅读学到很多。",
                ),
            ),
        ),
        GrammarLesson(
            number = 37, titleFrench = "Les prépositions et les verbes", titleChinese = "动词与介词", category = "句法·表达", bookPage = 160, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "有的动词接 à", rule = "penser à, réussir à, apprendre à, aider quelqu’un à 等要把介词当成动词搭配的一部分。", formula = "verbe + à + nom/infinitif",
                    examples = listOf(GrammarExample("Je réussis à finir.", "我成功完成。"), GrammarExample("Il pense à son avenir.", "他考虑未来。")),
                    tip = "不要只背动词中文意思，要背完整框架。",
                ),
                GrammarMemoryCard(
                    title = "有的动词接 de，也有的零介词", rule = "essayer de, décider de, arrêter de；aimer/espérer/vouloir 等常直接接 infinitif。", formula = "verbe + de / Ø + infinitif",
                    examples = listOf(GrammarExample("J’essaie de comprendre.", "我努力理解。"), GrammarExample("Je veux partir.", "我想离开。")),
                    tip = "这部分可以直接和你现有 à/de 专项题库合并复习。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "37_1", prompt = "J’essaie ___ comprendre.", options = listOf("de", "à", "—"),
                    answers = listOf("de"), explanation = "essayer de + infinitif。", chineseHint = "我努力理解。",
                ),
                GrammarExercise(
                    id = "37_2", prompt = "Elle réussit ___ terminer.", options = listOf("à", "de", "—"),
                    answers = listOf("à"), explanation = "réussir à + infinitif。", chineseHint = "她成功完成。",
                ),
            ),
        ),
        GrammarLesson(
            number = 38, titleFrench = "Les verbes de déplacement", titleChinese = "移动动词", category = "句法·表达", bookPage = 164, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "方向动词成对记", rule = "aller/venir, partir/arriver, sortir/entrer, monter/descendre, retourner/rentrer 等最好按方向对比。", formula = "aller ↔ venir · partir ↔ arriver · sortir ↔ entrer",
                    examples = listOf(GrammarExample("Je pars de Paris.", "我从巴黎出发。"), GrammarExample("J’arrive à Lyon.", "我到达里昂。")),
                    tip = "同时记“从哪里 de”和“到哪里 à”。",
                ),
                GrammarMemoryCard(
                    title = "移动动词与 passé composé", rule = "一部分不及物移动动词在 passé composé 中常用 être，过去分词要和主语配合。", formula = "elle est arrivée · ils sont partis",
                    examples = listOf(GrammarExample("Elle est arrivée hier.", "她昨天到了。"), GrammarExample("Ils sont partis tôt.", "他们很早就走了。")),
                    tip = "并非所有表示移动的动词都自动用 être，要按常用列表记。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "38_1", prompt = "Je pars ___ Chine demain.", options = listOf("de", "à", "en"),
                    answers = listOf("de"), explanation = "partir de = 从……出发。", chineseHint = "我明天从中国出发。",
                ),
                GrammarExercise(
                    id = "38_2", prompt = "Elle est ___ hier. (arriver)", options = listOf(),
                    answers = listOf("arrivée"), explanation = "être + 过去分词，阴性单数配合为 arrivée。", chineseHint = "她昨天到了。",
                ),
            ),
        ),
        GrammarLesson(
            number = 39, titleFrench = "Le futur proche", titleChinese = "最近将来时", category = "句法·表达", bookPage = 168, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "aller + infinitif", rule = "futur proche = aller 的现在时 + 动词原形，用于近期计划、明显即将发生的事。", formula = "aller au présent + infinitif",
                    examples = listOf(GrammarExample("Je vais partir demain.", "我明天要出发。"), GrammarExample("Il va pleuvoir.", "要下雨了。")),
                    tip = "只有 aller 变位，后面的动词保持原形。",
                ),
                GrammarMemoryCard(
                    title = "否定围住 aller", rule = "否定时 ne...pas 放在 aller 的变位形式两边。", formula = "ne + aller + pas + infinitif",
                    examples = listOf(GrammarExample("Je ne vais pas sortir.", "我不打算出去。"), GrammarExample("Nous n’allons pas attendre.", "我们不准备等。")),
                    tip = "不要把 pas 放到不定式后面。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "39_1", prompt = "Nous ___ partir ce soir.", options = listOf("allons", "sommes", "avons"),
                    answers = listOf("allons"), explanation = "futur proche 用 aller + infinitif。", chineseHint = "我们今晚要出发。",
                ),
                GrammarExercise(
                    id = "39_2", prompt = "Je ne ___ pas travailler demain.", options = listOf("vais", "suis", "ai"),
                    answers = listOf("vais"), explanation = "否定仍围住 aller。", chineseHint = "我明天不打算工作。",
                ),
            ),
        ),
        GrammarLesson(
            number = 40, titleFrench = "Le passé composé", titleChinese = "复合过去时", category = "过去·未来·高级", bookPage = 174, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "avoir/être + participe passé", rule = "passé composé 用助动词现在时 + 过去分词；大多数动词用 avoir，部分移动动词和代词式动词用 être。", formula = "auxiliaire + participe passé",
                    examples = listOf(GrammarExample("J’ai fini le travail.", "我完成了工作。"), GrammarExample("Elle est arrivée hier.", "她昨天到了。")),
                    tip = "先决定助动词，再决定过去分词。",
                ),
                GrammarMemoryCard(
                    title = "être 时要配合", rule = "用 être 时过去分词通常与主语性数配合；用 avoir 时基础阶段先掌握常见不配合情况。", formula = "elle est partie · ils sont partis",
                    examples = listOf(GrammarExample("Marie est née en 1997.", "玛丽出生于1997年。"), GrammarExample("Ils se sont levés tôt.", "他们很早起床。")),
                    tip = "代词式动词的配合以后可以逐步细化。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "40_1", prompt = "J’___ mangé.", options = listOf("ai", "suis", "est"),
                    answers = listOf("ai"), explanation = "manger 的 passé composé 用 avoir。", chineseHint = "我吃了。",
                ),
                GrammarExercise(
                    id = "40_2", prompt = "Elle est ___ . (partir)", options = listOf(),
                    answers = listOf("partie"), explanation = "être + 阴性单数，parti → partie。", chineseHint = "她离开了。",
                ),
            ),
        ),
        GrammarLesson(
            number = 41, titleFrench = "Le temps (3)", titleChinese = "过去时间与先后关系", category = "过去·未来·高级", bookPage = 198, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "从多久以前到现在/过去", rule = "depuis 表持续，il y a 表“……以前”，pendant 表一段完成时长，en 表完成某事所需时间。", formula = "depuis · il y a · pendant · en",
                    examples = listOf(GrammarExample("Je l’ai vu il y a deux jours.", "我两天前见过他。"), GrammarExample("J’ai travaillé pendant trois heures.", "我工作了三个小时。")),
                    tip = "时间介词要和时态一起判断。",
                ),
                GrammarMemoryCard(
                    title = "叙事顺序词", rule = "d’abord, puis, ensuite, après, enfin 等帮助叙述事件先后。", formula = "d’abord → puis/ensuite → enfin",
                    examples = listOf(GrammarExample("D’abord, j’ai appelé. Ensuite, je suis parti.", "首先我打了电话，然后我离开了。"), GrammarExample("Enfin, nous sommes rentrés.", "最后我们回家了。")),
                    tip = "TCF 叙事题里，这些连接词非常实用。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "41_1", prompt = "Je l’ai vu ___ deux jours.", options = listOf("il y a", "depuis", "pour"),
                    answers = listOf("il y a"), explanation = "“两天前”用 il y a + 时长。", chineseHint = "我两天前见过他。",
                ),
                GrammarExercise(
                    id = "41_2", prompt = "___, j’ai préparé mes affaires. Ensuite, je suis parti.", options = listOf("D’abord", "Depuis", "Pendant"),
                    answers = listOf("D’abord"), explanation = "叙事第一步用 d’abord。", chineseHint = "首先我收拾好东西，然后出发。",
                ),
            ),
        ),
        GrammarLesson(
            number = 42, titleFrench = "Le passif", titleChinese = "被动语态", category = "过去·未来·高级", bookPage = 204, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "être + participe passé", rule = "被动把受事放到主语位置，核心结构是 être + 过去分词；过去分词与主语配合。", formula = "sujet + être + participe passé",
                    examples = listOf(GrammarExample("Le livre est écrit en français.", "这本书是用法语写的。"), GrammarExample("Les lettres sont envoyées.", "信件被寄出了。")),
                    tip = "être 本身可以变化时态：est écrit / a été écrit / sera écrit。",
                ),
                GrammarMemoryCard(
                    title = "施事者常用 par", rule = "需要指出动作执行者时常用 par；如果不重要可以省略。", formula = "être + pp + par + agent",
                    examples = listOf(GrammarExample("Ce roman est écrit par Camus.", "这部小说由加缪创作。"), GrammarExample("La porte a été fermée.", "门被关上了。")),
                    tip = "被动句不是必须保留“谁做的”。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "42_1", prompt = "La lettre est ___ par Paul. (écrire)", options = listOf(),
                    answers = listOf("écrite"), explanation = "lettre 阴性单数，écrit → écrite。", chineseHint = "这封信由保罗写。",
                ),
                GrammarExercise(
                    id = "42_2", prompt = "Les documents sont ___ . (envoyer)", options = listOf(),
                    answers = listOf("envoyés"), explanation = "documents 阳性复数，envoyé → envoyés。", chineseHint = "文件被寄出了。",
                ),
            ),
        ),
        GrammarLesson(
            number = 43, titleFrench = "L’imparfait", titleChinese = "未完成过去时", category = "过去·未来·高级", bookPage = 206, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "构成很规则", rule = "取 nous 的 présent 词干去 -ons，加 -ais, -ais, -ait, -ions, -iez, -aient；être 特殊词干 ét-。", formula = "nous stem + ais/ais/ait/ions/iez/aient",
                    examples = listOf(GrammarExample("nous parlons → je parlais", "我们说 → 我过去常说/当时在说"), GrammarExample("j’étais", "我当时是")),
                    tip = "先找到 nous 形式，绝大多数动词就能推出来。",
                ),
                GrammarMemoryCard(
                    title = "背景、习惯、持续状态", rule = "imparfait 常描述过去背景、习惯、状态或进行中的动作；passé composé 更常推进完成事件。", formula = "background/habit → imparfait",
                    examples = listOf(GrammarExample("Quand j’étais petit, je jouais au basket.", "小时候我常打篮球。"), GrammarExample("Il pleuvait quand je suis sorti.", "我出门时正在下雨。")),
                    tip = "想象“背景画面”与“事件按钮”的区别。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "43_1", prompt = "Quand j’étais petit, je ___ souvent au basket. (jouer)", options = listOf(),
                    answers = listOf("jouais"), explanation = "过去习惯用 imparfait。", chineseHint = "小时候我经常打篮球。",
                ),
                GrammarExercise(
                    id = "43_2", prompt = "Il ___ quand je suis sorti. (pleuvoir)", options = listOf(),
                    answers = listOf("pleuvait"), explanation = "背景天气用 imparfait。", chineseHint = "我出去时正在下雨。",
                ),
            ),
        ),
        GrammarLesson(
            number = 44, titleFrench = "Le plus-que-parfait", titleChinese = "愈过去时", category = "过去·未来·高级", bookPage = 212, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "过去中的更早过去", rule = "plus-que-parfait = avoir/être 的 imparfait + 过去分词，表示在另一个过去时间点之前已经完成。", formula = "avais/étais + participe passé",
                    examples = listOf(GrammarExample("J’avais déjà mangé quand il est arrivé.", "他到时我已经吃过了。"), GrammarExample("Elle était partie avant midi.", "她中午前已经离开了。")),
                    tip = "把时间线画成：更早过去 → 后来的过去。",
                ),
                GrammarMemoryCard(
                    title = "助动词规则沿用 passé composé", rule = "哪个动词在 passé composé 用 être，这里也通常用 être 的 imparfait，并继续处理配合。", formula = "elle était arrivée",
                    examples = listOf(GrammarExample("Ils s’étaient levés tôt.", "他们此前已经很早起床。"), GrammarExample("Nous avions fini.", "我们此前已经完成。")),
                    tip = "先会 passé composé，再学 plus-que-parfait 会很轻松。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "44_1", prompt = "Il est arrivé, mais j’___ déjà ___. (partir)", options = listOf(),
                    answers = listOf("étais déjà parti", "étais parti"), explanation = "partir 用 être；更早的过去用 étais parti。", chineseHint = "他到时我已经走了。",
                ),
                GrammarExercise(
                    id = "44_2", prompt = "Nous ___ déjà fini avant midi.", options = listOf("avions", "avons", "aurons"),
                    answers = listOf("avions"), explanation = "plus-que-parfait 用 avoir 的 imparfait：avions。", chineseHint = "中午前我们已经完成了。",
                ),
            ),
        ),
        GrammarLesson(
            number = 45, titleFrench = "Le discours indirect au passé", titleChinese = "过去时中的间接引语", category = "过去·未来·高级", bookPage = 216, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "主句过去时常触发时态后移", rule = "常见对应：présent → imparfait；passé composé → plus-que-parfait；futur → conditionnel présent。", formula = "présent→imparfait · PC→PQP · futur→conditionnel",
                    examples = listOf(GrammarExample("Il a dit : « Je suis prêt. » → Il a dit qu’il était prêt.", "他说：“我准备好了。”→ 他说他准备好了。"), GrammarExample("Elle a dit : « Je viendrai. » → Elle a dit qu’elle viendrait.", "她说：“我会来。”→ 她说她会来。")),
                    tip = "先找原话时态，再按转述时间点后移。",
                ),
                GrammarMemoryCard(
                    title = "时间/地点词也可能变化", rule = "aujourd’hui → ce jour-là, demain → le lendemain, hier → la veille 等常随叙述视角调整。", formula = "demain → le lendemain",
                    examples = listOf(GrammarExample("Il a dit qu’il viendrait le lendemain.", "他说他第二天会来。"), GrammarExample("Elle a expliqué qu’elle était partie la veille.", "她解释说她前一天已经离开。")),
                    tip = "考试里不一定每次都强制变化，但要能识别经典对应。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "45_1", prompt = "Il a dit : « Je suis fatigué. » → Il a dit qu’il ___ fatigué.", options = listOf("était", "est", "sera"),
                    answers = listOf("était"), explanation = "过去主句下 présent 常后移为 imparfait。", chineseHint = "他说他累了。",
                ),
                GrammarExercise(
                    id = "45_2", prompt = "Elle a dit : « Je viendrai. » → Elle a dit qu’elle ___.", options = listOf("viendrait", "viendra", "venait"),
                    answers = listOf("viendrait"), explanation = "futur 后移为 conditionnel présent。", chineseHint = "她说她会来。",
                ),
            ),
        ),
        GrammarLesson(
            number = 46, titleFrench = "« Venir de », « Être en train de », « Être sur le point de »", titleChinese = "刚刚、正在、马上", category = "过去·未来·高级", bookPage = 222, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "三个时间视角", rule = "venir de + infinitif = 刚刚做完；être en train de + infinitif = 正在做；être sur le point de + infinitif = 马上要做。", formula = "venir de · être en train de · être sur le point de",
                    examples = listOf(GrammarExample("Je viens de finir.", "我刚刚完成。"), GrammarExample("Je suis en train de travailler.", "我正在工作。")),
                    tip = "把它们想成时间轴：刚过去 / 正现在 / 马上未来。",
                ),
                GrammarMemoryCard(
                    title = "主体动词负责变时态", rule = "venir / être 根据时间变位，后面的 infinitif 不变。", formula = "je venais de... · j’étais en train de...",
                    examples = listOf(GrammarExample("Il était sur le point de partir.", "他当时正要出发。"), GrammarExample("Nous venons de rentrer.", "我们刚回来。")),
                    tip = "不要把后面的不定式也一起变位。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "46_1", prompt = "Je ___ de finir.", options = listOf("viens", "suis", "vais"),
                    answers = listOf("viens"), explanation = "venir de + infinitif = 刚刚。", chineseHint = "我刚刚完成。",
                ),
                GrammarExercise(
                    id = "46_2", prompt = "Elle est ___ de partir.", options = listOf("sur le point", "en train", "venue"),
                    answers = listOf("sur le point"), explanation = "être sur le point de = 马上要。", chineseHint = "她马上要出发。",
                ),
            ),
        ),
        GrammarLesson(
            number = 47, titleFrench = "Le futur simple", titleChinese = "简单将来时", category = "过去·未来·高级", bookPage = 224, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "构成规则", rule = "大多数动词用不定式作词干，-re 动词去掉末尾 e，再加 -ai, -as, -a, -ons, -ez, -ont。", formula = "infinitif + ai/as/a/ons/ez/ont",
                    examples = listOf(GrammarExample("Je parlerai demain.", "我明天会说/谈。"), GrammarExample("Nous prendrons le train.", "我们会坐火车。")),
                    tip = "词尾与 avoir 的现在时很像，是很好的记忆钩子。",
                ),
                GrammarMemoryCard(
                    title = "常见不规则词干", rule = "être ser-, avoir aur-, aller ir-, faire fer-, venir viendr-, pouvoir pourr-, vouloir voudr-, devoir devr-。", formula = "ser-/aur-/ir-/fer-/viendr-...",
                    examples = listOf(GrammarExample("Il sera prêt.", "他会准备好。"), GrammarExample("Je viendrai demain.", "我明天会来。")),
                    tip = "和 conditionnel 共用同一批词干。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "47_1", prompt = "Nous ___ demain. (partir)", options = listOf(),
                    answers = listOf("partirons"), explanation = "partir + ons = partirons。", chineseHint = "我们明天会出发。",
                ),
                GrammarExercise(
                    id = "47_2", prompt = "Je ___ demain. (venir)", options = listOf(),
                    answers = listOf("viendrai"), explanation = "venir 的未来词干是 viendr-。", chineseHint = "我明天会来。",
                ),
            ),
        ),
        GrammarLesson(
            number = 48, titleFrench = "Le futur antérieur", titleChinese = "先将来时", category = "过去·未来·高级", bookPage = 226, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "将来中的更早完成", rule = "futur antérieur = avoir/être 的 futur simple + 过去分词，表示在另一个未来事件之前已经完成。", formula = "aurai/serai + participe passé",
                    examples = listOf(GrammarExample("J’aurai fini avant midi.", "中午前我就会完成。"), GrammarExample("Elle sera partie quand tu arriveras.", "你到的时候她已经走了。")),
                    tip = "时间线与 plus-que-parfait 类似，只是整体搬到未来。",
                ),
                GrammarMemoryCard(
                    title = "常和时间从句配合", rule = "quand, dès que, après que 等可以把两个未来动作按先后排列。", formula = "quand... futur antérieur / futur simple",
                    examples = listOf(GrammarExample("Quand j’aurai terminé, je t’appellerai.", "我完成后会给你打电话。"), GrammarExample("Dès qu’ils seront arrivés, nous mangerons.", "他们一到，我们就吃饭。")),
                    tip = "先完成的动作适合 futur antérieur。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "48_1", prompt = "Quand j’___ fini, je partirai.", options = listOf("aurai", "ai", "avais"),
                    answers = listOf("aurai"), explanation = "先于另一个未来动作完成，用 futur antérieur。", chineseHint = "我完成后就会离开。",
                ),
                GrammarExercise(
                    id = "48_2", prompt = "Elle ___ partie avant midi.", options = listOf("sera", "est", "était"),
                    answers = listOf("sera"), explanation = "partir 用 être；futur antérieur 为 sera partie。", chineseHint = "她中午前就会离开。",
                ),
            ),
        ),
        GrammarLesson(
            number = 49, titleFrench = "Le conditionnel (2)", titleChinese = "条件式（二）：想象与非现实", category = "过去·未来·高级", bookPage = 230, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "想象、假设的结果", rule = "conditionnel présent 可表达非现实或不确定情景中的结果、愿望和想象。", formula = "conditionnel = résultat hypothétique",
                    examples = listOf(GrammarExample("À ta place, je partirais.", "如果我是你，我会离开。"), GrammarExample("Ce serait formidable.", "那会很棒。")),
                    tip = "和 si 从句搭配时，条件式通常放在结果句，不放在 si 后。",
                ),
                GrammarMemoryCard(
                    title = "conditionnel passé", rule = "avoir/être 的 conditionnel présent + 过去分词，可表达过去本可以/本会发生但未发生的事。", formula = "aurais/serais + participe passé",
                    examples = listOf(GrammarExample("J’aurais aimé venir.", "我本来很想来。"), GrammarExample("Elle serait partie plus tôt.", "她本来会更早离开。")),
                    tip = "后面第50课假设句会把它用起来。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "49_1", prompt = "À ta place, je ___ plus tôt. (partir)", options = listOf(),
                    answers = listOf("partirais"), explanation = "假设性建议用 conditionnel présent。", chineseHint = "如果我是你，我会早点走。",
                ),
                GrammarExercise(
                    id = "49_2", prompt = "J’___ aimé venir.", options = listOf("aurais", "avais", "ai"),
                    answers = listOf("aurais"), explanation = "conditionnel passé：j’aurais aimé。", chineseHint = "我本来很想来。",
                ),
            ),
        ),
        GrammarLesson(
            number = 50, titleFrench = "Les hypothèses", titleChinese = "假设句", category = "过去·未来·高级", bookPage = 232, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "真实可能：si + présent", rule = "si + présent 可搭配 présent、futur 或 impératif，表达真实/可能条件。", formula = "si + présent → présent/futur/impératif",
                    examples = listOf(GrammarExample("S’il pleut, je resterai chez moi.", "如果下雨，我会待在家。"), GrammarExample("Si tu as le temps, appelle-moi.", "如果你有时间，给我打电话。")),
                    tip = "法语里一般不说 si + futur。",
                ),
                GrammarMemoryCard(
                    title = "非现实现在/过去", rule = "si + imparfait → conditionnel présent；si + plus-que-parfait → conditionnel passé。", formula = "si imparfait → cond. présent · si PQP → cond. passé",
                    examples = listOf(GrammarExample("Si j’avais le temps, je voyagerais.", "如果我有时间，我就会旅行。"), GrammarExample("Si j’avais su, je serais venu.", "如果我早知道，我就来了。")),
                    tip = "三套结构按“现实程度和时间”整体记。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "50_1", prompt = "Si j’ai le temps, je ___ demain. (venir)", options = listOf(),
                    answers = listOf("viendrai"), explanation = "真实可能条件：si + présent，结果可用 futur。", chineseHint = "如果我有时间，我明天会来。",
                ),
                GrammarExercise(
                    id = "50_2", prompt = "Si j’avais le temps, je ___. (voyager)", options = listOf(),
                    answers = listOf("voyagerais"), explanation = "si + imparfait → conditionnel présent。", chineseHint = "如果我有时间，我会旅行。",
                ),
            ),
        ),
        GrammarLesson(
            number = 51, titleFrench = "Le subjonctif", titleChinese = "虚拟式", category = "过去·未来·高级", bookPage = 238, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "常见触发：必要、愿望、情感、怀疑", rule = "il faut que, vouloir que, être content que, douter que 等常引出 subjonctif。", formula = "trigger + que + subjonctif",
                    examples = listOf(GrammarExample("Il faut que tu viennes.", "你必须来。"), GrammarExample("Je veux qu’il fasse attention.", "我希望他注意。")),
                    tip = "先识别“触发结构”，再处理变位，比死背规则更有效。",
                ),
                GrammarMemoryCard(
                    title = "基本构成", rule = "很多动词取 ils 的 présent 词干去 -ent，加 -e, -es, -e, -ions, -iez, -ent；être/avoir/faire/aller 等高频不规则要单记。", formula = "ils stem + e/es/e/ions/iez/ent",
                    examples = listOf(GrammarExample("qu’ils parlent → que je parle", "他们说 → 我（虚拟式）说"), GrammarExample("être → sois/sois/soit/soyons/soyez/soient", "être 的虚拟式核心形式")),
                    tip = "对 TCF 来说，先掌握高频触发 + 10来个高频动词最划算。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "51_1", prompt = "Il faut que tu ___. (venir)", options = listOf(),
                    answers = listOf("viennes"), explanation = "venir 的 subjonctif：tu viennes。", chineseHint = "你必须来。",
                ),
                GrammarExercise(
                    id = "51_2", prompt = "Je veux qu’il ___ attention. (faire)", options = listOf(),
                    answers = listOf("fasse"), explanation = "faire 的 subjonctif：il fasse。", chineseHint = "我希望他注意。",
                ),
            ),
        ),
        GrammarLesson(
            number = 52, titleFrench = "Les relations logiques", titleChinese = "逻辑关系", category = "过去·未来·高级", bookPage = 250, bodyAvailableInUploadedPdf = false,
            cards = listOf(
                GrammarMemoryCard(
                    title = "原因、结果、目的", rule = "原因可用 parce que/puisque/car；结果可用 donc/alors/c’est pourquoi；目的常用 pour + infinitif 或 pour que + subjonctif。", formula = "cause → conséquence → but",
                    examples = listOf(GrammarExample("Je reste chez moi parce qu’il pleut.", "因为下雨，我待在家。"), GrammarExample("Je travaille pour réussir.", "我努力学习/工作以便成功。")),
                    tip = "逻辑连接词是 TCF 写作组织段落的骨架。",
                ),
                GrammarMemoryCard(
                    title = "对立与让步", rule = "mais 表简单转折，pourtant/cependant 强调对立结果，bien que + subjonctif 表让步。", formula = "mais · pourtant/cependant · bien que + subj.",
                    examples = listOf(GrammarExample("Il est fatigué, pourtant il continue.", "他很累，但仍继续。"), GrammarExample("Bien qu’il soit fatigué, il continue.", "尽管他很累，他仍继续。")),
                    tip = "同一逻辑可以有不同正式程度，写作时轮换使用。",
                ),
            ),
            exercises = listOf(
                GrammarExercise(
                    id = "52_1", prompt = "Je reste chez moi ___ il pleut.", options = listOf("parce qu’", "pour", "donc"),
                    answers = listOf("parce qu'", "parce qu’"), explanation = "这里需要引出原因从句，用 parce que。", chineseHint = "因为下雨，我待在家。",
                ),
                GrammarExercise(
                    id = "52_2", prompt = "___ il soit fatigué, il continue.", options = listOf("Bien qu’", "Parce qu’", "Donc"),
                    answers = listOf("Bien qu'", "Bien qu’"), explanation = "让步结构 bien que + subjonctif。", chineseHint = "尽管他很累，他仍继续。",
                ),
            ),
        ),
    )

    fun lesson(number: Int): GrammarLesson? = lessons.firstOrNull { it.number == number }

    /**
     * V3.8 deepens every lesson without inventing new grammar claims: the extra
     * checks are generated only from the lesson's own authored formulas, titles
     * and examples. Two original application exercises + two checks per memory
     * card gives at least six practice points for the current two-card lessons.
     */
    fun practiceExercises(lesson: GrammarLesson): List<GrammarExercise> {
        val formulaOptions = lesson.cards.map { it.formula }.filter { it.isNotBlank() }.distinct()
        val exampleOptions = lesson.cards.flatMap { it.examples }.map { it.french }.filter { it.isNotBlank() }.distinct()
        val generated = lesson.cards.flatMapIndexed { index, card ->
            val items = mutableListOf<GrammarExercise>()
            if (card.formula.isNotBlank() && formulaOptions.size >= 2) {
                items += GrammarExercise(
                    id = "auto_formula_$index",
                    prompt = "「${card.title}」最对应哪个核心结构？",
                    options = formulaOptions,
                    answers = listOf(card.formula),
                    explanation = card.rule,
                    chineseHint = "记忆锚点：${card.tip}",
                )
            }
            val example = card.examples.firstOrNull()
            if (example != null && exampleOptions.size >= 2) {
                items += GrammarExercise(
                    id = "auto_example_$index",
                    prompt = "下面哪一句最能体现「${card.title}」？",
                    options = exampleOptions,
                    answers = listOf(example.french),
                    explanation = "${card.rule}\n例句：${example.french} — ${example.chinese}",
                    chineseHint = card.tip,
                )
            }
            items
        }
        return lesson.exercises + generated
    }

    fun allCardIds(): List<String> = lessons.flatMap { lesson -> lesson.cards.indices.map { "grammar:memory:${lesson.number}:$it" } }
    fun allExerciseIds(): List<String> = lessons.flatMap { lesson -> practiceExercises(lesson).map { "grammar:practice:${lesson.number}:${it.id}" } }
}
