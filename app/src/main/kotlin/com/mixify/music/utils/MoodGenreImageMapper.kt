/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.utils

import kotlin.math.abs

private val defaultFallbackPool = listOf(
    "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400",
    "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400",
    "https://images.unsplash.com/photo-1571330735066-03aaa9429d89?w=400",
    "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=400",
    "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=400",
    "https://images.unsplash.com/photo-1454922915609-78549ad709bb?w=400",
    "https://images.unsplash.com/photo-1508973379184-7517410fb0bc?w=400",
    "https://images.unsplash.com/photo-1524650359799-842906ca1c06?w=400",
    "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=400",
    "https://images.unsplash.com/photo-1483412033650-1015ddeb83d1?w=400",
    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=401",
    "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=400",
    "https://images.unsplash.com/photo-1521337581100-8ca9a73a5f79?w=400",
    "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400"
)

fun moodGenreImageUrl(title: String): String {
    val lower = title.lowercase()
    return when {
        "chill" in lower -> "https://images.unsplash.com/photo-1521017432531-fbd92d768814?w=400"
        "focus" in lower -> "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400"
        "commute" in lower -> "https://images.unsplash.com/photo-1494515843206-f3117d3f51b7?w=400"
        "energize" in lower || "workout" in lower || "gym" in lower -> "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=400"
        "party" in lower -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400"
        "feel good" in lower || "happy" in lower -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400"
        "romance" in lower || "love" in lower -> "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=400"
        "gaming" in lower -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400"
        "sleep" in lower || "relax" in lower -> "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400"
        "sad" in lower -> "https://images.unsplash.com/photo-1494883759339-0b042055a4ee?w=400"
        "podcast" in lower -> "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=400"
        else -> defaultFallbackPool[abs(title.hashCode()) % defaultFallbackPool.size]
    }
}
