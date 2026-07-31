package com.yplayer.ui.screens.nowplaying

import com.yplayer.data.model.Song
import com.yplayer.player.FakePlaybackController
import com.yplayer.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowPlayingViewModelTest {

    @Test
    fun togglePlayPause_delegatesToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.togglePlayPause()

        assertEquals(1, controller.toggles)
    }

    @Test
    fun seekTo_delegatesToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.seekTo(60_000)

        assertEquals(60_000L, controller.lastSeekMs)
    }

    @Test
    fun nextAndPrevious_delegateToController() {
        val controller = FakePlaybackController()
        val vm = NowPlayingViewModel(controller)

        vm.next()
        vm.previous()

        assertEquals(1, controller.nexts)
        assertEquals(1, controller.previous)
    }

    @Test
    fun state_reflectsControllerState() {
        val song = Song(1, "T", "A", "Al", 10, 1000, android.net.Uri.parse("content://media/1"))
        val stateFlow = MutableStateFlow(
            PlayerState(currentSong = song, isPlaying = true, positionMs = 500, durationMs = 1000)
        ).asStateFlow()
        val controller = object : FakePlaybackController() {
            override val state = stateFlow
        }
        val vm = NowPlayingViewModel(controller)

        assertEquals(song, vm.state.value.currentSong)
        assertEquals(true, vm.state.value.isPlaying)
        assertEquals(500L, vm.state.value.positionMs)
    }
}
