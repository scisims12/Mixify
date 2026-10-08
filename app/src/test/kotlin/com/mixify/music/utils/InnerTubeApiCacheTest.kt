package com.mixify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InnerTubeApiCacheTest {

    @Before
    fun setUp() {
        InnerTubeApiCache.clear()
    }

    @Test
    fun `put and get stores and retrieves cached data`() {
        val key = "home_accountA"
        val data = "account_a_mixes"
        InnerTubeApiCache.put(key, data)

        assertEquals("account_a_mixes", InnerTubeApiCache.get<String>(key))
    }

    @Test
    fun `invalidate removes single key`() {
        InnerTubeApiCache.put("home_accountA", "dataA")
        InnerTubeApiCache.put("home_accountB", "dataB")

        InnerTubeApiCache.invalidate("home_accountA")

        assertNull(InnerTubeApiCache.get<String>("home_accountA"))
        assertEquals("dataB", InnerTubeApiCache.get<String>("home_accountB"))
    }

    @Test
    fun `clear removes all account cached data on account switch or logout`() {
        InnerTubeApiCache.put("home_accountA", "dataA")
        InnerTubeApiCache.put("mixes_accountA", "mixesA")

        InnerTubeApiCache.clear()

        assertNull(InnerTubeApiCache.get<String>("home_accountA"))
        assertNull(InnerTubeApiCache.get<String>("mixes_accountA"))
    }

    @Test
    fun `account specific cache keys prevent cross-account data leakage`() {
        val accountACookieHash = "cookieA".hashCode()
        val accountBCookieHash = "cookieB".hashCode()

        val keyA = "home_$accountACookieHash"
        val keyB = "home_$accountBCookieHash"

        InnerTubeApiCache.put(keyA, "Account A HomePage")
        InnerTubeApiCache.put(keyB, "Account B HomePage")

        assertEquals("Account A HomePage", InnerTubeApiCache.get<String>(keyA))
        assertEquals("Account B HomePage", InnerTubeApiCache.get<String>(keyB))
    }
}
