/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.mixify.music.LocalPlayerAwareWindowInsets
import com.mixify.music.R
import com.mixify.music.ui.component.IconButton
import com.mixify.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.security),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "How we protect your privacy and data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Last updated: September 2026",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        val sections = listOf(
            "1. Overview" to "Mixify is a music player app that lets you search, stream, and organize music. This page explains what information the app accesses, what it stores, and what (if anything) leaves your device. Mixify is developed independently and is not affiliated with YouTube, Google, or any official streaming service.",
            "2. Account Login (Optional)" to "Mixify offers an optional sign-in feature that lets you connect your YouTube Music account to access personal playlists, likes, and history from that account. Login is never required to use the app — search, playback, and local library features work without signing in. If you choose to log in, authentication data (such as session cookies) is sent directly to YouTube's own servers to authenticate you — Mixify does not operate its own login servers and does not collect or store your account credentials. Login tokens/cookies are stored locally on your device only, used solely to keep you signed in. You can log out at any time from Settings, which clears this locally stored data.",
            "3. Search History & Local Data" to "Your search history, liked songs, playlists, and playback preferences are stored locally on your device in the app's own database. This data is never transmitted to any external server and is not accessible to the developer. You can clear this data at any time by clearing the app's storage/cache from your device settings, or using in-app options where available.",
            "4. Listen Together Feature" to "The \"Listen Together\" feature allows you to sync playback with friends in real time using a room code, via a WebSocket connection to a listen-together server. When using this feature, information required to sync playback (such as the current track, playback position, and a temporary username you choose) is relayed through the connected server so other participants in the room can stay in sync. This data is not stored beyond the active session — it is not logged, saved to a database, or used for any purpose other than real-time playback synchronization. You can view or change the server used for this feature in Settings → Listen Together. If you are not actively using Listen Together, no data related to this feature is sent anywhere.",
            "5. Streaming & Playback Data" to "To search for and play music, Mixify communicates directly with YouTube Music's public services to fetch song information and audio streams. This is the same type of request your device would make when using YouTube Music in a browser. Standard network information (such as your IP address) may be visible to YouTube as part of these requests, the same as with any music/video streaming service.",
            "6. Analytics & Crash Reporting" to "Mixify does not use any third-party analytics or advertising SDKs to track your behavior. The app may use standard crash-reporting tools solely to help identify and fix bugs; if enabled, this only includes technical information such as error logs and app version — never your personal music activity or account information.",
            "7. Data Sharing" to "Mixify does not sell, rent, or share your personal data with advertisers or third parties. Any data exchanged is limited strictly to what's necessary for the app's features to function, as described above (YouTube Music for streaming, and the Listen Together server only when that feature is actively used).",
            "8. Your Choices" to "Use the app entirely without logging in. Clear locally stored history/data anytime via device or in-app settings. Avoid using Listen Together if you don't want playback data relayed through a server. Uninstalling the app removes all locally stored data.",
            "9. Changes to This Policy" to "This privacy policy may be updated as the app's features evolve. Continued use of Mixify after changes are posted here constitutes acceptance of the updated policy.",
            "10. Contact" to "For questions about this privacy policy or the app, please contact us at priyanshusharm3674@gmail.com or open an issue on the project's GitHub repository."
        )

        sections.forEach { (heading, body) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val context = LocalContext.current
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:priyanshusharm3674@gmail.com".toUri()
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // ignore
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Official Support Email",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "priyanshusharm3674@gmail.com",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    TopAppBar(
        title = { Text("Privacy Policy") },
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
    )
}
