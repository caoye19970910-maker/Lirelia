package com.cy.languagereader.mobile.service

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object SentenceTranslationManager {
    data class Result(
        val text: String,
        val source: String,
        val error: String = "",
    )

    fun translate(
        context: Context,
        sentence: String,
        allowNetwork: Boolean,
    ): Result {
        val clean = sentence.trim()
        if (clean.isBlank()) return Result("", "", "句子为空")

        val prefs = context.getSharedPreferences("sentence_translation_cache", Context.MODE_PRIVATE)
        val key = "v52_google_fr_zh_${hash(clean)}"
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }?.let {
            val source = prefs.getString("${key}_source", "Google 翻译 · 缓存").orEmpty().ifBlank { "翻译缓存" }
            return Result(it, source)
        }

        if (!allowNetwork) {
            return Result("", "", "安全模式下未启用联网翻译")
        }

        // V5.2: sentence translation is explicitly Google-first. Pronunciation routing is
        // unrelated to translation quality, so it no longer changes this provider order.
        // MyMemory remains only as a fallback when Google is temporarily unreachable.
        val providers: List<Pair<String, () -> String>> = listOf(
            "Google 翻译" to { googleTranslate(clean) },
            "MyMemory 备用" to { myMemoryTranslate(clean) },
        )

        for ((label, provider) in providers) {
            val translated = runCatching { provider() }.getOrNull()?.trim().orEmpty()
            if (translated.isNotBlank()) {
                prefs.edit()
                    .putString(key, translated)
                    .putString("${key}_source", "$label · 缓存")
                    .apply()
                return Result(translated, label)
            }
        }

        return Result("", "", "网络翻译暂不可用")
    }

    private fun googleTranslate(text: String): String {
        val q = URLEncoder.encode(text, "UTF-8")
        val conn = (URL(
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=fr&tl=zh-CN&dt=t&q=$q"
        ).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_500
            readTimeout = 6_500
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONArray(raw)
            val parts = root.optJSONArray(0) ?: return ""
            return buildString {
                for (i in 0 until parts.length()) {
                    val item = parts.optJSONArray(i) ?: continue
                    append(item.optString(0))
                }
            }.trim()
        } finally {
            conn.disconnect()
        }
    }

    private fun myMemoryTranslate(text: String): String {
        val q = URLEncoder.encode(text.take(480), "UTF-8")
        val conn = (URL(
            "https://api.mymemory.translated.net/get?q=$q&langpair=fr%7Czh-CN"
        ).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_500
            readTimeout = 6_500
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return JSONObject(raw)
                .optJSONObject("responseData")
                ?.optString("translatedText")
                .orEmpty()
        } finally {
            conn.disconnect()
        }
    }

    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
}
