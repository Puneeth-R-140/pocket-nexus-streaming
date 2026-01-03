package com.vidora.app.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerWebView(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val window = activity?.window

    // Handle immersive mode and screen wake lock
    if (window != null && activity != null) {
        DisposableEffect(Unit) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            
            // Enter Immersive Mode - hide system bars
            windowInsetsController.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            
            // Keep screen on during playback
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            onDispose {
                // Restore system bars
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                
                // Remove keep screen on flag
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Force back button to exit the player screen
    BackHandler {
        onBack()
    }

    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            webViewClient = object : WebViewClient() {
                // If the website tries to navigate away from the player
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val newUrl = request?.url?.toString() ?: ""
                    if (!newUrl.contains("/watch/")) {
                        onBack()
                        return true
                    }
                    return false
                }

                // Basic Ad-blocking by intercepting requests
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val adDomains = listOf("doubleclick.net", "google-analytics.com", "adskeeper.com", "popads.net")
                    val requestUrl = request?.url?.toString() ?: ""
                    
                    if (adDomains.any { requestUrl.contains(it) }) {
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                    
                    return super.shouldInterceptRequest(view, request)
                }
            }
            
            loadUrl(url)
        }
    })
}
