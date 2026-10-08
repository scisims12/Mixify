package com.mixify.desktop

import com.mixify.desktop.playback.StreamResolver
import com.mixify.innertube.YouTube
import com.mixify.innertube.models.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamResolverTest {
    @Test
    fun testResolveStreamUrl() {
        runBlocking {
            val homeResult = YouTube.home()
            assertTrue("Failed to fetch home: ${homeResult.exceptionOrNull()?.message}", homeResult.isSuccess)
            val page = homeResult.getOrNull()
            val playlistItem = page?.sections?.asSequence()
                ?.flatMap { it.items }
                ?.filterIsInstance<PlaylistItem>()
                ?.firstOrNull()

            assertTrue("No playlist found", playlistItem != null)
            val playlistResult = YouTube.playlist(playlistItem!!.id)
            assertTrue("Failed to fetch playlist", playlistResult.isSuccess)
            val playlistPage = playlistResult.getOrNull()
            val song = playlistPage?.songs?.firstOrNull()
            assertTrue("No songs in playlist", song != null)

            val result = StreamResolver.resolveStreamUrl(song!!.id)
            if (result.isSuccess) {
                val url = result.getOrNull()
                println("=== RESOLVED STREAM URL FOR '${song.title}' (${song.id}) ===")
                println(url)
                assertTrue("Stream URL is blank", !url.isNullOrBlank())
            } else {
                val err = result.exceptionOrNull()
                org.junit.Assert.fail("Failed to resolve stream for ${song.title} (${song.id}): ${err?.message}\n${err?.stackTraceToString()}")
            }
        }
    }
}
