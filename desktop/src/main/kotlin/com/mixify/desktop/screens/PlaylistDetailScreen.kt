package com.mixify.desktop.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mixify.desktop.playback.AudioPlayer
import com.mixify.desktop.playback.StreamResolver
import com.mixify.desktop.utils.NetworkImage
import com.mixify.innertube.YouTube
import com.mixify.innertube.pages.PlaylistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playlistPageState by remember(playlistId) { mutableStateOf<Result<PlaylistPage>?>(null) }
    var isLoading by remember(playlistId) { mutableStateOf(true) }
    var nowPlayingTitle by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(playlistId) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            YouTube.playlist(playlistId)
        }
        playlistPageState = result
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = {
                AudioPlayer.stop()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Playlist Details",
                    style = MaterialTheme.typography.titleLarge
                )
                if (nowPlayingTitle != null) {
                    Text(
                        text = "Now playing: $nowPlayingTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            playlistPageState?.isFailure == true -> {
                val error = playlistPageState?.exceptionOrNull()
                Text(
                    text = "Error loading playlist: ${error?.message ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            playlistPageState?.isSuccess == true -> {
                val page = playlistPageState?.getOrNull()
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = page?.playlist?.title ?: "Playlist",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(page?.songs.orEmpty()) { song ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onClick = {
                                    nowPlayingTitle = song.title
                                    println("Clicked song: '${song.title}' (ID: ${song.id})")
                                    coroutineScope.launch(Dispatchers.IO) {
                                        println("Resolving stream URL for song: ${song.title} (${song.id})...")
                                        StreamResolver.resolveStreamUrl(song.id)
                                            .onSuccess { url ->
                                                println("SUCCESS: Resolved stream URL for ${song.title}: $url")
                                                AudioPlayer.play(url)
                                            }
                                            .onFailure { err ->
                                                println("FAILURE: Failed to resolve stream for ${song.title}: ${err.message}")
                                            }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                ) {
                                    NetworkImage(
                                        url = song.thumbnail,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = song.artists.joinToString { it.name },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
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
}
