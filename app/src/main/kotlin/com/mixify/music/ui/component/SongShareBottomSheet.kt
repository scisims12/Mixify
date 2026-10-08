/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.mixify.music.R
import com.mixify.music.di.LyricsHelperEntryPoint
import com.mixify.music.models.MediaMetadata
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ShareContentType {
    SONG,
    LYRICS,
}

data class ShareCardTheme(
    val id: Int,
    val name: String,
    val backgroundBrush: Brush,
    val primaryColor: Color,
    val secondaryColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color,
    val brandColor: Color,
    val previewColor: Color,
    val topColorHex: String,
    val bottomColorHex: String,
)

@Composable
fun SongShareBottomSheet(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
    initialLyrics: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var selectedContentType by remember { mutableStateOf(ShareContentType.SONG) }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    var lyricsText by remember { mutableStateOf(initialLyrics) }
    var isLoadingLyrics by remember { mutableStateOf(false) }
    var artworkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var themes by remember { mutableStateOf(createFallbackThemes()) }
    var isSharingImage by remember { mutableStateOf(false) }

    val shareUrl = "https://music.youtube.com/watch?v=${mediaMetadata.id}"

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Keep selectedContentType synchronized with pagerState.currentPage
    LaunchedEffect(pagerState.currentPage) {
        val targetType = when (pagerState.currentPage) {
            0 -> ShareContentType.SONG
            1 -> ShareContentType.LYRICS
            else -> ShareContentType.SONG
        }
        if (selectedContentType != targetType) {
            selectedContentType = targetType
        }
    }

    // Helper function when user taps a tab
    fun onTabSelected(type: ShareContentType) {
        val targetPage = when (type) {
            ShareContentType.SONG -> 0
            ShareContentType.LYRICS -> 1
        }
        selectedContentType = type
        scope.launch {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Load artwork bitmap for Palette and Card generation
    LaunchedEffect(mediaMetadata.thumbnailUrl) {
        if (!mediaMetadata.thumbnailUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(mediaMetadata.thumbnailUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bmp = result.image.toBitmap()
                        artworkBitmap = bmp
                        val palette = Palette.from(bmp).generate()
                        themes = generateThemesFromPalette(palette)
                    }
                } catch (_: Exception) {
                    // Fallback themes already set
                }
            }
        }
    }

    // Fetch lyrics if not provided
    LaunchedEffect(mediaMetadata.id) {
        if (lyricsText == null) {
            isLoadingLyrics = true
            withContext(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        LyricsHelperEntryPoint::class.java,
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetched = lyricsHelper.getLyrics(mediaMetadata)
                    if (fetched.lyrics.isNotBlank() && fetched.lyrics != "LYRICS_NOT_FOUND") {
                        lyricsText = fetched.lyrics
                    }
                } catch (_: Exception) {
                } finally {
                    isLoadingLyrics = false
                }
            }
        }
    }

    val currentTheme = themes.getOrElse(selectedThemeIndex) { themes.first() }

    // Helper to generate share card bitmap and execute share action
    fun executeShareWithCard(
        action: (imageUri: Uri?, shareUrl: String) -> Unit,
    ) {
        scope.launch {
            isSharingImage = true
            val uri = withContext(Dispatchers.IO) {
                ShareCardBitmapGenerator.generateCardUri(
                    context = context,
                    mediaMetadata = mediaMetadata,
                    contentType = selectedContentType,
                    lyricsText = lyricsText,
                    theme = currentTheme,
                    artworkBitmap = artworkBitmap,
                )
            }
            isSharingImage = false
            action(uri, shareUrl)
        }
    }

    // Responsive Card Width
    val screenWidth = configuration.screenWidthDp.dp
    val cardWidth = (screenWidth - 48.dp).coerceAtMost(330.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Horizontal Compact Preview Carousel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(
                    horizontal = ((screenWidth - cardWidth) / 2).coerceAtLeast(16.dp),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val cardContentType = when (page) {
                    0 -> ShareContentType.SONG
                    1 -> ShareContentType.LYRICS
                    else -> ShareContentType.SONG
                }

                Box(
                    modifier = Modifier.width(cardWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    when (cardContentType) {
                        ShareContentType.SONG -> MixifySongShareCard(
                            mediaMetadata = mediaMetadata,
                            theme = currentTheme,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        ShareContentType.LYRICS -> MixifyLyricsShareCard(
                            mediaMetadata = mediaMetadata,
                            lyricsText = lyricsText,
                            theme = currentTheme,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (isSharingImage) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Content Mode Segmented Selector (Song, Lyrics)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val isLyricsAvailable = !lyricsText.isNull_or_blank()
            val songLabel = stringResource(R.string.share_tab_song)
            val lyricsLabel = stringResource(R.string.share_tab_lyrics)

            ContentSegmentButton(
                label = songLabel,
                selected = selectedContentType == ShareContentType.SONG,
                enabled = true,
                onClick = { onTabSelected(ShareContentType.SONG) },
                modifier = Modifier.weight(1f),
            )

            ContentSegmentButton(
                label = if (isLoadingLyrics) "$lyricsLabel..." else lyricsLabel,
                selected = selectedContentType == ShareContentType.LYRICS,
                enabled = isLyricsAvailable,
                onClick = {
                    if (isLyricsAvailable) onTabSelected(ShareContentType.LYRICS)
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Theme Selector Circles
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            themes.forEachIndexed { index, theme ->
                val isSelected = index == selectedThemeIndex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.previewColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                )
                            },
                        )
                        .clickable { selectedThemeIndex = index },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Quick Share Destinations Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Copy Link
            ShareDestinationItem(
                iconRes = R.drawable.link,
                label = stringResource(R.string.share_link_copied).replace(" copied", "").replace("Copied", "Copy"),
                displayLabel = stringResource(R.string.copy_link),
                backgroundColor = Color(0xFF2C2D35),
                tint = Color.White,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Song Link", shareUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.resources.getString(R.string.share_link_copied), Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            )

            // WhatsApp
            ShareDestinationItem(
                iconRes = R.drawable.whatsapp,
                label = stringResource(R.string.share_to_whatsapp),
                backgroundColor = Color(0xFF25D366),
                tint = Color.White,
                enabled = true,
                onClick = {
                    executeShareWithCard { imageUri, url ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            val targetPkg = ShareAppUtils.getWhatsAppPackage(context)
                            if (targetPkg != null) setPackage(targetPkg)
                            if (imageUri != null) {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                            putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$url")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val chooser = Intent.createChooser(intent, context.resources.getString(R.string.share))
                            context.startActivity(chooser)
                        }
                        onDismiss()
                    }
                },
            )

            // Instagram Stories
            ShareDestinationItem(
                iconRes = R.drawable.instagram,
                label = stringResource(R.string.share_to_stories),
                backgroundColor = Color(0xFFE1306C),
                tint = Color.White,
                enabled = true,
                onClick = {
                    executeShareWithCard { imageUri, url ->
                        if (imageUri != null) {
                            val storyIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                                setDataAndType(imageUri, "image/*")
                                putExtra("interactive_asset_uri", imageUri)
                                putExtra("content_url", url)
                                putExtra("top_background_color", currentTheme.topColorHex)
                                putExtra("bottom_background_color", currentTheme.bottomColorHex)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                setPackage("com.instagram.android")
                            }
                            try {
                                context.startActivity(storyIntent)
                            } catch (_: Exception) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, imageUri)
                                    putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$url")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    setPackage("com.instagram.android")
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (_: Exception) {
                                    val chooser = Intent.createChooser(sendIntent, context.resources.getString(R.string.share))
                                    context.startActivity(chooser)
                                }
                            }
                        } else {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$url")
                                setPackage("com.instagram.android")
                            }
                            try {
                                context.startActivity(sendIntent)
                            } catch (_: Exception) {
                                val chooser = Intent.createChooser(sendIntent, context.resources.getString(R.string.share))
                                context.startActivity(chooser)
                            }
                        }
                        onDismiss()
                    }
                },
            )

            // Snapchat
            ShareDestinationItem(
                iconRes = R.drawable.snapchat,
                label = stringResource(R.string.share_to_snapchat),
                backgroundColor = Color(0xFFFFFC00),
                tint = Color.Black,
                enabled = true,
                onClick = {
                    executeShareWithCard { imageUri, url ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            setPackage("com.snapchat.android")
                            if (imageUri != null) {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                            putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$url")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val chooser = Intent.createChooser(intent, context.resources.getString(R.string.share))
                            context.startActivity(chooser)
                        }
                        onDismiss()
                    }
                },
            )

            // Text Message
            ShareDestinationItem(
                iconRes = R.drawable.message,
                label = stringResource(R.string.share_to_text_message),
                backgroundColor = Color(0xFF2C2D35),
                tint = Color.White,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                        putExtra("sms_body", "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$shareUrl")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
            )

            // More (System Chooser)
            ShareDestinationItem(
                iconRes = R.drawable.more_horiz,
                label = stringResource(R.string.share_more),
                backgroundColor = Color(0xFF2C2D35),
                tint = Color.White,
                onClick = {
                    executeShareWithCard { imageUri, url ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            if (imageUri != null) {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                            }
                            putExtra(Intent.EXTRA_TEXT, "${mediaMetadata.title} — ${mediaMetadata.artists.joinToString { it.name }}\n$url")
                        }
                        val chooser = Intent.createChooser(intent, context.resources.getString(R.string.share))
                        context.startActivity(chooser)
                        onDismiss()
                    }
                },
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TEMPLATE 1: COMPACT SONG CARD (Album Cover at Top Right)
// -----------------------------------------------------------------------------
@Composable
fun MixifySongShareCard(
    mediaMetadata: MediaMetadata,
    theme: ShareCardTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(theme.backgroundBrush)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Title & Artist on Left, Album Cover on Top Right Corner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        text = mediaMetadata.title.ifBlank { "Unknown Title" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name }.ifBlank { "Unknown Artist" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Album Cover in Top Right Corner
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!mediaMetadata.thumbnailUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = mediaMetadata.thumbnailUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        DefaultFallbackArtwork()
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Mixify Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mixify",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "· Shared from Mixify",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = theme.secondaryTextColor,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TEMPLATE 2: COMPACT LYRICS CARD (Sanitized Plain Text Lines)
// -----------------------------------------------------------------------------
@Composable
fun MixifyLyricsShareCard(
    mediaMetadata: MediaMetadata,
    lyricsText: String?,
    theme: ShareCardTheme,
    modifier: Modifier = Modifier,
) {
    val cleanLyrics = remember(lyricsText) { sanitizeLyrics(lyricsText) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(theme.backgroundBrush)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Small Artwork + Title + Artist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!mediaMetadata.thumbnailUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = mediaMetadata.thumbnailUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata.title.ifBlank { "Unknown Title" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name }.ifBlank { "Unknown Artist" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Area: Clean Plain Lyric Lines (Exactly up to 5 clean lines)
            Text(
                text = cleanLyrics,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor,
                lineHeight = 20.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Mixify Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mixify",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "· Shared from Mixify",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = theme.secondaryTextColor,
                )
            }
        }
    }
}

/**
 * Strips timestamps, time ranges, XML tags, curly-brace metadata (like {agent:v1}),
 * angle brackets, pipe tags, and karaoke markers from lyrics strings,
 * returning up to 5 clean plain lyric text lines.
 */
fun sanitizeLyrics(rawLyrics: String?): String {
    if (rawLyrics.isNullOrBlank() || rawLyrics == "LYRICS_NOT_FOUND") return "♪"

    val cleanedLines = rawLyrics.lines()
        .map { rawLine ->
            var line = rawLine.trim()

            // 1. Remove curly-brace metadata like {agent:v1} or any {...} tags
            line = line.replace(Regex("""\{[^}]*\}"""), "")

            // 2. Skip metadata header lines like [ar:...], [ti:...], [al:...]
            if (line.startsWith("[ar:") || line.startsWith("[ti:") ||
                line.startsWith("[al:") || line.startsWith("[by:") ||
                line.startsWith("[length:") || line.startsWith("[offset:")
            ) {
                return@map ""
            }

            // 3. Remove timestamps like [00:12.34], [01:23], <00:12.34>
            line = line.replace(Regex("""\[\d{1,2}:\d{2}(?:\.\d{1,3})?\]"""), "")
            line = line.replace(Regex("""<\d{1,2}:\d{2}(?:\.\d{1,3})?>"""), "")

            // 4. Remove word-sync markers like <word:12.34:56.78|word> or <Word:12.34|...>
            line = line.replace(Regex("""<[^>]*>"""), "")

            // 5. Remove pipe timing tags like |12.345
            line = line.replace(Regex("""\|\d+(?:\.\d+)?"""), "")

            line.trim()
        }
        .filter { it.isNotBlank() }
        .take(5)

    return if (cleanedLines.isNotEmpty()) {
        cleanedLines.joinToString("\n")
    } else {
        "♪"
    }
}

// Extension to safely check blank lyrics
private fun String?.isNull_or_blank(): Boolean {
    return this.isNullOrBlank() || this == "LYRICS_NOT_FOUND"
}

@Composable
private fun ContentSegmentButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                selected -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShareDestinationItem(
    iconRes: Int,
    label: String,
    backgroundColor: Color,
    tint: Color,
    displayLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayLabel ?: label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DefaultFallbackArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF253346), Color(0xFF0F1522)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

private fun createFallbackThemes(): List<ShareCardTheme> {
    return listOf(
        // Theme 0: Mixify Brand Signature
        ShareCardTheme(
            id = 0,
            name = "Mixify Dark",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF192435), Color(0xFF0F1522)),
            ),
            primaryColor = Color(0xFF03FFB8),
            secondaryColor = Color(0xFFFAA506),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            brandColor = Color(0xFF03FFB8),
            previewColor = Color(0xFF192435),
            topColorHex = "#192435",
            bottomColorHex = "#0F1522",
        ),
        // Theme 1: Crimson Velvet
        ShareCardTheme(
            id = 1,
            name = "Crimson",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4A0E17), Color(0xFF1F0509)),
            ),
            primaryColor = Color(0xFFFF4D6D),
            secondaryColor = Color(0xFFFFB3C1),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            brandColor = Color(0xFFFF4D6D),
            previewColor = Color(0xFF4A0E17),
            topColorHex = "#4A0E17",
            bottomColorHex = "#1F0509",
        ),
        // Theme 2: Midnight Purple
        ShareCardTheme(
            id = 2,
            name = "Midnight",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B124C), Color(0xFF140824)),
            ),
            primaryColor = Color(0xFF854F6C),
            secondaryColor = Color(0xFFDFB6B2),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            brandColor = Color(0xFFDFB6B2),
            previewColor = Color(0xFF2B124C),
            topColorHex = "#2B124C",
            bottomColorHex = "#140824",
        ),
        // Theme 3: Deep Emerald
        ShareCardTheme(
            id = 3,
            name = "Emerald",
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0D3B2E), Color(0xFF051913)),
            ),
            primaryColor = Color(0xFF6D3B47),
            secondaryColor = Color(0xFF03FFB8),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            brandColor = Color(0xFF03FFB8),
            previewColor = Color(0xFF0D3B2E),
            topColorHex = "#0D3B2E",
            bottomColorHex = "#051913",
        ),
    )
}

