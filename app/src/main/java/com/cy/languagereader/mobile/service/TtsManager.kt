package com.cy.languagereader.mobile.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var pendingText: String? = null
    @Volatile private var initFinished = false
    @Volatile private var networkFallbackEnabled = true
    @Volatile private var persistentCacheEnabled = true
    @Volatile private var pronunciationRoute = "AUTO"
    @Volatile private var failureFallbackEnabled = true
    @Volatile private var activeVoiceName = ""
    @Volatile private var activeVoiceRequiresNetwork = false
    @Volatile private var activeEnginePackage = ""
    @Volatile private var frenchVoiceCount = 0
    @Volatile private var installedEnginePackages: List<String> = emptyList()
    private val triedEnginePackages = linkedSetOf<String>()
    @Volatile private var lastEvent = "尚未发音"
    @Volatile private var lastSource = ""
    @Volatile private var lastError = ""

    private var mediaPlayer: MediaPlayer? = null
    private var requestSerial = 0L

    private val cacheDir = File(appContext.filesDir, "pronunciation_cache").apply { mkdirs() }
    private val tempDir = File(appContext.cacheDir, "language_reader_tts").apply { mkdirs() }

    private data class AudioCandidate(
        val label: String,
        val url: String,
    )

    fun configure(
        networkFallbackEnabled: Boolean,
        persistentCacheEnabled: Boolean,
        pronunciationRoute: String = "AUTO",
        fallbackEnabled: Boolean = true,
    ) {
        this.networkFallbackEnabled = networkFallbackEnabled
        this.persistentCacheEnabled = persistentCacheEnabled
        this.pronunciationRoute = pronunciationRoute.uppercase(Locale.US).let {
            if (it in setOf("AUTO", "MAINLAND", "GOOGLE", "OFFLINE")) it else "AUTO"
        }
        this.failureFallbackEnabled = fallbackEnabled
    }

    fun prepare() {
        if (tts != null) return
        triedEnginePackages.clear()
        startEngine(null)
    }

    fun refreshEngineDiscovery() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        initFinished = false
        activeEnginePackage = ""
        activeVoiceName = ""
        activeVoiceRequiresNetwork = false
        frenchVoiceCount = 0
        installedEnginePackages = emptyList()
        triedEnginePackages.clear()
        prepare()
    }

    private fun startEngine(packageName: String?) {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        initFinished = false
        activeVoiceName = ""
        activeVoiceRequiresNetwork = false

        if (packageName != null) {
            activeEnginePackage = packageName
            triedEnginePackages += packageName
        } else {
            activeEnginePackage = ""
        }

        runCatching {
            val listener = TextToSpeech.OnInitListener { status ->
                initFinished = true
                main.post { configureAfterInit(status) }
            }
            tts = if (packageName.isNullOrBlank()) {
                TextToSpeech(appContext, listener)
            } else {
                TextToSpeech(appContext, listener, packageName)
            }
            lastEvent = if (packageName.isNullOrBlank()) {
                "正在初始化系统默认 TTS"
            } else {
                "正在尝试 TTS 引擎：$packageName"
            }
        }.onFailure {
            lastError = it.message ?: "TTS 初始化失败"
            lastEvent = "TTS 引擎初始化失败"
            tryNextInstalledEngine()
        }
    }

    override fun onInit(status: Int) {
        // Kept for interface compatibility; startEngine() uses its own listener
        // so we know which engine instance is being initialized.
        initFinished = true
        main.post { configureAfterInit(status) }
    }

    private fun preferredEngineOrder(packages: List<String>): List<String> {
        val knownOffline = listOf(
            "eu.appsuite.espeakng",
            "com.reecedunn.espeak",
            "com.redzoc.ramees.tts.espeak",
        )
        return buildList {
            knownOffline.filter { it in packages }.forEach { add(it) }
            packages
                .filter { it !in knownOffline }
                .filterNot { it.contains("google", ignoreCase = true) }
                .forEach { add(it) }
            packages
                .filter { it.contains("google", ignoreCase = true) }
                .forEach { add(it) }
        }.distinct()
    }

    private fun tryNextInstalledEngine(): Boolean {
        val next = preferredEngineOrder(installedEnginePackages)
            .firstOrNull { it !in triedEnginePackages && it != activeEnginePackage }
            ?: return false

        lastEvent = "系统默认法语不可用，尝试其它 TTS：$next"
        startEngine(next)
        return true
    }

    private fun configureAfterInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            ready = false
            lastEvent = "当前 TTS 引擎不可用"
            lastError = "onInit=$status"
            if (!tryNextInstalledEngine()) {
                pendingText?.let {
                    pendingText = null
                    speakFallback(it)
                }
            }
            return
        }

        installedEnginePackages = runCatching {
            engine.engines.map { it.name }.distinct()
        }.getOrDefault(installedEnginePackages)

        if (activeEnginePackage.isBlank()) {
            activeEnginePackage = runCatching { engine.defaultEngine }.getOrNull().orEmpty()
            if (activeEnginePackage.isNotBlank()) triedEnginePackages += activeEnginePackage
        }

        // In Mainland mode never intentionally stay on Google TTS when a
        // non-Google Android engine is installed. This keeps the route fast and
        // predictable on Huawei/China-network devices.
        if (pronunciationRoute == "MAINLAND" && activeEnginePackage.contains("google", ignoreCase = true)) {
            val mainlandEngine = installedEnginePackages
                .firstOrNull { !it.contains("google", ignoreCase = true) && it != activeEnginePackage }
            if (mainlandEngine != null && mainlandEngine !in triedEnginePackages) {
                lastEvent = "中国大陆模式：切换到非 Google 系统 TTS"
                startEngine(mainlandEngine)
                return
            } else {
                ready = false
                lastEvent = "中国大陆模式：未发现非 Google 法语 TTS"
                lastError = "Installed TTS engines contain no usable non-Google engine"
                pendingText?.let { pending ->
                    pendingText = null
                    speakFallback(pending)
                }
                return
            }
        }

        val languageResult = runCatching { engine.setLanguage(Locale.FRANCE) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)

        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            ready = false
            lastEvent = "当前 TTS 没有法语语音"
            lastError = "French language unsupported/missing"
            if (!tryNextInstalledEngine()) {
                pendingText?.let {
                    pendingText = null
                    speakFallback(it)
                }
            }
            return
        }

        runCatching {
            val voices = engine.voices
                ?.filter { it.locale?.language.equals("fr", ignoreCase = true) }
                ?.sortedWith(
                    compareBy(
                        { it.isNetworkConnectionRequired },
                        { if (it.locale == Locale.FRANCE) 0 else 1 },
                    )
                )
                .orEmpty()

            frenchVoiceCount = voices.size

            val offlineVoice = voices.firstOrNull { !it.isNetworkConnectionRequired }
            val knownOfflineInstalled = preferredEngineOrder(installedEnginePackages)
                .firstOrNull {
                    it in KNOWN_OFFLINE_ENGINE_PACKAGES &&
                        it !in triedEnginePackages &&
                        it != activeEnginePackage
                }

            // If the current engine only offers a network French voice but an
            // offline eSpeak engine is installed, prefer the offline engine.
            if (offlineVoice == null && knownOfflineInstalled != null) {
                lastEvent = "检测到离线法语 TTS，优先切换"
                startEngine(knownOfflineInstalled)
                return
            }

            if (pronunciationRoute == "OFFLINE" && offlineVoice == null) {
                ready = false
                lastEvent = "离线模式：当前引擎没有离线法语 voice"
                lastError = "No offline French voice in current Android TTS engine"
                return
            }

            (offlineVoice ?: voices.firstOrNull())?.let {
                engine.voice = it
                activeVoiceName = it.name.orEmpty()
                activeVoiceRequiresNetwork = it.isNetworkConnectionRequired
            }

            engine.setSpeechRate(0.88f)
            engine.setPitch(1.0f)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    lastEvent = "系统 TTS 正在播放"
                    lastSource = if (activeEnginePackage.isBlank()) {
                        "Android 法语 TTS"
                    } else {
                        "Android TTS · $activeEnginePackage"
                    }
                    lastError = ""
                }

                override fun onDone(utteranceId: String?) {
                    lastEvent = "系统 TTS 播放完成"
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    lastError = "系统 TTS 播放错误"
                    lastEvent = "系统 TTS 播放失败"
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    lastError = "系统 TTS errorCode=$errorCode"
                    lastEvent = "系统 TTS 播放失败"
                }
            })

            ready = true
            lastEvent = if (activeEnginePackage in KNOWN_OFFLINE_ENGINE_PACKAGES) {
                "离线法语 TTS 已就绪"
            } else {
                "系统法语 TTS 已就绪"
            }
            lastError = ""
        }.onFailure {
            ready = false
            lastEvent = "系统法语 TTS 配置失败"
            lastError = it.message ?: "配置失败"
            if (!tryNextInstalledEngine()) {
                pendingText?.let {
                    pendingText = null
                    speakFallback(it)
                }
            }
            return
        }

        pendingText?.takeIf { it.isNotBlank() }?.let {
            pendingText = null
            speak(it)
        }
    }

    @Synchronized
    private fun ensureEngine(text: String?) {
        if (tts != null) return
        pendingText = text
        prepare()
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return

        requestSerial += 1
        stopNetworkAudio()
        lastEvent = "准备发音：${clean.take(40)}"
        lastError = ""

        // Successful downloaded audio is reused without network on later taps.
        if (persistentCacheEnabled) {
            findCachedAudio(clean)?.let { cached ->
                lastSource = "离线发音缓存"
                playLocalFile(
                    file = cached,
                    label = "离线发音缓存",
                    serial = requestSerial,
                    deleteAfter = false,
                    onFailure = {
                        runCatching { cached.delete() }
                        speakAfterCacheMiss(clean)
                    },
                )
                return
            }
        }

        speakAfterCacheMiss(clean)
    }

    private fun speakAfterCacheMiss(text: String) {
        if (!networkFallbackEnabled || pronunciationRoute == "OFFLINE") {
            if (ready) {
                speakSystem(text)
            } else if (tts == null && !initFinished) {
                ensureEngine(text)
            } else {
                lastEvent = "离线法语 TTS 不可用"
                lastError = "当前设备没有可用的法语系统语音"
                showMessage("当前设备没有可用的离线法语语音。")
            }
            return
        }

        when (pronunciationRoute) {
            // AUTO is deliberately conservative: reuse a working Android French
            // voice first, then try Mainland-friendly web pronunciation. Google
            // network audio is the final fallback, not the first thing a tap waits on.
            "AUTO" -> if (ready && !activeVoiceRequiresNetwork) speakSystem(text) else speakMainlandNetwork(text)
            "MAINLAND" -> speakMainlandNetwork(text)
            "GOOGLE" -> speakGoogleNetwork(text)
            else -> if (ready) speakSystem(text) else speakMainlandNetwork(text)
        }
    }

    private fun speakSystem(text: String) {
        val engine = tts ?: run {
            speakFallback(text)
            return
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "lr_${System.nanoTime()}")
        }.getOrDefault(TextToSpeech.ERROR)

        if (result == TextToSpeech.ERROR) {
            // Do not keep retrying a system engine that has just rejected speech;
            // network fallbacks must be able to exhaust without bouncing back here.
            ready = false
            lastError = "TextToSpeech.speak 返回 ERROR"
            when {
                pronunciationRoute == "AUTO" && failureFallbackEnabled && networkFallbackEnabled -> speakMainlandNetwork(text)
                pronunciationRoute == "GOOGLE" && failureFallbackEnabled && networkFallbackEnabled -> speakGoogleNetwork(text)
                pronunciationRoute == "MAINLAND" && failureFallbackEnabled && networkFallbackEnabled -> speakMainlandNetwork(text)
                else -> lastEvent = "系统法语 TTS 播放失败"
            }
        }
    }

    private fun speakFallback(text: String) {
        when {
            pronunciationRoute == "AUTO" && networkFallbackEnabled -> speakMainlandNetwork(text)
            pronunciationRoute == "MAINLAND" && networkFallbackEnabled -> speakMainlandNetwork(text)
            pronunciationRoute == "GOOGLE" && networkFallbackEnabled && failureFallbackEnabled -> speakGoogleNetwork(text)
            else -> {
                lastEvent = "备用发音已关闭"
                lastError = "当前发音路线没有可用语音"
                showMessage("当前发音路线没有可用语音。")
            }
        }
    }

    private fun speakMainlandNetwork(text: String) {
        val clean = text.trim().take(240)
        if (clean.isBlank()) return
        val serial = requestSerial

        if (!networkFallbackEnabled) {
            if (ready) speakSystem(clean) else showMessage("联网发音已关闭，且系统法语 TTS 不可用。")
            return
        }

        lastEvent = "正在连接国内法语发音"
        playCandidates(
            originalText = clean,
            candidates = buildMainlandCandidates(clean).let { if (failureFallbackEnabled) it else it.take(1) },
            index = 0,
            serial = serial,
            onExhausted = {
                when {
                    failureFallbackEnabled && ready -> {
                        lastEvent = "国内在线发音失败，切换系统法语 TTS"
                        speakSystem(clean)
                    }
                    pronunciationRoute == "AUTO" && failureFallbackEnabled -> {
                        lastEvent = "国内发音失败，AUTO 最后尝试 Google 网络发音"
                        speakGoogleNetwork(clean)
                    }
                    else -> {
                        lastEvent = "中国大陆发音路线不可用"
                        lastError = "国内网页发音与系统法语 TTS 均不可用"
                        showMessage("国内法语发音暂不可用。")
                    }
                }
            },
        )
    }

    private fun buildMainlandCandidates(text: String): List<AudioCandidate> {
        val encoded = URLEncoder.encode(text, "UTF-8")
        return listOf(
            AudioCandidate(
                "有道法语发音",
                "https://dict.youdao.com/dictvoice?audio=$encoded&le=fr&type=2",
            ),
            AudioCandidate(
                "百度法语发音备用",
                "https://fanyi.baidu.com/gettts?lan=fra&text=$encoded&spd=3&source=web",
            ),
        )
    }

    private fun speakGoogleNetwork(text: String) {
        val clean = text.trim().take(240)
        if (clean.isBlank()) return
        val serial = requestSerial

        if (!networkFallbackEnabled) {
            lastEvent = "联网备用发音已关闭"
            lastError = "没有系统法语 TTS，且联网备用发音被关闭"
            showMessage("当前没有法语系统语音；联网备用发音已关闭。")
            return
        }

        lastEvent = "正在连接 Google 法语发音"
        // Speed first: start the provider that already works on the phone immediately.
        // Do not wait several seconds for the public dictionary API before TTS begins.
        playCandidates(
            clean,
            buildCandidates(clean).let { if (failureFallbackEnabled) it else it.take(1) },
            0,
            serial,
        )
    }

    private fun isSingleWord(text: String): Boolean =
        text.matches(Regex("[A-Za-zÀ-ÖØ-öø-ÿŒœÆæ'’-]+"))

    private fun buildCandidates(text: String): List<AudioCandidate> {
        val encoded = URLEncoder.encode(text, "UTF-8")
        return buildList {
            add(
                AudioCandidate(
                    "Google 法语语音",
                    "https://translate.googleapis.com/translate_tts?ie=UTF-8&client=gtx&tl=fr&q=$encoded",
                )
            )
            add(
                AudioCandidate(
                    "Google 法语语音备用",
                    "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=fr&q=$encoded",
                )
            )
        }
    }

    private fun playCandidates(
        originalText: String,
        candidates: List<AudioCandidate>,
        index: Int,
        serial: Long,
        onExhausted: (() -> Unit)? = null,
    ) {
        if (serial != requestSerial) return
        if (index >= candidates.size) {
            if (onExhausted != null) {
                onExhausted()
            } else {
                lastEvent = "所有备用发音源失败"
                if (lastError.isBlank()) lastError = "没有可播放的备用音频"
                showMessage("所有备用发音源都失败了。诊断中心可以查看具体状态。")
            }
            return
        }

        val candidate = candidates[index]
        lastEvent = "尝试：${candidate.label}"

        Thread {
            val file = downloadCandidate(originalText, candidate, serial)
            main.post {
                if (serial != requestSerial) {
                    if (!persistentCacheEnabled) runCatching { file?.delete() }
                    return@post
                }

                if (file == null) {
                    playCandidates(originalText, candidates, index + 1, serial, onExhausted)
                } else {
                    playLocalFile(
                        file = file,
                        label = candidate.label,
                        serial = serial,
                        deleteAfter = !persistentCacheEnabled,
                        onFailure = {
                            if (persistentCacheEnabled) runCatching { file.delete() }
                            playCandidates(originalText, candidates, index + 1, serial, onExhausted)
                        },
                    )
                }
            }
        }.start()
    }

    private fun downloadCandidate(
        originalText: String,
        candidate: AudioCandidate,
        serial: Long,
    ): File? = runCatching {
        if (serial != requestSerial) return@runCatching null

        val connection = (URL(candidate.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = if (candidate.label.contains("有道") || candidate.label.contains("百度")) 2200 else 3500
            readTimeout = if (candidate.label.contains("有道") || candidate.label.contains("百度")) 3200 else 5000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "audio/mpeg,audio/ogg,audio/wav,audio/*;q=0.9,*/*;q=0.7")
            when {
                candidate.label.contains("有道") -> setRequestProperty("Referer", "https://dict.youdao.com/")
                candidate.label.contains("百度") -> setRequestProperty("Referer", "https://fanyi.baidu.com/")
            }
        }

        try {
            if (connection.responseCode !in 200..299) {
                lastError = "${candidate.label} HTTP ${connection.responseCode}"
                return@runCatching null
            }

            val contentType = connection.contentType.orEmpty().lowercase()
            if (
                contentType.startsWith("text/") ||
                "json" in contentType ||
                "html" in contentType
            ) {
                lastError = "${candidate.label} 返回的不是音频：$contentType"
                return@runCatching null
            }

            val extension = when {
                "ogg" in contentType || candidate.url.contains(".ogg", true) -> "ogg"
                "wav" in contentType || candidate.url.contains(".wav", true) -> "wav"
                else -> "mp3"
            }

            val hash = textHash(originalText)
            val file = if (persistentCacheEnabled) {
                File(cacheDir, "$hash.$extension")
            } else {
                File(tempDir, "tts_${serial}_${System.nanoTime()}.$extension")
            }

            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        if (serial != requestSerial) return@runCatching null
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > 4_000_000L) {
                            lastError = "${candidate.label} 音频过大"
                            return@runCatching null
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            if (total < 256L || !file.exists()) {
                runCatching { file.delete() }
                lastError = "${candidate.label} 下载内容过小（$total bytes）"
                null
            } else if (looksLikeTextResponse(file)) {
                runCatching { file.delete() }
                lastError = "${candidate.label} 返回 HTML/JSON，不是可播放音频"
                null
            } else {
                lastEvent = "${candidate.label} 已下载 ${total / 1024} KB"
                file
            }
        } finally {
            connection.disconnect()
        }
    }.getOrElse {
        lastError = "${candidate.label}：${it.message ?: it.javaClass.simpleName}"
        null
    }

    private fun looksLikeTextResponse(file: File): Boolean = runCatching {
        val bytes = ByteArray(24)
        val count = file.inputStream().use { it.read(bytes) }
        if (count <= 0) return@runCatching true
        val head = String(bytes, 0, count, Charsets.UTF_8).trimStart().lowercase()
        head.startsWith("<!doctype") ||
            head.startsWith("<html") ||
            head.startsWith("{") ||
            head.startsWith("[")
    }.getOrDefault(false)

    private fun playLocalFile(
        file: File,
        label: String,
        serial: Long,
        deleteAfter: Boolean,
        onFailure: () -> Unit,
    ) {
        stopNetworkAudio()

        val player = MediaPlayer()
        mediaPlayer = player

        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                if (serial == requestSerial) {
                    lastSource = label
                    lastEvent = "正在播放：$label"
                    lastError = ""
                    it.start()
                } else {
                    runCatching { it.release() }
                }
            }
            player.setOnCompletionListener {
                lastEvent = "$label 播放完成"
                runCatching { it.release() }
                if (deleteAfter) runCatching { file.delete() }
                if (mediaPlayer === it) mediaPlayer = null
            }
            player.setOnErrorListener { mp, what, extra ->
                lastEvent = "$label 播放失败"
                lastError = "MediaPlayer what=$what extra=$extra"
                runCatching { mp.release() }
                if (deleteAfter) runCatching { file.delete() }
                if (mediaPlayer === mp) mediaPlayer = null
                if (serial == requestSerial) onFailure()
                true
            }
            player.prepareAsync()
        }.onFailure {
            lastEvent = "$label 播放异常"
            lastError = it.message ?: it.javaClass.simpleName
            runCatching { player.release() }
            if (deleteAfter) runCatching { file.delete() }
            if (mediaPlayer === player) mediaPlayer = null
            if (serial == requestSerial) onFailure()
        }
    }

    private fun textHash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(text.trim().lowercase(Locale.FRENCH).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun findCachedAudio(text: String): File? {
        val prefix = textHash(text)
        return cacheDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith(prefix) && it.length() > 256L }
    }

    fun clearPronunciationCache(): Int {
        val files = cacheDir.listFiles().orEmpty()
        var deleted = 0
        files.forEach {
            if (runCatching { it.delete() }.getOrDefault(false)) deleted++
        }
        return deleted
    }

    fun offlineFrenchEngineStatus(): String {
        val installedKnown = KNOWN_OFFLINE_ENGINE_PACKAGES
            .filter { pkg ->
                runCatching {
                    appContext.packageManager.getPackageInfo(pkg, 0)
                }.isSuccess
            }

        return when {
            activeEnginePackage in KNOWN_OFFLINE_ENGINE_PACKAGES ->
                "离线法语引擎正在使用：$activeEnginePackage"
            installedKnown.isNotEmpty() ->
                "已安装离线引擎：${installedKnown.joinToString()}（下次初始化会优先尝试）"
            else ->
                "未检测到离线法语 TTS。中国大陆环境建议安装 eSpeak NG。"
        }
    }

    fun openOfflineFrenchTtsDownload() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://f-droid.org/zh_Hans/packages/com.reecedunn.espeak/")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    fun routeLabel(): String = when {
        !networkFallbackEnabled -> "离线 / 安全模式"
        pronunciationRoute == "AUTO" -> "自动 · 缓存 → 系统法语 → 有道/百度 → Google 备用"
        pronunciationRoute == "MAINLAND" -> "中国大陆 · 缓存 → 有道/百度 → 系统 TTS"
        pronunciationRoute == "OFFLINE" -> "离线 · Android TTS"
        else -> "国际网络 · Google 法语"
    }

    fun diagnosticsReport(): String {
        val files = cacheDir.listFiles()?.filter { it.isFile }.orEmpty()
        val bytes = files.sumOf { it.length() }
        return buildString {
            appendLine("系统 TTS 初始化：${if (initFinished) "已完成" else "未完成"}")
            appendLine("系统法语 TTS：${if (ready) "可用" else "不可用"}")
            appendLine("当前 TTS 引擎：${activeEnginePackage.ifBlank { "系统默认/未知" }}")
            appendLine("已发现 TTS 引擎：${installedEnginePackages.joinToString().ifBlank { "—" }}")
            appendLine("离线法语状态：${offlineFrenchEngineStatus()}")
            appendLine("法语 voice 数量：$frenchVoiceCount")
            appendLine("当前 voice：${activeVoiceName.ifBlank { "—" }}")
            appendLine("当前 voice 需要联网：${if (activeVoiceRequiresNetwork) "是" else "否"}")
            appendLine("发音路线：${routeLabel()}")
            appendLine("联网发音允许：${if (networkFallbackEnabled) "是" else "否"}")
            appendLine("失败后备用切换：${if (failureFallbackEnabled) "开启" else "关闭"}")
            appendLine("发音缓存：${if (persistentCacheEnabled) "开启" else "关闭"} · ${files.size} 个 · ${"%.1f".format(Locale.US, bytes / 1024.0 / 1024.0)} MB")
            appendLine("最近来源：${lastSource.ifBlank { "—" }}")
            appendLine("最近状态：$lastEvent")
            appendLine("最近错误：${lastError.ifBlank { "—" }}")
        }.trim()
    }

    fun openTtsSettings() {
        val intents = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        val intent = intents.firstOrNull {
            it.resolveActivity(appContext.packageManager) != null
        } ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    private fun stopNetworkAudio() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    private fun showMessage(message: String) {
        main.post { Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show() }
    }

    fun close() {
        ready = false
        pendingText = null
        initFinished = false
        requestSerial += 1
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        stopNetworkAudio()
    }

    companion object {
        private val KNOWN_OFFLINE_ENGINE_PACKAGES = setOf(
            "eu.appsuite.espeakng",
            "com.reecedunn.espeak",
            "com.redzoc.ramees.tts.espeak",
        )

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
    }
}
