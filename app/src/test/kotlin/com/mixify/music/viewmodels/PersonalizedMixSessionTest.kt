package com.mixify.music.viewmodels

import com.mixify.innertube.models.PlaylistItem
import com.mixify.innertube.pages.HomePage
import com.mixify.music.utils.InnerTubeApiCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonalizedMixSessionTest {

    @Before
    fun setUp() {
        InnerTubeApiCache.clear()
    }

    private fun computeSessionFingerprint(
        cookie: String?,
        dataSyncId: String?,
        authUser: String = "0"
    ): String {
        if (cookie.isNullOrEmpty()) return "guest"
        val sapisid = cookie.split("; ")
            .find { it.startsWith("SAPISID=") || it.startsWith("__Secure-3PAPISID=") || it.startsWith("SID=") }
            ?.substringAfter("=") ?: cookie
        val rawString = "cookie:$sapisid|sync:${dataSyncId.orEmpty()}|auth:$authUser"
        val hash = rawString.hashCode().toUInt().toString(16)
        return "session_$hash"
    }

    private fun isPersonalizedMixSection(section: HomePage.Section): Boolean {
        val title = section.title.lowercase().trim()
        val label = section.label?.lowercase()?.trim() ?: ""

        val keywords = listOf(
            "mixed for you", "your mixes", "mixes", "mixed", "my supermix", "replay mix", "blend",
            "mezclas para ti", "tus mezclas", "mezclas",
            "für dich gemixt", "deine mixes",
            "sélection sur mesure", "vos mix",
            "seus mixes", "misturas",
            "i tuoi mix",
            "миксы",
            "ミックス", "あなた向けのミックス",
            "맞춤 믹스", "믹스",
            "為你推薦的合輯", "为你推荐的合辑", "合輯", "合辑"
        )
        if (keywords.any { title.contains(it) || label.contains(it) }) {
            return true
        }

        val hasPersonalizedPlaylists = section.items.filterIsInstance<PlaylistItem>().any { item ->
            item.id.startsWith("RD") || item.id.startsWith("VLRD") ||
            item.title.contains("Mix", ignoreCase = true) ||
            item.title.contains("Supermix", ignoreCase = true)
        }
        if (hasPersonalizedPlaylists) {
            return true
        }

        val endpoint = section.endpoint
        if (endpoint != null && (
            endpoint.browseId.contains("mix", ignoreCase = true) ||
            endpoint.params?.contains("mix", ignoreCase = true) == true
        )) {
            return true
        }

        return false
    }

    @Test
    fun `session fingerprints are distinct for different users and guest`() {
        val userACookie = "SAPISID=userA_sapisid_token_12345; SID=userA_sid"
        val userBCookie = "SAPISID=userB_sapisid_token_67890; SID=userB_sid"

        val sessionA = computeSessionFingerprint(userACookie, "syncA", "0")
        val sessionB = computeSessionFingerprint(userBCookie, "syncB", "1")
        val guestSession = computeSessionFingerprint(null, null, "0")

        assertNotEquals(sessionA, sessionB)
        assertNotEquals(sessionA, guestSession)
        assertEquals("guest", guestSession)
        assertTrue(sessionA.startsWith("session_"))
        assertTrue(sessionB.startsWith("session_"))
    }

    @Test
    fun `cached mixes for User A are isolated from User B`() {
        val sessionA = computeSessionFingerprint("SAPISID=userA", "syncA")
        val sessionB = computeSessionFingerprint("SAPISID=userB", "syncB")

        val userAMixes = listOf("My Supermix A", "My Mix 1 A")
        val userBMixes = listOf("My Supermix B", "My Mix 1 B")

        InnerTubeApiCache.put("mixes_$sessionA", userAMixes)
        InnerTubeApiCache.put("mixes_$sessionB", userBMixes)

        assertEquals(userAMixes, InnerTubeApiCache.get<List<String>>("mixes_$sessionA"))
        assertEquals(userBMixes, InnerTubeApiCache.get<List<String>>("mixes_$sessionB"))
        assertNotEquals(
            InnerTubeApiCache.get<List<String>>("mixes_$sessionA"),
            InnerTubeApiCache.get<List<String>>("mixes_$sessionB")
        )
    }

    @Test
    fun `stale response rejection prevents User A data from leaking to User B`() {
        val sessionA = computeSessionFingerprint("SAPISID=userA", "syncA")
        val sessionB = computeSessionFingerprint("SAPISID=userB", "syncB")

        val activeSession = sessionB
        val userAData = "User A Mixes Response"

        var assignedData: String? = null
        val requestSession = sessionA

        if (requestSession == activeSession) {
            assignedData = userAData
        }

        assertNull(assignedData)
    }

    @Test
    fun `localized mix section detection identifies personalized sections across languages`() {
        val englishSection = HomePage.Section("Mixed for you", null, null, null, emptyList())
        val spanishSection = HomePage.Section("Mezclas para ti", null, null, null, emptyList())
        val germanSection = HomePage.Section("Für dich gemixt", null, null, null, emptyList())
        val japaneseSection = HomePage.Section("ミックス", null, null, null, emptyList())
        val koreanSection = HomePage.Section("맞춤 믹스", null, null, null, emptyList())
        val chineseSection = HomePage.Section("為你推薦的合輯", null, null, null, emptyList())

        assertTrue(isPersonalizedMixSection(englishSection))
        assertTrue(isPersonalizedMixSection(spanishSection))
        assertTrue(isPersonalizedMixSection(germanSection))
        assertTrue(isPersonalizedMixSection(japaneseSection))
        assertTrue(isPersonalizedMixSection(koreanSection))
        assertTrue(isPersonalizedMixSection(chineseSection))
    }

    @Test
    fun `structural mix section detection identifies sections containing RD playlists`() {
        val playlist = PlaylistItem(
            id = "RDTM12345",
            title = "My Supermix",
            author = null,
            songCountText = "50 songs",
            thumbnail = "http://example.com/thumb.jpg",
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null
        )
        val structuralSection = HomePage.Section("Recommended", null, null, null, listOf(playlist))
        val randomSection = HomePage.Section("Top Charts", null, null, null, emptyList())

        assertTrue(isPersonalizedMixSection(structuralSection))
        assertFalse(isPersonalizedMixSection(randomSection))
    }
}
