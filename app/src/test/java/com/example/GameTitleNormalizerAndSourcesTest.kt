package com.example

import com.example.data.metadata.CoverFetchResult
import com.example.data.metadata.DescriptionFetchResult
import com.example.data.metadata.GameTitleNormalizer
import com.example.data.metadata.LibretroCoverArtSource
import com.example.data.metadata.WikimediaDescriptionSource
import com.example.model.ConsoleCatalog
import com.example.model.GameEntry
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameTitleNormalizerAndSourcesTest {

    private val silentHillEntry = GameEntry(
        id = 1L,
        consoleId = ConsoleCatalog.PS1_ID,
        displayTitle = "Silent Hill (USA)",
        fileName = "Silent Hill (USA).cue",
        uriString = "content://com.android.externalstorage.documents/tree/primary%3AROM%2FPS1/document/SilentHill.cue",
        sourceFolderUri = "content://com.android.externalstorage.documents/tree/primary%3AROM%2FPS1",
        fileType = "CUE"
    )

    @Test
    fun `normalizes Silent Hill USA cue cleanly for search without modifying original name`() {
        assertEquals(
            "Silent Hill",
            GameTitleNormalizer.normalizeForSearch("Silent Hill (USA).cue")
        )
        assertEquals(
            "Chrono Cross",
            GameTitleNormalizer.normalizeForSearch("Chrono_Cross_(USA)_(Disc_1)_[SLUS-01041].chd")
        )
        assertEquals(
            "Metal Gear Solid",
            GameTitleNormalizer.normalizeForSearch("Metal Gear Solid (Europe) (Rev 1) (Track 01).pbp")
        )
        // Original entry fileName stays completely untouched
        assertEquals("Silent Hill (USA).cue", silentHillEntry.fileName)
    }

    @Test
    fun `sanitizes libretro special characters and builds prioritized boxart candidates`() {
        assertEquals(
            "Castlevania_ Symphony of the Night _ Extra",
            GameTitleNormalizer.sanitizeLibretroName("Castlevania: Symphony of the Night & Extra")
        )
        val candidates = GameTitleNormalizer.libretroBoxartCandidates("Silent Hill (USA).cue")
        assertTrue(candidates.first() == "Silent Hill (USA)")
        assertTrue(candidates.contains("Silent Hill"))
    }

    @Test
    fun `verifies PS1 platform and rejects sequels remakes films and disambiguations`() {
        assertTrue(
            GameTitleNormalizer.isValidPs1WikipediaMatch(
                queryTitle = "Silent Hill",
                articleTitle = "Silent Hill (1999 video game)",
                articleType = "standard",
                description = "1999 survival horror video game",
                extractOrSnippet = "Silent Hill is a 1999 survival horror game developed by Team Silent for the PlayStation."
            )
        )
        assertFalse(
            GameTitleNormalizer.isValidPs1WikipediaMatch(
                queryTitle = "Silent Hill",
                articleTitle = "Silent Hill 2",
                articleType = "standard",
                description = "2001 video game",
                extractOrSnippet = "Silent Hill 2 is a 2001 survival horror game for the PlayStation 2."
            )
        )
        assertFalse(
            GameTitleNormalizer.isValidPs1WikipediaMatch(
                queryTitle = "Silent Hill",
                articleTitle = "Silent Hill (2024 remake)",
                articleType = "standard",
                description = "2024 PlayStation 5 remake",
                extractOrSnippet = "Remake for PlayStation 5."
            )
        )
        assertFalse(
            GameTitleNormalizer.isValidPs1WikipediaMatch(
                queryTitle = "Silent Hill",
                articleTitle = "Silent Hill (film)",
                articleType = "standard",
                description = "2006 horror film",
                extractOrSnippet = "A 2006 supernatural horror film directed by Christophe Gans."
            )
        )
        assertFalse(
            GameTitleNormalizer.isValidPs1WikipediaMatch(
                queryTitle = "Silent Hill",
                articleTitle = "Silent Hill",
                articleType = "disambiguation",
                description = "Topics referred to by the same term",
                extractOrSnippet = "Silent Hill may refer to multiple video games on PlayStation."
            )
        )
    }

    @Test
    fun `libretro source downloads valid PNG and handles 403 or 404 gracefully`() = runBlocking {
        val fakePng = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        ) + ByteArray(64) { 0x01 }

        val okClient = mockClient { req ->
            if (req.url.toString().endsWith("Silent%20Hill%20(USA).png")) {
                binaryResponse(req, 200, "image/png", fakePng)
            } else {
                textResponse(req, 404, "Not found")
            }
        }
        val source = LibretroCoverArtSource(okClient, "https://example.test/Named_Boxarts")
        val found = source.fetchPs1Cover(silentHillEntry, "Silent Hill")
        assertTrue(found is CoverFetchResult.Found)
        assertEquals(LibretroCoverArtSource.SOURCE_LABEL, (found as CoverFetchResult.Found).sourceLabel)

        val blockedClient = mockClient { req -> textResponse(req, 403, "Forbidden") }
        val blockedSource = LibretroCoverArtSource(blockedClient, "https://example.test/Named_Boxarts")
        val blocked = blockedSource.fetchPs1Cover(silentHillEntry, "Silent Hill")
        assertTrue(blocked is CoverFetchResult.Blocked)
    }

    @Test
    fun `wikimedia source prioritizes PT then falls back to EN and rejects PS2 sequels`() = runBlocking {
        val seenUserAgents = mutableListOf<String?>()
        val client = mockClient { req ->
            seenUserAgents.add(req.header("User-Agent"))
            val url = req.url.toString()
            when {
                // PT search returns only Silent Hill (filme) and Silent Hill 2
                url.startsWith("https://pt.test/w/api.php") -> jsonResponse(
                    req,
                    """{"query":{"search":[{"title":"Silent Hill (filme)"},{"title":"Silent Hill 2"}]}}"""
                )
                url.contains("pt.test/api/rest_v1/page/summary/Silent_Hill_(filme)") -> jsonResponse(
                    req,
                    """{"type":"standard","title":"Silent Hill (filme)","description":"filme de 2006","extract":"Silent Hill é um filme de terror."}"""
                )
                url.contains("pt.test/api/rest_v1/page/summary/Silent_Hill_2") -> jsonResponse(
                    req,
                    """{"type":"standard","title":"Silent Hill 2","description":"jogo de PlayStation 2","extract":"Silent Hill 2 é um jogo de PlayStation 2."}"""
                )
                url.startsWith("https://pt.test/api/rest_v1/page/summary/") -> textResponse(req, 404, "{}")

                // EN search returns Silent Hill (1999 video game) after a PS2 hit
                url.startsWith("https://en.test/w/api.php") -> jsonResponse(
                    req,
                    """{"query":{"search":[{"title":"Silent Hill 2"},{"title":"Silent Hill (1999 video game)"}]}}"""
                )
                url.contains("en.test/api/rest_v1/page/summary/Silent_Hill_2") -> jsonResponse(
                    req,
                    """{"type":"standard","title":"Silent Hill 2","description":"2001 video game","extract":"Survival horror game for PlayStation 2."}"""
                )
                url.contains("en.test/api/rest_v1/page/summary/Silent_Hill_(1999_video_game)") -> jsonResponse(
                    req,
                    """
                    {
                      "type": "standard",
                      "title": "Silent Hill (1999 video game)",
                      "description": "1999 survival horror video game",
                      "extract": "Silent Hill is a 1999 survival horror video game published by Konami for the PlayStation.",
                      "content_urls": {
                        "desktop": {
                          "page": "https://en.wikipedia.org/wiki/Silent_Hill_(1999_video_game)"
                        }
                      }
                    }
                    """.trimIndent()
                )
                else -> textResponse(req, 404, "{}")
            }
        }

        val source = WikimediaDescriptionSource(
            httpClient = client,
            ptBaseUrl = "https://pt.test",
            enBaseUrl = "https://en.test"
        )
        val result = source.fetchPs1Description(silentHillEntry, "Silent Hill")
        assertTrue(result is DescriptionFetchResult.Found)
        val found = result as DescriptionFetchResult.Found
        assertEquals("Wikipedia (EN)", found.sourceLabel)
        assertEquals("CC BY-SA 4.0", found.licenseText)
        assertEquals("https://en.wikipedia.org/wiki/Silent_Hill_(1999_video_game)", found.sourceUrl)
        assertTrue(found.description.contains("1999 survival horror video game"))
        assertTrue(seenUserAgents.all { !it.isNullOrBlank() && it.contains("DomDeck") })
    }

    private fun mockClient(handler: (Request) -> Response): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain -> handler(chain.request()) })
            .build()
    }

    private fun jsonResponse(req: Request, body: String): Response = Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    private fun textResponse(req: Request, code: Int, body: String): Response = Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("Status $code")
        .body(body.toResponseBody("text/plain".toMediaType()))
        .build()

    private fun binaryResponse(req: Request, code: Int, mime: String, bytes: ByteArray): Response = Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("OK")
        .body(bytes.toResponseBody(mime.toMediaType()))
        .build()
}
