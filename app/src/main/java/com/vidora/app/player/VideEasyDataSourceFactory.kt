package com.vidora.app.player

import android.net.Uri
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * Custom DataSource.Factory for the NewParadise/TigerFlare CDN (newparadise.site).
 *
 * The CDN serves MPEG-TS video segments disguised as .jpg or .js files with
 * Content-Type: image/jpeg. ExoPlayer rejects these because the MIME type doesn't
 * match. This factory wraps every segment response and forces the content type to
 * video/mp2t so ExoPlayer parses them correctly.
 *
 * Additionally, it injects the correct Referer & Cookie headers for AES-128
 * key requests so the CDN doesn't respond with 403 Forbidden.
 */
class VideEasyDataSourceFactory(
    private val userAgent: String,
    private val referer: String,
    private val cookies: String?,
    private val additionalHeaders: Map<String, String> = emptyMap()
) : DataSource.Factory {

    companion object {
        private const val NEWPARADISE_HOST = "newparadise.site"

        /** Base64-decoded patterns from the TigerFlare spec */
        private val FAKE_EXTENSION_REGEX = Regex(""".*\.(jpg|js|png)$""", RegexOption.IGNORE_CASE)
    }

    private fun buildHttpFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(buildMap {
                put("Referer", referer)
                put("Origin", referer.trimEnd('/'))
                put("X-Requested-With", "com.android.chrome")
                if (!cookies.isNullOrEmpty()) {
                    put("Cookie", cookies)
                }
                putAll(additionalHeaders)
            })
    }

    override fun createDataSource(): DataSource {
        val httpDataSource = buildHttpFactory().createDataSource()
        return MimeOverrideDataSource(httpDataSource)
    }

    /**
     * Wraps an inner DataSource and overrides the Content-Type to video/mp2t
     * when the URL matches a NewParadise segment pattern (.jpg / .js / .png
     * served from newparadise.site).
     */
    private class MimeOverrideDataSource(
        private val inner: DataSource
    ) : DataSource {

        override fun addTransferListener(transferListener: TransferListener) {
            inner.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            return inner.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return inner.read(buffer, offset, length)
        }

        override fun getUri(): Uri? = inner.uri

        // Override response headers — swap Content-Type when the URL looks
        // like a disguised MPEG-TS segment from the NewParadise CDN.
        override fun getResponseHeaders(): Map<String, List<String>> {
            val original = inner.responseHeaders
            val uri = inner.uri?.toString() ?: return original

            val isNewParadise = uri.contains(NEWPARADISE_HOST)
            val isFakeExtension = FAKE_EXTENSION_REGEX.matches(uri.substringBefore("?"))

            return if (isNewParadise && isFakeExtension) {
                // Replace Content-Type with video/mp2t so ExoPlayer parses as MPEG-TS
                original.toMutableMap().apply {
                    put("Content-Type", listOf("video/mp2t"))
                    remove("content-type") // case-insensitive cleanup
                }
            } else {
                original
            }
        }

        override fun close() = inner.close()
    }
}
