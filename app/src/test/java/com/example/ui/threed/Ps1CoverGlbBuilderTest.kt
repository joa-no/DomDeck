package com.example.ui.threed

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Ps1CoverGlbBuilderTest {
    @Test
    fun `replaces temporary cover using valid embedded glb texture`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = context.assets.open("models/domdeck_ps1_test.glb").use { it.readBytes() }
        val coverFile = File(context.cacheDir, "test_ps1_artwork.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            coverFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        val result = try {
            Ps1CoverGlbBuilder.build(original, coverFile)
        } finally {
            coverFile.delete()
        }

        assertTrue(result.size > original.size)
        assertEquals(0x46546c67, ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).getInt(0))
        val buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(result.size, buffer.getInt(8))
        val jsonLen = buffer.getInt(12)
        val json = JSONObject(String(result, 20, jsonLen, Charsets.UTF_8).trim())
        val artworkNode = json.getJSONArray("nodes").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }
                .first { it.getString("name") == "Artwork" }
        }
        val primitive = json.getJSONArray("meshes")
            .getJSONObject(artworkNode.getInt("mesh"))
            .getJSONArray("primitives").getJSONObject(0)
        assertTrue(primitive.getJSONObject("attributes").has("TEXCOORD_0"))
        val material = json.getJSONArray("materials")
            .getJSONObject(primitive.getInt("material"))
            .getJSONObject("pbrMetallicRoughness")
        assertTrue(material.has("baseColorTexture"))
        assertEquals("image/png", json.getJSONArray("images").getJSONObject(0).getString("mimeType"))
        assertEquals(2, json.getJSONArray("animations").length())
        val binOffset = 20 + jsonLen
        assertEquals(result.size, binOffset + 8 + buffer.getInt(binOffset))
    }
}
