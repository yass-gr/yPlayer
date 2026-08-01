package com.yplayer.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import okio.Buffer
import okio.FileSystem
import java.io.ByteArrayOutputStream

data class SongArt(val songUri: Uri?, val albumId: Long)

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
        val bitmap = loadArt(context, data) ?: return null
        val png = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        val source = ImageSource(
            source = Buffer().write(png),
            fileSystem = FileSystem.SYSTEM,
            metadata = null,
        )
        return SourceFetchResult(source = source, mimeType = "image/png", dataSource = DataSource.DISK)
    }

    private suspend fun loadArt(context: PlatformContext, data: SongArt): Bitmap? {
        val resolver = context.contentResolver
        val songBitmap = data.songUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { resolver.loadThumbnail(uri, Size(512, 512), null) }.getOrNull()
            } else {
                null
            }
        }
        if (songBitmap != null) return songBitmap
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.loadThumbnail(albumArtUri(data.albumId), Size(512, 512), null)
            } else {
                @Suppress("DEPRECATION")
                resolver.openInputStream(albumArtUri(data.albumId))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull()
    }
}

class SongArtKeyer : Keyer<SongArt> {
    override fun key(data: SongArt, options: Options): String =
        "${data.songUri}|${data.albumId}"
}
