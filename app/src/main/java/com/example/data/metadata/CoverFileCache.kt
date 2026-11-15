package com.example.data.metadata

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class CoverFileCache(context: Context) {

    private val appContext = context.applicationContext
    private val autoCoversDir = File(appContext.filesDir, "metadata_covers").apply { mkdirs() }
    private val customCoversDir = File(appContext.filesDir, "custom_covers").apply { mkdirs() }

    suspend fun saveAutomaticCover(gameUri: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            autoCoversDir.mkdirs()
            val ext = detectExtension(bytes)
            val fileName = "${sha256Hex(gameUri)}.$ext"
            val target = File(autoCoversDir, fileName)
            val tmp = File(autoCoversDir, "$fileName.tmp")
            tmp.writeBytes(bytes)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                target.writeBytes(bytes)
                tmp.delete()
            }
            target.absolutePath
        }.getOrNull()
    }

    suspend fun saveCustomCoverFromUri(gameUri: String, pickedUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            customCoversDir.mkdirs()
            val bytes = appContext.contentResolver.openInputStream(pickedUri)?.use { input ->
                val out = input.readBytes()
                if (out.isEmpty() || out.size > MAX_CUSTOM_COVER_BYTES) null else out
            } ?: return@withContext null

            val ext = detectExtension(bytes)
            val fileName = "${sha256Hex(gameUri)}_${System.currentTimeMillis()}.$ext"
            val target = File(customCoversDir, fileName)
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrNull()
    }

    fun deleteFileIfOwned(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val canonical = runCatching { file.canonicalPath }.getOrNull() ?: return
        val autoCanonical = runCatching { autoCoversDir.canonicalPath }.getOrNull()
        val customCanonical = runCatching { customCoversDir.canonicalPath }.getOrNull()
        if ((autoCanonical != null && canonical.startsWith(autoCanonical)) ||
            (customCanonical != null && canonical.startsWith(customCanonical))
        ) {
            runCatching { file.delete() }
        }
    }

    fun isReadableFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val file = File(path)
        return file.exists() && file.isFile && file.length() > 0L
    }

    private fun detectExtension(bytes: ByteArray): String {
        if (bytes.size >= 12) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return "jpg"
            if (bytes[0] == 'R'.code.toByte() && bytes[8] == 'W'.code.toByte()) return "webp"
        }
        return "png"
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_CUSTOM_COVER_BYTES = 10 * 1024 * 1024
    }
}
