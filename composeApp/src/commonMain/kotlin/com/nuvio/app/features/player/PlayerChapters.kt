package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.compose_player_playing
import nuvio.composeapp.generated.resources.player_chapter_default_title
import nuvio.composeapp.generated.resources.player_panel_chapters
import org.jetbrains.compose.resources.stringResource

/** One chapter mark read from the media file. [title] may be blank; the UI supplies a numbered fallback. */
data class PlayerChapter(
    val index: Int,
    val title: String,
    val startMs: Long,
)

/** Chapters closer than this to the current position count as "current" so a seek that lands a hair early still highlights the right row. */
private const val ChapterBoundaryToleranceMs = 300L

/** Sorts by start time, drops duplicate start times, and renumbers so [PlayerChapter.index] is the list position. */
internal fun List<PlayerChapter>.normalizedChapters(): List<PlayerChapter> =
    sortedBy { it.startMs }
        .distinctBy { it.startMs }
        .mapIndexed { position, chapter -> chapter.copy(index = position) }

/**
 * Reads the chapter list from an mpv-style property accessor (libmpv on Android and iOS).
 * mpv exposes `chapter-list/count`, `chapter-list/N/title` and `chapter-list/N/time` (seconds).
 */
internal fun mpvChapters(property: (String) -> String?): List<PlayerChapter> {
    val count = property("chapter-list/count")?.trim()?.toIntOrNull() ?: return emptyList()
    if (count <= 0) return emptyList()
    return (0 until count)
        .mapNotNull { position ->
            val seconds = property("chapter-list/$position/time")?.trim()?.toDoubleOrNull()
                ?: return@mapNotNull null
            PlayerChapter(
                index = position,
                title = property("chapter-list/$position/title")?.trim().orEmpty(),
                startMs = (seconds * 1000.0).toLong().coerceAtLeast(0L),
            )
        }
        .normalizedChapters()
}

/** Index of the chapter playing at [positionMs], or -1 when there are no chapters or playback is before the first one. */
internal fun List<PlayerChapter>.currentChapterIndex(positionMs: Long): Int {
    if (isEmpty()) return -1
    val position = positionMs + ChapterBoundaryToleranceMs
    return indexOfLast { it.startMs <= position }
}

/** The chapters button appears whenever the loaded media reports at least two chapters. */
internal fun shouldShowChaptersButton(chapters: List<PlayerChapter>): Boolean = chapters.size >= 2

@Composable
internal fun PlayerChaptersPanel(
    visible: Boolean,
    chapters: List<PlayerChapter>,
    positionMs: Long,
    onChapterSelected: (PlayerChapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = chapters.currentChapterIndex(positionMs)
    val listState = rememberLazyListState()

    LaunchedEffect(visible) {
        if (visible && currentIndex > 0) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    PlayerSidePanel(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            PlayerPanelHeader(title = stringResource(Res.string.player_panel_chapters)) {
                PlayerDialogButton(
                    label = stringResource(Res.string.action_close),
                    onClick = onDismiss,
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(chapters, key = { _, chapter -> chapter.index }) { position, chapter ->
                    PlayerChapterRow(
                        chapter = chapter,
                        displayNumber = position + 1,
                        isCurrent = position == currentIndex,
                        onClick = { onChapterSelected(chapter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerChapterRow(
    chapter: PlayerChapter,
    displayNumber: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val title = chapter.title.ifBlank {
        stringResource(Res.string.player_chapter_default_title, displayNumber)
    }
    val playingLabel = stringResource(Res.string.compose_player_playing)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) tokens.colors.accent.copy(alpha = 0.16f) else tokens.colors.surfaceCard,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                if (isCurrent) stateDescription = playingLabel
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (isCurrent) tokens.colors.accent else tokens.colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatPlaybackTime(chapter.startMs),
            color = tokens.colors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
