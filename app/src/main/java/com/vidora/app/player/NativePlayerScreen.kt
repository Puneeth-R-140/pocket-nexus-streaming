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
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
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
    var showSpeedDialog by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var webViewKey by remember { mutableStateOf(0) } // For forcing WebView recreation on retry
    var cookies by remember { mutableStateOf<String?>(null) } // Captured cookies from WebView
    var streamReferer by remember { mutableStateOf<String?>(null) } // Referer for AES-128 key requests
    var userSettings by remember { mutableStateOf<com.vidora.app.data.local.SettingsEntity?>(null) }
    var showAutoPlayCountdown by remember { mutableStateOf(false) }
    var nextEpisodeInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var totalEpisodesInSeason by remember { mutableStateOf<Int?>(null) }
    var totalSeasons by remember { mutableStateOf<Int?>(null) }
    
    var currentMedia by remember { mutableStateOf(media) }
    
    // Fetch real metadata if we got a placeholder
    LaunchedEffect(media.id) {
        if (media.displayTitle == "Loading...") {
            Log.d(TAG, "Media title is placeholder, fetching real metadata...")
            repository.getDetails(media.realMediaType, media.id).collect { result ->
                if (result is NetworkResult.Success) {
                    currentMedia = result.data
                    Log.d(TAG, "Metadata recovered: ${result.data.displayTitle}")
                }
            }
        }
    }

    // Fetch total seasons
    LaunchedEffect(currentMedia.id) {
        if (currentMedia.realMediaType == "tv") {
            repository.getDetails("tv", currentMedia.id).collect { result ->
                if (result is NetworkResult.Success) {
                    totalSeasons = result.data.numberOfSeasons ?: result.data.seasons?.size
                }
            }
        }
    }
    
    // Extract season and episode from URL for TV shows
    val (season, episode) = remember(playerUrl, currentMedia.realMediaType) {
        if (currentMedia.realMediaType == "tv") {
            extractSeasonEpisodeFromUrl(playerUrl)
        } else {
            null to null
        }
    }

    // Fetch episodes to know when to switch seasons
    LaunchedEffect(currentMedia.id, season) {
        if (currentMedia.realMediaType == "tv" && season != null) {
            repository.getEpisodes(currentMedia.id, season).collect { result ->
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
    
    // Subtitles removed at user request
    
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

                                // FIX: Inject BEFORE the page's JS runs to neutralize
                                // the debugger loop before the WASM module arms it.
                                override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    Log.d(TAG, "Page started: $url — injecting anti-debugger")
                                    injectDebuggerBypass(view)
                                }

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "Page finished loading: $url")
                                    // Re-inject on finish as a safety net (some loaders defer JS)
                                    injectDebuggerBypass(view)
                                    url?.let {
                                        val extractedCookies = android.webkit.CookieManager.getInstance().getCookie(it)
                                        if (!extractedCookies.isNullOrEmpty()) {
                                            cookies = extractedCookies
                                            Log.d(TAG, "Cookies extracted from $it")
                                        }
                                    }
                                    // Auto-click the movish.net play button (id="preloader-play-btn")
                                    // Falls back to clicking any visible play-related element
                                    view?.evaluateJavascript("""
                                        (function() {
                                            var btn = document.getElementById('preloader-play-btn');
                                            if (btn) {
                                                btn.click();
                                                console.log('[Vidora] Clicked #preloader-play-btn');
                                                return;
                                            }
                                            // Fallback: any element with class containing 'play'
                                            var fallback = document.querySelector('[class*="play"]');
                                            if (fallback) { fallback.click(); console.log('[Vidora] Clicked fallback play'); }
                                        })();
                                    """.trimIndent(), null)
                                }

                                override fun shouldInterceptRequest(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): android.webkit.WebResourceResponse? {
                                    val url = request?.url?.toString() ?: ""

                                    // Intercept stream URL from any provider:
                                    // - Standard HLS .m3u8
                                    // - movish.net: /moviebox-stream?url=... (direct MP4 proxy)
                                    // - hakunaymatata.com direct .mp4 links
                                    // - NewParadise CDN pattern
                                    // - vidsrc-embed.ru CDN
                                    val isStream = url.contains(".m3u8") ||
                                        url.contains("movish.net/moviebox-stream") ||
                                        (url.contains("movish.net") && url.contains(".mp4")) ||
                                        (url.contains("hakunaymatata.com") && url.contains(".mp4")) ||
                                        (url.contains("newparadise.site") && url.contains("file2")) ||
                                        (url.contains("orbitbear66.pro") && url.contains("file2")) ||
                                         (url.contains("akcloud.animanga.fun") && (url.contains("proxy") || url.contains("mp4-proxy") || url.contains("ts-proxy"))) ||
                                         (url.contains("neonhorizonworkshops.com") && url.contains("/pl/"))


                                    if (isStream && extractedUrl == null) {
                                        Log.d(TAG, "Intercepted stream: $url")
                                        extractedUrl = url

                                        // Capture Referer for ExoPlayer segment/key requests
                                        val pageReferer = request?.requestHeaders?.get("Referer")
                                            ?: "https://vidnest.fun/"
                                        streamReferer = pageReferer
                                        Log.d(TAG, "Using Referer: ${'$'}pageReferer")

                                        // Give the page 1 second to request subtitles before playing
                                        scope.launch {
                                            kotlinx.coroutines.delay(1000)
                                            state = PlayerState.Playing(
                                                StreamInfo(
                                                    streamUrl = url
                                                )
                                            )
                                        }
                                    }

                                    return super.shouldInterceptRequest(view, request)
                                }
                            }

                            // Belt-and-suspenders: also inject at 10% load via ChromeClient
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    if (newProgress in 10..15) {
                                        Log.d(TAG, "Progress $newProgress% — re-injecting anti-debugger")
                                        injectDebuggerBypass(view)
                                    }
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
                    media = currentMedia,
                    repository = repository,
                    season = season,
                    episode = episode,
                    streamInfo = currentState.streamInfo,
                    onBack = onBack,
                    cookies = cookies,
                    streamReferer = streamReferer,
                    webViewKey = webViewKey,
                    onWebViewKeyChange = { webViewKey = it },
                    exoPlayerInstance = exoPlayerInstance,
                    onExoPlayerInstanceChange = { exoPlayerInstance = it },
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

