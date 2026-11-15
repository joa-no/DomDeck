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
        val nodes = json.getJSONArray("nodes")
        val quadIndex = (0 until nodes.length()).first {
            nodes.getJSONObject(it).getString("name") == "CoverArtFront"
        }
        val lidPivot = (0 until nodes.length()).map { nodes.getJSONObject(it) }
            .first { it.getString("name") == "LidPivot" }
        assertTrue(lidPivot.getJSONArray("children").toString().contains(quadIndex.toString()))
        val coverNode = nodes.getJSONObject(quadIndex)
        assertTrue(coverNode.getJSONArray("translation").getDouble(2) > 0.05)
        val primitive = json.getJSONArray("meshes")
            .getJSONObject(coverNode.getInt("mesh"))
            .getJSONArray("primitives").getJSONObject(0)
        assertTrue(primitive.getJSONObject("attributes").has("TEXCOORD_0"))
        val material = json.getJSONArray("materials")
            .getJSONObject(primitive.getInt("material"))
        assertTrue(material.getJSONObject("pbrMetallicRoughness").has("baseColorTexture"))
        assertTrue(material.getJSONObject("extensions").has("KHR_materials_unlit"))
        assertTrue(json.getJSONArray("extensionsUsed").toString().contains("KHR_materials_unlit"))
        assertEquals("image/png", json.getJSONArray("images").getJSONObject(0).getString("mimeType"))
        assertEquals(2, json.getJSONArray("animations").length())
        val binOffset = 20 + jsonLen
        assertEquals(result.size, binOffset + 8 + buffer.getInt(binOffset))
    }
}
