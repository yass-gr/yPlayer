package com.yplayer.ui.screens.albums

import com.yplayer.data.library.FakeLibrarySource
import com.yplayer.data.library.LibraryRepository
import com.yplayer.data.model.Song
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
class AlbumsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun albums_areLoadedOnInit() = runTest(dispatcher) {
        val songs = listOf(
            Song(1, "A", "Artist", "Album One", 10, 1000, android.net.Uri.parse("content://media/1")),
            Song(2, "B", "Artist", "Album Two", 20, 1000, android.net.Uri.parse("content://media/2")),
        )
        val vm = AlbumsViewModel(LibraryRepository(FakeLibrarySource(songs)))
        advanceUntilIdle()

        assertEquals(listOf("Album One", "Album Two"), vm.albums.value.map { it.title })
    }
}
