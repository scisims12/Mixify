/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

const val MAX_FETCH_LOG_ENTRIES = 500

@Entity(tableName = "fetch_log")
data class FetchLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val songId: String,
    val songTitle: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
