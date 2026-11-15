package com.example.data.metadata

import android.net.Uri
import com.example.model.GameEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Fetches PlayStation 1 box art from Libretro Thumbnails public repository:
 * https://thumbnails.libretro.com/Sony%20-%20PlayStation/Named_Boxarts/<Name>.png
 *
 * Handles missing covers (404), rate limiting (429), or forbidden responses (403/451) cleanly
 * without bundling copyrighted artwork in the APK.
 */
class LibretroCoverArtSource(
    private val httpClient: OkHttpClient,
    private val baseBoxartsUrl: String = DEFAULT_PS1_BOXARTS_URL
) : CoverArtSource {

    override suspend fun fetchPs1Cover(game: GameEntry, searchTitle: String): CoverFetchResult =
        withContext(Dispatchers.IO) {
            val candidates = GameTitleNormalizer.libretroBoxartCandidates(game.fileName)
            var lastNetworkError: String? = null

            for (candidate in candidates) {
                val encodedFile = Uri.encode("$candidate.png")
                val url = "${baseBoxartsUrl.trimEnd('/')}/$encodedFile"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                try {
                    httpClient.newCall(request).execute().use { response ->
                        when {
                            response.isSuccessful -> {
                                val bytes = response.body?.bytes()
                                if (bytes != null && bytes.size in 64..MAX_COVER_BYTES && isValidImageHeader(bytes)) {
                                    return@withContext CoverFetchResult.Found(
                                        bytes = bytes,
                                        sourceLabel = SOURCE_LABEL,
                                        resolvedUrl = url
                                    )
                                }
                            }
                            response.code == 403 || response.code == 451 -> {
                                return@withContext CoverFetchResult.Blocked(
                                    "Download de capa bloqueado pela origem (HTTP ${response.code})."
                                )
                            }
                            response.code == 429 -> {
                                return@withContext CoverFetchResult.NetworkError(
                                    "Limite temporário de requisições de capa atingido (HTTP 429)."
                                )
                            }
                            response.code == 404 -> {
                                // Try next candidate
                            }
                            response.code >= 500 -> {
                                lastNetworkError = "Servidor de capas indisponível (HTTP ${response.code})."
                            }
                        }
                    }
                } catch (e: IOException) {
                    lastNetworkError = e.message ?: "Falha de rede ao consultar capas."
                }
            }

            lastNetworkError?.let { CoverFetchResult.NetworkError(it) }
                ?: CoverFetchResult.NotFound
        }

    companion object {
        const val DEFAULT_PS1_BOXARTS_URL =
            "https://thumbnails.libretro.com/Sony%20-%20PlayStation/Named_Boxarts"
        const val SOURCE_LABEL = "Libretro Thumbnails"
        private const val USER_AGENT = "DomDeck/0.2 (Android; Retro Frontend)"
        private const val MAX_COVER_BYTES = 5 * 1024 * 1024

        internal fun isValidImageHeader(bytes: ByteArray): Boolean {
            if (bytes.size < 12) return false
            // PNG: 89 50 4E 47
            val isPng = bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
            // JPEG: FF D8 FF
            val isJpeg = bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
            // WebP: RIFF....WEBP
            val isWebP = bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            return isPng || isJpeg || isWebP
        }
    }
}