private fun generateThemesFromPalette(palette: Palette): List<ShareCardTheme> {
    val vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) }
    val dominant = palette.dominantSwatch?.rgb?.let { Color(it) }
    val darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
    val darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) }
    val lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) }

    val base1 = vibrant ?: dominant ?: Color(0xFF2C3E50)
    val base2 = darkVibrant ?: darkMuted ?: Color(0xFF1A252F)
    val base3 = lightVibrant ?: Color(0xFF34495E)

    fun Color.adjustForBackground(): Color {
        return Color(
            red = red * 0.45f,
            green = green * 0.45f,
            blue = blue * 0.45f,
            alpha = 1f,
        )
    }

    fun Color.toHex(): String {
        val argb = toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }

    val theme0 = ShareCardTheme(
        id = 0,
        name = "Artwork Dominant",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(base1.adjustForBackground(), base2.adjustForBackground()),
        ),
        primaryColor = base1,
        secondaryColor = base2,
        textColor = Color.White,
        secondaryTextColor = Color.White.copy(alpha = 0.8f),
        brandColor = Color(0xFF03FFB8),
        previewColor = base1,
        topColorHex = base1.adjustForBackground().toHex(),
        bottomColorHex = base2.adjustForBackground().toHex(),
    )

    val theme1 = ShareCardTheme(
        id = 1,
        name = "Deep Shadow",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(base2.adjustForBackground(), Color(0xFF0D1117)),
        ),
        primaryColor = base2,
        secondaryColor = base1,
        textColor = Color.White,
        secondaryTextColor = Color.White.copy(alpha = 0.75f),
        brandColor = Color(0xFF03FFB8),
        previewColor = base2,
        topColorHex = base2.adjustForBackground().toHex(),
        bottomColorHex = "#0D1117",
    )

    val theme2 = ShareCardTheme(
        id = 2,
        name = "Vibrant Mood",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(base3.adjustForBackground(), base1.adjustForBackground()),
        ),
        primaryColor = base3,
        secondaryColor = base1,
        textColor = Color.White,
        secondaryTextColor = Color.White.copy(alpha = 0.8f),
        brandColor = Color(0xFF03FFB8),
        previewColor = base3,
        topColorHex = base3.adjustForBackground().toHex(),
        bottomColorHex = base1.adjustForBackground().toHex(),
    )

    val theme3 = ShareCardTheme(
        id = 3,
        name = "Mixify Signature",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF192435), Color(0xFF0F1522)),
        ),
        primaryColor = Color(0xFF03FFB8),
        secondaryColor = Color(0xFFFAA506),
        textColor = Color.White,
        secondaryTextColor = Color.White.copy(alpha = 0.8f),
        brandColor = Color(0xFF03FFB8),
        previewColor = Color(0xFF192435),
        topColorHex = "#192435",
        bottomColorHex = "#0F1522",
    )

    return listOf(theme0, theme1, theme2, theme3)
}

