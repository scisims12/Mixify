package com.mixify.desktop.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mixify.desktop.utils.NetworkImage
import com.mixify.innertube.YouTube
import com.mixify.innertube.models.PlaylistItem
import com.mixify.innertube.pages.HomePage

@Composable
fun HomeScreen(
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var homePageState by remember { mutableStateOf<Result<HomePage>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = YouTube.home()
        homePageState = result
        isLoading = false
    }

    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            homePageState?.isFailure == true -> {
                val error = homePageState?.exceptionOrNull()
                Text(
                    text = "Error fetching YouTube Music home: ${error?.message ?: "Unknown error"}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            homePageState?.isSuccess == true -> {
                val homePage = homePageState?.getOrNull()
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                ) {
                    Text(
                        text = "Mixify Home",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    homePage?.sections?.forEach { section ->
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            items(section.items) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    onClick = {
                                        val typeName = item::class.simpleName ?: "Unknown"
                                        println("Clicked item: '${item.title}' [Type: $typeName, ID: ${item.id}]")
                                        if (item is PlaylistItem) {
                                            onPlaylistClick(item.id)
                                        }
                                    },
                                    modifier = Modifier.width(160.dp).height(160.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        NetworkImage(
                                            url = item.thumbnail,
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxWidth().height(100.dp)
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2
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
}
