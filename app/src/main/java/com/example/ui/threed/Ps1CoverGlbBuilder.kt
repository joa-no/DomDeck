package com.example.ui.threed

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Creates an isolated GLB with a locally cached game cover applied to the
 * existing "Artwork" mesh. The source GLB and the user's image are untouched.
 *
 * No Filament calls occur here: this work is safe on Dispatchers.IO.
 */
internal object Ps1CoverGlbBuilder {
    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942

    fun build(source: ByteArray, coverFile: File): ByteArray {
        require(coverFile.isFile && coverFile.length() > 0L) { "Capa indisponível" }
        val input = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN)
        require(source.size >= 28 && input.getInt(0) == GLB_MAGIC && input.getInt(4) == 2) {
            "Arquivo GLB inválido"
        }

        val jsonSize = input.getInt(12)
        require(input.getInt(16) == JSON_CHUNK && jsonSize > 0 && 20L + jsonSize + 8 <= source.size) {
            "Cabeçalho JSON inválido"
        }
        val binHeader = 20 + jsonSize
        val binSize = input.getInt(binHeader)
        require(input.getInt(binHeader + 4) == BIN_CHUNK &&
            binSize >= 0 && binHeader.toLong() + 8L + binSize <= source.size) {
            "Dados binários do GLB inválidos"
        }

        val json = JSONObject(String(source, 20, jsonSize, Charsets.UTF_8).trim())
        val oldBin = source.copyOfRange(binHeader + 8, binHeader + 8 + binSize)

        val cover = BitmapFactory.decodeFile(coverFile.absolutePath)
            ?: error("Não foi possível ler a capa")
        val png = try {
            val maxSide = maxOf(cover.width, cover.height)
            val texture = if (maxSide > 1024) {
                val factor = 1024f / maxSide
                Bitmap.createScaledBitmap(
                    cover,
                    (cover.width * factor).toInt().coerceAtLeast(1),
                    (cover.height * factor).toInt().coerceAtLeast(1),
                    true
                )
            } else cover
            try {
                ByteArrayOutputStream().use { stream ->
                    check(texture.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    stream.toByteArray()
                }
            } finally {
                if (texture !== cover) texture.recycle()
            }
        } finally {
            cover.recycle()
        }

        val nodes = json.getJSONArray("nodes")
        val artworkNode = (0 until nodes.length())
            .map { nodes.getJSONObject(it) }
            .firstOrNull { it.optString("name") == "Artwork" }
            ?: error("Malha Artwork não encontrada")
        val primitive = json.getJSONArray("meshes")
            .getJSONObject(artworkNode.getInt("mesh"))
            .getJSONArray("primitives")
            .getJSONObject(0)

        val binOut = ByteArrayOutputStream()
        binOut.write(oldBin)
        pad(binOut, 0)
        val uvOffset = binOut.size()

        // The cube generator has 24 vertices, 4 for each face.
        // Face +Z is vertices 16..19: front-facing cover art.
        val uv = FloatArray(48)
        val faceUvs = floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
        for (i in 0 until 8) uv[16 * 2 + i] = faceUvs[i]
        val uvBuffer = ByteBuffer.allocate(uv.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        uv.forEach { uvBuffer.putFloat(it) }
        binOut.write(uvBuffer.array())

        pad(binOut, 0)
        val imageOffset = binOut.size()
        binOut.write(png)
        pad(binOut, 0)

        val bufferViews = json.getJSONArray("bufferViews")
        val uvViewIndex = bufferViews.length()
        bufferViews.put(JSONObject().put("buffer", 0).put("byteOffset", uvOffset)
            .put("byteLength", uvBuffer.capacity()).put("target", 34962))
        val imageViewIndex = bufferViews.length()
        bufferViews.put(JSONObject().put("buffer", 0).put("byteOffset", imageOffset)
            .put("byteLength", png.size))

        val accessors = json.getJSONArray("accessors")
        val uvAccessorIndex = accessors.length()
        accessors.put(JSONObject().put("bufferView", uvViewIndex).put("componentType", 5126)
            .put("count", 24).put("type", "VEC2"))
        primitive.getJSONObject("attributes").put("TEXCOORD_0", uvAccessorIndex)

        val images = json.optJSONArray("images") ?: JSONArray().also { json.put("images", it) }
        val imageIndex = images.length()
        images.put(JSONObject().put("bufferView", imageViewIndex).put("mimeType", "image/png"))
        val samplers = json.optJSONArray("samplers") ?: JSONArray().also { json.put("samplers", it) }
        val samplerIndex = samplers.length()
        samplers.put(JSONObject().put("magFilter", 9729).put("minFilter", 9729))
        val textures = json.optJSONArray("textures") ?: JSONArray().also { json.put("textures", it) }
        val textureIndex = textures.length()
        textures.put(JSONObject().put("source", imageIndex).put("sampler", samplerIndex))

        val pbr = json.getJSONArray("materials")
            .getJSONObject(primitive.getInt("material"))
            .getJSONObject("pbrMetallicRoughness")
        pbr.put("baseColorFactor", JSONArray(listOf(1, 1, 1, 1)))
        pbr.put("baseColorTexture", JSONObject().put("index", textureIndex))
        json.getJSONArray("buffers").getJSONObject(0).put("byteLength", binOut.size())

        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
        val paddedJson = (jsonBytes.size + 3) and -4
        val binary = binOut.toByteArray()
        val finalSize = 12 + 8 + paddedJson + 8 + binary.size
        val result = ByteBuffer.allocate(finalSize).order(ByteOrder.LITTLE_ENDIAN)
        result.putInt(GLB_MAGIC).putInt(2).putInt(finalSize)
        result.putInt(paddedJson).putInt(JSON_CHUNK)
        result.put(jsonBytes)
        repeat(paddedJson - jsonBytes.size) { result.put(0x20.toByte()) }
        result.putInt(binary.size).putInt(BIN_CHUNK).put(binary)
        return result.array()
    }

    private fun pad(out: ByteArrayOutputStream, value: Int) {
        while (out.size() % 4 != 0) out.write(value)
    }
}
