package com.vidora.app.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "HlsManifestParser"

/**
 * Data class to hold stream information
 */
data class StreamInfo(
    val streamUrl: String,
    val qualities: List<QualityLevel> = emptyList()
)


data class QualityLevel(
    val resolution: String,
    val bandwidth: Int,
    val url: String
)

/**
 * Parses HLS manifests to extract subtitle tracks and quality levels.
 *
 * Handles the "double manifest" pattern used by NewParadise/TigerFlare:
 *   Master manifest → #EXT-X-STREAM-INF → Media manifest → #EXT-X-KEY + #EXTINF segments
 *
 * We intentionally pass the MASTER manifest URL to ExoPlayer's HlsMediaSource —
 * ExoPlayer's HLS stack handles quality selection and AES-128 decryption natively.
 * This parser only needs to extract the quality list for the in-app quality picker
 * and, crucially, resolve any AES-128 key URL so we can verify auth headers are correct.
 *
 * FIX: The original parser would pass the master .m3u8 to a custom segment parser which
 * does not understand #EXT-X-STREAM-INF. We now detect master vs media manifests and
 * resolve one level down to check for #EXT-X-KEY when needed.
 */
class HlsManifestParser(
    private val referer: String? = null,
    private val cookies: String? = null
) {

    suspend fun parseManifest(manifestUrl: String): StreamInfo = withContext(Dispatchers.IO) {
        try {
            val manifestContent = fetchManifest(manifestUrl)
                ?: return@withContext StreamInfo(streamUrl = manifestUrl)

            val isMasterManifest = manifestContent.contains("#EXT-X-STREAM-INF")

            if (isMasterManifest) {
                Log.d(TAG, "Detected MASTER manifest — resolving quality variants")
                val qualities = parseQualities(manifestContent, manifestUrl)

                // Pick the highest-bandwidth variant to also resolve its media manifest
                // so we can log+verify the AES-128 key URL is reachable.
                val topVariant = qualities.firstOrNull()
                if (topVariant != null) {
                    Log.d(TAG, "Resolving media manifest: ${topVariant.url}")
                    val mediaContent = fetchManifest(topVariant.url)
                    if (mediaContent != null) {
                        val keyUrl = parseAesKeyUrl(mediaContent, topVariant.url)
                        if (keyUrl != null) {
                            Log.d(TAG, "AES-128 key URL found: $keyUrl")
                        }
                    }
                }

                // Always return the MASTER manifest URL to ExoPlayer —
                // ExoPlayer's HlsMediaSource handles quality switching and decryption natively.
                StreamInfo(
                    streamUrl = manifestUrl,
                    qualities = qualities
                )
            } else {
                // This is already a media-level manifest (single rendition or no variants)
                Log.d(TAG, "Detected MEDIA manifest — no quality variants, passing directly")
                val keyUrl = parseAesKeyUrl(manifestContent, manifestUrl)
                if (keyUrl != null) {
                    Log.d(TAG, "AES-128 key URL: $keyUrl")
                }
                StreamInfo(
                    streamUrl = manifestUrl,
                    qualities = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse manifest: ${e.message}")
            StreamInfo(streamUrl = manifestUrl)
        }
    }

    /**
     * Fetch a manifest with the correct Referer, Origin, and Cookie headers
     * so CDN auth checks pass (same headers ExoPlayer will send for segments).
     */
    private fun fetchManifest(url: String): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                // Origin + Referer must both be present for Cloudflare/Nginx checks
                referer?.let {
                    setRequestProperty("Referer", it)
                    setRequestProperty("Origin", it.trimEnd('/'))
                }
                cookies?.let { setRequestProperty("Cookie", it) }
                setRequestProperty("Accept", "*/*")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                Log.w(TAG, "Manifest fetch returned ${connection.responseCode} for $url")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchManifest failed for $url: ${e.message}")
            null
        }
    }


    private fun parseQualities(manifest: String, baseUrl: String): List<QualityLevel> {
        val qualities = mutableListOf<QualityLevel>()
        val lines = manifest.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val bandwidth = extractAttribute(line, "BANDWIDTH")?.toIntOrNull() ?: 0
                val resolution = extractAttribute(line, "RESOLUTION") ?: "Auto"
                if (i + 1 < lines.size) {
                    val url = lines[i + 1].trim()
                    if (url.isNotEmpty() && !url.startsWith("#")) {
                        qualities.add(QualityLevel(resolution, bandwidth, resolveUrl(baseUrl, url)))
                    }
                }
            }
            i++
        }
        return qualities.sortedByDescending { it.bandwidth }
    }

    /**
     * Extract the AES-128 key URL from a media manifest (#EXT-X-KEY).
     * Used for diagnostic logging — ExoPlayer fetches the key automatically.
     */
    private fun parseAesKeyUrl(manifest: String, baseUrl: String): String? {
        manifest.lines().forEach { line ->
            if (line.startsWith("#EXT-X-KEY:")) {
                val uri = extractAttribute(line, "URI")
                if (uri != null) return resolveUrl(baseUrl, uri)
            }
        }
        return null
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val withQuotes = Regex("""$attribute="([^"]+)"""").find(line)?.groupValues?.get(1)
        if (withQuotes != null) return withQuotes
        return Regex("""$attribute=([^,\s]+)""").find(line)?.groupValues?.get(1)
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return if (relativeUrl.startsWith("http")) {
            relativeUrl
        } else {
            val base = baseUrl.substringBeforeLast("/")
            "$base/$relativeUrl"
        }
    }
}
