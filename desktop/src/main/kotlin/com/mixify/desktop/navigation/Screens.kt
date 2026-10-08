package com.mixify.desktop.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu

sealed class DesktopScreen(
    val title: String,
    val icon: ImageVector,
    val route: String,
) {
    object Home : DesktopScreen(
        title = "Home",
        icon = Icons.Default.Home,
        route = "home"
    )

    object Search : DesktopScreen(
        title = "Search",
        icon = Icons.Default.Search,
        route = "search"
    )

    object Library : DesktopScreen(
        title = "Library",
        icon = Icons.Default.Menu,
        route = "library"
    )
}

object DesktopScreens {
    val MainScreens = listOf(DesktopScreen.Home, DesktopScreen.Search, DesktopScreen.Library)
}
