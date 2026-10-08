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
import com.mixify.innertube.models.Artist
import com.mixify.innertube.models.ArtistItem
import com.mixify.innertube.models.PlaylistItem
import com.mixify.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import com.mixify.innertube.models.WatchEndpoint
import com.mixify.innertube.models.BrowseEndpoint
import com.mixify.innertube.models.YTItem
import com.mixify.innertube.models.filterExplicit
import com.mixify.innertube.models.filterVideoSongs
import com.mixify.innertube.models.filterYoutubeShorts
import com.mixify.innertube.pages.ExplorePage
import com.mixify.innertube.pages.HomePage
import com.mixify.innertube.utils.completed
import com.mixify.music.constants.AccountNameKey
import com.mixify.music.constants.DataSyncIdKey
import com.mixify.music.constants.HideExplicitKey
import com.mixify.music.constants.HideVideoSongsKey
import com.mixify.music.constants.HideYoutubeShortsKey
import com.mixify.music.constants.InnerTubeAuthUserKey
import com.mixify.music.constants.InnerTubeCookieKey
import com.mixify.music.constants.VisitorDataKey
import com.mixify.music.constants.QuickPicks
import com.mixify.music.constants.QuickPicksKey
import com.mixify.music.constants.ShowWrappedCardKey
import com.mixify.music.constants.WrappedSeenKey
import com.mixify.music.db.MusicDatabase
import com.mixify.music.db.entities.Album
import com.mixify.music.db.entities.Playlist
import com.mixify.music.db.entities.PlaylistEntity
import com.mixify.music.db.entities.LocalItem
import com.mixify.music.R
import com.mixify.music.db.entities.Song
import com.mixify.music.db.entities.SpeedDialItem
import com.mixify.music.extensions.filterVideoSongs
import com.mixify.music.extensions.toEnum
import com.mixify.music.models.SimilarRecommendation
import com.mixify.music.ui.screens.wrapped.WrappedAudioService
import com.mixify.music.ui.screens.wrapped.WrappedManager
import com.mixify.music.utils.NetworkConnectivityObserver
import com.mixify.music.utils.SyncUtils
import com.mixify.music.utils.InnerTubeApiCache
import com.mixify.music.utils.dataStore
import com.mixify.music.utils.safeDataStoreEdit
import com.mixify.music.utils.get
import com.mixify.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

internal fun buildSpeedDialItems(
    pinned: List<YTItem>,
    keepListening: List<YTItem>,
    quickPicks: List<YTItem>,
    home: List<YTItem>,
): List<YTItem> =
    (pinned + keepListening + quickPicks + home)
        .distinctBy { it.id }
        .take(27)

