package com.mixify.music.cloud

import android.content.Context
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.mixify.music.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** GMS implementation. Debug/PR packages never contact the production project. */
object AppCloud {
    private const val PREFS = "mixify_cloud"
    private const val ANALYTICS = "analytics_enabled"
    private val mutableSettings = MutableStateFlow(RemoteAppSettings())
    val settings = mutableSettings.asStateFlow()
    private var analytics: FirebaseAnalytics? = null
    private var remoteConfig: FirebaseRemoteConfig? = null
    val available: Boolean get() = analytics != null

    fun initialize(context: Context) {
        if (BuildConfig.DEBUG || context.packageName != "com.mixify.app" || remoteConfig != null) return
        runCatching {
            // Resources are scoped to GMS release, so debug/Izzy need no Firebase registration.
            val options = FirebaseOptions.fromResource(context)
            if (options == null || options.apiKey.isBlank()) {
                Timber.w("Firebase client configuration missing; cloud services disabled")
                return
            }
            if (FirebaseApp.initializeApp(context) == null) return
            analytics = FirebaseAnalytics.getInstance(context).also {
                it.setAnalyticsCollectionEnabled(isAnalyticsEnabled(context))
            }
            val config = FirebaseRemoteConfig.getInstance()
            remoteConfig = config
            val defaults = mapOf<String, Any>(
                "home_title" to "",
                "announcement_enabled" to false,
                "announcement_text" to "",
            )
            config.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .setFetchTimeoutInSeconds(10)
                    .build()
            ).continueWithTask { config.setDefaultsAsync(defaults) }
                .addOnSuccessListener {
                    publish(config) // Includes previously activated values when offline.
                    config.fetchAndActivate()
                        .addOnSuccessListener { publish(config) }
                        .addOnFailureListener { Timber.w(it, "Remote configuration unavailable; using cached values") }
                }
            config.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    config.activate().addOnSuccessListener { publish(config) }
                }
                override fun onError(error: FirebaseRemoteConfigException) {
                    Timber.w(error, "Remote configuration listener unavailable")
                }
            })
        }.onFailure { Timber.w(it, "Cloud services unavailable; continuing offline") }
    }

    private fun publish(config: FirebaseRemoteConfig) {
        mutableSettings.value = RemoteAppSettings(
            homeTitle = config.getString("home_title").trim().take(120),
            announcementEnabled = config.getBoolean("announcement_enabled"),
            announcementText = config.getString("announcement_text").trim().take(500),
        )
    }

    fun isAnalyticsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ANALYTICS, true)

    fun setAnalyticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ANALYTICS, enabled).apply()
        analytics?.setAnalyticsCollectionEnabled(enabled)
        if (!enabled) analytics?.resetAnalyticsData()
    }
}
