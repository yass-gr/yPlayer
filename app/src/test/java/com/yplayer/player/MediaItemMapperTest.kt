package com.yplayer.player

import androidx.media3.common.MediaMetadata
import com.yplayer.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemMapperTest {

    @Test
    fun song_toMediaItem_setsMediaIdAndMetadata() {
        val song = Song(42, "Blinding Lights", "The Weeknd", "After Hours", 100, 200_000,
            android.net.Uri.parse("content://media/external/audio/media/42"))

        val item = song.toMediaItem()

        assertEquals("content://media/external/audio/media/42", item.mediaId)
        assertEquals("Blinding Lights", item.mediaMetadata.title)
        assertEquals("The Weeknd", item.mediaMetadata.artist)
        assertEquals("After Hours", item.mediaMetadata.albumTitle)
        assertEquals(200_000L, item.mediaMetadata.durationMs ?: -1L)
    }

    @Test
    fun mediaItem_toSong_reconstructsSong() {
        val item = androidx.media3.common.MediaItem.Builder()
            .setMediaId("content://media/external/audio/media/7")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("T")
                    .setArtist("A")
                    .setAlbumTitle("Al")
                    .setDurationMs(1234L)
                    .build()
            )
            .build()

        val song = item.toSong()

        assertEquals(7L, song.id)
        assertEquals("T", song.title)
        assertEquals("A", song.artist)
        assertEquals("Al", song.album)
        assertEquals(1234L, song.durationMs)
        assertEquals("content://media/external/audio/media/7", song.uri.toString())
    }
}
