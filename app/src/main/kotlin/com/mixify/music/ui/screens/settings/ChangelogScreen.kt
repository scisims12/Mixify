/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mixify.music.R
import com.mixify.music.BuildConfig
import com.mixify.music.utils.ReleaseInfo
import com.mixify.music.utils.Updater

private val markdownLinkRegex = Regex("(@[a-zA-Z0-9_-]+)|(https?://[\\w-]+(\\.[\\w-]+)+[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])")

private data class ChangelogSection(
    val header: String,
    val items: List<String>
)

private fun parseChangelogSections(text: String): List<ChangelogSection> {
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    val sections = mutableListOf<ChangelogSection>()
    var currentHeader = ""
    var currentItems = mutableListOf<String>()

    for (line in lines) {
        if (line.startsWith("#")) {
            if (currentHeader.isNotBlank() || currentItems.isNotEmpty()) {
                sections.add(ChangelogSection(currentHeader, currentItems))
                currentItems = mutableListOf()
            }
            val level = line.takeWhile { it == '#' }.length
            currentHeader = line.substring(level).trim()
        } else {
            currentItems.add(line)
        }
    }
    if (currentHeader.isNotBlank() || currentItems.isNotEmpty()) {
        sections.add(ChangelogSection(currentHeader, currentItems))
    }
    return sections
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogScreen(
    onDismiss: () -> Unit
) {
    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        Updater.getAllReleases().onSuccess { allReleases ->
            releases = allReleases.filter { release ->
                Updater.compareVersions(BuildConfig.BASE_VERSION_NAME, release.tagName) >= 0
            }
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    val showFab by remember {
        derivedStateOf { sheetState.targetValue != SheetValue.Hidden }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.changelog),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    val density = LocalDensity.current
                    val stroke = remember(density) {
                        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
                    }
                    LinearWavyProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        stroke = stroke,
                        trackStroke = stroke,
                        amplitude = { 1f }
                    )
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (releases.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.changelog_empty))
                        }
                    }
                } else {
                    items(releases) { release ->
                        ReleaseItem(release)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                AnimatedVisibility(
                    visible = showFab,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    val githubReleasesUrl = stringResource(R.string.github_releases_url)
                    ExtendedFloatingActionButton(
                        onClick = { uriHandler.openUri(githubReleasesUrl) },
                        icon = { Icon(painterResource(R.drawable.github), contentDescription = null, modifier = Modifier.size(24.dp)) },
                        text = { Text(stringResource(R.string.view_on_github)) },
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun ReleaseItem(release: ReleaseInfo) {
    val sections = remember(release.description) { parseChangelogSections(release.description) }
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = release.tagName,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = release.releaseDate.split("T").firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        sections.forEachIndexed { index, section ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (section.header.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = section.header,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        section.items.forEach { line ->
                            val trimmedLine = line.trim()
                            val isListItem = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")
                            val contentText = if (isListItem) {
                                trimmedLine.substring(2).trim()
                            } else {
                                trimmedLine
                            }

                            val annotatedString = buildAnnotatedString {
                                var lastIndex = 0
                                markdownLinkRegex.findAll(contentText).forEach { result ->
                                    append(contentText.substring(lastIndex, result.range.first))
                                    
                                    val match = result.value
                                    val link = if (match.startsWith("@")) "https://github.com/${match.substring(1)}" else match
                                    
                                    pushStringAnnotation(tag = "URL", annotation = link)
                                    withStyle(style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary, 
                                        fontWeight = if (match.startsWith("@")) FontWeight.Bold else FontWeight.Normal,
                                        textDecoration = if (match.startsWith("@")) TextDecoration.None else TextDecoration.Underline
                                    )) {
                                        append(match)
                                    }
                                    pop()
                                    lastIndex = result.range.last + 1
                                }
                                append(contentText.substring(lastIndex))
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (isListItem) {
                                    Text(
                                        text = stringResource(R.string.list_bullet),
                                        modifier = Modifier.padding(end = 8.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                ClickableText(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                uriHandler.openUri(annotation.item)
                                            }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
