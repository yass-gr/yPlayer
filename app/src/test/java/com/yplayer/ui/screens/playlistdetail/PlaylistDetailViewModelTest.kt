package com.yplayer.ui.screens.playlistdetail

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
import com.yplayer.data.playlist.FakePlaylistRepository
import com.yplayer.player.FakePlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaylistDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val songs = listOf(
        Song(1, "A", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "B", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "C", "Artist", "Album", 10, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun songs_resolveMediaIdsInOrder() = runTest(dispatcher) {
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, FakePlaybackController(), playlistId = 1)
        advanceUntilIdle()

        assertEquals(listOf(3L, 1L), vm.songs.value.map { it.id })
    }

    @Test
    fun removeSong_updatesList() = runTest(dispatcher) {
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, FakePlaybackController(), playlistId = 1)
        advanceUntilIdle()

        vm.removeSong(0)
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.songs.value.map { it.id })
    }

    @Test
    fun playSong_usesPlaylistContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val playlistRepo = FakePlaylistRepository().also {
            it.create("Mix")
            it.addSong(1, 3)
            it.addSong(1, 1)
        }
        val library = LibraryRepository(FakeLibrarySource(songs))
        val vm = PlaylistDetailViewModel(playlistRepo, library, controller, playlistId = 1)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(listOf(3L, 1L), controller.lastQueue?.map { it.id })
        assertEquals(1, controller.lastStartIndex)
    }
}
