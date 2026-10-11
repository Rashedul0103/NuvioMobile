package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerChaptersTest {
    private fun properties(vararg chapters: Pair<String?, String?>): (String) -> String? {
        val values = mutableMapOf("chapter-list/count" to chapters.size.toString())
        chapters.forEachIndexed { index, (title, time) ->
            title?.let { values["chapter-list/$index/title"] = it }
            time?.let { values["chapter-list/$index/time"] = it }
        }
        return { name -> values[name] }
    }

    @Test
    fun mpvChaptersReadsTitlesAndTimes() {
        val chapters = mpvChapters(
            properties(
                "Opening" to "0.000000",
                "Chapter Two" to "62.500000",
                "Epilogue" to "3601.250000",
            ),
        )
        assertEquals(listOf("Opening", "Chapter Two", "Epilogue"), chapters.map { it.title })
        assertEquals(listOf(0L, 62_500L, 3_601_250L), chapters.map { it.startMs })
        assertEquals(listOf(0, 1, 2), chapters.map { it.index })
    }

    @Test
    fun mpvChaptersReturnsEmptyWithoutCount() {
        assertTrue(mpvChapters { null }.isEmpty())
        assertTrue(mpvChapters { name -> if (name == "chapter-list/count") "0" else null }.isEmpty())
        assertTrue(mpvChapters { name -> if (name == "chapter-list/count") "abc" else null }.isEmpty())
    }

    @Test
    fun mpvChaptersSkipsEntriesWithoutTimeAndKeepsBlankTitles() {
        val chapters = mpvChapters(
            properties(
                "A" to "0",
                "Broken" to null,
                null to "30",
            ),
        )
        assertEquals(2, chapters.size)
        assertEquals("A", chapters[0].title)
        assertEquals("", chapters[1].title)
        assertEquals(30_000L, chapters[1].startMs)
        assertEquals(listOf(0, 1), chapters.map { it.index })
    }

    @Test
    fun normalizedChaptersSortsAndDropsDuplicateStarts() {
        val chapters = listOf(
            PlayerChapter(0, "Late", 90_000L),
            PlayerChapter(1, "Early", 0L),
            PlayerChapter(2, "Duplicate", 90_000L),
        ).normalizedChapters()
        assertEquals(listOf("Early", "Late"), chapters.map { it.title })
        assertEquals(listOf(0, 1), chapters.map { it.index })
    }

    @Test
    fun currentChapterIndexFollowsPosition() {
        val chapters = listOf(
            PlayerChapter(0, "One", 0L),
            PlayerChapter(1, "Two", 60_000L),
            PlayerChapter(2, "Three", 120_000L),
        )
        assertEquals(0, chapters.currentChapterIndex(0L))
        assertEquals(0, chapters.currentChapterIndex(59_000L))
        assertEquals(1, chapters.currentChapterIndex(60_000L))
        assertEquals(2, chapters.currentChapterIndex(500_000L))
    }

    @Test
    fun currentChapterIndexToleratesSeeksLandingJustBeforeTheMark() {
        val chapters = listOf(
            PlayerChapter(0, "One", 0L),
            PlayerChapter(1, "Two", 60_000L),
        )
        assertEquals(1, chapters.currentChapterIndex(59_800L))
        assertEquals(0, chapters.currentChapterIndex(59_000L))
    }

    @Test
    fun currentChapterIndexIsMinusOneWhenEmptyOrBeforeFirstChapter() {
        assertEquals(-1, emptyList<PlayerChapter>().currentChapterIndex(10_000L))
        val chapters = listOf(PlayerChapter(0, "One", 10_000L), PlayerChapter(1, "Two", 20_000L))
        assertEquals(-1, chapters.currentChapterIndex(1_000L))
    }

    @Test
    fun chaptersButtonNeedsAtLeastTwoChapters() {
        assertFalse(shouldShowChaptersButton(emptyList()))
        assertFalse(shouldShowChaptersButton(listOf(PlayerChapter(0, "Only", 0L))))
        assertTrue(
            shouldShowChaptersButton(
                listOf(PlayerChapter(0, "One", 0L), PlayerChapter(1, "Two", 1_000L)),
            ),
        )
    }
}
