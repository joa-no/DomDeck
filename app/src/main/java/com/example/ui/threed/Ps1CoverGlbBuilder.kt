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
 * Adds a dedicated, camera-facing artwork quad to the 3D lid instead of reusing
 * the original cube material. The original assets stay untouched.
 *
 * The front quad sits slightly above the prototype's gold decoration. It is a
 * child of LidPivot, so it follows Open/Close without any animation changes.
 */
internal object Ps1CoverGlbBuilder {
    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942
    private const val ARRAY_BUFFER = 34962
    private const val ELEMENT_ARRAY_BUFFER = 34963

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

        val nodes = json.getJSONArray("nodes")
        val lidPivotIndex = (0 until nodes.length()).firstOrNull {
            nodes.getJSONObject(it).optString("name") == "LidPivot"
        } ?: error("Dobradiça LidPivot não encontrada")

        val png = readCover(coverFile)
        val binOut = ByteArrayOutputStream()
        binOut.write(oldBin)

        val views = json.getJSONArray("bufferViews")
        val accessors = json.getJSONArray("accessors")

        fun addBufferView(data: ByteArray, target: Int? = null): Int {
            while (binOut.size() % 4 != 0) binOut.write(0)
            val offset = binOut.size()
            binOut.write(data)
            val view = JSONObject()
                .put("buffer", 0)
                .put("byteOffset", offset)
                .put("byteLength", data.size)
            if (target != null) view.put("target", target)
            val index = views.length()
            views.put(view)
            return index
        }

        fun floatBytes(values: FloatArray): ByteArray {
            val buf = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            values.forEach(buf::putFloat)
            return buf.array()
        }

        fun addAccessor(view: Int, count: Int, type: String, component: Int): Int {
            val index = accessors.length()
            accessors.put(JSONObject()
                .put("bufferView", view)
                .put("componentType", component)
                .put("count", count)
                .put("type", type))
            return index
        }

        // A new front surface, not the pre-existing artwork cube. This ensures
        // the image is visible instead of being obscured by the placeholder bars.
        val positions = floatArrayOf(
            -0.62f, -0.60f, 0f,
             0.62f, -0.60f, 0f,
             0.62f,  0.60f, 0f,
            -0.62f,  0.60f, 0f
        )
        val normals = floatArrayOf(
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f
        )
        val uvs = floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
        val positionsAccessor = addAccessor(
            addBufferView(floatBytes(positions), ARRAY_BUFFER), 4, "VEC3", 5126
        )
        // Required for POSITION attributes by glTF 2.0.
        accessors.getJSONObject(positionsAccessor)
            .put("min", JSONArray(listOf(-0.62f, -0.60f, 0f)))
            .put("max", JSONArray(listOf(0.62f, 0.60f, 0f)))
        val normalAccessor = addAccessor(
            addBufferView(floatBytes(normals), ARRAY_BUFFER), 4, "VEC3", 5126
        )
        val uvAccessor = addAccessor(
            addBufferView(floatBytes(uvs), ARRAY_BUFFER), 4, "VEC2", 5126
        )

        val indices = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        listOf(0, 1, 2, 0, 2, 3).forEach { indices.putShort(it.toShort()) }
        val indexAccessor = addAccessor(
            addBufferView(indices.array(), ELEMENT_ARRAY_BUFFER), 6, "SCALAR", 5123
        )

        val imageView = addBufferView(png)
        while (binOut.size() % 4 != 0) binOut.write(0)

        val images = json.optJSONArray("images") ?: JSONArray().also { json.put("images", it) }
        val imageIndex = images.length()
        images.put(JSONObject().put("bufferView", imageView).put("mimeType", "image/png"))

        val textures = json.optJSONArray("textures") ?: JSONArray().also { json.put("textures", it) }
        val textureIndex = textures.length()
        textures.put(JSONObject().put("source", imageIndex))

        // Unlit cover art is deliberate: game artwork must remain readable
        // even with the minimalist lighting of the experimental 3D viewport.
        val materialIndex = json.getJSONArray("materials").length()
        json.getJSONArray("materials").put(JSONObject()
            .put("name", "LocalGameCoverFront")
            .put("doubleSided", true)
            .put("pbrMetallicRoughness", JSONObject()
                .put("baseColorFactor", JSONArray(listOf(1, 1, 1, 1)))
                .put("baseColorTexture", JSONObject().put("index", textureIndex))
                .put("metallicFactor", 0)
                .put("roughnessFactor", 1))
            .put("extensions", JSONObject().put("KHR_materials_unlit", JSONObject())))

        val extensionsUsed = json.optJSONArray("extensionsUsed")
            ?: JSONArray().also { json.put("extensionsUsed", it) }
        if ((0 until extensionsUsed.length()).none {
                extensionsUsed.getString(it) == "KHR_materials_unlit"
            }) {
            extensionsUsed.put("KHR_materials_unlit")
        }

        val primitive = JSONObject()
            .put("attributes", JSONObject()
                .put("POSITION", positionsAccessor)
                .put("NORMAL", normalAccessor)
                .put("TEXCOORD_0", uvAccessor))
            .put("indices", indexAccessor)
            .put("material", materialIndex)
        val meshes = json.getJSONArray("meshes")
        val meshIndex = meshes.length()
        meshes.put(JSONObject().put("name", "TexturedCoverQuad")
            .put("primitives", JSONArray().put(primitive)))

        val newNodeIndex = nodes.length()
        nodes.put(JSONObject()
            .put("name", "CoverArtFront")
            .put("mesh", meshIndex)
            // Lid pivot is at x=-0.70. Move the quad to the lid center x=0.70,
            // slightly ABOVE the old placeholder's decorative gold bars.
            .put("translation", JSONArray(listOf(0.70, 0, 0.065))))
        val lidPivot = nodes.getJSONObject(lidPivotIndex)
        val children = lidPivot.optJSONArray("children")
            ?: JSONArray().also { lidPivot.put("children", it) }
        children.put(newNodeIndex)

        json.getJSONArray("buffers").getJSONObject(0).put("byteLength", binOut.size())
        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
        val paddedJsonSize = (jsonBytes.size + 3) and -4
        val binary = binOut.toByteArray()
        val finalSize = 12 + 8 + paddedJsonSize + 8 + binary.size
        val result = ByteBuffer.allocate(finalSize).order(ByteOrder.LITTLE_ENDIAN)
        result.putInt(GLB_MAGIC).putInt(2).putInt(finalSize)
        result.putInt(paddedJsonSize).putInt(JSON_CHUNK)
        result.put(jsonBytes)
        repeat(paddedJsonSize - jsonBytes.size) { result.put(0x20.toByte()) }
        result.putInt(binary.size).putInt(BIN_CHUNK).put(binary)
        return result.array()
    }

    private fun readCover(file: File): ByteArray {
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("Não foi possível ler a capa")
        try {
            val maxSide = maxOf(original.width, original.height)
            val bitmap = if (maxSide > 1024) {
                val factor = 1024f / maxSide
                Bitmap.createScaledBitmap(original,
                    (original.width * factor).toInt().coerceAtLeast(1),
                    (original.height * factor).toInt().coerceAtLeast(1), true)
            } else original
            try {
                ByteArrayOutputStream().use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    return stream.toByteArray()
                }
            } finally {
                if (bitmap !== original) bitmap.recycle()
            }
        } finally {
            original.recycle()
        }
    }
}
