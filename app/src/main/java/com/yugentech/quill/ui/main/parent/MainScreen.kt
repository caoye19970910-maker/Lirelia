package com.yugentech.quill.ui.main.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.main.components.BottomBar
import com.yugentech.quill.ui.main.components.ExitConfirmationDialog
import com.yugentech.quill.ui.main.components.QuillTab
import com.yugentech.quill.ui.main.components.ResumeFab
import com.yugentech.quill.ui.tabs.libraryScreen.parent.LibraryScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.parent.SourcesScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Lirelia V0.1 home: a deliberately small, local-first shell.
 * The core path is Library -> Import -> Reader.
 */
@Composable
fun MainScreen(
    onBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSeeAllClick: (title: String) -> Unit,
    onExitApp: () -> Unit = {},
    libraryViewModel: LibraryViewModel,
    sourcesViewModel: SourcesViewModel = koinViewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(QuillTab.Library) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val lastReadBook by libraryViewModel.lastReadBook.collectAsStateWithLifecycle()

    BackHandler(enabled = currentTab != QuillTab.Library) {
        currentTab = QuillTab.Library
    }
    BackHandler(enabled = currentTab == QuillTab.Library) {
        showExitDialog = true
    }

    Scaffold(
        bottomBar = {
            BottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        floatingActionButton = {
            ResumeFab(
                visible = currentTab == QuillTab.Library && lastReadBook != null,
                isScrollingDown = false,
                onClick = { lastReadBook?.let { onResumeClick(it.toBook()) } }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                QuillTab.Library -> LibraryScreen(
                    contentPadding = innerPadding,
                    onLibraryBookClick = onBookClick,
                    onResumeClick = onResumeClick,
                    onSeeAllClick = onSeeAllClick,
                    viewModel = libraryViewModel
                )

                QuillTab.Sources -> SourcesScreen(
                    contentPadding = innerPadding,
                    onLocalFilesClick = { currentTab = QuillTab.Library },
                    viewModel = sourcesViewModel
                )
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                onExitApp()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}
