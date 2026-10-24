/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.mixify.music.ui.component

/** Keep overlapping lyrics stable, but let an explicit playback jump move either way. */
internal fun resolveLyricsScrollTarget(
    currentTarget: Int,
    activeIndices: Set<Int>,
    mainLineIndices: List<Int>,
    lastMainMaxSeen: Int,
    previouslyHadActiveLines: Boolean,
    resyncTarget: Int? = null,
): Int {
    if (resyncTarget != null) return resyncTarget
    val activeTarget = activeIndices.filter { it in mainLineIndices }.maxOrNull()
        ?: activeIndices.maxOrNull()
    if (currentTarget !in activeIndices) {
        if (activeTarget != null && activeTarget > currentTarget) return activeTarget
        if (activeTarget == null && previouslyHadActiveLines) {
            return mainLineIndices.firstOrNull { it > lastMainMaxSeen } ?: currentTarget
        }
    }
    return currentTarget
}
