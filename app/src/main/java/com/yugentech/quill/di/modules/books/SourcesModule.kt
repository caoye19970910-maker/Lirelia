package com.yugentech.quill.di.modules.books

import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Lirelia local-only import module.
 *
 * Remote OPDS/Gutenberg sources remain in the upstream history for reference,
 * but they are deliberately not wired into the clean runtime.
 */
val sourcesModule = module {
    viewModel {
        SourcesViewModel(
            bookDao = get()
        )
    }
}
