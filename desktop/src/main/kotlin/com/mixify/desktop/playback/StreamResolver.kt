package com.mixify.desktop.playback

import com.mixify.innertube.YouTube
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParser
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

object StreamResolver {
    private val httpClient = HttpClient(OkHttp)
    private val transport = YouTube.extractionTransport()
    private val logger = InnerTubeLogger { }
    private val playerConfigRepo = PlayerConfigRepository.disabled()
    private val remoteStore = RemotePlayerConfigStore(httpClient, playerConfigRepo, logger)
    private val cipherService = YouTubeCipherService(httpClient, remoteStore, logger)
    private val tokenProvider = object : TokenProvider {
        override val capabilities = TokenProviderCapabilities(providers = emptySet(), usesWebView = false)
        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? = null
        override suspend fun close() {}
    }
    private val configParser = YtConfigParserImpl(httpClient, transport.innerTube, remoteStore, logger).withEmbeddedConfigFallback()
    private val extractor = InnerTubeExtractor(
        configParser = configParser,
        cipherService = cipherService,
        innerTube = transport.innerTube,
        tokenProvider = tokenProvider,
        logger = logger
    )

    private fun YtConfigParser.withEmbeddedConfigFallback(): YtConfigParser =
        object : YtConfigParser by this {
            override suspend fun fetchConfig(
                videoId: String,
                useLoginCookies: Boolean,
            ) =
                try {
                    this@withEmbeddedConfigFallback.fetchConfig(videoId, useLoginCookies)
                } catch (_: IllegalStateException) {
                    this@withEmbeddedConfigFallback.fetchEmbeddedConfig(videoId, useLoginCookies = false)
                }
        }

    suspend fun resolveStreamUrl(videoId: String): Result<String> {
        return try {
            val stream = extractor.extract(
                videoId = videoId,
                hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false),
                excludedClients = emptySet(),
                audioQuality = AudioQuality.HIGH,
                clientPlaybackNonce = generateClientPlaybackNonce()
            )
            if (stream != null) {
                Result.success(stream.audioUrl)
            } else {
                Result.failure(Exception("InnerTubeExtractor returned null stream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
