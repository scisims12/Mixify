/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mixify.innertube.YouTube
import com.mixify.innertube.models.AlbumItem
import com.mixify.innertube.models.ArtistItem
import com.mixify.innertube.models.PlaylistItem
import com.mixify.innertube.models.SongItem
import com.mixify.innertube.models.WatchEndpoint
import com.mixify.innertube.models.YTItem
import com.mixify.innertube.models.filterExplicit
import com.mixify.innertube.models.filterVideoSongs
import com.mixify.innertube.utils.YouTubeUrlParser
import com.mixify.innertube.pages.ChartsPage
import com.mixify.music.constants.HideExplicitKey
import com.mixify.music.constants.HideVideoSongsKey
import com.mixify.music.db.MusicDatabase
import com.mixify.music.db.entities.SearchHistory
import com.mixify.music.utils.dataStore
import com.mixify.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    // Cached trending items flow
    private val trendingFlow = MutableStateFlow<List<YTItem>>(emptyList())

    init {
        // Fetch trending/top charts once, in the background
        viewModelScope.launch {
            Log.d("TrendingDebug", "Starting home items fetch as fallback for Trending...")
            val homeResult = YouTube.home()
            val home = homeResult.getOrNull()
            
            if (homeResult.isFailure) {
                Log.e("TrendingDebug", "Home fetch FAILED", homeResult.exceptionOrNull())
            } else {
                Log.d("TrendingDebug", "Home fetch SUCCESS. Sections: ${home?.sections?.map { it.title }}")
            }

            trendingFlow.value =
                home
                    ?.sections
                    ?.firstOrNull { it.title.contains("Trending", ignoreCase = true) || it.title.contains("Hit", ignoreCase = true) }
                    ?.items
                    ?.take(10)
                    ?: home
                        ?.sections
                        ?.firstOrNull { it.items.isNotEmpty() }
                        ?.items
                        ?.take(10)
                            ?: emptyList()
            
            Log.d("TrendingDebug", "Trending items populated from Home: size=${trendingFlow.value.size}, items=${trendingFlow.value.map { it.title }}")
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(query, trendingFlow) { query, trending ->
                query to trending
            }
                .flatMapLatest { (query, trending) ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                                trending = trending,
                            )
                        }
                    } else {
                        // Check if query is a YouTube URL
                        val parsedUrl = YouTubeUrlParser.parse(query)
                        if (parsedUrl != null) {
                            // Fetch content from YouTube URL
                            val parsedItem = fetchParsedUrlItem(parsedUrl)
                            database
                                .searchHistory(query)
                                .map { it.take(3) }
                                .map { history ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions = emptyList(),
                                        items = parsedItem?.let { listOf(it) } ?: emptyList(),
                                        parsedUrlItem = parsedItem,
                                        isUrlQuery = true,
                                    )
                                }
                        } else {
                            val result = YouTube.searchSuggestions(query).getOrNull()
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                            database
                                .searchHistory(query)
                                .map { it.take(3) }
                                .map { history ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions =
                                            result
                                                ?.queries
                                                ?.filter { suggestionQuery ->
                                                    history.none { it.query == suggestionQuery }
                                                }.orEmpty(),
                                        items =
                                            result
                                                ?.recommendedItems
                                                ?.distinctBy { it.id }
                                                ?.filterExplicit(hideExplicit)
                                                ?.filterVideoSongs(hideVideoSongs)
                                                .orEmpty(),
                                    )
                                }
                        }
                    }
                }.collect {
                    Log.d("TrendingDebug", "Updating ViewState: query='${query.value}', trending items count=${it.trending.size}")
                    _viewState.value = it
                }
        }
    }

    private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? =
        when (parsedUrl) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                // Use next() to get the song details from a video ID
                YouTube
                    .next(WatchEndpoint(videoId = parsedUrl.id))
                    .getOrNull()
                    ?.items
                    ?.firstOrNull()
            }

            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                // Fetch playlist details
                YouTube
                    .playlist(parsedUrl.id)
                    .getOrNull()
                    ?.playlist
            }

            is YouTubeUrlParser.ParsedUrl.Album -> {
                // For albums, we need to get the browseId from the playlist
                // First, try to get the album page
                val albumResult = YouTube.album("MPREb_${parsedUrl.id}")
                if (albumResult.isSuccess) {
                    albumResult.getOrNull()?.album
                } else {
                    // If that fails, treat it as a playlist
                    YouTube
                        .playlist(parsedUrl.id)
                        .getOrNull()
                        ?.playlist
                }
            }

            is YouTubeUrlParser.ParsedUrl.Artist -> {
                // Fetch artist details
                if (parsedUrl.id.startsWith("MPRE")) {
                    // It's a browse ID
                    YouTube
                        .artist(parsedUrl.id)
                        .getOrNull()
                        ?.artist
                } else {
                    // It's a channel ID, we need to find the browse ID
                    // For now, try using the channel ID as browse ID
                    YouTube
                        .artist(parsedUrl.id)
                        .getOrNull()
                        ?.artist
                }
            }
        }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val parsedUrlItem: YTItem? = null,
    val isUrlQuery: Boolean = false,
    val trending: List<YTItem> = emptyList(),
)