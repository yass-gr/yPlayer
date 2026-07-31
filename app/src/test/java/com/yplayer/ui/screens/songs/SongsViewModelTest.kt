package com.yplayer.ui.screens.songs

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
class SongsViewModelTest {

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
    )

    @Test
    fun songs_areLoadedOnInit() = runTest(dispatcher) {
        val vm = SongsViewModel(LibraryRepository(FakeLibrarySource(songs)), FakePlaybackController())
        advanceUntilIdle()

        assertEquals(songs, vm.songs.value)
    }

    @Test
    fun playSong_delegatesToController() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val vm = SongsViewModel(LibraryRepository(FakeLibrarySource(songs)), controller)
        advanceUntilIdle()

        vm.playSong(1)

        assertEquals(songs, controller.lastQueue)
        assertEquals(1, controller.lastStartIndex)
    }
}
