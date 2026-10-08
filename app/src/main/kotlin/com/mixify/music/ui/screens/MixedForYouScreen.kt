/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.palette.graphics.Palette
import com.mixify.innertube.models.PlaylistItem
import com.mixify.music.LocalPlayerAwareWindowInsets
import com.mixify.music.R
import com.mixify.music.db.entities.PlaylistEntity
import com.mixify.music.ui.component.IconButton
import com.mixify.music.ui.component.LocalMenuState
import com.mixify.music.ui.component.YouTubeGridItem
import com.mixify.music.ui.menu.YouTubePlaylistMenu
import com.mixify.music.ui.theme.PlayerColorExtractor
import com.mixify.music.ui.utils.backToMain
import com.mixify.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixedForYouScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    showBackButton: Boolean = false,
) {
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val mixesPlaylists by viewModel.mixesPlaylists.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (mixesPlaylists.isNullOrEmpty()) {
            viewModel.loadMixes()
        }
    }

    val firstMixThumbnail = mixesPlaylists?.firstOrNull()?.thumbnail
    var glowColor by remember { mutableStateOf(Color.Transparent) }
    val animatedGlowColor by animateColorAsState(glowColor, tween(500), label = "glowColorAnimation")
    val context = LocalContext.current
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(firstMixThumbnail) {
        val thumbnailUrl = firstMixThumbnail ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(thumbnailUrl)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()

            val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette
                                .from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                    val extractedColors =
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor,
                        )
                    val primary = extractedColors.firstOrNull() ?: Color(fallbackColor)
                    withContext(Dispatchers.Main) {
                        glowColor = primary
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.loadMixes(forceRefresh = true)
                        isRefreshing = false
                    }
                },
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(animatedGlowColor.copy(alpha = 0.5f), Color.Transparent),
                            ),
                        ),
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (showBackButton) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.mixes),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF03FFB8),
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = navController::navigateUp,
                                    onLongClick = navController::backToMain,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.arrow_back),
                                        contentDescription = null,
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.TopStart,
                ) {
                    when {
                        mixesPlaylists == null -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        mixesPlaylists!!.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.alarm_no_playlists),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> {
                            val playerInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                            val gridTopPadding = if (showBackButton) 12.dp else playerInsets.calculateTopPadding() + 12.dp
                            val gridBottomPadding = playerInsets.calculateBottomPadding() + 16.dp

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(
                                    top = gridTopPadding,
                                    bottom = gridBottomPadding,
                                    start = 16.dp,
                                    end = 16.dp,
                                ),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(mixesPlaylists!!, key = { it.id }) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                if (item.id == PlaylistEntity.LIKED_PLAYLIST_ID || item.id == "LM") {
                                                    navController.navigate("auto_playlist/liked")
                                                } else {
                                                    navController.navigate("online_playlist/${item.id.removePrefix("VL")}")
                                                }
                                            },
                                            onLongClick = {
                                                if (item is PlaylistItem) {
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
