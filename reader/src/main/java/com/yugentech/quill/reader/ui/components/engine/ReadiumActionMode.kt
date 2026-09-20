package com.yugentech.quill.reader.ui.components.engine

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View

/**
 * Keeps Android's native text-selection handles while suppressing its floating
 * menu. Lirelia renders its own Compose selection toolbar instead.
 */
class WrappedCallback(
    private val original: ActionMode.Callback,
    private val onDestroy: () -> Unit = {}
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        original.onActionItemClicked(mode, item)

    override fun onDestroyActionMode(mode: ActionMode) {
        original.onDestroyActionMode(mode)
        onDestroy()
    }
}

class WrappedCallback2(
    private val original: ActionMode.Callback2,
    onDestroy: () -> Unit = {}
) : ActionMode.Callback2() {

    private val delegate = WrappedCallback(original, onDestroy)

    override fun onCreateActionMode(mode: ActionMode, menu: Menu) =
        delegate.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) =
        delegate.onPrepareActionMode(mode, menu)

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
        delegate.onActionItemClicked(mode, item)

    override fun onDestroyActionMode(mode: ActionMode) =
        delegate.onDestroyActionMode(mode)

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        original.onGetContentRect(mode, view, outRect)
        outRect.left = outRect.left.coerceAtLeast(0)
        outRect.top = outRect.top.coerceAtLeast(0)
        outRect.right = outRect.right.coerceIn(outRect.left + 1, view.width.coerceAtLeast(1))
        outRect.bottom = outRect.bottom.coerceIn(outRect.top + 1, view.height.coerceAtLeast(1))
    }
}
