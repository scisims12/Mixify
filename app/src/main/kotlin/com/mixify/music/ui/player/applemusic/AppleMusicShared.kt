/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.player.applemusic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mixify.music.LocalDatabase
import com.mixify.music.R
import com.mixify.music.models.MediaMetadata
import com.mixify.music.playback.PlayerConnection
import com.mixify.music.ui.component.IconButton
import com.mixify.music.utils.joinToArtistString

enum class AppleMusicView { MAIN, LYRICS, QUEUE }

internal val AppleMusicTextSecondary = Color.White.copy(alpha = 0.72f)
internal val AppleMusicPillInactive = Color.White.copy(alpha = 0.24f)
internal val AppleMusicTrackInactive = Color.White.copy(alpha = 0.26f)
internal val AppleMusicTrackActive = Color.White.copy(alpha = 0.92f)

@Composable
internal fun Modifier.appleMusicPressInflate(pressedScale: Float = 1.35f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "appleMusicPressInflate",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

@Composable
internal fun AppleMusicCompactHeader(
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val librarySong by database.song(mediaMetadata?.id ?: "").collectAsStateWithLifecycle(initialValue = null)
    val isLiked = librarySong?.song?.liked == true

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = mediaMetadata?.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata?.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                }
                Text(
                    text = mediaMetadata?.artists?.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleMusicTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(onClick = playerConnection::toggleLike) {
            Icon(
                painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = null,
                tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun AppleMusicThinSlider(
    value: Float,
    activeColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val trackHeight by animateDpAsState(if (pressed || dragged) 14.dp else 7.dp, spring(dampingRatio = 0.5f, stiffness = 300f), label = "trackHeight")
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            interactionSource = interactionSource,
            track = {
                val fraction = value.coerceIn(0f, 1f)
                Box(modifier = Modifier.fillMaxWidth().height(trackHeight).clip(RoundedCornerShape(percent = 50)).background(AppleMusicTrackInactive)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(activeColor))
                }
            },
            thumb = { Spacer(Modifier.size(0.dp)) },
        )
    }
}

@Composable
internal fun AppleMusicTransportRow(
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
) {
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(58.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = canSkipPrevious,
            onClick = { playerConnection.player.seekToPreviousMediaItem() },
            modifier = Modifier.size(46.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = null,
                tint = if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = {
                if (playbackState == Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.togglePlayPause()
                }
            },
            modifier = Modifier.size(66.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (playbackState == Player.STATE_ENDED) R.drawable.replay
                    else if (isPlaying) R.drawable.pause
                    else R.drawable.play
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }

        IconButton(
            enabled = canSkipNext,
            onClick = { playerConnection.player.seekToNext() },
            modifier = Modifier.size(46.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
internal fun AppleMusicDockButton(
    icon: ImageVector?,
    drawableRes: Int?,
    active: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier.appleMusicPressInflate().size(40.dp).clip(CircleShape).background(if (active) activeColor else Color.Transparent).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = "",
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    active -> activeContentColor
                    else -> Color.White.copy(alpha = 0.85f)
                },
                modifier = Modifier.size(22.dp),
            )
        } else if (drawableRes != null) {
            Icon(
                painter = painterResource(drawableRes),
                contentDescription = "",
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    active -> activeContentColor
                    else -> Color.White.copy(alpha = 0.85f)
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun AppleMusicDock(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    lyricsAvailable: Boolean,
    activeColor: Color,
    activeContentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleMusicDockButton(
            icon = null,
            drawableRes = R.drawable.lyrics,
            active = viewState == AppleMusicView.LYRICS,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            enabled = lyricsAvailable,
            onClick = { onSelectView(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS) },
        )
        AppleMusicDockButton(
            icon = null,
            drawableRes = R.drawable.queue_music,
            active = viewState == AppleMusicView.QUEUE,
            activeColor = activeColor,
            activeContentColor = activeContentColor,
            onClick = { onSelectView(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE) },
        )
    }
}

@Composable
internal fun AppleMusicBottomCluster(
    position: Long,
    duration: Long,
    activeColor: Color,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    lyricsAvailable: Boolean,
    activePillContainer: Color,
    activePillContent: Color,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
            val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            AppleMusicThinSlider(
                value = progress,
                activeColor = activeColor,
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderChangeFinished,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        AppleMusicTransportRow(playerConnection = playerConnection)
        Spacer(modifier = Modifier.height(14.dp))
        AppleMusicDock(
            viewState = viewState,
            onSelectView = onSelectView,
            lyricsAvailable = lyricsAvailable,
            activeColor = activePillContainer,
            activeContentColor = activePillContent,
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
