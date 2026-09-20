package com.yugentech.quill.reader.dictionary.repository

import com.yugentech.quill.reader.dictionary.model.DictionaryEntry
import java.util.Locale

/**
 * Tiny bootstrap lexicon used to validate Lirelia's dictionary interaction.
 *
 * This is deliberately separated from the UI so the map can later be replaced
 * by the real bundled French-Chinese dictionary without changing ReaderScreen.
 */
object LocalFrenchDictionary {

    private val entries = mapOf(
        "livre" to DictionaryEntry(
            headword = "livre",
            ipa = "/livʁ/",
            partOfSpeech = "nom masculin",
            meanings = listOf("书；书籍"),
            example = "Je lis un livre.",
            exampleTranslation = "我在读一本书。"
        ),
        "manger" to DictionaryEntry(
            headword = "manger",
            ipa = "/mɑ̃.ʒe/",
            partOfSpeech = "verbe",
            meanings = listOf("吃；进食"),
            example = "Nous allons manger ensemble.",
            exampleTranslation = "我们要一起吃饭。"
        ),
        "devenir" to DictionaryEntry(
            headword = "devenir",
            ipa = "/də.və.niʁ/",
            partOfSpeech = "verbe",
            meanings = listOf("成为；变成"),
            example = "Il veut devenir professeur.",
            exampleTranslation = "他想成为老师。"
        ),
        "intéressant" to DictionaryEntry(
            headword = "intéressant",
            ipa = "/ɛ̃.te.ʁe.sɑ̃/",
            partOfSpeech = "adjectif",
            meanings = listOf("有趣的；值得关注的"),
            example = "Ce livre est très intéressant.",
            exampleTranslation = "这本书很有趣。"
        ),
        "être" to DictionaryEntry(
            headword = "être",
            ipa = "/ɛtʁ/",
            partOfSpeech = "verbe",
            meanings = listOf("是；存在；处于"),
            example = "Je suis professeur.",
            exampleTranslation = "我是老师。"
        ),
        "avoir" to DictionaryEntry(
            headword = "avoir",
            ipa = "/a.vwaʁ/",
            partOfSpeech = "verbe",
            meanings = listOf("有；拥有"),
            example = "J'ai un livre.",
            exampleTranslation = "我有一本书。"
        )
    )

    fun normalize(raw: String): String =
        raw.trim()
            .lowercase(Locale.FRENCH)
            .replace('’', '\'')
            .trim { ch -> !ch.isLetter() && ch != '\'' && ch != '-' }

    fun lookup(raw: String): DictionaryEntry? {
        val token = normalize(raw)
        return entries[token]
    }
}
