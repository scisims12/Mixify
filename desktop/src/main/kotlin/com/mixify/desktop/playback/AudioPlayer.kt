package com.mixify.desktop.playback

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

object AudioPlayer {
    private var mediaPlayerFactory: MediaPlayerFactory? = null
    private var mediaPlayer: EmbeddedMediaPlayer? = null
    private var isInitialized = false

    init {
        try {
            mediaPlayerFactory = MediaPlayerFactory()
            mediaPlayer = mediaPlayerFactory?.mediaPlayers()?.newEmbeddedMediaPlayer()
            isInitialized = true
            println("AudioPlayer: VLCJ initialized successfully.")
        } catch (e: Throwable) {
            isInitialized = false
            println("AudioPlayer: Failed to initialize VLCJ (VLC media player might not be installed on this system): ${e.message}")
        }
    }

    fun play(url: String) {
        if (!isInitialized || mediaPlayer == null) {
            println("AudioPlayer: Cannot play — VLCJ is not initialized (libvlc not found). URL: $url")
            return
        }
        try {
            mediaPlayer?.media()?.play(url)
            println("AudioPlayer: Playing URL: $url")
        } catch (e: Exception) {
            println("AudioPlayer: Error playing media: ${e.message}")
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.controls()?.stop()
            println("AudioPlayer: Stopped playback.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayerFactory?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
