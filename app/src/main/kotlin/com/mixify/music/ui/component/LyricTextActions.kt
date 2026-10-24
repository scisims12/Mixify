/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.mixify.music.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.semantics.onLongClick as semanticsOnLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult

/** Keep full-width lyric alignment, but only seek/select inside a rendered text line. */
@Composable
internal fun Modifier.lyricTextActions(
    layout: () -> TextLayoutResult?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    val latestLayout by rememberUpdatedState(layout)
    val latestClick by rememberUpdatedState(onClick)
    val latestLongClick by rememberUpdatedState(onLongClick)
    return semantics {
        // Accessibility actions do not have coordinates and should remain directly usable.
        semanticsOnClick { latestClick(); true }
        semanticsOnLongClick { latestLongClick(); true }
    }.pointerInput(Unit) {
        detectTapGestures(
            onTap = { position ->
                if (latestLayout()?.containsLyricText(position) == true) latestClick()
            },
            onLongPress = { position ->
                if (latestLayout()?.containsLyricText(position) == true) latestLongClick()
            },
        )
    }
}

private fun TextLayoutResult.containsLyricText(position: Offset): Boolean {
    if (lineCount == 0 || position.y < 0f || position.y >= size.height) return false
    val line = getLineForVerticalPosition(position.y)
    val start = getLineStart(line)
    val end = getLineEnd(line, visibleEnd = true)
    if (start == end) return false
    return position.x >= minOf(getLineLeft(line), getLineRight(line)) &&
        position.x <= maxOf(getLineLeft(line), getLineRight(line)) &&
        position.y >= getLineTop(line) && position.y <= getLineBottom(line)
}
