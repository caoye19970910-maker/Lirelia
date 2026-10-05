package com.cy.languagereader.mobile

/**
 * Lightweight bridge between Activity hardware keys and the Compose reader.
 * Callbacks are set only while a reader is open.
 */
object ReaderKeyBridge {
    var onVolumeUp: (() -> Unit)? = null
    var onVolumeDown: (() -> Unit)? = null

    fun clear() {
        onVolumeUp = null
        onVolumeDown = null
    }
}
