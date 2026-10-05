package com.cy.languagereader.mobile.readium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.BookRepository
import com.cy.languagereader.mobile.data.DictionaryRepository
import com.cy.languagereader.mobile.data.LearningStore
import com.cy.languagereader.mobile.service.TtsManager

class ReadiumReaderActivity : FragmentActivity() {
    private var tts: TtsManager? = null
    private var dictionaryRepository: DictionaryRepository? = null
    private var learningStore: LearningStore? = null
    private var pageBackward: (() -> Unit)? = null
    private var pageForward: (() -> Unit)? = null
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        // Readium's FragmentFactory depends on an asynchronously opened Publication.
        // We persist the locator ourselves and intentionally don't restore an old fragment tree.
        super.onCreate(null)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        val repository = BookRepository(this)
        val book = repository.listBooks().firstOrNull { it.id == bookId }
        if (book == null) {
            finish()
            return
        }

        settings = AppSettings(this)
        val dictionary = DictionaryRepository(this).also { dictionaryRepository = it }
        val learning = LearningStore(this).also { learningStore = it }
        val speech = TtsManager(this)
        tts = speech
        speech.configure(
            networkFallbackEnabled = !settings.safeMode && settings.pronunciationRoute != "OFFLINE",
            persistentCacheEnabled = settings.pronunciationCacheEnabled,
            pronunciationRoute = settings.pronunciationRoute,
            fallbackEnabled = settings.pronunciationFallbackEnabled,
        )
        speech.prepare()

        setContent {
            ReadiumReaderScreen(
                activity = this,
                book = book,
                bookRepository = repository,
                dictionary = dictionary,
                learningStore = learning,
                tts = speech,
                settings = settings,
                onBack = { finish() },
                onPageCallbacks = { backward, forward ->
                    pageBackward = backward
                    pageForward = forward
                },
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            ::settings.isInitialized &&
            settings.readerVolumeKeyTurnsPage &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    pageBackward?.invoke()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    pageForward?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        // Dispose Compose/Fragment scopes before closing repositories they may still
        // reference from cancellation/finalization callbacks.
        super.onDestroy()
        tts?.close()
        tts = null
        dictionaryRepository?.close()
        dictionaryRepository = null
        learningStore?.close()
        learningStore = null
        pageBackward = null
        pageForward = null
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"

        fun intent(context: Context, bookId: String): Intent =
            Intent(context, ReadiumReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
    }
}
