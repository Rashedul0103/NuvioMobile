package com.nuvio.app.features.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame

/**
 * Reads ID3v2 `CHAP` frames (the usual chapter format for MP3 audiobooks) from the loaded tracks.
 * ExoPlayer does not surface MP4/M4B or Matroska chapters, so those files need the libmpv engine.
 * Must be called on the thread that owns the player.
 */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun ExoPlayer.id3Chapters(): List<PlayerChapter> {
    val seenIds = mutableSetOf<String>()
    val found = mutableListOf<PlayerChapter>()
    for (group in currentTracks.groups) {
        for (trackIndex in 0 until group.length) {
            val metadata = group.getTrackFormat(trackIndex).metadata ?: continue
            for (entryIndex in 0 until metadata.length()) {
                val frame = metadata.get(entryIndex) as? ChapterFrame ?: continue
                if (!seenIds.add(frame.chapterId)) continue
                var title = ""
                for (subIndex in 0 until frame.subFrameCount) {
                    val subFrame = frame.getSubFrame(subIndex)
                    if (subFrame is TextInformationFrame && subFrame.id == "TIT2") {
                        title = subFrame.values.firstOrNull().orEmpty().trim()
                        if (title.isNotEmpty()) break
                    }
                }
                found += PlayerChapter(
                    index = found.size,
                    title = title,
                    startMs = frame.startTimeMs.toLong().coerceAtLeast(0L),
                )
            }
        }
    }
    return found.normalizedChapters()
}
