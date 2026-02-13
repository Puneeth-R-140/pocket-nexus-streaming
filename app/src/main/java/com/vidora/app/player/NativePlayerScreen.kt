package com.vidora.app.player

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import com.vidora.app.player.components.CustomPlayerControls
import com.vidora.app.player.components.DoubleTapControls
import com.vidora.app.player.components.SpeedControlPanel
import com.vidora.app.player.components.AutoPlayCountdown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.vidora.app.data.remote.MediaItem as VidoraMediaItem
import com.vidora.app.ui.components.ErrorStateView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.vidora.app.util.NetworkResult
import okhttp3.OkHttpClient
import okhttp3.Request
import android.net.Uri
import com.google.android.exoplayer2.util.MimeTypes
import org.json.JSONArray
import org.json.JSONObject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.vidora.app.data.repository.MediaRepository

private const val TAG = "NativePlayer"
private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

data class VideoQuality(
    val height: Int,
    val label: String,
    val index: Int
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NativePlayerEntryPoint {
    fun repository(): MediaRepository
}

@Composable
fun NativePlayerScreen(
    media: VidoraMediaItem,
    playerUrl: String,
    onBack: () -> Unit,
    onNavigateToEpisode: ((season: Int, episode: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            NativePlayerEntryPoint::class.java
        )
    }
    val repository = remember { entryPoint.repository() }
    var state by remember { mutableStateOf<PlayerState>(PlayerState.Loading("Initializing player...")) }
    var extractedUrl by remember { mutableStateOf<String?>(null) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    val detectedSubtitles = remember { mutableStateMapOf<String, SubtitleTrack>() }
    var webViewKey by remember { mutableStateOf(0) } // For forcing WebView recreation on retry
    var cookies by remember { mutableStateOf<String?>(null) } // Captured cookies from WebView
    var userSettings by remember { mutableStateOf<com.vidora.app.data.local.SettingsEntity?>(null) }
    var hasAutoSelectedSubtitle by remember { mutableStateOf(false) }
    var showAutoPlayCountdown by remember { mutableStateOf(false) }
    var nextEpisodeInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var totalEpisodesInSeason by remember { mutableStateOf<Int?>(null) }
    var totalSeasons by remember { mutableStateOf<Int?>(null) }
    
    // Fetch total seasons
    LaunchedEffect(media.id) {
        if (media.realMediaType == "tv") {
            repository.getDetails("tv", media.id).collect { result ->
                if (result is NetworkResult.Success) {
                    totalSeasons = result.data.numberOfSeasons ?: result.data.seasons?.size
                }
            }
        }
    }
    
    // Extract season and episode from URL for TV shows
    val (season, episode) = remember(playerUrl, media.realMediaType) {
        if (media.realMediaType == "tv") {
            extractSeasonEpisodeFromUrl(playerUrl)
        } else {
            null to null
        }
    }

    // Fetch episodes to know when to switch seasons
    LaunchedEffect(media.id, season) {
        if (media.realMediaType == "tv" && season != null) {
            repository.getEpisodes(media.id, season).collect { result ->
                if (result is NetworkResult.Success) {
                    totalEpisodesInSeason = result.data.size
                }
            }
        }
    }
    
    // Load user settings for subtitle preference
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        try {
            userSettings = repository.getSettings()
            Log.d(TAG, "Loaded user settings: default subtitle language = ${userSettings?.defaultSubtitleLanguage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading settings: ${e.message}", e)
        }
    }
    
    // Proactively fetch subtitles from API
    LaunchedEffect(media.id, season, episode) {
        Log.d(TAG, "Fetching subtitles for TMDB ID: ${media.id}, Season: ${season}, Episode: ${episode}")
        try {
            val subtitles = repository.getSubtitles(media.id, media.realMediaType, season, episode)
            Log.d(TAG, "Fetched ${subtitles.size} subtitles from API")
            
            subtitles.forEach { subtitle ->
                val trackKey = subtitle.language
                if (!detectedSubtitles.containsKey(trackKey)) {
                    detectedSubtitles[trackKey] = SubtitleTrack(
                        language = subtitle.language,
                        label = subtitle.display,
                        url = subtitle.url
                    )
                    Log.d(TAG, "Added subtitle: ${subtitle.display} (${subtitle.language})")
                }
            }
            
            // Auto-select subtitle based on user preference
            if (!hasAutoSelectedSubtitle && detectedSubtitles.isNotEmpty() && exoPlayerInstance != null) {
                val defaultLang = userSettings?.defaultSubtitleLanguage
                if (!defaultLang.isNullOrEmpty() && defaultLang != "None") {
                    val normalizedDefaultLang = normalizeLanguageCode(defaultLang)
                    val matchingTrack = detectedSubtitles.values.find { track ->
                        normalizeLanguageCode(track.language) == normalizedDefaultLang
                    }
                    
                    if (matchingTrack != null) {
                        Log.d(TAG, "Auto-selecting preferred subtitle: ${matchingTrack.label}")
                        exoPlayerInstance?.let { player ->
                            val trackSelector = player.trackSelector as? DefaultTrackSelector
                            val newParams = trackSelector?.buildUponParameters()
                                ?.setRendererDisabled(getTextRendererIndex(player), false)
                                ?.setPreferredTextLanguage(matchingTrack.language)
                                ?.setSelectUndeterminedTextLanguage(true)
                                ?.build()
                            if (newParams != null && trackSelector != null) {
                                trackSelector.parameters = newParams
                            }
                            hasAutoSelectedSubtitle = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subtitles: ${e.message}", e)
        }
    }
    
    // Timeout tracking with optimized duration
    LaunchedEffect(webViewKey) {
        delay(15000) // 15 seconds (reduced from 30s for faster feedback)
        if (state is PlayerState.Loading) {
            state = PlayerState.Error("Stream extraction timed out. The website may be blocking automated access.")
        }
    }

    // Retry function
    val retry = {
        Log.d(TAG, "Retrying stream extraction...")
        extractedUrl = null
        detectedSubtitles.clear()
        webViewKey++ // Force WebView recreation
        state = PlayerState.Loading("Retrying stream extraction...")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val currentState = state) {
            is PlayerState.Loading -> {
                LoadingState(media, currentState.message, onBack)
                
                // Invisible WebView for extraction - recreated when webViewKey changes
                LaunchedEffect(webViewKey) {
                    // This effect will restart when webViewKey changes, forcing AndroidView recreation
                }
                
                AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.userAgentString = USER_AGENT
                            alpha = 0f
                            
                            Log.d(TAG, "WebView created, loading: $playerUrl with UA: $USER_AGENT")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "Page finished loading: ${url}")
                                    url?.let {
                                        val extractedCookies = android.webkit.CookieManager.getInstance().getCookie(it)
                                        if (!extractedCookies.isNullOrEmpty()) {
                                            cookies = extractedCookies
                                            Log.d(TAG, "Cookies extracted: $extractedCookies")
                                        }
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? {
                                    val url = request?.url?.toString() ?: ""
                                    
                                    // Intercept HLS manifest
                                    if (url.contains(".m3u8") && extractedUrl == null) {
                                        Log.d(TAG, "Intercepted M3U8: $url")
                                        extractedUrl = url
                                        scope.launch {
                                            state = PlayerState.Loading("Parsing stream manifest...")
                                            try {
                                                val parser = HlsManifestParser()
                                                val streamInfo = parser.parseManifest(url)
                                                Log.d(TAG, "Stream ready, ${detectedSubtitles.size} subtitles available from API")
                                                
                                                // Use only API-fetched subtitles
                                                state = PlayerState.Playing(streamInfo.copy(subtitles = detectedSubtitles.values.toList()))
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Failed to parse manifest, using raw URL", e)
                                                state = PlayerState.Playing(StreamInfo(url, detectedSubtitles.values.toList()))
                                            }
                                        }
                                    }
                                    
                                    return super.shouldInterceptRequest(view, request)
                                }
                            }
                            
                            loadUrl(playerUrl)
                        }
                    },
                    modifier = Modifier.size(0.dp)
                )
            }
            
            is PlayerState.Playing -> {
                ExoPlayerView(
                    media = media,
                    repository = repository,
                    season = season,
                    episode = episode,
                    streamInfo = currentState.streamInfo,
                    onBack = onBack,
                    cookies = cookies,
                    userSettings = userSettings,
                    onVideoEnded = { nextSeason, nextEpisode ->
                        nextEpisodeInfo = nextSeason to nextEpisode
                        showAutoPlayCountdown = true
                    },
                    onQualitiesDetected = { qualities, player ->
                        availableQualities = qualities
                        exoPlayerInstance = player
                        if (selectedQuality == null && qualities.isNotEmpty()) {
                            // Auto-select quality based on user preference
                            val preferredQuality = userSettings?.preferredQuality ?: "Auto"
                            selectedQuality = when (preferredQuality) {
                                "720p" -> qualities.find { it.label.contains("720") }
                                "1080p" -> qualities.find { it.label.contains("1080") }
                                else -> null // "Auto" - let player decide
                            } ?: qualities.firstOrNull() // Fallback to first available
                            
                            Log.d(TAG, "Auto-selected quality: ${selectedQuality?.label ?: "Auto"} based on preference: $preferredQuality")
                            
                            // Apply the quality selection
                            selectedQuality?.let { quality ->
                                setVideoQuality(player, quality)
                            }
                        }
                    },
                    onShowQualityDialog = { showQualityDialog = true },
                    onShowSubtitleDialog = { showSubtitleDialog = true },
                    onShowSpeedDialog = { showSpeedDialog = true },
                    onNextEpisode = if (media.realMediaType == "tv" && season != null && episode != null) {
                        {
                           // Check if we need to go to next season
                           if (totalEpisodesInSeason != null && episode >= totalEpisodesInSeason!!) {
                               // Start of next season
                               if (totalSeasons != null && season < totalSeasons!!) {
                                   nextEpisodeInfo = (season + 1) to 1
                                   showAutoPlayCountdown = true
                               } else {
                                   // No more seasons or unknown
                                   android.widget.Toast.makeText(context, "No more episodes", android.widget.Toast.LENGTH_SHORT).show()
                               }
                           } else {
                               // Next episode in current season
                               nextEpisodeInfo = season to (episode + 1)
                               showAutoPlayCountdown = true
                           }
                        }
                    } else null,
                    detectedSubtitles = detectedSubtitles.values.toList()
                )
            }
            
            is PlayerState.Error -> {
                PlayerErrorState(currentState.message, retry)
            }
        }
        
        if (showQualityDialog && availableQualities.isNotEmpty()) {
            QualitySelectionDialog(
                qualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = { quality ->
                    selectedQuality = quality
                    exoPlayerInstance?.let { setVideoQuality(it, quality) }
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
            )
        }
        
        if (showSubtitleDialog) {
            SubtitleSelectionDialog(
                tracks = detectedSubtitles.values.toList(),
                exoPlayer = exoPlayerInstance,
                onDismiss = { showSubtitleDialog = false }
            )
        }
        
        if (showSpeedDialog) {
            SpeedControlPanel(
                player = exoPlayerInstance!!,
                onDismiss = { showSpeedDialog = false }
            )
        }
        
        // Auto-play countdown overlay
        if (showAutoPlayCountdown && nextEpisodeInfo != null) {
            val (nextSeason, nextEpisode) = nextEpisodeInfo!!
            AutoPlayCountdown(
                nextEpisodeTitle = "Season $nextSeason Episode $nextEpisode",
                onPlayNext = {
                    showAutoPlayCountdown = false
                    onNavigateToEpisode?.invoke(nextSeason, nextEpisode)
                },
                onCancel = {
                    showAutoPlayCountdown = false
                }
            )
        }
    }
}

// Helper function to convert language code to readable name
private fun getLanguageName(code: String): String {
    return when (code.lowercase()) {
        "en" -> "English"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "ar" -> "Arabic"
        "hi" -> "Hindi"
        "ru" -> "Russian"
        else -> code.uppercase() // Fallback to uppercase code
    }
}

// Helper function to normalize language codes (eng -> en, spa -> es, etc.)
private fun normalizeLanguageCode(code: String): String {
    val normalized = code.lowercase().trim()
    return when (normalized) {
        "eng", "english" -> "en"
        "spa", "spanish" -> "es"
        "fra", "fre", "french" -> "fr"
        "deu", "ger", "german" -> "de"
        "ita", "italian" -> "it"
        "por", "portuguese" -> "pt"
        "rus", "russian" -> "ru"
        "jpn", "japanese" -> "ja"
        "kor", "korean" -> "ko"
        "zho", "chi", "chinese" -> "zh"
        "ara", "arabic" -> "ar"
        "hin", "hindi" -> "hi"
        "ind", "indonesian" -> "id"
        "tur", "turkish" -> "tr"
        "pol", "polish" -> "pl"
        "ukr", "ukrainian" -> "uk"
        "vie", "vietnamese" -> "vi"
        "tha", "thai" -> "th"
        else -> if (normalized.length == 2) normalized else normalized.take(2)
    }
}

// Helper function to get display name from language code
private fun getLanguageDisplayName(code: String): String {
    val normalized = normalizeLanguageCode(code)
    return when (normalized) {
        "en" -> "English"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "ru" -> "Russian"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "ar" -> "Arabic"
        "hi" -> "Hindi"
        "id" -> "Indonesian"
        "tr" -> "Turkish"
        "pl" -> "Polish"
        "uk" -> "Ukrainian"
        "vi" -> "Vietnamese"
        "th" -> "Thai"
        else -> code.uppercase()
    }
}

// Helper function to extract language from URL
private fun extractLanguageFromUrl(url: String): String {
    // Try common patterns with support for 2-3 letter codes and locales
    val patterns = listOf(
        Regex("[/_-]([a-z]{2,3}(?:-[A-Z]{2})?)[/._-]", RegexOption.IGNORE_CASE),  // /en/ or /eng/ or /en-US/
        Regex("lang[=:]([a-z]{2,3}(?:-[A-Z]{2})?)", RegexOption.IGNORE_CASE),     // lang=en or lang=eng
        Regex("\\.([a-z]{2,3})\\.", RegexOption.IGNORE_CASE),                      // .en. or .eng.
        Regex("/([a-z]{2,3})/", RegexOption.IGNORE_CASE)                           // /en/ or /eng/
    )
    
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            val code = match.groupValues[1].split("-")[0] // Extract base code from locale
            return normalizeLanguageCode(code)
        }
    }
    
    // Check for full language names in URL
    val urlLower = url.lowercase()
    return when {
        urlLower.contains("english") -> "en"
        urlLower.contains("spanish") -> "es"
        urlLower.contains("french") -> "fr"
        urlLower.contains("german") -> "de"
        urlLower.contains("italian") -> "it"
        urlLower.contains("portuguese") -> "pt"
        urlLower.contains("russian") -> "ru"
        urlLower.contains("japanese") -> "ja"
        urlLower.contains("korean") -> "ko"
        urlLower.contains("hindi") -> "hi"
        urlLower.contains("chinese") -> "zh"
        else -> "unknown" // Will be filtered out or labeled generically
    }
}

