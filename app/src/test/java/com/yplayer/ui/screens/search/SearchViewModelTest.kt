package com.yplayer.ui.screens.search

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
class SearchViewModelTest {

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
        Song(1, "Blinding Lights", "The Weeknd", "After Hours", 10, 1000, android.net.Uri.parse("content://media/1")),
        Song(2, "Starboy", "The Weeknd", "Starboy", 20, 1000, android.net.Uri.parse("content://media/2")),
        Song(3, "Lose Yourself", "Eminem", "8 Mile", 30, 1000, android.net.Uri.parse("content://media/3")),
    )

    @Test
    fun query_filtersResultsLive() = runTest(dispatcher) {
        val vm = SearchViewModel(LibraryRepository(FakeLibrarySource(songs)), FakePlaybackController())
        advanceUntilIdle()

        assertEquals(3, vm.results.value.size)

        vm.onQueryChange("weeknd")
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), vm.results.value.map { it.id })

        vm.onQueryChange("")
        advanceUntilIdle()

        assertEquals(3, vm.results.value.size)
    }

    @Test
    fun playSong_usesSearchContext() = runTest(dispatcher) {
        val controller = FakePlaybackController()
        val vm = SearchViewModel(LibraryRepository(FakeLibrarySource(songs)), controller)
        advanceUntilIdle()
        vm.onQueryChange("weeknd")
        advanceUntilIdle()

        vm.playSong(0)

        assertEquals(listOf(1L, 2L), controller.lastQueue?.map { it.id })
        assertEquals(0, controller.lastStartIndex)
    }
}
