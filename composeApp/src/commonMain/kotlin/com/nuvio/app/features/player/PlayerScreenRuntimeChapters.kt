package com.nuvio.app.features.player

internal fun PlayerScreenRuntime.openChaptersPanel() {
    showChaptersPanel = true
    showSourcesPanel = false
    showEpisodesPanel = false
    controlsVisible = false
}

internal fun PlayerScreenRuntime.selectChapter(chapter: PlayerChapter) {
    val positionMs = chapter.startMs.coerceAtLeast(0L)
    finishTimelineScrub(positionMs)
    playerController?.seekTo(positionMs)
    scheduleProgressSyncAfterSeek()
    showChaptersPanel = false
    controlsVisible = true
}
