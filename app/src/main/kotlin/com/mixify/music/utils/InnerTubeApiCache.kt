/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.utils

import java.util.concurrent.ConcurrentHashMap

object InnerTubeApiCache {
    const val DEFAULT_TTL_MS = 7 * 60 * 1000L // 7 minutes
    const val SHORT_TTL_MS = 3 * 60 * 1000L // 3 minutes
    const val LONG_TTL_MS = 15 * 60 * 1000L // 15 minutes

    class CacheEntry(
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(ttlMs: Long): Boolean = (System.currentTimeMillis() - timestamp) < ttlMs
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun <T> get(key: String, ttlMs: Long = DEFAULT_TTL_MS): T? {
        val entry = cache[key] ?: return null
        return if (entry.isValid(ttlMs)) {
            @Suppress("UNCHECKED_CAST")
            entry.data as? T
        } else {
            cache.remove(key)
            null
        }
    }

    fun <T> put(key: String, data: T) {
        if (data != null) {
            cache[key] = CacheEntry(data as Any)
        }
    }

    fun invalidate(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }
}