/**
 * Neutralizes the anti-bot debugger loop used by videasy.net / newparadise.site.
 *
 * Called from three points for maximum coverage:
 *   1. onPageStarted  — before WASM module initializes (earliest possible)
 *   2. onProgressChanged at ~10% — when initial JS bundles are parsed
 *   3. onPageFinished — safety net for lazy-loaded scripts
 */
private fun injectDebuggerBypass(view: android.webkit.WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            if (window.__vidoraDebuggerKilled) return;
            window.__vidoraDebuggerKilled = true;

            // 1. Kill the Function-based debugger trap
            const _constructor = window.Function.prototype.constructor;
            window.Function.prototype.constructor = function(arg) {
                if (arg === 'debugger') return function() {};
                return _constructor.apply(this, arguments);
            };

            // 2. Neutralize the 'debugger' keyword in loops
            setInterval = (function(oldSetInterval) {
                return function(func, delay) {
                    if (func.toString().indexOf('debugger') !== -1) return null;
                    return oldSetInterval(func, delay);
                };
            })(setInterval);

            // 3. (Intentionally skip mass clearInterval — providers use intervals
            //    to keep session cookies alive. Killing them causes 403s mid-stream.
            //    The setInterval override above already blocks debugger-containing callbacks.)  

            console.log('[Vidora] Anti-debugger injected at: ' + document.readyState);
        })();
        """.trimIndent(),
        null
    )
}

// Helper function to extract season and episode from provider URL
// URL patterns supported:
//   videasy.net: https://player.videasy.net/tv/{id}/{season}/{episode}
//   legacy:      https://watch.vidora.su/watch/tv/{id}/{season}/{episode}
private fun extractSeasonEpisodeFromUrl(url: String): Pair<Int?, Int?> {
    return try {
        val cleanUrl = if (url.contains("?")) url.substringBefore("?") else url
        val parts = cleanUrl.split("/")
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
    streamReferer: String?,
    webViewKey: Int,
    onWebViewKeyChange: (Int) -> Unit,
    exoPlayerInstance: ExoPlayer?,
    onExoPlayerInstanceChange: (ExoPlayer?) -> Unit,
    userSettings: com.vidora.app.data.local.SettingsEntity?,
    onVideoEnded: (season: Int, episode: Int) -> Unit,
    onQualitiesDetected: (List<VideoQuality>, ExoPlayer) -> Unit,
    onShowQualityDialog: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onNextEpisode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val internalScope = rememberCoroutineScope()
    var currentPos by remember { mutableStateOf(0L) }
    var startupPosition by remember { mutableStateOf<Long?>(null) }
    var isHistoryLoaded by remember { mutableStateOf(false) }
    var rotationMode by remember { mutableStateOf(RotationMode.AUTO) }
    var scalingMode by remember { mutableStateOf(ScalingMode.FIT) }
    var showControls by remember { mutableStateOf(true) }
    val activity = context.findActivity()
    val window = activity?.window
    
    // Create ExoPlayer ONCE — never recreated. No cookies/streamReferer in the
    // remember key. Headers are updated post-creation via playerHolder.updateHeaders()
    // so cookies that arrive from WebView late are transparently injected into future
    // HLS segment and AES-128 key requests without any player reset.
    val playerHolder = remember {
        Log.d(TAG, "Creating PlayerHolder (one-time)")
        PlayerFactory.createPlayerHolder(
            context,
            userAgent = USER_AGENT,
            cookies = null,   // headers injected below once WebView extracts them
            referer = null
        ).also { onExoPlayerInstanceChange(it.player) }
    }
    val exoPlayer = playerHolder.player

    // Live header injection: whenever cookies or referer arrive from the WebView,
    // push them into the DataSource.Factory. No player restart needed.
    LaunchedEffect(cookies, streamReferer) {
        val ref = streamReferer ?: "https://vidnest.fun/"
        playerHolder.updateHeaders(ref, cookies)
        Log.d(TAG, "Headers updated — referer: $ref, cookies: ${cookies?.take(30)}")
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
        isHistoryLoaded = true
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
            
            // Enable drawing behind cutouts (notches)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                w.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
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
            exoPlayerInstance?.release()
        }
    }
    
    
    // --- One-time player setup: set MediaItem and prepare ONCE. ---
    // Subtitles are handled entirely by SubtitleOverlay (see below) and never
    // require a player reset. This is the ONLY place prepare() is called.
    LaunchedEffect(streamInfo.streamUrl, isHistoryLoaded) {
        val player = exoPlayerInstance ?: return@LaunchedEffect
        if (!isHistoryLoaded || streamInfo.streamUrl.isEmpty()) return@LaunchedEffect
        // Only set MediaItem if the player hasn't been set up yet
        if (player.mediaItemCount > 0) return@LaunchedEffect

        val mediaItem = ExoMediaItem.Builder()
            .setUri(streamInfo.streamUrl)
            .apply {
                val url = streamInfo.streamUrl.lowercase()
                when {
                    url.contains(".m3u8") || url.contains("/proxy") -> 
                        setMimeType(com.google.android.exoplayer2.util.MimeTypes.APPLICATION_M3U8)
                    url.contains(".mp4") || url.contains("/mp4-proxy") -> 
                        setMimeType(com.google.android.exoplayer2.util.MimeTypes.VIDEO_MP4)
                    url.contains(".ts") || url.contains("/ts-proxy") -> 
                        setMimeType(com.google.android.exoplayer2.util.MimeTypes.VIDEO_MP2T)
                }
            }
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()

        // Restore saved watch position (or start from beginning)
        startupPosition?.let { pos ->
            player.seekTo(pos)
            startupPosition = null
        }

        player.playWhenReady = true
    }

    
    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    onQualitiesDetected(getAvailableQualities(exoPlayer), exoPlayer)
                    Log.d(TAG, "Player ready")
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
        exoPlayerInstance?.let { player ->
            DoubleTapControls(
                player = player,
                onToggleControls = { showControls = !showControls },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Custom Player Controls Overlay
        exoPlayerInstance?.let { player ->
            CustomPlayerControls(
                player = player,
                visible = showControls,
                onVisibilityChange = { showControls = it },
                onShowQualityDialog = onShowQualityDialog,
                onShowSpeedDialog = onShowSpeedDialog,
                onNextEpisode = onNextEpisode,
                onBack = {
                    if (rotationMode == RotationMode.AUTO) {
                        onBack()
                    } else {
                        rotationMode = RotationMode.AUTO
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
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
                        onPress = {
                            tryAwaitRelease()
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
                        onPress = {
                            tryAwaitRelease()
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

    // Subtitles removed at user request

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
