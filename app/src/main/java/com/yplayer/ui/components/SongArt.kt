package com.yplayer.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Size
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import kotlinx.coroutines.CancellationException
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.ByteArrayOutputStream
import java.io.File

data class SongArt(val songUri: Uri?, val albumId: Long)

private const val ART_SIZE = 512
private val PLACEHOLDER_COLOR = Color.rgb(0x24, 0x24, 0x28)

private fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

class SongArtFetcher(
    private val context: PlatformContext,
    private val data: SongArt,
) : Fetcher {

    class Factory : Fetcher.Factory<SongArt> {
        override fun create(data: SongArt, options: Options, imageLoader: ImageLoader): Fetcher? =
            SongArtFetcher(options.context, data)
    }

    override suspend fun fetch(): FetchResult? {
        val cacheFile = artCacheFile(context, data)
        loadCached(cacheFile)?.let { return it }

        val bytes = try {
            val bitmap = loadArt(context, data) ?: placeholderBitmap()
            try {
                encodePng(bitmap)
            } finally {
                bitmap.recycle()
            }
        } catch (e: OutOfMemoryError) {
            return null
        }

        if (writeCached(cacheFile, bytes)) {
            return fileResult(cacheFile)
        }
        return bufferResult(bytes)
    }

    private fun loadCached(cacheFile: File): FetchResult? {
        if (!isValidPng(cacheFile)) return null
        return fileResult(cacheFile)
    }

    private fun fileResult(cacheFile: File): FetchResult =
        SourceFetchResult(
            source = ImageSource(
                file = cacheFile.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = data.diskCacheKey(),
            ),
            mimeType = "image/png",
            dataSource = DataSource.DISK,
        )

    private fun bufferResult(bytes: ByteArray): FetchResult =
        SourceFetchResult(
            source = ImageSource(
                source = Buffer().write(bytes),
                fileSystem = FileSystem.SYSTEM,
                metadata = null,
            ),
            mimeType = "image/png",
            dataSource = DataSource.DISK,
        )

    private fun writeCached(cacheFile: File, bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        return runCatching {
            val parent = cacheFile.parentFile ?: return false
            parent.mkdirs()
            val tmp = File(parent, "${cacheFile.name}.tmp")
            tmp.writeBytes(bytes)
            if (tmp.renameTo(cacheFile)) {
                true
            } else {
                tmp.delete()
                cacheFile.delete()
                false
            }
        }.getOrDefault(false)
    }

    private suspend fun loadArt(context: PlatformContext, data: SongArt): Bitmap? {
        val resolver = context.contentResolver
        val songBitmap = data.songUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                loadThumbnail { resolver.loadThumbnail(uri, Size(ART_SIZE, ART_SIZE), null) }
            } else {
                null
            }
        }
        if (songBitmap != null) return songBitmap
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadThumbnail { resolver.loadThumbnail(albumArtUri(data.albumId), Size(ART_SIZE, ART_SIZE), null) }
        } else {
            @Suppress("DEPRECATION")
            try {
                resolver.openInputStream(albumArtUri(data.albumId))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    private inline fun loadThumbnail(block: () -> Bitmap): Bitmap? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun placeholderBitmap(): Bitmap =
        Bitmap.createBitmap(ART_SIZE, ART_SIZE, Bitmap.Config.ARGB_8888).apply {
            eraseColor(PLACEHOLDER_COLOR)
        }

    private fun encodePng(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
}

class SongArtKeyer : Keyer<SongArt> {
    override fun key(data: SongArt, options: Options): String? =
        data.diskCacheKey()
}

private fun SongArt.diskCacheKey(): String =
    "${songUri}|$albumId"

private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

private fun isValidPng(file: File): Boolean {
    if (!file.exists() || file.length() < PNG_MAGIC.size) return false
    return runCatching {
        val magic = ByteArray(PNG_MAGIC.size)
        file.inputStream().use { it.read(magic) }
        magic.contentEquals(PNG_MAGIC)
    }.getOrDefault(false)
}

private fun artCacheFile(context: PlatformContext, data: SongArt): File {
    val dir = File(context.cacheDir, "song_art")
    val name = "${sha256Hex(data.diskCacheKey())}.png"
    return File(dir, name)
}

private fun sha256Hex(value: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
