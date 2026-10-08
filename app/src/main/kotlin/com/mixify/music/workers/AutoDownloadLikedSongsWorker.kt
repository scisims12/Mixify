/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.workers

import android.content.Context
import androidx.media3.exoplayer.offline.Download
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mixify.music.constants.AutoDownloadOnLikeKey
import com.mixify.music.constants.MAX_AUTO_DOWNLOAD_LIKED_SONGS
import com.mixify.music.db.MusicDatabase
import com.mixify.music.playback.DownloadUtil
import com.mixify.music.utils.dataStore
import com.mixify.music.utils.get
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class AutoDownloadLikedSongsWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun database(): MusicDatabase
        fun downloadUtil(): DownloadUtil
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        val database = entryPoint.database()
        val downloadUtil = entryPoint.downloadUtil()

        val enabled = applicationContext.dataStore.get(AutoDownloadOnLikeKey, false)
        if (!enabled) return Result.success()

        try {
            val likedSongs = database.likedSongsByCreateDateAsc().first()
            val downloadsMap = downloadUtil.downloads.value

            val sortedLikedSongs = likedSongs.reversed()
            var downloadedCount = sortedLikedSongs.count { song ->
                song.isDownloaded || downloadsMap[song.id]?.state == Download.STATE_COMPLETED
            }

            for (song in sortedLikedSongs) {
                if (downloadedCount >= MAX_AUTO_DOWNLOAD_LIKED_SONGS) break
                val isDownloaded = song.isDownloaded || downloadsMap[song.id]?.state == Download.STATE_COMPLETED
                if (!isDownloaded) {
                    downloadUtil.download(song)
                    downloadedCount++
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
