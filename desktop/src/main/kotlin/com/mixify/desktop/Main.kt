package com.mixify.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mixify.desktop.navigation.AppNavigationRail
import com.mixify.desktop.navigation.DesktopScreen
import com.mixify.desktop.navigation.DesktopScreens
import com.mixify.desktop.screens.HomeScreen
import com.mixify.desktop.screens.PlaylistDetailScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mixify Desktop"
    ) {
        MaterialTheme {
            var currentRoute by remember { mutableStateOf(DesktopScreen.Home.route) }
            var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

            Surface(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        navigationItems = DesktopScreens.MainScreens,
                        currentRoute = currentRoute,
                        onItemClick = { screen ->
                            currentRoute = screen.route
                            selectedPlaylistId = null
                        }
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (selectedPlaylistId != null) {
                            PlaylistDetailScreen(
                                playlistId = selectedPlaylistId!!,
                                onBack = { selectedPlaylistId = null }
                            )
                        } else {
                            when (currentRoute) {
                                DesktopScreen.Home.route -> HomeScreen(
                                    onPlaylistClick = { playlistId ->
                                        selectedPlaylistId = playlistId
                                    }
                                )
                                DesktopScreen.Search.route -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Search screen coming soon", style = MaterialTheme.typography.titleLarge)
                                }
                                DesktopScreen.Library.route -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Library screen coming soon", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