// App package checking helper
object ShareAppUtils {
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getWhatsAppPackage(context: Context): String? {
        if (isAppInstalled(context, "com.whatsapp")) return "com.whatsapp"
        if (isAppInstalled(context, "com.whatsapp.w4b")) return "com.whatsapp.w4b"
        return null
    }

    fun isInstagramInstalled(context: Context): Boolean = isAppInstalled(context, "com.instagram.android")
    fun isSnapchatInstalled(context: Context): Boolean = isAppInstalled(context, "com.snapchat.android")
}

// Bitmap Share Card Generator
object ShareCardBitmapGenerator {
    fun generateCardUri(
        context: Context,
        mediaMetadata: MediaMetadata,
        contentType: ShareContentType,
        lyricsText: String?,
        theme: ShareCardTheme,
        artworkBitmap: Bitmap?,
    ): Uri? {
        return try {
            val width = 1080
            val height = 1080 // Compact 1:1 format for exported share cards
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val topColor = try {
                android.graphics.Color.parseColor(theme.topColorHex)
            } catch (_: Exception) {
                android.graphics.Color.parseColor("#192435")
            }
            val bottomColor = try {
                android.graphics.Color.parseColor(theme.bottomColorHex)
            } catch (_: Exception) {
                android.graphics.Color.parseColor("#0F1522")
            }

            // 1. Background Gradient
            val bgPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val titleText = mediaMetadata.title.ifBlank { "Unknown Title" }
            val artistText = mediaMetadata.artists.joinToString { it.name }.ifBlank { "Unknown Artist" }

            when (contentType) {
                ShareContentType.SONG -> {
                    // Song Poster Template on Canvas: Top Right Corner artwork
                    val artworkSize = 240
                    val artworkLeft = 740f
                    val artworkTop = 100f
                    val artworkRect = RectF(artworkLeft, artworkTop, artworkLeft + artworkSize, artworkTop + artworkSize)

                    if (artworkBitmap != null) {
                        val path = Path().apply {
                            addRoundRect(artworkRect, 32f, 32f, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawBitmap(artworkBitmap, null, artworkRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        canvas.restore()
                    } else {
                        val placeholderPaint = Paint().apply { color = android.graphics.Color.parseColor("#192435") }
                        canvas.drawRoundRect(artworkRect, 32f, 32f, placeholderPaint)

                        val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_app_logo)
                        if (logoDrawable != null) {
                            val logoSize = 100
                            val logoLeft = (artworkLeft + (artworkSize - logoSize) / 2).toInt()
                            val logoTop = (artworkTop + (artworkSize - logoSize) / 2).toInt()
                            logoDrawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                            logoDrawable.draw(canvas)
                        }
                    }

                    // Title
                    val titlePaint = TextPaint().apply {
                        color = theme.textColor.toArgb()
                        textSize = 52f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, 600)
                        .setMaxLines(2)
                        .build()

                    canvas.save()
                    canvas.translate(100f, 100f)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    // Artist
                    val artistPaint = TextPaint().apply {
                        color = theme.secondaryTextColor.toArgb()
                        textSize = 38f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        isAntiAlias = true
                    }
                    val artistLayout = StaticLayout.Builder.obtain(artistText, 0, artistText.length, artistPaint, 600)
                        .setMaxLines(1)
                        .build()

                    canvas.save()
                    canvas.translate(100f, 100f + titleLayout.height + 16f)
                    artistLayout.draw(canvas)
                    canvas.restore()
                }

                ShareContentType.LYRICS -> {
                    // Lyrics Poster Template on Canvas
                    val thumbSize = 120
                    val thumbLeft = 100f
                    val thumbTop = 100f
                    val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)

                    if (artworkBitmap != null) {
                        val path = Path().apply {
                            addRoundRect(thumbRect, 24f, 24f, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawBitmap(artworkBitmap, null, thumbRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        canvas.restore()
                    }

                    // Title & Artist next to thumbnail
                    val titlePaint = TextPaint().apply {
                        color = theme.textColor.toArgb()
                        textSize = 42f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, 700)
                        .setMaxLines(1)
                        .build()

                    canvas.save()
                    canvas.translate(250f, 100f)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    val artistPaint = TextPaint().apply {
                        color = theme.secondaryTextColor.toArgb()
                        textSize = 34f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        isAntiAlias = true
                    }
                    val artistLayout = StaticLayout.Builder.obtain(artistText, 0, artistText.length, artistPaint, 700)
                        .setMaxLines(1)
                        .build()

                    canvas.save()
                    canvas.translate(250f, 100f + titleLayout.height + 8f)
                    artistLayout.draw(canvas)
                    canvas.restore()

                    // Main Sanitized Lyric Text
                    val excerpt = sanitizeLyrics(lyricsText)
                    val lyricTextPaint = TextPaint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 50f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }

                    val lyricLayout = StaticLayout.Builder.obtain(excerpt, 0, excerpt.length, lyricTextPaint, 880)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(20f, 1f)
                        .setMaxLines(5)
                        .build()

                    canvas.save()
                    canvas.translate(100f, 280f)
                    lyricLayout.draw(canvas)
                    canvas.restore()
                }
            }

            // Mixify Branding Row at Bottom
            val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_app_logo)
            if (logoDrawable != null) {
                val logoSize = 64
                val logoLeft = 100
                val logoTop = 920
                logoDrawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
                logoDrawable.draw(canvas)

                val brandPaint = TextPaint().apply {
                    color = theme.secondaryTextColor.toArgb()
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("Mixify · Shared from Mixify", (logoLeft + logoSize + 20).toFloat(), (logoTop + 44).toFloat(), brandPaint)
            }

            // Save to Cache and FileProvider
            val shareDir = File(context.cacheDir, "share")
            if (!shareDir.exists()) shareDir.mkdirs()
            val file = File(shareDir, "mixify_share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
