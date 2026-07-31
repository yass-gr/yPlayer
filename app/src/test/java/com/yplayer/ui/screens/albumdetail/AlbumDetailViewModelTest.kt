package com.yplayer.ui.screens.albumdetail

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
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
class AlbumDetailViewModelTest {

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
        Song(3, "C", "Other", "Other Album", 20, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun songs_filterToAlbum() = runTest(dispatcher) {
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = AlbumDetailViewModel(repo, FakePlaybackController(), albumId = 10)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.songs.value.map { it.id })
    }

    @Test
    fun playSong_usesAlbumContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val repo = LibraryRepository(FakeLibrarySource(songs))
        val vm = AlbumDetailViewModel(repo, controller, albumId = 10)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(listOf(1L, 2L), controller.lastQueue?.map { it.id })
        assertEquals(1, controller.lastStartIndex)
    }
}
