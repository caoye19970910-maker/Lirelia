package com.yugentech.quill.reader.ui.components.overlay.parent

/**
 * Actions exposed by Lirelia's reading controls.
 *
 * AI-specific actions from the upstream project are intentionally removed.
 * The future language-learning dictionary will use its own explicit actions.
 */
sealed interface ReaderAction {
    data object OnBackClick : ReaderAction
    data object OnSettingsClick : ReaderAction
    data object OnTocClick : ReaderAction
    data object OnSoundClick : ReaderAction
    data object OnScrubStart : ReaderAction
    data object OnScrubEnd : ReaderAction
    data object OnSoundQuickToggle : ReaderAction
    data class OnSeek(val progress: Float) : ReaderAction
    data class OnBrightnessInteraction(val isInteracting: Boolean) : ReaderAction
}
