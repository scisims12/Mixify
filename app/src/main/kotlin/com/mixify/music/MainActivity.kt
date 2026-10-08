/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.mixify.innertube.YouTube
import com.mixify.innertube.models.SongItem
import com.mixify.innertube.models.WatchEndpoint
import com.mixify.music.constants.AppBarHeight
import com.mixify.music.constants.AppLanguageKey
import com.mixify.music.constants.CheckForUpdatesKey
import com.mixify.music.constants.DarkModeKey
import com.mixify.music.constants.DefaultOpenTabKey
import com.mixify.music.constants.DismissedKmpUpdateKey
import com.mixify.music.constants.DismissedStandaloneUpdateKey
import com.mixify.music.constants.DensityScaleKey
import com.mixify.music.constants.DisableScreenshotKey
import com.mixify.music.constants.DynamicThemeKey
import com.mixify.music.constants.EnableHighRefreshRateKey
import com.mixify.music.constants.EnableLandscapeScalingKey
import com.mixify.music.constants.InnerTubeCookieKey
import com.mixify.music.constants.LastSeenVersionKey
import com.mixify.music.constants.ListenTogetherInTopBarKey
import com.mixify.music.constants.ListenTogetherUsernameKey
import com.mixify.music.constants.LyricsProviderOrderKey
import com.mixify.music.constants.MiniPlayerBottomSpacing
import com.mixify.music.constants.MiniPlayerHeight
import com.mixify.music.constants.NavigationBarAnimationSpec
import com.mixify.music.constants.NavigationBarHeight
import com.mixify.music.constants.PauseListenHistoryKey
import com.mixify.music.constants.OnboardingShownKey
import com.mixify.music.constants.OnboardingCompletedKey
import com.mixify.music.constants.PauseSearchHistoryKey
import com.mixify.music.constants.PreferredLyricsProvider
import com.mixify.music.constants.PreferredLyricsProviderKey
import com.mixify.music.constants.PureBlackKey
import com.mixify.music.constants.SYSTEM_DEFAULT
import com.mixify.music.constants.SelectedThemeColorKey
import com.mixify.music.constants.SimpMusicMigrationDoneKey
import com.mixify.music.constants.SlimNavBarKey
import com.mixify.music.constants.StopMusicOnTaskClearKey
import com.mixify.music.constants.UseNewMiniPlayerDesignKey
import com.mixify.music.constants.VideoThumbnailMigrationDoneKey
import com.mixify.music.db.MusicDatabase
import com.mixify.music.db.entities.SearchHistory
import com.mixify.music.listentogether.ListenTogetherManager
import com.mixify.music.models.toMediaMetadata
import com.mixify.music.playback.DownloadUtil
import com.mixify.music.playback.MusicService
import com.mixify.music.playback.MusicService.MusicBinder
import com.mixify.music.playback.PlayerConnection
import com.mixify.music.playback.queues.YouTubeQueue
import com.mixify.music.ui.component.AccountSettingsDialog
import com.mixify.music.ui.component.AppNavigationBar
import com.mixify.music.ui.component.AppNavigationRail
import com.mixify.music.ui.component.BottomSheetMenu
import com.mixify.music.ui.component.BottomSheetPage
import com.mixify.music.ui.component.LocalBottomSheetPageState
import com.mixify.music.ui.component.LocalMenuState
import com.mixify.music.ui.component.LocalLiquidGlassEnabled
import com.mixify.music.ui.component.rememberAppBackdrop
import com.mixify.music.ui.component.layerBackdrop
import com.mixify.music.ui.component.OnboardingOverlay
import com.mixify.music.ui.component.OnboardingStep
import com.mixify.music.ui.screens.onboarding.OnboardingFlow
import com.mixify.music.ui.component.rememberBottomSheetState
import com.mixify.music.ui.component.shimmer.ShimmerTheme
import com.mixify.music.ui.menu.YouTubeSongMenu
import com.mixify.music.ui.player.BottomSheetPlayer
import com.mixify.music.ui.screens.Screens
import com.mixify.music.ui.screens.navigationBuilder
import com.mixify.music.ui.screens.settings.ChangelogScreen
import com.mixify.music.ui.screens.settings.DarkMode
import com.mixify.music.ui.screens.settings.NavigationTab
import com.mixify.music.ui.theme.ColorSaver
import com.mixify.music.ui.theme.DefaultThemeColor
import com.mixify.music.ui.theme.MixifyTheme
import com.mixify.music.ui.theme.extractThemeColor
import com.mixify.music.ui.utils.appBarScrollBehavior
import com.mixify.music.ui.utils.resetHeightOffset
import com.mixify.music.utils.ReleaseInfo
import com.mixify.music.utils.SearchRoutes
import com.mixify.music.utils.SyncUtils
import com.mixify.music.utils.ArtistNameAliases
import com.mixify.music.utils.Updater
import com.mixify.music.utils.dataStore
import com.mixify.music.utils.safeDataStoreEdit
import com.mixify.music.utils.get
import com.mixify.music.utils.rememberEnumPreference
import com.mixify.music.utils.rememberPreference
import com.mixify.music.utils.setAppLocale
import com.mixify.music.viewmodels.HomeViewModel
import com.mixify.music.widget.PlaylistWidgetReceiver
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

