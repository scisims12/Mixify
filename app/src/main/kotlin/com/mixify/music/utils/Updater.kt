/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.utils

import com.mixify.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val name: String = "",
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "gms" or "gmsFull"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/scisims12/Mixify"
    private const val KMP_LATEST_RELEASE_URL = "https://api.github.com/repos/scisims12/Mixify-KMP/releases/latest"
    private const val KMP_APK_NAME = "Mixify.apk"

    /**
     * Extracts pure version string from GitHub tag_name (e.g., "v1.0.6" -> "1.0.6").
     */
    fun extractVersionFromTag(tagName: String): String {
        return tagName.removePrefix("v").removePrefix("V").trim()
    }

    /**
     * Compares two version strings.
     * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        fun parseParts(version: String): List<Int> {
            val mainVersion = version.substringBefore("+").substringBefore("-")
            val parts = Regex("""\d+""").findAll(mainVersion).map { it.value.toIntOrNull() ?: 0 }.toList()
            return parts.ifEmpty { listOf(0) }
        }

        val v1Parts = parseParts(v1)
        val v2Parts = parseParts(v2)
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }
        return 0
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Version comparison relies exclusively on version strings derived from tag_name.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        val cleanLatestVersion = extractVersionFromTag(latestVersion)
        val hasUpdate = compareVersions(cleanLatestVersion, currentVersion) > 0
        Timber.tag("Updater").d("isUpdateAvailable check: currentVersion=$currentVersion, cleanLatestVersion=$cleanLatestVersion (from raw $latestVersion) -> hasUpdate=$hasUpdate")
        return hasUpdate
    }

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gmsFull" else "gms"
        return architecture to variant
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            
            // Parse architecture and variant from filename
            val (arch, variant) = when {
                name == "Mixify.apk" -> "universal" to "gms"
                name == "Mixify-with-Google-Cast.apk" -> "universal" to "gmsFull"
                name.startsWith("app-") && name.endsWith("-release.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-release.apk")
                    arch to "gms"
                }
                name.startsWith("app-") && name.endsWith("-with-Google-Cast.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-with-Google-Cast.apk")
                    arch to "gmsFull"
                }
                else -> null to null
            }
            
            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Return cached if available and not forcing refresh
                if (cachedReleaseInfo != null && !forceRefresh) {
                    Timber.tag("Updater").d("Returning cached release info for ${cachedReleaseInfo?.tagName}")
                    return@runCatching cachedReleaseInfo!!
                }
                
                Timber.tag("Updater").d("Fetching latest release from $GITHUB_API_BASE/releases/latest")
                val response = client.get("$GITHUB_API_BASE/releases/latest")
                    .bodyAsText()
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name")
                val name = json.optString("name").takeUnless { json.isNull("name") }.orEmpty()
                val releaseInfo = ReleaseInfo(
                    tagName = tagName,
                    versionName = extractVersionFromTag(tagName),
                    name = name,
                    description = json.optString("body").takeUnless { json.isNull("body") }.orEmpty(),
                    releaseDate = json.getString("published_at"),
                    assets = parseAssets(json.getJSONArray("assets"))
                )
                
                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                Timber.tag("Updater").d("Fetched latest release: tagName=$tagName, versionName=${releaseInfo.versionName}, assetsCount=${releaseInfo.assets.size}")
                releaseInfo
            }.onFailure { e ->
                Timber.tag("Updater").e(e, "Failed to fetch latest release")
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                        .bodyAsText()
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        val tagName = releaseObj.getString("tag_name")
                        val name = releaseObj.optString("name").takeUnless { releaseObj.isNull("name") }.orEmpty()
                        releases.add(ReleaseInfo(
                            tagName = tagName,
                            versionName = extractVersionFromTag(tagName),
                            name = name,
                            description = releaseObj.optString("body").takeUnless { releaseObj.isNull("body") }.orEmpty(),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets"))
                        ))
                    }
                    
                    page++
                }
                
                cachedAllReleases = releases
                Timber.tag("Updater").d("Fetched ${releases.size} total releases")
                releases
            }.onFailure { e ->
                Timber.tag("Updater").e(e, "Failed to fetch all releases")
            }
        }

    internal fun parseKmpRelease(response: String): ReleaseInfo? {
        val release = JSONObject(response)
        val assets = parseAssets(release.getJSONArray("assets")).filter { it.name == KMP_APK_NAME }
        val tagName = release.getString("tag_name")
        val name = release.optString("name").takeUnless { release.isNull("name") }.orEmpty()

        return ReleaseInfo(
            tagName = tagName,
            versionName = extractVersionFromTag(tagName),
            name = name,
            description = release.optString("body").takeUnless { release.isNull("body") }.orEmpty(),
            releaseDate = release.getString("published_at"),
            assets = assets,
        ).takeIf { assets.isNotEmpty() }
    }

    /**
     * Returns the latest stable KMP release when it includes an Android APK.
     */
    suspend fun getLatestKmpRelease(): Result<ReleaseInfo?> =
        withContext(Dispatchers.IO) {
            runCatching {
                parseKmpRelease(client.get(KMP_LATEST_RELEASE_URL).bodyAsText())
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        val downloadUrl = releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
            ?: releaseInfo.assets.find { it.variant == currentVariant }?.downloadUrl
            ?: releaseInfo.assets.firstOrNull()?.downloadUrl
        Timber.tag("Updater").d("getDownloadUrlForCurrentVariant: currentArch=$currentArch, currentVariant=$currentVariant -> downloadUrl=$downloadUrl")
        return downloadUrl
    }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Check if we should fetch (2 hour interval)
                val shouldFetch = forceRefresh || 
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (!shouldFetch && cachedReleaseInfo != null) {
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.BASE_VERSION_NAME,
                        cachedReleaseInfo!!.versionName
                    )
                    Timber.tag("Updater").d("checkForUpdate (cached): baseVersion=${BuildConfig.BASE_VERSION_NAME}, latestVersion=${cachedReleaseInfo!!.versionName}, hasUpdate=$hasUpdate")
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }
                
                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.BASE_VERSION_NAME,
                        releaseInfo.versionName
                    )
                    Timber.tag("Updater").d("checkForUpdate (fetched): baseVersion=${BuildConfig.BASE_VERSION_NAME}, latestVersion=${releaseInfo.versionName}, hasUpdate=$hasUpdate")
                    releaseInfo to hasUpdate
                } else {
                    val exception = result.exceptionOrNull() ?: Exception("Unknown error")
                    Timber.tag("Updater").e(exception, "checkForUpdate failed")
                    throw exception
                }
            }
        }

    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}
