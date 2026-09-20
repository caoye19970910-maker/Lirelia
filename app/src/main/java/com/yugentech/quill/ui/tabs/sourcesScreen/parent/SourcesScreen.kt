package com.yugentech.quill.ui.tabs.sourcesScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.ui.tabs.sourcesScreen.components.CatalogInfo
import com.yugentech.quill.ui.tabs.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportStatusSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.LargeCatalogCard
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel

/**
 * The clean Lirelia baseline intentionally exposes one source only:
 * EPUB files already on the device. Remote catalogs can be reconsidered
 * later without coupling the core reading path to networking or accounts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onSourceClick: (BookSource) -> Unit = {},
    onLocalFilesClick: () -> Unit,
    viewModel: SourcesViewModel
) {
    val context = LocalContext.current
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importResults by viewModel.importResults.collectAsStateWithLifecycle()
    var showFilePickerSheet by remember { mutableStateOf(false) }

    val localImport = CatalogInfo(
        source = BookSource.USER_IMPORTED,
        title = "Import EPUB",
        subtitle = "Choose books from this device",
        description = "Import your own EPUB books. They stay on this device and are available offline.",
        icon = Icons.Default.FolderOpen,
        shape = MaterialShapes.Bun.toShape(),
        containerColor = { MaterialTheme.colorScheme.tertiaryContainer },
        contentColor = { MaterialTheme.colorScheme.onTertiaryContainer },
        buttonContainerColor = { MaterialTheme.colorScheme.tertiary },
        buttonContentColor = { MaterialTheme.colorScheme.onTertiary },
        buttonText = if (isImporting) "Importing..." else "Choose EPUB",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Import",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LargeCatalogCard(
                catalog = localImport,
                onClick = { showFilePickerSheet = true }
            )
        }

        if (showFilePickerSheet) {
            FilePickerBottomSheet(
                onDismiss = { showFilePickerSheet = false },
                onFilesSelected = { uris ->
                    showFilePickerSheet = false
                    viewModel.importFiles(context, uris)
                },
            )
        }

        if (importResults.isNotEmpty()) {
            ImportStatusSheet(
                results = importResults,
                onDismiss = {
                    val hasSuccess = importResults.any { it is ImportResult.Success }
                    viewModel.clearResults()
                    if (hasSuccess) {
                        onLocalFilesClick()
                    }
                },
            )
        }
    }
}
