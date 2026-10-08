package com.mixify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdaterTest {
    @Test
    fun parsesKmpReleaseArtifact() {
        val response =
            """
            {
              "tag_name": "v1.2.3",
              "name": "Mixify v1.2.3 — Autumn Release",
              "body": null,
              "published_at": "2026-09-05T12:00:00Z",
              "assets": [{
                "name": "Mixify.apk",
                "browser_download_url": "https://example.com/Mixify.apk",
                "size": 42
              }]
            }
            """.trimIndent()
        val release = checkNotNull(Updater.parseKmpRelease(response))

        assertEquals("v1.2.3", release.tagName)
        assertEquals("1.2.3", release.versionName)
        assertEquals("Mixify v1.2.3 — Autumn Release", release.name)
        assertEquals("", release.description)
        assertEquals("https://example.com/Mixify.apk", release.assets.single().downloadUrl)
        assertNull(Updater.parseKmpRelease(response.replace("Mixify.apk", "Mixify-with-Google-Cast.apk")))
    }

    @Test
    fun comparesStandardVersions() {
        assertTrue(Updater.compareVersions("1.0.4", "1.0.3") > 0)
        assertTrue(Updater.compareVersions("v1.0.4", "1.0.3") > 0)
        assertEquals(0, Updater.compareVersions("1.0.4", "1.0.4"))
        assertTrue(Updater.compareVersions("1.0.3", "1.0.4") < 0)
    }

    @Test
    fun versionComparisonReliesExclusivelyOnTagName() {
        val version106 = Updater.extractVersionFromTag("v1.0.6")
        assertEquals("1.0.6", version106)

        // Tag "v1.0.6" with installed 1.0.5 -> update available
        assertTrue(Updater.isUpdateAvailable("1.0.5", version106))

        // Tag "v1.0.6" with installed 1.0.6 -> no update available
        assertFalse(Updater.isUpdateAvailable("1.0.6", version106))

        // Passing raw tag_name "v1.0.6" extracts version and compares correctly
        assertTrue(Updater.isUpdateAvailable("1.0.5", "v1.0.6"))
        assertFalse(Updater.isUpdateAvailable("1.0.6", "v1.0.6"))
    }
}
