package com.yplayer.player

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.yplayer.data.model.Song

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(uri.toString())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs)
            .build()
    )
    .build()

fun MediaItem.toSong(): Song {
    val uri = Uri.parse(mediaId)
    val id = uri.lastPathSegment?.toLongOrNull() ?: 0L
    return Song(
        id = id,
        title = mediaMetadata.title?.toString() ?: "",
        artist = mediaMetadata.artist?.toString() ?: "",
        album = mediaMetadata.albumTitle?.toString() ?: "",
        albumId = 0L,
        durationMs = mediaMetadata.durationMs ?: 0L,
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
    )
}
