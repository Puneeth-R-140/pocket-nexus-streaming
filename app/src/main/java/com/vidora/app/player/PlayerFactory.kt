package com.vidora.app.player

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

object PlayerFactory {
    
    // Default Mobile Chrome User-Agent
    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun createExoPlayer(
        context: Context,
        userAgent: String = DEFAULT_USER_AGENT,
        cookies: String? = null
    ): ExoPlayer {
        android.util.Log.d("PlayerFactory", "Creating player with UA: $userAgent, Cookies: ${cookies?.take(20)}...")
        
        // 1. Configure the HTTP Data Source with longer timeouts for mobile networks
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(30000) // Increased to 30s
            .setReadTimeoutMs(30000)    // Increased to 30s
            .setDefaultRequestProperties(
                buildMap {
                    put("Referer", "https://watch.vidora.su/")
                    put("Origin", "https://watch.vidora.su")
                    put("X-Requested-With", "com.android.chrome")
                    if (!cookies.isNullOrEmpty()) {
                        put("Cookie", cookies)
                    }
                }
            )

        // 2. Configure the Media Source Factory to use our HTTP source
        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
        
        val trackSelector = DefaultTrackSelector(context)
        
        // 3. Configure LoadControl for better buffering
        val loadControl = com.google.android.exoplayer2.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000, // Min buffer (50s)
                120_000, // Max buffer (120s)
                2_500, // Buffer for playback (2.5s)
                5_000  // Buffer for rebuffer (5s)
            )
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    }
}
