package com.yugentech.quill.reader.dictionary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.dictionary.model.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySheet(
    query: String,
    entry: DictionaryEntry?,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    onSave: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry?.headword ?: query,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val meta = listOfNotNull(entry?.ipa, entry?.partOfSpeech)
                        .joinToString("  ·  ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { onSpeak(entry?.headword ?: query) }) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Pronounce")
                }
                IconButton(onClick = { onSave(entry?.headword ?: query) }) {
                    Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Save word")
                }
            }

            HorizontalDivider()

            if (entry == null) {
                Text(
                    text = "这个词暂时还没有收录。词典框架已经接通，下一步会换成完整的法中离线词典。",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                entry.meanings.forEachIndexed { index, meaning ->
                    Text(
                        text = if (entry.meanings.size > 1) "${index + 1}. $meaning" else meaning,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (entry.example != null) {
                    HorizontalDivider()
                    Text(
                        text = entry.example,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic
                    )
                    entry.exampleTranslation?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("关闭")
            }
        }
    }
}
