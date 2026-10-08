/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.listentogether

object RoomCodeUtils {
    const val ROOM_CODE_LENGTH = 6

    fun normalizeRoomCode(input: String): String {
        return input.trim().uppercase()
    }

    fun sanitizeRoomCodeInput(input: String): String {
        val normalized = normalizeRoomCode(input)
        return if (normalized.length > ROOM_CODE_LENGTH) {
            normalized.take(ROOM_CODE_LENGTH)
        } else {
            normalized
        }
    }

    fun isValidRoomCode(input: String): Boolean {
        return normalizeRoomCode(input).length == ROOM_CODE_LENGTH
    }

    fun isJoinRoomEnabled(username: String, savedUsername: String, roomCodeInput: String): Boolean {
        val hasUsername = username.trim().isNotBlank() || savedUsername.isNotBlank()
        return hasUsername && isValidRoomCode(roomCodeInput)
    }
}