private data class AvailableUpdate(
    val release: ReleaseInfo,
    val downloadUrl: String,
    val isKmp: Boolean,
) {
    val dismissalKey = if (isKmp) DismissedKmpUpdateKey else DismissedStandaloneUpdateKey
}

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.mixify.music.action.SEARCH"
        private const val ACTION_LIBRARY = "com.mixify.music.action.LIBRARY"
        const val ACTION_RECOGNITION = "com.mixify.music.action.RECOGNITION"
        const val ACTION_OPEN_WIDGET_TARGET = "com.mixify.music.action.OPEN_WIDGET_TARGET"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
        const val EXTRA_WIDGET_TARGET_TYPE = "widget_target_type"
        const val EXTRA_WIDGET_TARGET_ID = "widget_target_id"
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.BASE_VERSION_NAME)

    private var playerConnection: PlayerConnection? = null
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)

    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                playerConnectionSnapshot = playerConnection
                listenTogetherManager.setPlayerConnection(playerConnection)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
        }
    }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Service was not bound when attempting to unbind in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
            }
        }

        if (!MusicService.isRunning) {
            val serviceIntent = Intent(this, MusicService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to start foreground service")
            }
        }

        if (!isServiceBound) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
            isServiceBound = true
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            listenTogetherManager.disconnect()
        }
        super.onDestroy()
        val stopServiceOnClear = dataStore.get(StopMusicOnTaskClearKey, true) &&
                playerConnection?.isEffectivelyPlaying?.value == true &&
                isFinishing

        playerConnection?.dispose()
        playerConnection = null
        playerConnectionSnapshot = null

        safeUnbindService("onDestroy()")

        if (stopServiceOnClear) {
            stopService(Intent(this, MusicService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleWidgetTargetIntent(intent, navController)
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        listenTogetherManager.initialize()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val preferences = dataStore.data.first()
            val currentVersion = BuildConfig.BASE_VERSION_NAME

            if (preferences[SimpMusicMigrationDoneKey] != true) {
                safeDataStoreEdit { settings ->
                    val currentOrder = settings[LyricsProviderOrderKey] ?: ""
                    if (currentOrder.contains("SimpMusic")) {
                        val orderList = currentOrder.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() && it != "SimpMusic" }
                            .toMutableList()
                        settings[LyricsProviderOrderKey] = orderList.joinToString(",")
                    }
                    if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                        settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                    }
                    settings[SimpMusicMigrationDoneKey] = true
                    settings[LastSeenVersionKey] = currentVersion
                }
            }

            if (preferences[VideoThumbnailMigrationDoneKey] != true) {
                database.repairMissingVideoThumbnails()
                safeDataStoreEdit { settings ->
                    settings[VideoThumbnailMigrationDoneKey] = true
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            safeDataStoreEdit { settings ->
                settings[LastSeenVersionKey] = BuildConfig.BASE_VERSION_NAME
            }
        }

        setContent {
            MixifyApp(
                latestVersionName = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection = playerConnectionSnapshot,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MixifyApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val context = LocalContext.current
        val onboardingCompletedState = remember {
            context.dataStore.data
                .map { it[OnboardingCompletedKey] ?: false }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = null)

        LaunchedEffect(onboardingCompletedState.value) {
            val state = onboardingCompletedState.value
            Timber.tag("Startup").d(
                "Startup: onboardingCompletedState = $state, isFirstRun = ${state == false}, homeAllowed = ${state == true}"
            )
        }
        val scope = rememberCoroutineScope()

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val enableLandscapeScaling by rememberPreference(EnableLandscapeScalingKey, defaultValue = false)
        val userDensityScale by rememberPreference(DensityScaleKey, defaultValue = 1f)
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)

        val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

        val showChangelog = rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(enableHighRefreshRate) {
            val window = this@MainActivity.window
            val layoutParams = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (enableHighRefreshRate) {
                    layoutParams.preferredDisplayModeId = 0
                } else {
                    val modes = window.windowManager.defaultDisplay.supportedModes
                    val mode60 = modes.firstOrNull { abs(it.refreshRate - 60f) < 1f }
                        ?: modes.minByOrNull { abs(it.refreshRate - 60f) }

                    if (mode60 != null) {
                        layoutParams.preferredDisplayModeId = mode60.modeId
                    }
                }
            } else {
                if (enableHighRefreshRate) {
                    layoutParams.preferredRefreshRate = 0f
                } else {
                    layoutParams.preferredRefreshRate = 60f
                }
            }
            window.attributes = layoutParams
        }

        val (checkForUpdates) = rememberPreference(CheckForUpdatesKey, defaultValue = true)
        var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }

        var downloadId by remember { mutableLongStateOf(-1L) }
        var downloadProgress by remember { mutableFloatStateOf(0f) }
        var isDownloading by remember { mutableStateOf(false) }
        val downloadManager = remember { this@MainActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

        LaunchedEffect(downloadId) {
            if (downloadId == -1L) return@LaunchedEffect
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                        downloadProgress = 1f
                        val fileName = if (availableUpdate?.isKmp == true) "Mixify-KMP.apk" else "Mixify-update.apk"
                        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                        if (file.exists()) {
                            val downloadUri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${this@MainActivity.packageName}.FileProvider",
                                file
                            )
                            installApk(downloadUri)
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                        downloadId = -1L
                        Toast.makeText(this@MainActivity, "Download failed", Toast.LENGTH_SHORT).show()
                    } else {
                        val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val downloadedBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalBytes = if (totalBytesIndex != -1) cursor.getLong(totalBytesIndex) else 0L
                        val downloadedBytes = if (downloadedBytesIndex != -1) cursor.getLong(downloadedBytesIndex) else 0L
                        if (totalBytes > 0) {
                            downloadProgress = downloadedBytes.toFloat() / totalBytes
                        }
                    }
                }
                cursor?.close()
                delay(1000)
            }
        }

        if (BuildConfig.UPDATER_AVAILABLE && checkForUpdates) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val result = Updater.checkForUpdate(forceRefresh = false).getOrNull()
                        if (result != null) {
                            val (releaseInfo, hasUpdate) = result
                            if (hasUpdate && releaseInfo != null) {
                                val downloadUrl = Updater.getDownloadUrlForCurrentVariant(releaseInfo)
                                if (downloadUrl != null) {
                                    withContext(Dispatchers.Main) {
                                        availableUpdate = AvailableUpdate(releaseInfo, downloadUrl, isKmp = false)
                                    }
                                }
                            }
                        }

                        // Fallback KMP check - Now with version comparison!
                        if (availableUpdate == null) {
                            Updater.getLatestKmpRelease().getOrNull()?.let { release ->
                                if (Updater.isUpdateAvailable(BuildConfig.BASE_VERSION_NAME, release.versionName)) {
                                    release.assets.firstOrNull()?.let { asset ->
                                        withContext(Dispatchers.Main) {
                                            availableUpdate = AvailableUpdate(release, asset.downloadUrl, isKmp = true)
                                        }
                                    }
                                }
                            }
                        }

                        availableUpdate?.let { update ->
                            withContext(Dispatchers.Main) {
                                onLatestVersionNameChange(update.release.versionName)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("Updater").e(e, "Background update check failed silently")
                    }
                }
            }
        }

        var themeColor by rememberSaveable(stateSaver = ColorSaver) {
            mutableStateOf(selectedThemeColor)
        }

        val themeColorCache = remember { mutableMapOf<String, Color>() }

        LaunchedEffect(selectedThemeColor) {
            if (!enableDynamicTheme) {
                themeColor = selectedThemeColor
            }
        }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            if (!enableDynamicTheme || playerConnection == null) {
                themeColor = selectedThemeColor
                return@LaunchedEffect
            }

            playerConnection.service.currentMediaMetadata
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collectLatest { song ->
                    if (song?.thumbnailUrl != null) {
                        val cached = themeColorCache[song.thumbnailUrl]
                        if (cached != null) {
                            withFrameNanos { }
                            themeColor = cached
                            return@collectLatest
                        }
                        withContext(Dispatchers.IO) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest.Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build(),
                                )
                                val extractedColor = result.image?.toBitmap()?.extractThemeColor() ?: selectedThemeColor
                                themeColorCache[song.thumbnailUrl] = extractedColor
                                withFrameNanos { }
                                themeColor = extractedColor
                            } catch (e: Exception) {
                                withFrameNanos { }
                                themeColor = selectedThemeColor
                            }
                        }
                    } else {
                        themeColor = selectedThemeColor
                    }
                }
        }

        MixifyTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            availableUpdate?.let { update ->
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false,
                        usePlatformDefaultWidth = false
                    ),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            val formattedVersion = buildAnnotatedString {
                                append(stringResource(R.string.update_available_title).split(" ").first() + " update ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                    append(update.release.versionName)
                                }
                            }

                            Text(
                                text = formattedVersion,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(8.dp))

                            val releaseDate = remember(update.release.releaseDate) {
                                try {
                                    val parsed = ZonedDateTime.parse(update.release.releaseDate)
                                    parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                                } catch (e: Exception) {
                                    update.release.releaseDate.take(10)
                                }
                            }
                            val asset = update.release.assets.find { it.downloadUrl == update.downloadUrl }
                            val sizeMb = asset?.let { "%.1f MB".format(it.size / (1024f * 1024f)) } ?: "Unknown"

                            Text(
                                text = "Released on: $releaseDate\nSize: $sizeMb",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(24.dp))

                            // Banner Card - Supports dynamic JPG from GitHub description
                            val bannerUrl = remember(update.release.description) {
                                // Regex to find the first markdown image link: ![alt](url)
                                val regex = """!\[.*?]\((.*?)\)""".toRegex()
                                regex.find(update.release.description)?.groupValues?.get(1)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.8f),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (bannerUrl != null) {
                                        AsyncImage(
                                            model = bannerUrl,
                                            contentDescription = "Update Banner",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        // Fallback to Styled Gradient if no image found
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "MIXIFY",
                                                    style = MaterialTheme.typography.displayMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "MUSIC",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    letterSpacing = 4.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    // Version badge overlay
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = update.release.versionName,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            val summary = update.release.description.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: ""
                            if (summary.isNotEmpty()) {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            Text(
                                text = stringResource(R.string.changelog),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            val changelogItems = remember(update.release.description) {
                                update.release.description
                                    .lines()
                                    .map { it.trim() }
                                    .filter { it.startsWith("-") || it.startsWith("*") }
                                    .map { it.removePrefix("-").removePrefix("*").trim() }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (changelogItems.isEmpty()) {
                                    Text(
                                        text = update.release.description.ifBlank { stringResource(R.string.changelog_empty) },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    changelogItems.forEach { item ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = item,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            if (isDownloading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Text(
                                        text = "Downloading update... ${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            Button(
                                onClick = {
                                    if (isDownloading) return@Button
                                    val fileName = if (update.isKmp) "Mixify-KMP.apk" else "Mixify-update.apk"
                                    val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
                                        .setTitle("Downloading Mixify Update")
                                        .setDescription("Version ${update.release.versionName}")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                        .setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, fileName)
                                        .setMimeType("application/vnd.android.package-archive")

                                    val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                                    if (file.exists()) file.delete()

                                    downloadId = downloadManager.enqueue(request)
                                    isDownloading = true
                                },
                                enabled = !isDownloading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Text(
                                    text = if (isDownloading) "Downloading..."
                                    else stringResource(if (update.isKmp) R.string.kmp_upgrade_action else R.string.update_action),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
                return@MixifyTheme
            }

            when (onboardingCompletedState.value) {
                null -> {
                    Timber.tag("Startup").d("Startup: waiting for onboarding preference from DataStore...")
                    return@MixifyTheme
                }
                false -> {
                    Timber.tag("Startup").d("Startup: rendering OnboardingFlow")
                    OnboardingFlow(onFinished = {
                        scope.launch {
                            safeDataStoreEdit { it[OnboardingCompletedKey] = true }
                        }
                    })
                    return@MixifyTheme
                }
                true -> {
                    Timber.tag("Startup").d("Startup: onboarding completed, rendering Home UI")
                }
            }

            val currentDensity = LocalDensity.current
            val windowInfo = LocalWindowInfo.current
            val containerSize = windowInfo.containerDpSize
            val smallestDimensionDp = minOf(containerSize.width, containerSize.height)

            val landscapeDensityScale = remember(smallestDimensionDp, enableLandscapeScaling) {
                if (enableLandscapeScaling) {
                    when {
                        smallestDimensionDp >= 840.dp -> 1.15f
                        smallestDimensionDp >= 720.dp -> 1.1f
                        smallestDimensionDp >= 600.dp -> 1.05f
                        else -> 1.0f
                    }
                } else {
                    1.0f
                }
            }
            val scaledDensity: Density = remember(currentDensity, landscapeDensityScale, userDensityScale) {
                Density(
                    density = currentDensity.density * landscapeDensityScale * userDensityScale,
                    fontScale = currentDensity.fontScale,
                )
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
                ) {
                    val density = LocalDensity.current
                    val cutoutInsets = WindowInsets.displayCutout
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                    val navController = rememberNavController()
                    this@MainActivity.navController = navController

                    LaunchedEffect(Unit) {
                        val lastSeenVersion = dataStore.data.first()[LastSeenVersionKey] ?: ""
                        val currentVersion = BuildConfig.BASE_VERSION_NAME
                        if (lastSeenVersion != currentVersion) {
                            showChangelog.value = true
                        }
                    }

                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
                    val isLoggedIn = remember(innerTubeCookie) { innerTubeCookie.isNotBlank() }

                    val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                    val navigationItems = remember(listenTogetherInTopBar, isLoggedIn) {
                        val base = if (listenTogetherInTopBar) {
                            Screens.MainScreens.filter { it != Screens.ListenTogether }
                        } else {
                            Screens.MainScreens
                        }
                        if (isLoggedIn) {
                            val mutable = base.toMutableList()
                            val libraryIndex = mutable.indexOf(Screens.Library)
                            if (libraryIndex != -1) {
                                mutable.add(libraryIndex, Screens.Mix)
                            } else {
                                mutable.add(Screens.Mix)
                            }
                            mutable
                        } else {
                            base
                        }
                    }
                    val routeIndexMap = remember(navigationItems) {
                        navigationItems.mapIndexed { i, s -> s.route to i }.toMap()
                    }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                    val (defaultOpenTabInt) = rememberPreference(DefaultOpenTabKey, defaultValue = NavigationTab.HOME.name)
                    val defaultOpenTab = remember(defaultOpenTabInt) {
                        try {
                            NavigationTab.valueOf(defaultOpenTabInt)
                        } catch (_: IllegalArgumentException) {
                            NavigationTab.HOME
                        }
                    }
                    val tabOpenedFromShortcut = remember {
                        when (intent?.action) {
                            ACTION_SEARCH -> NavigationTab.SEARCH
                            ACTION_LIBRARY -> NavigationTab.LIBRARY
                            else -> null
                        }
                    }

                    val topLevelScreens = remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Mix.route,
                            Screens.Library.route,
                            Screens.ListenTogether.route,
                            "settings",
                        )
                    }

                    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }

                    val onSearch: (String) -> Unit = remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                navController.navigate(SearchRoutes.resultRoute(searchQuery))

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        runCatching {
                                            database.insert(SearchHistory(query = searchQuery))
                                        }.onFailure { throwable ->
                                            Timber.tag("MainActivity").w(throwable, "Failed to save search history for query: %s", searchQuery)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val currentRoute by remember {
                        derivedStateOf { navBackStackEntry?.destination?.route }
                    }

                    val inSearchScreen by remember {
                        derivedStateOf { currentRoute?.startsWith("search/") == true }
                    }
                    val navigationItemRoutes = remember(navigationItems) {
                        navigationItems.map { it.route }.toSet()
                    }

                    val shouldShowNavigationBar = remember(currentRoute, navigationItemRoutes) {
                        currentRoute == null ||
                                navigationItemRoutes.contains(currentRoute) ||
                                currentRoute!!.startsWith("search/")
                    }

                    val isLandscape = windowInfo.containerDpSize.width > windowInfo.containerDpSize.height
                    val isTablet = windowInfo.containerDpSize.width >= 600.dp

                    val showRail = (isLandscape || isTablet) && !inSearchScreen

                    // Floating bottom navigation occupies 64.dp for the pill itself
                    // plus 6.dp vertical padding on both sides = 76.dp total.
                    val floatingNavBarHeight = 76.dp

                    val navPadding = if (shouldShowNavigationBar && !showRail) {
                        floatingNavBarHeight
                    } else {
                        0.dp
                    }

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset +
                                (if (!showRail && shouldShowNavigationBar) navPadding else 0.dp) +
                                (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                    val playerReadyState = playerConnection?.service?.isPlayerReady?.collectAsStateWithLifecycle()
                        ?: remember { mutableStateOf(false) }
                    val playerReady by playerReadyState
                    val activePlayerConnection = if (playerReady) playerConnection else null

                    val playerAwareWindowInsets = remember(
                        bottomInset,
                        shouldShowNavigationBar,
                        playerBottomSheetState.isDismissed,
                        showRail,
                        navPadding,
                        useNewMiniPlayerDesign,
                    ) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar && !showRail) {
                            bottom += navPadding
                        }
                        if (!playerBottomSheetState.isDismissed) {
                            bottom += MiniPlayerHeight
                            if (useNewMiniPlayerDesign) {
                                bottom += MiniPlayerBottomSpacing
                            }
                        }
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    val topAppBarScrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        },
                    )

                    // Navigation tracking
                    LaunchedEffect(navBackStackEntry) {
                        if (inSearchScreen) {
                            val searchQuery = SearchRoutes.decodeQuery(navBackStackEntry?.arguments?.getString("query").orEmpty())
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }

                        // Reset scroll behavior for main navigation items
                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }

                        // Collapse player when navigating to equalizer
                        if (navBackStackEntry?.destination?.route == "equalizer" && playerBottomSheetState.isExpanded) {
                            playerBottomSheetState.collapseSoft()
                        }

                        // Track previous tab for animations
                        navController.currentBackStackEntry?.destination?.route?.let {
                            setPreviousTab(it)
                        }
                    }

                    LaunchedEffect(activePlayerConnection) {
                        val player = runCatching { activePlayerConnection?.player }.getOrNull()
                        if (player?.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                            return@LaunchedEffect
                        }

                        if (playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }

                    DisposableEffect(activePlayerConnection, playerBottomSheetState) {
                        val player = runCatching { activePlayerConnection?.player }.getOrNull()
                            ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                    mediaItem != null &&
                                    playerBottomSheetState.isDismissed
                                ) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    val (onboardingShown, setOnboardingShown) = rememberPreference(OnboardingShownKey, defaultValue = false)
                    var showOnboarding by remember { mutableStateOf(false) }

                    var historyIconBounds by remember { mutableStateOf<Rect?>(null) }
                    var statsIconBounds by remember { mutableStateOf<Rect?>(null) }
                    var listenTogetherIconBounds by remember { mutableStateOf<Rect?>(null) }
                    var accountIconBounds by remember { mutableStateOf<Rect?>(null) }

                    LaunchedEffect(onboardingShown, historyIconBounds, statsIconBounds, listenTogetherIconBounds, accountIconBounds) {
                        if (!onboardingShown &&
                            historyIconBounds != null &&
                            statsIconBounds != null &&
                            listenTogetherIconBounds != null &&
                            accountIconBounds != null
                        ) {
                            showOnboarding = true
                        }
                    }

                    LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                        val currentRoute = navBackStackEntry?.destination?.route
                        val isListenTogetherScreen =
                            currentRoute == Screens.ListenTogether.route ||
                                    currentRoute == "listen_together_from_topbar"
                        shouldShowTopBar = currentRoute in topLevelScreens &&
                                currentRoute != "settings" &&
                                !(isListenTogetherScreen && listenTogetherInTopBar)
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember { mutableStateOf(null) }
                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleWidgetTargetIntent(pendingIntent!!, navController)
                            handleRecognitionIntent(pendingIntent!!, navController)
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleWidgetTargetIntent(intent, navController)
                            handleRecognitionIntent(intent, navController)
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent ->
                            handleWidgetTargetIntent(intent, navController)
                            handleRecognitionIntent(intent, navController)
                            handleDeepLinkIntent(intent, navController)
                        }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitleRes = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Mix.route -> R.string.mixes
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            Screens.ListenTogether.route -> R.string.together
                            else -> null
                        }
                    }

                    var showAccountDialog by remember { mutableStateOf(false) }

                    val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                    val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
                    val showHistoryButton = remember(pauseListenHistory, eventCount) {
                        !(pauseListenHistory && eventCount == 0)
                    }

                    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                    val artistNameAliases by ArtistNameAliases.aliases.collectAsStateWithLifecycle()

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalNavController provides navController,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        LocalListenTogetherManager provides listenTogetherManager,
                        LocalChangelogState provides showChangelog,
                        LocalArtistNameAliases provides artistNameAliases,
                    ) {
                        if (showChangelog.value) {
                            ChangelogScreen(onDismiss = { showChangelog.value = false })
                        }

                        val appBackdrop = rememberAppBackdrop(MaterialTheme.colorScheme.surface)

                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                AnimatedVisibility(
                                    visible = shouldShowTopBar,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                                ) {
                                    TopAppBar(
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                val currentRoute = navBackStackEntry?.destination?.route
                                                if (currentRoute == Screens.Home.route || currentRoute == Screens.Mix.route || currentRoute == Screens.Library.route) {
                                                    Image(
                                                        painter = painterResource(R.drawable.ic_app_logo),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                                Text(
                                                    text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = Color(0xFF03FFB8),
                                                )
                                            }
                                        },
                                        actions = {
                                            if (showHistoryButton) {
                                                IconButton(
                                                    onClick = { navController.navigate("history") },
                                                    modifier = Modifier.onGloballyPositioned { historyIconBounds = it.boundsInWindow() }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.history),
                                                        contentDescription = stringResource(R.string.history),
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { navController.navigate("stats") },
                                                modifier = Modifier.onGloballyPositioned { statsIconBounds = it.boundsInWindow() }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.stats),
                                                    contentDescription = stringResource(R.string.stats),
                                                )
                                            }
                                            if (listenTogetherInTopBar) {
                                                IconButton(
                                                    onClick = { navController.navigate("listen_together_from_topbar") },
                                                    modifier = Modifier.onGloballyPositioned { listenTogetherIconBounds = it.boundsInWindow() }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.group_outlined),
                                                        contentDescription = stringResource(R.string.together),
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { showAccountDialog = true },
                                                modifier = Modifier.onGloballyPositioned { accountIconBounds = it.boundsInWindow() }
                                            ) {
                                                BadgedBox(badge = {
                                                    if (latestVersionName != BuildConfig.BASE_VERSION_NAME) {
                                                        Badge()
                                                    }
                                                }) {
                                                    if (accountImageUrl != null) {
                                                        AsyncImage(
                                                            model = accountImageUrl,
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape),
                                                        )
                                                    } else {
                                                        Icon(
                                                            painter = painterResource(R.drawable.account),
                                                            contentDescription = stringResource(R.string.account),
                                                            modifier = Modifier.size(24.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        scrollBehavior = topAppBarScrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        modifier = Modifier.windowInsetsPadding(
                                            if (showRail) {
                                                WindowInsets(left = NavigationBarHeight)
                                                    .add(cutoutInsets.only(WindowInsetsSides.Start))
                                            } else {
                                                cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                            },
                                        ),
                                    )
                                }
                            },
                            bottomBar = {
                                val currentBackStackEntry = navController.currentBackStackEntry

                                val onNavItemClick: (Screens, Boolean) -> Unit = remember(
                                    navController,
                                    coroutineScope,
                                    topAppBarScrollBehavior,
                                    playerBottomSheetState,
                                    currentBackStackEntry,
                                ) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }
                                        if (isSelected) {
                                            val targetEntry = try {
                                                val route = navController.currentBackStackEntry?.destination?.route
                                                if (route == SearchRoutes.ROUTE || route == "search_input") {
                                                    navController.getBackStackEntry("search_input")
                                                } else {
                                                    navController.currentBackStackEntry
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }

                                            if (screen == Screens.Search) {
                                                val current = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                                targetEntry?.savedStateHandle?.set("scrollToTopCount", current + 1)
                                            } else {
                                                targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                            }

                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                                val onSearchLongClick: () -> Unit = remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }



                                if (!showRail && currentRoute != "wrapped") {
                                    Box {
                                        // BottomSheetPlayer is rendered first
                                        if (activePlayerConnection != null) {
                                            BottomSheetPlayer(
                                                state = playerBottomSheetState,
                                                navController = navController,
                                                pureBlack = pureBlack,
                                                backdrop = appBackdrop,
                                            )
                                        }

                                        // AppNavigationBar is rendered on top of BottomSheetPlayer when collapsed,
                                        // so Home, Search, and Library buttons receive all touch events without obstruction.
                                        // When the player expands (progress > 0.05f), AppNavigationBar is removed from the composition
                                        // so all expanded player controls (Lyrics, Queue, Sleep Timer, etc.) receive all touches.
                                        if (playerBottomSheetState.progress <= 0.05f) {
                                            AppNavigationBar(
                                                navigationItems = navigationItems,
                                                currentRoute = currentRoute,
                                                onItemClick = onNavItemClick,
                                                pureBlack = pureBlack,
                                                slimNav = slimNav,
                                                backdrop = appBackdrop,
                                                liquidGlassEnabled = LocalLiquidGlassEnabled.current,
                                                onSearchLongClick = onSearchLongClick,
                                                onHomeLongHold = { showAccountDialog = true },
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .height(bottomInset + navPadding)
                                                    .padding(bottom = bottomInset),
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .height(bottomInsetDp)
                                                    .background(baseBg),
                                            )
                                        }
                                    }
                                } else if (currentRoute != "wrapped") {
                                    if (activePlayerConnection != null) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                            backdrop = appBackdrop,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                        ) {
                            Row(Modifier.fillMaxSize()) {
                                val onRailItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }

                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                }

                                if (showRail && currentRoute != "wrapped") {
                                    AppNavigationRail(
                                        navigationItems = navigationItems,
                                        currentRoute = currentRoute,
                                        onItemClick = onRailItemClick,
                                        pureBlack = pureBlack,
                                        onSearchLongClick = {
                                            navController.navigate("recognition") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onHomeLongHold = { showAccountDialog = true },
                                    )
                                }
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .layerBackdrop(appBackdrop),
                                ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                            NavigationTab.HOME -> Screens.Home
                                            NavigationTab.SEARCH -> Screens.Search
                                            NavigationTab.LIBRARY -> Screens.Library
                                        }.route,
                                        enterTransition = {
                                            val currentRouteIndex = routeIndexMap[targetState.destination.route] ?: -1
                                            val previousRouteIndex = routeIndexMap[initialState.destination.route] ?: -1

                                            if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                                slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                            } else {
                                                slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                            }
                                        },
                                        exitTransition = {
                                            val currentRouteIndex = routeIndexMap[initialState.destination.route] ?: -1
                                            val targetRouteIndex = routeIndexMap[targetState.destination.route] ?: -1

                                            if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                                slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                            } else {
                                                slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                            }
                                        },
                                        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                                    ) {
                                        navigationBuilder(
                                            navController = navController,
                                            scrollBehavior = topAppBarScrollBehavior,
                                            latestVersionName = latestVersionName,
                                            activity = this@MainActivity,
                                            snackbarHostState = snackbarHostState,
                                            homeViewModel = homeViewModel,
                                        )
                                    }
                                }
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )

                        if (showAccountDialog) {
                            AccountSettingsDialog(
                                onDismiss = {
                                    showAccountDialog = false
                                    homeViewModel.refresh()
                                },
                                latestVersionName = latestVersionName,
                            )
                        }

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = androidx.compose.material3.AlertDialogDefaults.containerColor,
                                        tonalElevation = androidx.compose.material3.AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showOnboarding) {
                            val steps = listOfNotNull(
                                historyIconBounds?.let { OnboardingStep(it, "History", "Find all your listening history here.") },
                                statsIconBounds?.let { OnboardingStep(it, "Stats", "Check out your music stats here.") },
                                listenTogetherIconBounds?.let { OnboardingStep(it, "Listen Together", "Listen to music together with friends.") },
                                accountIconBounds?.let { OnboardingStep(it, "Account & Settings", "Manage your account and app settings.") }
                            )
                            OnboardingOverlay(
                                steps = steps,
                                onFinish = {
                                    showOnboarding = false
                                    lifecycleScope.launch {
                                        safeDataStoreEdit { it[OnboardingShownKey] = true }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleRecognitionIntent(intent: Intent, navController: NavHostController) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private sealed class WidgetTargetRoute(val route: String) {
        data class LocalPlaylist(val id: String) : WidgetTargetRoute("local_playlist/$id")
        data class OnlinePlaylist(val id: String) : WidgetTargetRoute("online_playlist/$id")
        data object LikedSongs : WidgetTargetRoute("auto_playlist/liked")
        data object DownloadedSongs : WidgetTargetRoute("auto_playlist/downloaded")
        data class TopSongs(val limit: String) : WidgetTargetRoute("top_playlist/$limit")
    }

    private fun handleWidgetTargetIntent(intent: Intent, navController: NavHostController) {
        if (intent.action != ACTION_OPEN_WIDGET_TARGET) return
        val targetType = intent.getStringExtra(EXTRA_WIDGET_TARGET_TYPE)
        val targetId = intent.getStringExtra(EXTRA_WIDGET_TARGET_ID)
        intent.action = null
        val targetRoute = when (targetType) {
            PlaylistWidgetReceiver.TARGET_TYPE_LOCAL -> targetId?.let { WidgetTargetRoute.LocalPlaylist(it) }
            PlaylistWidgetReceiver.TARGET_TYPE_ONLINE -> targetId?.let { WidgetTargetRoute.OnlinePlaylist(it) }
            PlaylistWidgetReceiver.TARGET_TYPE_LIKED -> WidgetTargetRoute.LikedSongs
            PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED -> WidgetTargetRoute.DownloadedSongs
            PlaylistWidgetReceiver.TARGET_TYPE_TOP -> WidgetTargetRoute.TopSongs(targetId ?: "50")
            else -> null
        } ?: return
        navController.navigate(targetRoute.route)
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        val listenCode = uri.getQueryParameter("code") ?: uri.getQueryParameter("room") ?: uri.pathSegments.getOrNull(1)
        if (!listenCode.isNullOrBlank() && (uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true)) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> {
                val playlistId = uri.getQueryParameter("list") ?: return
                if (playlistId.startsWith("OLAK5uy_")) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                withContext(Dispatchers.Main) { navController.navigate("album/$browseId") }
                            }
                        }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }
            "browse" -> uri.lastPathSegment?.let { navController.navigate("album/$it") }
            "channel", "c" -> uri.lastPathSegment?.let { navController.navigate("artist/$it") }
            "search" -> uri.getQueryParameter("q")?.let { navController.navigate(SearchRoutes.resultRoute(it)) }
            else -> {
                val videoId = if (path == "watch") uri.getQueryParameter("v") else if (uri.host == "youtu.be") uri.pathSegments.firstOrNull() else null
                val playlistId = uri.getQueryParameter("list")
                if (videoId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        YouTube.queue(listOf(videoId), playlistId).onSuccess { queue ->
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId), queue.firstOrNull()?.toMediaMetadata()))
                            }
                        }
                    }
                } else if (playlistId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        YouTube.queue(null, playlistId).onSuccess { queue ->
                            val firstItem = queue.firstOrNull()
                            withContext(Dispatchers.Main) {
                                playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId), firstItem?.toMediaMetadata()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    private fun installApk(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<ListenTogetherManager?> { null }
val LocalChangelogState = staticCompositionLocalOf<MutableState<Boolean>> { error("No LocalChangelogState provided") }
val LocalArtistNameAliases = staticCompositionLocalOf<Map<String, String>> { emptyMap() }
val LocalIsPlayerExpanded = compositionLocalOf { false }