// Helper function to extract season and episode from provider URL
// URL pattern: https://watch.vidora.su/watch/tv/{id}/{season}/{episode}
private fun extractSeasonEpisodeFromUrl(url: String): Pair<Int?, Int?> {
    return try {
        val parts = url.split("/")
        if (parts.size >= 3) {
            val episode = parts.last().toIntOrNull()
            val season = parts[parts.size - 2].toIntOrNull()
            season to episode
        } else {
            null to null
        }
    } catch (e: Exception) {
        null to null
    }
}

enum class ScalingMode {
    FIT, ZOOM, STRETCH;
    
    fun next(): ScalingMode = when(this) {
        FIT -> ZOOM
        ZOOM -> STRETCH
        STRETCH -> FIT
    }
}

enum class RotationMode {
    AUTO, PORTRAIT, LANDSCAPE;
    
    fun next(): RotationMode = when(this) {
        AUTO -> PORTRAIT
        PORTRAIT -> LANDSCAPE
        LANDSCAPE -> AUTO
    }
    
    fun toOrientation(): Int = when(this) {
        AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

@Composable
private fun ExoPlayerView(
    media: VidoraMediaItem,
    repository: com.vidora.app.data.repository.MediaRepository,
    season: Int?,
    episode: Int?,
    streamInfo: StreamInfo,
    onBack: () -> Unit,
    cookies: String?,
    userSettings: com.vidora.app.data.local.SettingsEntity?,
    onVideoEnded: (season: Int, episode: Int) -> Unit,
    onQualitiesDetected: (List<VideoQuality>, ExoPlayer) -> Unit,
    onShowQualityDialog: () -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onNextEpisode: (() -> Unit)? = null,
    detectedSubtitles: List<SubtitleTrack>
) {
    val context = LocalContext.current
    val internalScope = rememberCoroutineScope()
    var currentPos by remember { mutableStateOf(0L) }
    var startupPosition by remember { mutableStateOf<Long?>(null) }
    var rotationMode by remember { mutableStateOf(RotationMode.AUTO) }
    var scalingMode by remember { mutableStateOf(ScalingMode.ZOOM) }
    var showControls by remember { mutableStateOf(true) }
    val activity = context.findActivity()
    val window = activity?.window
    
    val exoPlayer = remember(cookies) {
        Log.d(TAG, "Creating ExoPlayer with cookies: ${cookies?.take(50)}... and UA: $USER_AGENT")
        PlayerFactory.createExoPlayer(context, userAgent = USER_AGENT, cookies = cookies)
    }
    
    // Load initial position from database
    LaunchedEffect(media.id, season, episode) {
        val historyItem = repository.getHistoryItem(media.id, season, episode)
        historyItem?.let {
            if (it.positionMs > 0 && it.durationMs > 0) {
                // Only resume if not finished (e.g., at least 5s before end)
                if (it.positionMs < (it.durationMs - 5000)) {
                    startupPosition = it.positionMs
                    Log.d(TAG, "Found saved position: ${it.positionMs}ms")
                }
            }
        }
    }
    
    // Apply rotation mode whenever it changes
    LaunchedEffect(rotationMode) {
        activity?.requestedOrientation = rotationMode.toOrientation()
    }
    
    DisposableEffect(Unit) {
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, w.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { w ->
                val controller = WindowCompat.getInsetsController(w, w.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            // Restore orientation
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // Release player to stop playback
            exoPlayer.release()
        }
    }
    
    
    
    // Apply rotation mode whenever it changes
    
    // Update MediaItem when subtitles are detected
    LaunchedEffect(detectedSubtitles.size, streamInfo.streamUrl, exoPlayer) {
        val mediaItemBuilder = ExoMediaItem.Builder()
            .setUri(streamInfo.streamUrl)
        
        // Inject all detected subtitles
        if (detectedSubtitles.isNotEmpty()) {
            val subtitleConfigs = detectedSubtitles.map { track ->
                val mimeType = when {
                    track.url.contains(".vtt") || track.url.contains("format=vtt") -> MimeTypes.TEXT_VTT
                    track.url.contains(".srt") || track.url.contains("format=srt") -> MimeTypes.APPLICATION_SUBRIP
                    else -> MimeTypes.TEXT_VTT // Default to VTT
                }
                
                ExoMediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                    .setMimeType(mimeType)
                    .setLanguage(track.language)
                    .setLabel(track.label)
                    .setSelectionFlags(0) // Not auto-selected
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
            Log.d(TAG, "Injected ${subtitleConfigs.size} subtitle tracks into ExoPlayer")
        }
        
        val wasPlaying = exoPlayer.isPlaying
        
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        
        // Restore playback state Or resume from history
        startupPosition?.let { pos ->
            exoPlayer.seekTo(pos)
            startupPosition = null // consume it
            Log.d(TAG, "Resumed from $pos")
        } ?: run {
            val currentPosition = exoPlayer.currentPosition
            if (currentPosition > 0) {
                exoPlayer.seekTo(currentPosition)
            }
        }
        
        exoPlayer.playWhenReady = wasPlaying || exoPlayer.currentPosition == 0L
    }
    
    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onQualitiesDetected(getAvailableQualities(exoPlayer), exoPlayer)
                    Log.d(TAG, "Player ready, ${detectedSubtitles.size} subtitles available")
                }
                if (playbackState == Player.STATE_ENDED) {
                    // Check if auto-play next episode is enabled
                   val isAutoPlayEnabled = userSettings?.autoPlayNextEpisode ?: true
                    
                    if (isAutoPlayEnabled && media.realMediaType == "tv" && season != null && episode != null) {
                        // Calculate next episode
                        val nextEpisode = episode + 1
                        
                        Log.d(TAG, "Video ended. Auto-play enabled. Triggering countdown for S${season}E${nextEpisode}")
                        
                        // Trigger countdown for next episode
                        onVideoEnded(season, nextEpisode)
                    } else {
                        // Not a TV show or auto-play disabled, just go back
                        onBack()
                    }
                }
            }
            
            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                Log.e(TAG, "Playback error: ${error.message}", error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Periodic history saving & position tracking
    LaunchedEffect(exoPlayer) {
        var saveCounter = 0
        while (true) {
            currentPos = exoPlayer.currentPosition
            
            // Save to history every 5 seconds if playing
            if (exoPlayer.isPlaying) {
                saveCounter++
                if (saveCounter >= 50) { // 50 * 100ms = 5s
                    saveCounter = 0
                    repository.updateHistory(
                        media,
                        exoPlayer.currentPosition,
                        exoPlayer.duration,
                        season,
                        episode
                    )
                }
            }
            
            delay(100)
        }
    }
    
    // Pause playback when app is minimized or closed
    DisposableEffect(exoPlayer) {
        val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                    // Save progress on pause
                    internalScope.launch {
                        repository.updateHistory(
                            media,
                            exoPlayer.currentPosition,
                            exoPlayer.duration,
                            season,
                            episode
                        )
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { 
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    var playerView by remember { mutableStateOf<StyledPlayerView?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = false  // Disable default controls
                    
                    playerView = this
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = when(scalingMode) {
                    ScalingMode.FIT -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScalingMode.ZOOM -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ScalingMode.STRETCH -> com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        
        // Gesture Controls Overlay with brightness/volume
        GestureControlsOverlay(
            exoPlayer = exoPlayer,
            playerView = playerView,
            window = window,
            modifier = Modifier.fillMaxSize()
        )
        
        // Double-tap Controls (YouTube-style seek)
        DoubleTapControls(
            player = exoPlayer,
            onToggleControls = { showControls = !showControls },
            modifier = Modifier.fillMaxSize()
        )
        
        // Custom Player Controls Overlay
        CustomPlayerControls(
            player = exoPlayer,
            visible = showControls,
            onVisibilityChange = { showControls = it },
            onBack = {
                if (rotationMode == RotationMode.AUTO) {
                    onBack()
                } else {
                    rotationMode = RotationMode.AUTO
                }
            },
            onShowQualityDialog = onShowQualityDialog,
            onShowSubtitleDialog = onShowSubtitleDialog,
            onShowSpeedDialog = onShowSpeedDialog,
            onNextEpisode = onNextEpisode,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GestureControlsOverlay(
    exoPlayer: ExoPlayer,
    playerView: StyledPlayerView?,
    window: android.view.Window?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var speedFeedback by remember { mutableStateOf(false) }
    var brightnessFeedback by remember { mutableStateOf<Float?>(null) }
    var volumeFeedback by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        // LEFT edge - brightness + double-tap seek
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight(0.70f)
                .align(Alignment.TopStart)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        window?.let { w ->
                            val layoutParams = w.attributes
                            val delta = -dragAmount.y / size.height
                            val newBrightness = (layoutParams.screenBrightness + delta).coerceIn(0f, 1f)
                            layoutParams.screenBrightness = newBrightness
                            w.attributes = layoutParams
                            
                            brightnessFeedback = newBrightness
                            scope.launch {
                                delay(500)
                                brightnessFeedback = null
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val seekAmount = -10000L
                            val newPosition = (exoPlayer.currentPosition + seekAmount).coerceAtLeast(0)
                            exoPlayer.seekTo(newPosition)
                            
                            seekFeedback = SeekFeedback(true, "-10s")
                            scope.launch {
                                delay(800)
                                seekFeedback = null
                            }
                        },
                        onLongPress = {
                            exoPlayer.setPlaybackSpeed(2f)
                            speedFeedback = true
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (speedFeedback) {
                                exoPlayer.setPlaybackSpeed(1f)
                                speedFeedback = false
                            }
                        }
                    )
                }
        )
        
        // RIGHT edge - volume + double-tap seek
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight(0.70f)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                        val delta = (-dragAmount.y / size.height * maxVolume).toInt()
                        val newVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                        
                        volumeFeedback = newVolume
                        scope.launch {
                            delay(500)
                            volumeFeedback = null
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val seekAmount = 10000L
                            val newPosition = (exoPlayer.currentPosition + seekAmount).coerceAtLeast(0)
                            exoPlayer.seekTo(newPosition)
                            
                            seekFeedback = SeekFeedback(false, "+10s")
                            scope.launch {
                                delay(800)
                                seekFeedback = null
                            }
                        },
                        onLongPress = {
                            exoPlayer.setPlaybackSpeed(2f)
                            speedFeedback = true
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (speedFeedback) {
                                exoPlayer.setPlaybackSpeed(1f)
                                speedFeedback = false
                            }
                        }
                    )
                }
        )
        
        // Brightness feedback
        brightnessFeedback?.let { brightness ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Brightness",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        
        // Volume feedback
        volumeFeedback?.let { volume ->
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (volume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(volume * 100 / maxVolume)}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        
        // Seek feedback
        seekFeedback?.let { feedback ->
            Box(
                modifier = Modifier
                    .align(if (feedback.isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 48.dp)
            ) {
                Text(
                    text = feedback.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            }
        }
        
        // Speed feedback
        AnimatedVisibility(
            visible = speedFeedback,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = "2x",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}



private data class SeekFeedback(
    val isLeft: Boolean,
    val text: String
)

@Composable
private fun SubtitleOverlay(cues: List<SubtitleCue>, currentPositionMs: Long) {
    val activeCue = remember(cues, currentPositionMs) {
        cues.find { currentPositionMs in it.startTimeMs..it.endTimeMs }
    }
    activeCue?.let { cue ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = cue.text,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

data class SubtitleGroup(
    val languageName: String,
    val languageCode: String,
    val options: List<SubtitleTrack>
)

@Composable
private fun SubtitleSelectionDialog(
    tracks: List<SubtitleTrack>,
    exoPlayer: ExoPlayer?,
    onDismiss: () -> Unit
) {
    var selectedTrackLanguage by remember { mutableStateOf<String?>(null) }
    
    // Group subtitles by language
    val groupedSubtitles = remember(tracks) {
        tracks.groupBy { normalizeLanguageCode(it.language) }
            .map { (langCode, subtitleList) ->
                SubtitleGroup(
                    languageName = getLanguageName(langCode),
                    languageCode = langCode,
                    options = subtitleList.mapIndexed { index, subtitle ->
                        // Add option number if multiple subtitles for same language
                        if (subtitleList.size > 1) {
                            subtitle.copy(
                                label = "${subtitle.label} (Option ${index + 1})"
                            )
                        } else {
                            subtitle
                        }
                    }
                )
            }
            .sortedBy { it.languageName }
    }
    
    // Use event-driven listener instead of polling for better performance
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: com.google.android.exoplayer2.Tracks) {
                // Find selected text track
                val textTrackGroup = tracks.groups.find { 
                    it.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT && it.isSelected
                }
                
                selectedTrackLanguage = if (textTrackGroup != null && textTrackGroup.length > 0) {
                    textTrackGroup.mediaTrackGroup.getFormat(0).language
                } else {
                    null
                }
            }
        }
        
        exoPlayer?.addListener(listener)
        
        // Initial state
        exoPlayer?.let { player ->
            val currentTracks = player.currentTracks
            val textTrackGroup = currentTracks.groups.find { 
                it.type == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT && it.isSelected
            }
            selectedTrackLanguage = if (textTrackGroup != null && textTrackGroup.length > 0) {
                textTrackGroup.mediaTrackGroup.getFormat(0).language
            } else {
                null
            }
        }
        
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)) {
                Text("Subtitles (${tracks.size} available)", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                SubtitleOption("Off", selectedTrackLanguage == null) { 
                    exoPlayer?.let { player ->
                        val trackSelector = player.trackSelector as? DefaultTrackSelector
                        trackSelector?.parameters = trackSelector?.buildUponParameters()
                            ?.setRendererDisabled(getTextRendererIndex(player), true)
                            ?.build() ?: return@let
                        selectedTrackLanguage = null
                    }
                    onDismiss()
                }
                
                if (groupedSubtitles.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        groupedSubtitles.forEach { group ->
                            item {
                                // Language header
                                Text(
                                    text = group.languageName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
                            
                            // Options for this language
                            items(group.options.size) { index ->
                                val track = group.options[index]
                                SubtitleOption(track.label, selectedTrackLanguage == track.language) {
                                    exoPlayer?.let { player ->
                                        val trackSelector = player.trackSelector as? DefaultTrackSelector
                                        val newParams = trackSelector?.buildUponParameters()
                                            ?.setRendererDisabled(getTextRendererIndex(player), false)
                                            ?.setPreferredTextLanguage(track.language)
                                            ?.setSelectUndeterminedTextLanguage(true)
                                            ?.setForceHighestSupportedBitrate(false)
                                            ?.build()
                                        if (newParams != null && trackSelector != null) {
                                            trackSelector.parameters = newParams
                                        }
                                        selectedTrackLanguage = track.language
                                        
                                        // Force ExoPlayer to refresh tracks
                                        player.prepare()
                                    }
                                    onDismiss()
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No subtitles detected yet. They may appear as the video loads.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioSelectionDialog(
    exoPlayer: ExoPlayer?,
    onDismiss: () -> Unit
) {
    var selectedAudioTrack by remember { mutableStateOf<String?>(null) }
    var availableAudioTracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    // Detect available audio tracks
    LaunchedEffect(exoPlayer) {
        exoPlayer?.let { player ->
            val tracks = player.currentTracks
            val audioTracks = mutableListOf<Pair<String, String>>()
            
            tracks.groups.forEach { group ->
                if (group.type == com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO && group.length > 0) {
                    for (i in 0 until group.length) {
                        val format = group.mediaTrackGroup.getFormat(i)
                        val language = format.language ?: "Unknown"
                        val label = "${getLanguageName(normalizeLanguageCode(language))} - ${format.sampleRate / 1000}kHz"
                        audioTracks.add(Pair(language, label))
                    }
                }
            }
            availableAudioTracks = audioTracks.distinctBy { it.first }
            
            // Find currently selected audio track
            tracks.groups.find { 
                it.type == com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO && it.isSelected 
            }?.let { selectedGroup ->
                if (selectedGroup.length > 0) {
                    selectedAudioTrack = selectedGroup.mediaTrackGroup.getFormat(0).language
                }
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)) {
                Text("Audio Tracks (${availableAudioTracks.size} available)", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                if (availableAudioTracks.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableAudioTracks.size) { index ->
                            val (lang, label) = availableAudioTracks[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            val trackSelector = player.trackSelector as? DefaultTrackSelector
                                            val newParams = trackSelector?.buildUponParameters()
                                                ?.setPreferredAudioLanguage(lang)
                                                ?.build()
                                            if (newParams != null && trackSelector != null) {
                                                trackSelector.parameters = newParams
                                            }
                                            selectedAudioTrack = lang
                                        }
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAudioTrack == lang,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, fontSize = 16.sp, color = if (selectedAudioTrack == lang) MaterialTheme.colorScheme.primary else Color.White)
                            }
                        }
                    }
                } else {
                    Text(
                        "No additional audio tracks available",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

// Helper function to get text renderer index
private fun getTextRendererIndex(player: ExoPlayer): Int {
    val trackSelector = player.trackSelector as? DefaultTrackSelector
    val mappedTrackInfo = trackSelector?.currentMappedTrackInfo ?: return 0
    for (i in 0 until mappedTrackInfo.rendererCount) {
        if (mappedTrackInfo.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_TEXT) {
            return i
        }
    }
    return 0
}

@Composable
private fun SubtitleOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}

@Composable
private fun QualitySelectionDialog(
    qualities: List<VideoQuality>,
    selectedQuality: VideoQuality?,
    onQualitySelected: (VideoQuality?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).widthIn(max = 300.dp)) {
                Text("Video Quality", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                QualityOption("Auto (Recommended)", selectedQuality == null) { onQualitySelected(null) }
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(qualities) { quality ->
                        QualityOption(quality.label, selectedQuality == quality) {
                            onQualitySelected(quality)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
    }
}

fun setVideoQuality(player: ExoPlayer, quality: VideoQuality?) {
    val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
    if (quality == null) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .clearVideoSizeConstraints()
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .build()
    } else {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setMaxVideoSize(Int.MAX_VALUE, quality.height)
            .setMinVideoSize(quality.height, quality.height)
            .build()
    }
}

fun getAvailableQualities(player: ExoPlayer): List<VideoQuality> {
    val mappedTrackInfo = (player.trackSelector as? DefaultTrackSelector)?.currentMappedTrackInfo ?: return emptyList()
    val qualities = mutableListOf<VideoQuality>()
    for (i in 0 until mappedTrackInfo.rendererCount) {
        if (mappedTrackInfo.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO) {
            val groups = mappedTrackInfo.getTrackGroups(i)
            for (j in 0 until groups.length) {
                val group = groups[j]
                for (k in 0 until group.length) {
                    val h = group.getFormat(k).height
                    if (h > 0 && qualities.none { it.height == h }) {
                        val label = when {
                            h >= 2160 -> "4K"
                            h >= 1080 -> "1080p"
                            h >= 720 -> "720p"
                            h >= 480 -> "480p"
                            else -> "${h}p"
                        }
                        qualities.add(VideoQuality(h, label, k))
                    }
                }
            }
        }
    }
    return qualities.sortedByDescending { it.height }
}

@Composable
private fun LoadingState(media: VidoraMediaItem, message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        media.posterPath?.let {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500$it",
                contentDescription = null,
                modifier = Modifier.width(200.dp).height(300.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 16.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(media.displayTitle, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
private fun PlayerErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Playback Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry Stream Extraction")
            }
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

private sealed class PlayerState {
    data class Loading(val message: String) : PlayerState()
    data class Playing(val streamInfo: StreamInfo) : PlayerState()
    data class Error(val message: String) : PlayerState()
}
