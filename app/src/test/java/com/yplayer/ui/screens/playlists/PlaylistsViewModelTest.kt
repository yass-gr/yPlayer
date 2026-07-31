package com.yplayer.ui.screens.playlists

import com.yplayer.data.playlist.FakePlaylistRepository
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
class PlaylistsViewModelTest {

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
    fun createPlaylist_appearsInList() = runTest(dispatcher) {
        val repo = FakePlaylistRepository()
        val vm = PlaylistsViewModel(repo)
        advanceUntilIdle()

        vm.createPlaylist("Road Trip")
        advanceUntilIdle()

        assertEquals(listOf("Road Trip"), vm.playlists.value.map { it.name })
    }

    @Test
    fun deletePlaylist_removesFromList() = runTest(dispatcher) {
        val repo = FakePlaylistRepository()
        val vm = PlaylistsViewModel(repo)
        advanceUntilIdle()

        vm.createPlaylist("Road Trip")
        advanceUntilIdle()
        val id = vm.playlists.value.single().id
        vm.deletePlaylist(id)
        advanceUntilIdle()

        assertEquals(0, vm.playlists.value.size)
    }
}