const val MAX_KEEP_LISTENING_ITEMS = 8

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val mixesPlaylists = MutableStateFlow<List<YTItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    // Official API data for podcast sections
    val savedPodcastShows = MutableStateFlow<List<com.mixify.innertube.models.PodcastItem>>(emptyList())
    val episodesForLater = MutableStateFlow<List<SongItem>>(emptyList())

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val pinnedSpeedDialItems: StateFlow<List<SpeedDialItem>> =
        database.speedDialDao.getAll()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val speedDialItems: StateFlow<List<YTItem>> =
        combine(
            database.speedDialDao.getAll(),
            keepListening,
            quickPicks,
            homePage,
        ) { pinned, keepListening, quick, home ->
            buildSpeedDialItems(
                pinned = pinned.map { it.toYTItem() },
                keepListening =
                    keepListening.orEmpty().mapNotNull { item ->
                        when (item) {
                            is Song ->
                                SongItem(
                                    id = item.id,
                                    title = item.title,
                                    artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                    thumbnail = item.thumbnailUrl ?: "",
                                )

                            is Album ->
                                AlbumItem(
                                    browseId = item.id,
                                    playlistId = item.album.playlistId ?: "",
                                    title = item.title,
                                    artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                    year = item.album.year,
                                    thumbnail = item.thumbnailUrl ?: "",
                                )

                            is com.mixify.music.db.entities.Artist ->
                                ArtistItem(
                                    id = item.id,
                                    title = item.title,
                                    thumbnail = item.thumbnailUrl,
                                    shuffleEndpoint = null,
                                    radioEndpoint = null,
                                )

                            else -> null
                        }
                    },
                quickPicks =
                    quick.orEmpty().map { song ->
                        SongItem(
                            id = song.id,
                            title = song.title,
                            artists = song.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = song.thumbnailUrl ?: "",
                        )
                    },
                home = home?.sections.orEmpty().flatMap { it.items },
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getRandomItem(): YTItem? {
        try {
            isRandomizing.value = true
            // Visual feedback for the animation
            kotlinx.coroutines.delay(1000)

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            quickPicks.value?.let { songs ->
                userSongs.addAll(songs.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "",
                        explicit = false
                    )
                })
            }

            keepListening.value?.let { items ->
                items.forEach { item ->
                    when (item) {
                        is Song -> userSongs.add(SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        ))
                        is com.mixify.music.db.entities.Artist -> otherSources.add(ArtistItem(
                            id = item.id,
                            title = item.title,
                            thumbnail = item.thumbnailUrl,
                            shuffleEndpoint = null,
                            radioEndpoint = null
                        ))
                        else -> {}
                    }
                }
            }

            otherSources.addAll(allYtItems.value)

            // Probability: 80% User Songs, 20% Other Sources
            val item = if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

            return item
        } finally {
            isRandomizing.value = false
        }
    }

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

	val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 2, 1))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.safeDataStoreEdit {
                it[WrappedSeenKey] = true
            }
        }
    }
    private suspend fun getDailyDiscover() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        if (likedSongs.isEmpty()) return

        val seeds = likedSongs.shuffled().distinctBy { it.id }.take(5)
        
        // Use a synchronized list to collect results safely from concurrent coroutines
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            val recommendations = page.songs
                                .filter { item ->
                                    if (hideVideoSongs && item.isVideoSong) return@filter false
                                    if (item.explicit) return@filter false
                                    true
                                }
                                .shuffled()

                            // Simple check to avoid immediate duplicate of seed
                            val recommendation = recommendations.firstOrNull { rec ->
                                rec.id != seed.id
                            }

                            if (recommendation != null) {
                                items.add(
                                    DailyDiscoverItem(
                                        seed = seed,
                                        recommendation = recommendation,
                                        relatedEndpoint = endpoint
                                    )
                                )
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }
        
        // Final deduplication just in case multiple seeds recommended the same song
        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
    }

    private suspend fun getQuickPicks() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                val relatedSongs = database.quickPicks().first().filterVideoSongs(hideVideoSongs)
                val forgotten = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).take(8)

                // Get similar songs from YouTube based on recent listening
                val recentSong = database.latestEvent().first()?.song
                val ytSimilarSongs = mutableListOf<Song>()

                if (recentSong != null) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            // Convert YouTube songs to local Song format if they exist in database
                            page.songs.take(10).forEach { ytSong ->
                                database.song(ytSong.id).first()?.let { localSong ->
                                    if (!hideVideoSongs || !localSong.song.isVideo) {
                                        ytSimilarSongs.add(localSong)
                                    }
                                }
                            }
                        }
                    }
                }

                // Combine all sources and remove duplicates
                val combined = (relatedSongs + forgotten + ytSimilarSongs)
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)

                quickPicks.value = combined.ifEmpty { relatedSongs.shuffled().take(20) }
            }
            QuickPicks.LAST_LISTEN -> {
                val song = database.latestEvent().first()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().filterVideoSongs(hideVideoSongs).shuffled().take(20)
                }
            }
        }
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = LocalDateTime.now().minusWeeks(4)
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 5, offset = 0, toTimeStamp = LocalDateTime.now()).first()
            .shuffled().take(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    YouTube.artist(seed.id).onSuccess { page ->
                        page.sections.forEach { section ->
                            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    playlist.author?.name != seed.artist.name &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
            
            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    YouTube.playlist(playlist.id).onSuccess { page ->
                        val songs = page.songs.take(10)
                        if (songs.isNotEmpty()) {
                            // Use song count from the playlist page if available, otherwise use original
                            val songCountText = page.playlist.songCountText ?: playlist.songCountText
                            val updatedPlaylist = playlist.copy(songCountText = songCountText)
                            playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    private suspend fun load(forceRefresh: Boolean = false) {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        val fromTimeStamp = LocalDateTime.now().minusWeeks(2)

        // Phase 1: Load essential sections in parallel — local DB (fast) + YouTube home page.
        // isLoading is set to false as soon as all Phase 1 tasks complete so the UI appears quickly.
        coroutineScope {
            launch(Dispatchers.IO) { getQuickPicks() }

            launch(Dispatchers.IO) {
                forgottenFavorites.value = database.forgottenFavorites().first()
                    .filterVideoSongs(hideVideoSongs).shuffled().take(20)
            }

            launch(Dispatchers.IO) {
                val songs = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 15, offset = 5, toTimeStamp = LocalDateTime.now()).first()
                    .filterVideoSongs(hideVideoSongs).shuffled().take(10)
                val albums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first()
                    .filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                val artists = database.mostPlayedArtists(fromTimeStamp).first()
                    .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
                val items = mutableListOf<LocalItem>()
                items.add(
                    Playlist(
                        playlist = PlaylistEntity(
                            id = PlaylistEntity.LIKED_PLAYLIST_ID,
                            name = context.getString(R.string.liked),
                        ),
                        songCount = 0,
                        songThumbnails = emptyList()
                    )
                )
                val remainingSlots = MAX_KEEP_LISTENING_ITEMS - items.size
                items.addAll((songs + albums + artists).shuffled().take(remainingSlots))
                keepListening.value = items
            }

            launch(Dispatchers.IO) {
                val requestSessionKey = activeSessionKey
                val cacheKey = getHomeCacheKey(requestSessionKey)
                val cachedPage = if (!forceRefresh) InnerTubeApiCache.get<HomePage>(cacheKey) else null
                if (cachedPage != null) {
                    if (requestSessionKey == activeSessionKey) {
                        homePage.value = cachedPage.copy(
                            sections = cachedPage.sections.mapNotNull { section ->
                                val filtered = section.items
                                    .filterOutNulls()
                                    .filterExplicit(hideExplicit)
                                    .filterVideoSongs(hideVideoSongs)
                                    .filterYoutubeShorts(hideYoutubeShorts)
                                if (filtered.isEmpty()) null else section.copy(items = filtered)
                            }
                        )
                    }
                } else {
                    YouTube.home().onSuccess { page ->
                        if (requestSessionKey == activeSessionKey) {
                            InnerTubeApiCache.put(cacheKey, page)
                            homePage.value = page.copy(
                                sections = page.sections.mapNotNull { section ->
                                    val filtered = section.items
                                        .filterOutNulls()
                                        .filterExplicit(hideExplicit)
                                        .filterVideoSongs(hideVideoSongs)
                                        .filterYoutubeShorts(hideYoutubeShorts)
                                    if (filtered.isEmpty()) null else section.copy(items = filtered)
                                }
                            )
                        }
                    }.onFailure { reportException(it) }
                }
            }

            if (YouTube.cookie != null) {
                launch(Dispatchers.IO) { loadAccountInfo() }
                launch(Dispatchers.IO) { loadAccountPlaylists() }
                launch(Dispatchers.IO) { loadMixes(forceRefresh = forceRefresh) }
            }
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        isLoading.value = false

        // Phase 2: Heavy multi-request operations — run in background without blocking the UI.
        viewModelScope.launch(Dispatchers.IO) { getDailyDiscover() }

        viewModelScope.launch(Dispatchers.IO) { getCommunityPlaylists() }

        viewModelScope.launch(Dispatchers.IO) {
            YouTube.explore().onSuccess { page ->
                explorePage.value = page.copy(
                    newReleaseAlbums = page.newReleaseAlbums.filterOutNulls().filterExplicit(hideExplicit),
                    moodAndGenres = page.moodAndGenres.filterOutNulls()
                )
            }.onFailure { reportException(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(4)
                .mapNotNull {
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(it.id).onSuccess { page ->
                        page.sections.takeLast(3).forEach { section -> items += section.items }
                    }
                    SimilarRecommendation(
                        title = it,
                        items = items
                            .distinctBy { item -> item.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled().take(12)
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            val songRecommendations = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 15, offset = 0, toTimeStamp = LocalDateTime.now()).first()
                .filter { it.album != null }
                .shuffled().take(3)
                .mapNotNull { song ->
                    val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                        ?: return@mapNotNull null
                    val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                    SimilarRecommendation(
                        title = song,
                        items = (page.songs.shuffled().take(10) +
                                page.albums.shuffled().take(5) +
                                page.artists.shuffled().take(3) +
                                page.playlists.shuffled().take(3))
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            val albumRecommendations = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
                .filter { it.album.thumbnailUrl != null }
                .shuffled().take(2)
                .mapNotNull { album ->
                    val items = mutableListOf<YTItem>()
                    YouTube.album(album.id).onSuccess { page ->
                        page.otherVersions.let { items += it }
                    }
                    album.artists.firstOrNull()?.id?.let { artistId ->
                        YouTube.artist(artistId).onSuccess { page ->
                            page.sections.lastOrNull()?.items?.let { items += it }
                        }
                    }
                    SimilarRecommendation(
                        title = album,
                        items = items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled().take(10)
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            similarRecommendations.value = (artistRecommendations + songRecommendations + albumRecommendations).shuffled()
            allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                    homePage.value?.sections?.flatMap { it.items }.orEmpty()
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).mapNotNull { section ->
                    val filteredItems = section.items
                        .filterOutNulls()
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.mapNotNull { section ->
                    section.copy(items = section.items.filterOutNulls().filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                }
            )
            selectedChip.value = chip

            // Fetch podcast-specific data when podcasts chip is selected
            if (chip.title.contains("Podcast", ignoreCase = true)) {
                fetchPodcastData()
            }
        }
    }

    private suspend fun fetchPodcastData() {
        // Fetch saved podcast shows from official API
        YouTube.savedPodcastShows().onSuccess { shows ->
            savedPodcastShows.value = shows.filterOutNulls()
        }.onFailure {
            reportException(it)
        }

        // Fetch episodes for later from official API
        YouTube.episodesForLater().onSuccess { episodes ->
            episodesForLater.value = episodes.filterOutNulls()
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadAccountInfo() {
        val requestSessionKey = activeSessionKey
        YouTube.accountInfo().onSuccess { info ->
            if (requestSessionKey == activeSessionKey) {
                accountName.value = info.name
                accountImageUrl.value = info.thumbnailUrl
            }
        }.onFailure {
            reportException(it)
        }
    }

    private data class SessionStateData(
        val cookie: String?,
        val visitorData: String?,
        val dataSyncId: String?,
        val authUser: String,
        val savedAccountName: String?
    )

    @Volatile
    private var activeSessionKey: String = computeSessionFingerprint(
        cookie = YouTube.cookie,
        dataSyncId = YouTube.dataSyncId,
        authUser = YouTube.authUser
    )

    private var homeDataJob: Job? = null
    private var mixesJob: Job? = null

    private fun computeSessionFingerprint(
        cookie: String?,
        dataSyncId: String?,
        authUser: String
    ): String {
        if (cookie.isNullOrEmpty()) return "guest"
        val sapisid = cookie.split("; ")
            .find { it.startsWith("SAPISID=") || it.startsWith("__Secure-3PAPISID=") || it.startsWith("SID=") }
            ?.substringAfter("=") ?: cookie
        val rawString = "cookie:$sapisid|sync:${dataSyncId.orEmpty()}|auth:$authUser"
        val hash = rawString.hashCode().toUInt().toString(16)
        return "session_$hash"
    }

    fun getAccountSessionKey(): String = activeSessionKey

    private fun getHomeCacheKey(sessionKey: String = activeSessionKey): String = "home_$sessionKey"
    private fun getMixesCacheKey(sessionKey: String = activeSessionKey): String = "mixes_$sessionKey"

    private fun cancelSessionJobs() {
        homeDataJob?.cancel()
        mixesJob?.cancel()
    }

    private fun isPersonalizedMixSection(section: HomePage.Section): Boolean {
        val title = section.title.lowercase().trim()
        val label = section.label?.lowercase()?.trim() ?: ""

        val keywords = listOf(
            "mixed for you", "your mixes", "mixes", "mixed", "my supermix", "replay mix", "blend",
            "mezclas para ti", "tus mezclas", "mezclas",
            "für dich gemixt", "deine mixes",
            "sélection sur mesure", "vos mix",
            "seus mixes", "misturas",
            "i tuoi mix",
            "миксы",
            "ミックス", "あなた向けのミックス",
            "맞춤 믹스", "믹스",
            "為你推薦的合輯", "为你推荐的合辑", "合輯", "合辑"
        )
        if (keywords.any { title.contains(it) || label.contains(it) }) {
            return true
        }

        val hasPersonalizedPlaylists = section.items.filterIsInstance<PlaylistItem>().any { item ->
            item.id.startsWith("RD") || item.id.startsWith("VLRD") ||
            item.title.contains("Mix", ignoreCase = true) ||
            item.title.contains("Supermix", ignoreCase = true)
        }
        if (hasPersonalizedPlaylists) {
            return true
        }

        val endpoint = section.endpoint
        if (endpoint != null && (
            endpoint.browseId.contains("mix", ignoreCase = true) ||
            endpoint.params?.contains("mix", ignoreCase = true) == true
        )) {
            return true
        }

        return false
    }

    fun loadAccountPlaylists() {
        loadMixes()
    }

    fun loadMixes(forceRefresh: Boolean = false) {
        mixesJob?.cancel()
        val requestSessionKey = activeSessionKey

        mixesJob = viewModelScope.launch(Dispatchers.IO) {
            val hasAuth = !YouTube.cookie.isNullOrEmpty()
            if (!hasAuth) {
                mixesPlaylists.value = emptyList()
                Timber.d("MixFetch: requestSession=$requestSessionKey, currentSession=$activeSessionKey, homeSections=0, personalizedSectionFound=false, browseId=none, playlistCount=0 (Unauthenticated)")
                return@launch
            }

            val cacheKey = getMixesCacheKey(requestSessionKey)
            val homeCacheKey = getHomeCacheKey(requestSessionKey)

            if (forceRefresh) {
                InnerTubeApiCache.invalidate(cacheKey)
                InnerTubeApiCache.invalidate(homeCacheKey)
            } else {
                val cachedMixes = InnerTubeApiCache.get<List<PlaylistItem>>(cacheKey)
                if (cachedMixes != null) {
                    if (requestSessionKey == activeSessionKey) {
                        mixesPlaylists.value = cachedMixes
                        Timber.d("MixFetch: requestSession=$requestSessionKey, currentSession=$activeSessionKey, cached=true, playlistCount=${cachedMixes.size}")
                    }
                    return@launch
                }
            }

            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

            var page = if (!forceRefresh) homePage.value else null
            if (page == null && !forceRefresh) {
                page = InnerTubeApiCache.get<HomePage>(homeCacheKey)
            }
            if (page == null) {
                page = YouTube.home().getOrNull()
                if (page != null && requestSessionKey == activeSessionKey) {
                    InnerTubeApiCache.put(homeCacheKey, page)
                    homePage.value = page
                }
            }

            if (requestSessionKey != activeSessionKey) {
                Timber.d("MixFetch: Session changed during initial home fetch ($requestSessionKey vs $activeSessionKey). Discarding response.")
                return@launch
            }

            var homeSectionsCount = page?.sections?.size ?: 0
            var foundPersonalizedSection = false
            var safeBrowseId = "none"

            var mixedSection = page?.sections?.firstOrNull { isPersonalizedMixSection(it) }

            var currentContinuation = page?.continuation
            var attempts = 0
            while (mixedSection == null && currentContinuation != null && attempts < 3) {
                attempts++
                val nextHomePage = YouTube.home(currentContinuation).getOrNull() ?: break
                if (requestSessionKey != activeSessionKey) return@launch

                currentContinuation = nextHomePage.continuation
                val existingSections = homePage.value?.sections.orEmpty()
                val updatedPage = (homePage.value ?: page)?.copy(
                    sections = existingSections + nextHomePage.sections,
                    continuation = currentContinuation,
                )
                if (requestSessionKey == activeSessionKey) {
                    homePage.value = updatedPage
                    InnerTubeApiCache.put(homeCacheKey, updatedPage)
                }

                homeSectionsCount += nextHomePage.sections.size
                mixedSection = nextHomePage.sections.firstOrNull { isPersonalizedMixSection(it) }
            }

            val sectionEndpoint = mixedSection?.endpoint
            val items = mutableListOf<PlaylistItem>()

            if (mixedSection != null) {
                foundPersonalizedSection = true
            }

            if (sectionEndpoint != null) {
                safeBrowseId = sectionEndpoint.browseId
                val browseResult = YouTube.browse(sectionEndpoint.browseId, sectionEndpoint.params).getOrNull()
                if (requestSessionKey != activeSessionKey) return@launch

                if (browseResult != null) {
                    val allYTItems = browseResult.items.flatMap { it.items }
                    val fetchedPlaylists = allYTItems
                        .filterIsInstance<PlaylistItem>()
                        .filterOutNulls()
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    items.addAll(fetchedPlaylists)
                }
            }

            if (items.isEmpty() && mixedSection != null) {
                val directPlaylists = mixedSection.items
                    .filterIsInstance<PlaylistItem>()
                    .filterOutNulls()
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .filterYoutubeShorts(hideYoutubeShorts)
                items.addAll(directPlaylists)
            }

            val allHomeSections = (homePage.value ?: page)?.sections.orEmpty()
            val extraMixPlaylists = allHomeSections.flatMap { section ->
                section.items.filterIsInstance<PlaylistItem>()
            }.filter { item ->
                item.id.startsWith("RD") || item.id.startsWith("VLRD") ||
                item.title.contains("Mix", ignoreCase = true) ||
                item.title.contains("Supermix", ignoreCase = true) ||
                item.title.contains("Blend", ignoreCase = true)
            }.filterOutNulls()
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .filterYoutubeShorts(hideYoutubeShorts)

            items.addAll(extraMixPlaylists)

            val deduplicatedItems = items.distinctBy { it.id }

            if (requestSessionKey == activeSessionKey) {
                InnerTubeApiCache.put(cacheKey, deduplicatedItems)
                mixesPlaylists.value = deduplicatedItems
                Timber.d("MixFetch: requestSession=$requestSessionKey, currentSession=$activeSessionKey, homeSections=$homeSectionsCount, personalizedSectionFound=$foundPersonalizedSection, browseId=$safeBrowseId, playlistCount=${deduplicatedItems.size}")
            } else {
                Timber.d("MixFetch: Discarding final mix response because activeSessionKey changed ($requestSessionKey vs $activeSessionKey)")
            }
        }
    }

    /**
     * Safely filters out null items from a list whose type says non-null
     * but may contain nulls at runtime due to JSON parsing.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> List<T>.filterOutNulls(): List<T> =
        (this as List<T?>).filterNotNull()

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val currentChip = selectedChip.value
            if (currentChip != null) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                val nextSections = YouTube.home(params = currentChip.endpoint?.params).getOrNull()
                if (nextSections != null) {
                    homePage.value = nextSections.copy(
                        chips = homePage.value?.chips,
                        sections = nextSections.sections.mapNotNull { section ->
                            section.copy(items = section.items.filterOutNulls().filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                        }
                    )
                }
            } else {
                load()
            }
            isRefreshing.value = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wrappedManager.dispose()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }

        var wasOffline = !networkConnectivity.networkStatus.value
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectivity.networkStatus.collect { isConnected ->
                if (!isConnected) {
                    wasOffline = true
                } else if (wasOffline) {
                    wasOffline = false
                    refresh()
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            showWrappedCard.collect { shouldShow ->
                if (shouldShow && !wrappedManager.state.value.isDataReady) {
                    try {
                        wrappedManager.prepare()
                        val state = wrappedManager.state.first { it.isDataReady }
                        val trackMap = state.trackMap
                        if (trackMap.isNotEmpty()) {
                            val firstTrackId = trackMap.entries.first().value
                            wrappedAudioService.prepareTrack(firstTrackId)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map {
                    SessionStateData(
                        cookie = it[InnerTubeCookieKey],
                        visitorData = it[VisitorDataKey],
                        dataSyncId = it[DataSyncIdKey],
                        authUser = it[InnerTubeAuthUserKey] ?: "0",
                        savedAccountName = it[AccountNameKey]
                    )
                }
                .distinctUntilChanged()
                .collect { (cookie, visitorData, dataSyncId, authUser, savedAccountName) ->
                    val newSessionKey = computeSessionFingerprint(cookie, dataSyncId, authUser)
                    val oldSessionKey = activeSessionKey
                    val sessionChanged = newSessionKey != oldSessionKey

                    if (sessionChanged) {
                        Timber.d("AccountSwitch: oldSession=$oldSessionKey, newSession=$newSessionKey, personalizedStateCleared=true")
                        cancelSessionJobs()
                        activeSessionKey = newSessionKey

                        homePage.value = null
                        mixesPlaylists.value = null
                        accountPlaylists.value = null
                        savedPodcastShows.value = emptyList()
                        episodesForLater.value = emptyList()

                        InnerTubeApiCache.clear()
                    }

                    YouTube.cookie = cookie
                    if (!visitorData.isNullOrEmpty()) {
                        YouTube.visitorData = visitorData
                    }
                    YouTube.dataSyncId = dataSyncId
                    YouTube.authUser = authUser

                    val hasCookie = !cookie.isNullOrEmpty()
                    val hasVisitorData = !visitorData.isNullOrEmpty()
                    Timber.d("MixAuth: sessionFingerprint=$newSessionKey, hasCookie=$hasCookie, hasVisitorData=$hasVisitorData")

                    if (hasCookie) {
                        accountName.value = savedAccountName.orEmpty().ifBlank { "Guest" }
                        if (sessionChanged) {
                            loadAccountInfo()
                            loadHomeData(forceRefresh = true)
                        } else {
                            loadAccountInfo()
                        }
                    } else {
                        accountName.value = "Guest"
                        accountImageUrl.value = null
                        mixesPlaylists.value = emptyList()
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (YouTube.cookie != null && accountPlaylists.value != null) {
                        loadAccountPlaylists()
                    }
                }
        }
    }

    private var isHomeDataLoaded = false

    fun loadHomeData(forceRefresh: Boolean = false) {
        if (isHomeDataLoaded && !forceRefresh) return
        homeDataJob?.cancel()
        val requestSessionKey = activeSessionKey

        homeDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val cookie = context.dataStore.data
                    .map { it[InnerTubeCookieKey] }
                    .distinctUntilChanged()
                    .first()

                if (!cookie.isNullOrEmpty()) {
                    YouTube.cookie = cookie
                }

                if (requestSessionKey != activeSessionKey) return@launch

                isHomeDataLoaded = true
                load(forceRefresh = forceRefresh)
            } catch (e: Exception) {
                isHomeDataLoaded = false
                Timber.e(e, "Failed to load home data")
            }
        }
    }
}
