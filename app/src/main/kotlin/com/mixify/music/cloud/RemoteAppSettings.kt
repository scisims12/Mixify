package com.mixify.music.cloud

/** Only data configured here can change remotely; no downloaded executable code. */
data class RemoteAppSettings(
    val homeTitle: String = "",
    val announcementEnabled: Boolean = false,
    val announcementText: String = "",
)
