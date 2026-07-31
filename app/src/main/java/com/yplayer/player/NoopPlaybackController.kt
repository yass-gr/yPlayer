package com.yplayer.player

import com.yplayer.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Temporary no-op until the Media3-backed implementation lands in Task 6.
class NoopPlaybackController : PlaybackController {
    override val state: StateFlow<PlayerState> = MutableStateFlow(PlayerState()).asStateFlow()

    override fun playQueue(songs: List<Song>, startIndex: Int) {}

    override fun togglePlayPause() {}

    override fun next() {}

    override fun previous() {}

    override fun seekTo(positionMs: Long) {}

    override fun setShuffle(on: Boolean) {}

    override fun cycleRepeat() {}
}
