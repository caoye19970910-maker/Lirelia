package com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.util.LocalBookImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lirelia clean baseline importer.
 *
 * Importing an EPUB is intentionally independent of billing, Firebase and
 * AI indexing. A successfully imported book is immediately available to
 * the local library and reader.
 */
class SourcesViewModel(
    private val bookDao: BookDao
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importResults = MutableStateFlow<List<ImportResult>>(emptyList())
    val importResults = _importResults.asStateFlow()

    fun importFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            _importResults.value = try {
                LocalBookImporter.importFiles(
                    context = context,
                    bookDao = bookDao,
                    uris = uris
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearResults() {
        _importResults.value = emptyList()
    }
}
