/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.player.applemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.mixify.music.LocalPlayerConnection
import com.mixify.music.R
import com.mixify.music.constants.PlayerBackgroundStyle
import com.mixify.music.extensions.metadata
import com.mixify.music.extensions.move
import com.mixify.music.extensions.toggleRepeatMode
import com.mixify.music.models.MediaMetadata
import com.mixify.music.ui.component.BottomSheet
import com.mixify.music.ui.component.BottomSheetState
import com.mixify.music.ui.component.LocalBottomSheetPageState
import com.mixify.music.ui.component.LocalMenuState
import com.mixify.music.ui.component.MediaMetadataListItem
import com.mixify.music.ui.menu.PlayerMenu
import com.mixify.music.ui.utils.ShowMediaInfo
import com.mixify.music.utils.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun AppleMusicQueueView(
    state: BottomSheetState,
    position: Long,
    duration: Long,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    playerBottomSheetState: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    showInlineLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    textButtonColor: Color,
    iconButtonColor: Color,
    TextBackgroundColor: Color,
    playerBackground: PlayerBackgroundStyle,
    backgroundColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val offset = currentWindowIndex + 1
    val upcoming = remember(queueWindows, currentWindowIndex) {
        if (currentWindowIndex < 0) emptyList() else queueWindows.withIndex().drop(offset)
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val safeFrom = (from.index - 0).coerceIn(0, upcoming.lastIndex)
        val safeTo = (to.index - 0).coerceIn(0, upcoming.lastIndex)
        val absoluteFrom = safeFrom + offset
        val absoluteTo = safeTo + offset

        if (!playerConnection.player.shuffleModeEnabled) {
            playerConnection.player.moveMediaItem(absoluteFrom, absoluteTo)
        } else {
            playerConnection.player.setShuffleOrder(
                DefaultShuffleOrder(
                    queueWindows
                        .map { it.firstPeriodIndex }
                        .toMutableList()
                        .move(absoluteFrom, absoluteTo)
                        .toIntArray(),
                    System.currentTimeMillis(),
                ),
            )
        }
    }

    LaunchedEffect(currentWindowIndex) {
        lazyListState.animateScrollBy(-lazyListState.firstVisibleItemScrollOffset.toFloat())
    }

    // Force always dark gradient background for immersive Apple Music style
    val forcedDarkColors = if (backgroundColors.isNotEmpty()) {
        backgroundColors.map { lerp(it, Color.Black, 0.65f) }
    } else {
        listOf(Color(0xFF1A1A1A), Color.Black)
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        collapsedContent = {
            AppleMusicCollapsedQueueRow(
                state = state,
                onToggleLyrics = onToggleLyrics,
            )
        },
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White,
                secondary = Color.White.copy(alpha = 0.85f),
                surfaceVariant = Color.DarkGray,
            ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(forcedDarkColors)),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        AppleMusicCompactHeader(
                            mediaMetadata = mediaMetadata,
                            playerConnection = playerConnection,
                        )

                        // Pills row: Info, Shuffle, Repeat
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AppleMusicQueuePill(
                                iconRes = R.drawable.info,
                                active = false,
                                activeContainer = activePillContainer,
                                activeContent = activePillContent,
                                onClick = {
                                    mediaMetadata?.let { meta ->
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = meta,
                                                playerBottomSheetState = playerBottomSheetState,
                                                onShowDetailsDialog = {
                                                    meta.id.let {
                                                        bottomSheetPageState.show {
                                                            ShowMediaInfo(it)
                                                        }
                                                    }
                                                },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AppleMusicQueuePill(
                                iconRes = R.drawable.shuffle,
                                active = shuffleModeEnabled,
                                activeContainer = activePillContainer,
                                activeContent = activePillContent,
                                onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                                modifier = Modifier.weight(1f),
                            )
                            AppleMusicQueuePill(
                                iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                                active = repeatMode != Player.REPEAT_MODE_OFF,
                                activeContainer = activePillContainer,
                                activeContent = activePillContent,
                                onClick = { playerConnection.player.toggleRepeatMode() },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Continue Playing / Endless Queue header
                        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                        val endlessQueueKey = remember { booleanPreferencesKey("appleMusicEndlessQueue") }
                        val context = LocalContext.current
                        val endlessQueueFlow = remember(context) { context.dataStore.data.map { prefs -> prefs[endlessQueueKey] == true } }
                        val endlessQueueEnabled by endlessQueueFlow.collectAsStateWithLifecycle(initialValue = false)

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(R.string.now_playing), style = MaterialTheme.typography.labelSmall, color = AppleMusicTextSecondary)
                                    val source = queueTitle.orEmpty()
                                    if (source.isNotBlank()) {
                                        Text(text = source, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(text = stringResource(R.string.auto_radio_queue), style = MaterialTheme.typography.labelSmall, color = AppleMusicTextSecondary, modifier = Modifier.padding(end = 8.dp))
                                Switch(
                                    checked = endlessQueueEnabled,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            context.dataStore.edit { prefs -> prefs[endlessQueueKey] = checked }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = activePillContainer,
                                        checkedThumbColor = activePillContent,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedTrackColor = Color.Transparent,
                                        uncheckedBorderColor = Color.White.copy(alpha = 0.45f),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                                    ),
                                    modifier = Modifier.appleMusicPressInflate(pressedScale = 1.08f),
                                )
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            itemsIndexed(upcoming, key = { _, (_, window) -> window.uid.hashCode() }) { _, (absoluteIndex, window) ->
                                ReorderableItem(state = reorderableState, key = window.uid.hashCode()) { _ ->
                                    val metadata = window.mediaItem.metadata
                                    if (metadata != null) {
                                        Box(
                                            modifier = Modifier
                                                .animateItem()
                                                .clickable {
                                                    playerConnection.player.seekToDefaultPosition(absoluteIndex)
                                                    playerConnection.player.playWhenReady = true
                                                },
                                        ) {
                                            MediaMetadataListItem(
                                                mediaMetadata = metadata,
                                                isSelected = false,
                                                isActive = false,
                                                isPlaying = false,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppleMusicBottomCluster(
                            position = position,
                            duration = duration,
                            activeColor = AppleMusicTrackActive,
                            onSliderChange = onSliderChange,
                            onSliderChangeFinished = onSliderChangeFinished,
                            viewState = viewState,
                            onSelectView = onSelectView,
                            lyricsAvailable = true,
                            activePillContainer = activePillContainer,
                            activePillContent = activePillContent,
                            playerConnection = playerConnection,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleMusicCollapsedQueueRow(
    state: BottomSheetState,
    onToggleLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = {
            if (state.isExpanded) {
                state.collapseSoft()
            } else {
                state.expandSoft()
            }
        }) {
            Text(stringResource(R.string.queue), color = Color.White)
        }
        TextButton(onClick = onToggleLyrics) {
            Text(stringResource(R.string.lyrics), color = Color.White)
        }
    }
}

@Composable
private fun AppleMusicQueuePill(
    iconRes: Int,
    active: Boolean,
    activeContainer: Color,
    activeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .appleMusicPressInflate(pressedScale = 1.08f)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) activeContainer else AppleMusicPillInactive)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = if (active) activeContent else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
