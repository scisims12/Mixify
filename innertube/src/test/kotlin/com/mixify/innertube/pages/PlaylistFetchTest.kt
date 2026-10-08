package com.mixify.innertube.pages

import com.mixify.innertube.YouTube
import com.mixify.innertube.models.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PlaylistFetchTest {
    @Test
    fun testFetchPlaylist() {
        runBlocking {
            val homeResult = YouTube.home()
            if (homeResult.isSuccess) {
                val page = homeResult.getOrNull()
                val playlistItem = page?.sections?.asSequence()
                    ?.flatMap { it.items }
                    ?.filterIsInstance<PlaylistItem>()
                    ?.firstOrNull()

                if (playlistItem != null) {
                    println("Found playlist: ${playlistItem.title} (ID: ${playlistItem.id})")
                    val playlistResult = YouTube.playlist(playlistItem.id)
                    if (playlistResult.isSuccess) {
                        val playlistPage = playlistResult.getOrNull()
                        println("Successfully fetched playlist '${playlistPage?.playlist?.title}' with ${playlistPage?.songs?.size ?: 0} songs:")
                        playlistPage?.songs?.take(5)?.forEach { song ->
                            println(" - Song: '${song.title}' (ID: ${song.id})")
                        }
                    } else {
                        println("Failed to fetch playlist: ${playlistResult.exceptionOrNull()?.message}")
                    }
                } else {
                    println("No PlaylistItem found on Home page.")
                }
            } else {
                println("Failed to fetch Home page: ${homeResult.exceptionOrNull()?.message}")
            }
        }
    }
}
