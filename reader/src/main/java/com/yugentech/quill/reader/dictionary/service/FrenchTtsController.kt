package com.yugentech.quill.reader.dictionary.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class FrenchTtsController(context: Context) : TextToSpeech.OnInitListener {

    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var pendingText: String? = null
    private var ready: Boolean = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return

        engine?.language = Locale.FRENCH
        pendingText?.let {
            pendingText = null
            speak(it)
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return

        val tts = engine ?: return
        if (!ready) {
            pendingText = clean
            return
        }

        tts.language = Locale.FRENCH
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "lirelia_french_word")
    }

    fun shutdown() {
        pendingText = null
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
