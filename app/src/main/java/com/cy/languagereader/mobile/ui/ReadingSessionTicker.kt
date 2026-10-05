package com.cy.languagereader.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.cy.languagereader.mobile.data.AppSettings
import com.cy.languagereader.mobile.data.ReadingStatsStore
import kotlinx.coroutines.delay

/**
 * Counts reading time only while the host Activity is RESUMED.
 * Disk-backed stats are flushed in small batches instead of once per second.
 */
@Composable
internal fun rememberReadingSessionSeconds(
    bookId: String,
    settings: AppSettings,
    readingStats: ReadingStatsStore,
): Long {
    val activity = LocalContext.current.findActivity()
    val owner = activity as? LifecycleOwner
    var sessionSeconds by remember(bookId) { mutableLongStateOf(0L) }
    var pendingSeconds by remember(bookId) { mutableLongStateOf(0L) }
    var resumed by remember(owner) {
        mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true)
    }

    fun flush() {
        val seconds = pendingSeconds
        if (seconds <= 0L) return
        pendingSeconds = 0L
        settings.readerSessionSeconds = settings.readerSessionSeconds + seconds
        readingStats.addSeconds(bookId, seconds)
    }

    DisposableEffect(owner, bookId) {
        if (owner == null) {
            resumed = true
            onDispose { flush() }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> resumed = true
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP -> {
                        resumed = false
                        flush()
                    }
                    else -> Unit
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
                flush()
            }
        }
    }

    LaunchedEffect(bookId, resumed) {
        if (!resumed) return@LaunchedEffect
        while (true) {
            delay(1_000L)
            sessionSeconds += 1L
            pendingSeconds += 1L
            if (pendingSeconds >= 15L) flush()
        }
    }

    return sessionSeconds
}
