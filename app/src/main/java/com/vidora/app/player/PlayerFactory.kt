package com.vidora.app.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.C

/**
 * Retries failed HLS segment / AES-128 key downloads up to 6 times before giving up.
 * Uses exponential backoff (1s × errorCount, capped at 5s) so a transient CDN 403
 * or timeout is retried rather than silently skipped — which is the root cause of
 * random mid-stream 4-8 second jumps.
 */
private class SegmentRetryPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        return if (loadErrorInfo.errorCount <= 6)
            minOf(1000L * loadErrorInfo.errorCount, 5_000L) // 1s, 2s, 3s, 4s, 5s, 5s
        else
            C.TIME_UNSET // give up after 6 retries
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = 6
}

/**
 * Holds an ExoPlayer and the live DataSource.Factory used to create it.
 * Call [updateHeaders] at any time to inject cookies/referer into future
 * HLS segment and AES-128 key requests WITHOUT recreating the player.
 */
data class PlayerHolder(
    val player: ExoPlayer,
    private val dataSourceFactory: DefaultHttpDataSource.Factory
) {
    /**
     * Replaces the request headers the factory sends on every subsequent
     * HTTP request (segments, key requests, manifest refreshes).
     * Safe to call from the main thread at any time.
     */
    fun updateHeaders(referer: String, cookies: String?) {
        dataSourceFactory.setDefaultRequestProperties(buildMap {
            put("Referer", referer)
            put("Origin", referer.trimEnd('/'))
            put("X-Requested-With", "com.android.chrome")
            if (!cookies.isNullOrEmpty()) put("Cookie", cookies)
        })
    }
}

object PlayerFactory {

    // Default Mobile Chrome User-Agent
    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    // Fallback referer if provider page doesn't set one
    private const val DEFAULT_REFERER = "https://vidnest.fun/"

    fun createExoPlayer(
        context: Context,
        userAgent: String = DEFAULT_USER_AGENT,
        cookies: String? = null,
        streamUrl: String? = null,
        referer: String? = null
    ): ExoPlayer = createPlayerHolder(context, userAgent, cookies, referer).player

    /**
     * Creates an ExoPlayer together with a live-updatable DataSource.Factory.
     * Use this instead of [createExoPlayer] when you need to update headers
     * after construction (e.g. when WebView cookies arrive asynchronously).
     */
    fun createPlayerHolder(
        context: Context,
        userAgent: String = DEFAULT_USER_AGENT,
        cookies: String? = null,
        referer: String? = null
    ): PlayerHolder {
        val effectiveReferer = referer ?: DEFAULT_REFERER
        android.util.Log.d("PlayerFactory", "Creating PlayerHolder with referer: $effectiveReferer")

        // Build the factory with initial headers. The factory instance is kept
        // alive in PlayerHolder so updateHeaders() can replace them at any time.
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(buildMap {
                put("Referer", effectiveReferer)
                put("Origin", effectiveReferer.trimEnd('/'))
                put("X-Requested-With", "com.android.chrome")
                if (!cookies.isNullOrEmpty()) put("Cookie", cookies)
            })

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(SegmentRetryPolicy())
        val trackSelector = DefaultTrackSelector(context)

        // LoadControl: generous buffers to handle HLS with AES-128 decryption overhead
        val loadControl = com.google.android.exoplayer2.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000,  // Min buffer forward (50s)
                120_000, // Max buffer forward (120s)
                2_500,   // Buffer for playback start (2.5s)
                10_000   // Buffer for rebuffer after stall (10s)
            )
            // Keep 120s of already-played content in memory so rewinding is instant
            // (retainBackBufferFromKeyframe=true ensures seek lands on a valid keyframe)
            .setBackBuffer(120_000, true)
            .build()

        val renderersFactory = com.google.android.exoplayer2.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(context)
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

        return PlayerHolder(player, dataSourceFactory)
    }
}
