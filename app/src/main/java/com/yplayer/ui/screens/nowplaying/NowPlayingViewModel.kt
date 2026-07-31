package com.yplayer.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import com.yplayer.player.PlaybackController
import com.yplayer.player.PlayerState
import kotlinx.coroutines.flow.StateFlow

class NowPlayingViewModel(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val state: StateFlow<PlayerState> = playbackController.state

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun next() = playbackController.next()

    fun previous() = playbackController.previous()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun setShuffle(on: Boolean) = playbackController.setShuffle(on)

    fun cycleRepeat() = playbackController.cycleRepeat()
}
