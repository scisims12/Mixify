package com.mixify.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsScrollTargetTest {
    private val mainLines = listOf(0, 2, 4, 6, 8)

    @Test
    fun `backward lyric seek overrides the previous forward target`() {
        assertEquals(2, target(current = 8, active = setOf(2, 3), lastSeen = 8, resync = 2))
    }

    @Test
    fun `seek into a gap uses the timeline resync target instead of the old target`() {
        assertEquals(2, target(current = 8, active = emptySet(), lastSeen = 8, resync = 2))
    }

    @Test
    fun `forward seek bypasses an overlapping old line`() {
        assertEquals(8, target(current = 2, active = setOf(2, 8), lastSeen = 2, resync = 8))
    }

    @Test
    fun `overlapping vocals do not move focus before the current line ends`() {
        assertEquals(2, target(current = 2, active = setOf(2, 3, 4), lastSeen = 2))
    }

    @Test
    fun `focus advances when the old line ends and a new main line is active`() {
        assertEquals(4, target(current = 2, active = setOf(3, 4, 5), lastSeen = 2))
    }

    @Test
    fun `gap after a backward seek advances from the new location not the old maximum`() {
        val afterSeek = target(current = 8, active = setOf(2), lastSeen = 8, resync = 2)
        assertEquals(4, target(current = afterSeek, active = emptySet(), lastSeen = afterSeek, previouslyActive = true))
    }

    @Test
    fun `background vocal index is skipped when advancing into a gap`() {
        assertEquals(4, target(current = 2, active = emptySet(), lastSeen = 2, previouslyActive = true))
    }

    @Test
    fun `initial focus follows an active main line and remains stable after the final line`() {
        assertEquals(2, target(current = -1, active = setOf(2, 3), lastSeen = -1))
        assertEquals(8, target(current = 8, active = emptySet(), lastSeen = 8, previouslyActive = true))
    }

    private fun target(
        current: Int,
        active: Set<Int>,
        lastSeen: Int,
        previouslyActive: Boolean = false,
        resync: Int? = null,
    ) = resolveLyricsScrollTarget(current, active, mainLines, lastSeen, previouslyActive, resync)
}
