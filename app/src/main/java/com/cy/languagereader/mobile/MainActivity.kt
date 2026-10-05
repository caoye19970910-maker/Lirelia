package com.cy.languagereader.mobile

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ScrollView
import android.widget.TextView
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cy.languagereader.mobile.data.BookRepository
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.service.TtsManager
import com.cy.languagereader.mobile.ui.LanguageReaderApp

class MainActivity : ComponentActivity() {
    private var tts: TtsManager? = null
    private var dictionaryRepository: DictionaryRepository? = null
    private var learningStore: LearningStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLightSystemBars()

        try {
            // Keep startup deliberately light. Heavy modules (PDFBox/TTS/dictionary extraction)
            // are initialized only when the corresponding feature is used.
            val books = BookRepository(this)
            val dictionary = DictionaryRepository(this).also { dictionaryRepository = it }
            val learning = LearningStore(this).also { learningStore = it }
            val speech = TtsManager(this)
            tts = speech

            setContent {
                LanguageReaderApp(
                    bookRepository = books,
                    dictionary = dictionary,
                    learningStore = learning,
                    tts = speech,
                )
            }
        } catch (t: Throwable) {
            showStartupError(t)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLightSystemBars() {
        val paper = Color.rgb(247, 243, 234)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        val lightFlags =
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0) or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or lightFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.systemBars())
                controller.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            }
        }
    }

    private fun showStartupError(t: Throwable) {
        val title = TextView(this).apply {
            text = buildString {
                append("Lirelia 启动失败\n\n")
                append(t::class.java.simpleName)
                append(": ")
                append(t.message ?: "未知错误")
                append("\n\n把这个页面截图发给 ChatGPT，我会直接修复。\n\n")
                append("详细信息：\n")
                append(t.stackTraceToString().take(7000))
            }
            setTextColor(Color.rgb(45, 42, 38))
            setBackgroundColor(Color.rgb(247, 243, 234))
            textSize = 15f
            gravity = Gravity.START
            setPadding(36, 48, 36, 48)
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(title) })
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    ReaderKeyBridge.onVolumeUp?.let {
                        it.invoke()
                        return true
                    }
                }

                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    ReaderKeyBridge.onVolumeDown?.let {
                        it.invoke()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        // Let Activity/Compose tear down UI scopes first so no cancelled UI job can
        // race a database close during configuration changes or reader exit.
        super.onDestroy()
        tts?.close()
        tts = null
        dictionaryRepository?.close()
        dictionaryRepository = null
        learningStore?.close()
        learningStore = null
    }
}
