package com.vidora.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerWebView(url: String, onBack: () -> Unit) {
    // Force back button to exit the player screen
    BackHandler {
        onBack()
    }

    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            webViewClient = object : WebViewClient() {
                // If the website tries to navigate away from the player (e.g. clicking a back button in the web UI)
                // we should close this WebView screen instead of showing the website's mobile pages.
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
