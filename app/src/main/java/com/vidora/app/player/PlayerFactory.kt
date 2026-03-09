package com.vidora.app.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

object PlayerFactory {

    // Default Mobile Chrome User-Agent
    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // flixer.su is the provider. Its CDN requires Referer: https://flixer.su/ 
    // on segment and key requests.
    private const val DEFAULT_REFERER = "https://flixer.su/"

    fun createExoPlayer(
        context: Context,
        userAgent: String = DEFAULT_USER_AGENT,
        cookies: String? = null,
        streamUrl: String? = null,
        referer: String? = null
    ): ExoPlayer {
        android.util.Log.d("PlayerFactory", "Creating player with referer: ${referer ?: DEFAULT_REFERER}")
        val effectiveReferer = referer ?: DEFAULT_REFERER

        // Standard HTTP factory — cloudnestra.com CDN uses plain HTTPS with no MIME tricks
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(buildMap {
                put("Referer", effectiveReferer)
                put("Origin", effectiveReferer.trimEnd('/'))
                put("X-Requested-With", "com.android.chrome")
                if (!cookies.isNullOrEmpty()) {
                    put("Cookie", cookies)
                }
            })

        // Media source factory
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val trackSelector = DefaultTrackSelector(context)

        // LoadControl: generous buffers to handle HLS with AES-128 decryption overhead
        val loadControl = com.google.android.exoplayer2.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000,  // Min buffer (50s)
                120_000, // Max buffer (120s)
                2_500,   // Buffer for playback start (2.5s)
                5_000    // Buffer for rebuffer (5s)
            )
            .build()

        val renderersFactory = com.google.android.exoplayer2.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_NETWORK)
            }
    }
}
