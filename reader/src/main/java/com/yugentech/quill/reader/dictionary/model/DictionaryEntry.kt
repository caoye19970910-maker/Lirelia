package com.yugentech.quill.reader.dictionary.model

data class DictionaryEntry(
    val headword: String,
    val ipa: String? = null,
    val partOfSpeech: String? = null,
    val meanings: List<String>,
    val example: String? = null,
    val exampleTranslation: String? = null
)
