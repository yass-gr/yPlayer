package com.yplayer.data.library

import com.yplayer.data.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRepositoryTest {

    private val songs = listOf(
        Song(1, "Blinding Lights", "The Weeknd", "After Hours", 100, 200_000, uriOf(1)),
        Song(2, "Starboy", "The Weeknd", "Starboy", 200, 220_000, uriOf(2)),
        Song(3, "Save Your Tears", "The Weeknd", "After Hours", 100, 210_000, uriOf(3)),
        Song(4, "Lose Yourself", "Eminem", "8 Mile", 300, 320_000, uriOf(4)),
    )

    @Test
    fun refresh_populatesSongs() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))

        assertEquals(emptyList<Song>(), repo.songs.value)

        repo.refresh()

        assertEquals(songs, repo.songs.value)
    }

    @Test
    fun albums_groupSongsByAlbumId() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        val albums = repo.albums.value
        assertEquals(listOf("After Hours", "Starboy", "8 Mile"), albums.map { it.title })
        assertEquals(2, albums.first { it.title == "After Hours" }.songs.size)
    }

    @Test
    fun artists_groupSongsByArtist() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        val artists = repo.artists.value
        assertEquals(listOf("Eminem", "The Weeknd"), artists.map { it.name })
        assertEquals(3, artists.first { it.name == "The Weeknd" }.songs.size)
    }

    @Test
    fun songsOfAlbum_filtersByAlbumId() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        assertEquals(listOf(1L, 3L), repo.songsOfAlbum(100).map { it.id })
    }

    @Test
    fun search_matchesTitleArtistAndAlbum() = runTest {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        repo.refresh()

        assertEquals(listOf(3L), repo.search("tears").map { it.id })
        assertEquals(listOf(1L, 2L, 3L), repo.search("the weeknd").map { it.id })
        assertEquals(listOf(1L, 3L), repo.search("after hours").map { it.id })
        assertEquals(emptyList<Song>(), repo.search("zzz"))
    }

    private fun uriOf(id: Long) = android.net.Uri.parse("content://media/external/audio/media/$id")
}
