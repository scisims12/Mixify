/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.utils

import java.util.Calendar
import java.time.LocalDate

const val FRIEND_BIRTHDAY_MONTH = 8  // August
const val FRIEND_BIRTHDAY_DAY = 4

fun cheerfulGreeting(): String {
    val today = LocalDate.now()
    val isFriendBirthday = today.monthValue == FRIEND_BIRTHDAY_MONTH && today.dayOfMonth == FRIEND_BIRTHDAY_DAY
    if (isFriendBirthday) {
        return "Happy Birthday Chuhiya! 🎂"
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val pool = when (hour) {
        in 5..11 -> listOf(
            "Rise and vibe",
            "Let's make some noise",
            "Start your day with a beat",
            "Good morning, sunshine"
        )
        in 12..16 -> listOf(
            "Keep the music going",
            "Midday mood, pick your tune",
            "Afternoon groove time"
        )
        in 17..20 -> listOf(
            "Unwind with some tunes",
            "Evening vibes loading",
            "Time to chill, evening's here"
        )
        else -> listOf(
            "Sweet melodies for tonight",
            "Late night listening mode",
            "Still up? Let's find the perfect track"
        )
    }
    return pool.random()
}

fun makeTimeString(duration: Long?): String {
    if (duration == null || duration < 0) return ""
    var sec = duration / 1000
    val day = sec / 86400
    sec %= 86400
    val hour = sec / 3600
    sec %= 3600
    val minute = sec / 60
    sec %= 60
    return when {
        day > 0 -> "%d:%02d:%02d:%02d".format(day, hour, minute, sec)
        hour > 0 -> "%d:%02d:%02d".format(hour, minute, sec)
        else -> "%d:%02d".format(minute, sec)
    }
}

fun joinByBullet(vararg str: String?) =
    str
        .filterNot {
            it.isNullOrEmpty()
        }.joinToString(separator = " • ")
