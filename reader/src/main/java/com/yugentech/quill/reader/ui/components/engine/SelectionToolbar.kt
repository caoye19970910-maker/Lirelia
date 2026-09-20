package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.theme.service.HapticService
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import org.koin.compose.koinInject

/**
 * Selection actions for Lirelia.
 *
 * Dictionary lookup is a first-class reading action. AI-specific actions from
 * the upstream reader are intentionally absent from the clean baseline.
 */
@Composable
fun SelectionToolbar(
    selectionInfo: SelectionInfo,
    hazeState: HazeState,
    readerBgIsLight: Boolean = false,
    onDictionary: (String) -> Unit,
    onHighlight: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = koinInject<HapticService>()

    val hazeBackgroundColor = if (readerBgIsLight) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.36f)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(28.dp))
            .hazeEffect(
                state = hazeState,
                style = HazeDefaults.style(
                    backgroundColor = hazeBackgroundColor,
                    blurRadius = 14.dp,
                    noiseFactor = 0.05f
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Search,
                label = "词典",
                onClick = {
                    haptic.performHaptic()
                    onDictionary(selectionInfo.text)
                }
            )

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Brush,
                label = "标注",
                onClick = {
                    haptic.performHaptic()
                    onHighlight()
                }
            )

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.ContentCopy,
                label = "复制",
                onClick = {
                    haptic.performHaptic()
                    onCopy(selectionInfo.text)
                }
            )

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Share,
                label = "分享",
                onClick = {
                    haptic.performHaptic()
                    onShare(selectionInfo.text)
                }
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.height(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
