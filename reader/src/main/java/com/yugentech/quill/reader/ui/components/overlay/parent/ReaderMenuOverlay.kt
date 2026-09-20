package com.yugentech.quill.reader.ui.components.overlay.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.ReaderBottomControls
import com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button.SoundToggleButton
import com.yugentech.quill.reader.ui.components.overlay.components.topBar.ReaderTopBar
import kotlin.math.roundToInt

/**
 * Minimal local reader chrome for Lirelia.
 *
 * The upstream AI peek bar and subscription-aware controls are deliberately
 * absent. This keeps the reader overlay independent from accounts and AI.
 */
@Composable
fun ReaderMenuOverlay(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    readerOverlayState: ReaderOverlayState,
    currentSound: BackgroundSound = BackgroundSound.NONE,
    lastSelectedSound: BackgroundSound = BackgroundSound.RAIN,
    onAction: (ReaderAction) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(readerOverlayState.progress) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragging) {
        if (isDragging) onAction(ReaderAction.OnScrubStart)
        else onAction(ReaderAction.OnScrubEnd)
    }

    LaunchedEffect(readerOverlayState.progress) {
        if (!isDragging) sliderPosition = readerOverlayState.progress
    }

    val currentPage = remember(sliderPosition, readerOverlayState.totalPages) {
        val pageCount = readerOverlayState.totalPages.coerceAtLeast(1)
        ((sliderPosition * (pageCount - 1)).roundToInt())
            .coerceIn(0, pageCount - 1) + 1
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ReaderTopBar(
                isVisible = isVisible,
                bookTitle = readerOverlayState.bookTitle,
                onBackClick = { onAction(ReaderAction.OnBackClick) },
                onTocClick = { onAction(ReaderAction.OnTocClick) },
                onSoundClick = { onAction(ReaderAction.OnSoundClick) },
                onSettingsClick = { onAction(ReaderAction.OnSettingsClick) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut()
            ) {
                SoundToggleButton(
                    currentSound = currentSound,
                    lastSelectedSound = lastSelectedSound,
                    onClick = { onAction(ReaderAction.OnSoundQuickToggle) }
                )
            }

            ReaderBottomControls(
                isVisible = isVisible,
                readerOverlayState = readerOverlayState,
                sliderPosition = sliderPosition,
                interactionSource = interactionSource,
                currentPage = currentPage,
                onSeek = { onAction(ReaderAction.OnSeek(it)) }
            )
        }
    }
}
