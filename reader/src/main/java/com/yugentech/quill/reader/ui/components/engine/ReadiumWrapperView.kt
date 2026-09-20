package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView

class ReadiumWrapperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onSelectionStarted: () -> Unit = {}
    var onSelectionEnded: () -> Unit = {}
    var onSelectionChanged: (text: String) -> Unit = {}

    private var activeActionMode: ActionMode? = null
    val container = FragmentContainerView(context).also { addView(it) }

    fun finishActionMode() {
        activeActionMode?.finish()
        activeActionMode = null
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        val webView = (originalView as? WebView) ?: findWebView(this)
        webView?.setLayerType(LAYER_TYPE_SOFTWARE, null)

        onSelectionStarted()

        val onDestroy: () -> Unit = {
            webView?.setLayerType(LAYER_TYPE_HARDWARE, null)
            onSelectionEnded()
        }

        val wrapped = if (callback is ActionMode.Callback2) {
            WrappedCallback2(callback, onDestroy)
        } else {
            WrappedCallback(callback, onDestroy)
        }

        val mode = super.startActionModeForChild(originalView, wrapped, type)
        activeActionMode = mode
        return mode
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
