package com.mixify.music.cloud

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The F-Droid flavor has no Firebase SDKs, collection, or cloud requests. */
object AppCloud {
    val settings = MutableStateFlow(RemoteAppSettings()).asStateFlow()
    val available = false
    fun initialize(context: Context) = Unit
    fun isAnalyticsEnabled(context: Context) = false
    fun setAnalyticsEnabled(context: Context, enabled: Boolean) = Unit
}
